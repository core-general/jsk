package sk.spring.errors;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2026 Core General
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import sk.services.async.IAsync;
import sk.services.http.IHttp;
import sk.services.json.IJson;
import sk.services.time.ITime;

import java.time.Duration;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class QueueingErrorReporter implements ILoggedErrorReporter {
    public static final int QUEUE_CAPACITY = 100;
    public static final int MAX_BATCH_EVENTS = 100;
    public static final int MAX_BATCH_BYTES = 1024 * 1024;
    public static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(4);
    public static final Duration TOTAL_TIMEOUT = Duration.ofSeconds(10);
    public static final Duration FIRST_RETRY_DELAY = Duration.ofSeconds(1);
    public static final Duration MAX_RETRY_DELAY = Duration.ofSeconds(30);
    public static final Duration IDLE_DELAY = Duration.ofSeconds(1);

    private final ErrorReportingSettings settings;
    private final ErrorEventFactory events;
    private final IHttp http;
    private final IJson json;
    private final IAsync async;
    private final ITime time;
    private final BlockingQueue<ErrorOccurrence> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private final AtomicLong dropped = new AtomicLong();
    private volatile Thread sendingThread;
    private volatile ScheduledExecutorService sender;
    private PreparedBatch unsent;
    private byte[] pendingEvent;
    private Duration retryDelay = Duration.ZERO;

    public QueueingErrorReporter(ErrorReportingSettings settings, ErrorEventFactory events, IHttp http, IJson json,
            IAsync async, ITime time) {
        this.settings = settings;
        this.events = events;
        this.http = http;
        this.json = json;
        this.async = async;
        this.time = time;
    }

    @PostConstruct
    public void start() {
        sender = async.newDedicatedScheduledExecutor("ErrorReporting-" + UUID.randomUUID()).getUnderlying();
        scheduleSending(Duration.ZERO);
    }

    @PreDestroy
    public void stop() {
        ScheduledExecutorService current = sender;
        if (current != null) {
            current.shutdownNow();
        }
    }

    @Override
    public void report(String operation, Throwable error) {
        if (error != null && !isSendingThread()) {
            try {
                offer(new ErrorOccurrence.Thrown(operation, error, time.now()));
            } catch (RuntimeException failure) {
                countDropped(1);
            }
        }
    }

    @Override
    public void reportProblem(int httpCode, String code, String detail, String apiMethod) {
        if (!isSendingThread()) {
            try {
                offer(new ErrorOccurrence.Problem(httpCode, code, detail, apiMethod, time.now()));
            } catch (RuntimeException failure) {
                countDropped(1);
            }
        }
    }

    @Override
    public void reportLogged(String logger, StackTraceElement site, String template, String message) {
        if (!isSendingThread()) {
            try {
                offer(new ErrorOccurrence.Logged(logger, site, template, message, time.now()));
            } catch (RuntimeException failure) {
                countDropped(1);
            }
        }
    }

    Duration sendNext() {
        sendingThread = Thread.currentThread();
        try {
            if (unsent == null) {
                unsent = nextBatch();
            }
            if (unsent == null) {
                return IDLE_DELAY;
            }
            Delivery delivery = deliver(unsent);
            if (delivery == Delivery.RETRY) {
                retryDelay = retryDelay.isZero() ? FIRST_RETRY_DELAY : shorter(retryDelay.multipliedBy(2), MAX_RETRY_DELAY);
                return retryDelay;
            }
            if (delivery == Delivery.ACCEPTED) {
                dropped.addAndGet(-unsent.dropped());
            } else {
                countDropped(unsent.eventCount());
            }
            unsent = null;
            retryDelay = Duration.ZERO;
            return queue.isEmpty() && pendingEvent == null ? IDLE_DELAY : Duration.ZERO;
        } finally {
            sendingThread = null;
        }
    }

    private boolean isSendingThread() {
        return Thread.currentThread() == sendingThread;
    }

    private void offer(ErrorOccurrence occurrence) {
        if (!queue.offer(occurrence)) {
            countDropped(1);
        }
    }

    private void countDropped(long count) {
        dropped.getAndUpdate(current -> current > Long.MAX_VALUE - count ? Long.MAX_VALUE : current + count);
    }

    private void scheduleSending(Duration delay) {
        try {
            sender.schedule(this::sendAndReschedule, delay.toMillis(), TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException stopped) {
        }
    }

    private void sendAndReschedule() {
        Duration delay = IDLE_DELAY;
        try {
            delay = sendNext();
        } catch (Throwable unexpected) {
        }
        scheduleSending(delay);
    }

    private PreparedBatch nextBatch() {
        if (pendingEvent == null && queue.isEmpty() && dropped.get() == 0) return null;
        ErrorEventBatch metadata = events.batch(0, List.of());
        byte[] envelope = prefix(metadata);
        byte[] eventsPrefix = ",\"events\":[".getBytes(StandardCharsets.UTF_8);
        int envelopeLimit = envelope.length + Long.toString(Long.MAX_VALUE).length() + eventsPrefix.length + 2;
        int size = envelopeLimit;
        List<byte[]> batchEvents = new ArrayList<>(MAX_BATCH_EVENTS);
        for (int examined = 0; examined < MAX_BATCH_EVENTS; examined++) {
            byte[] encoded = pendingEvent;
            if (encoded == null) {
                ErrorOccurrence occurrence = queue.poll();
                if (occurrence == null) break;
                try {
                    encoded = json.to(events.create(occurrence)).getBytes(StandardCharsets.UTF_8);
                } catch (Throwable unbuildable) {
                    countDropped(1);
                    continue;
                }
            }
            if (encoded.length > MAX_BATCH_BYTES - envelopeLimit) {
                pendingEvent = null;
                countDropped(1);
                continue;
            }
            int separator = batchEvents.isEmpty() ? 0 : 1;
            if (encoded.length > MAX_BATCH_BYTES - size - separator) {
                pendingEvent = encoded;
                break;
            }
            pendingEvent = null;
            batchEvents.add(encoded);
            size += encoded.length + separator;
        }
        long includedDropped = dropped.get();
        if (batchEvents.isEmpty() && includedDropped == 0) return null;
        ByteArrayOutputStream body = new ByteArrayOutputStream(size);
        body.writeBytes(envelope);
        body.writeBytes(Long.toString(includedDropped).getBytes(StandardCharsets.UTF_8));
        body.writeBytes(eventsPrefix);
        for (int index = 0; index < batchEvents.size(); index++) {
            if (index != 0) body.write(',');
            body.writeBytes(batchEvents.get(index));
        }
        body.writeBytes(new byte[]{']', '}'});
        return new PreparedBatch(includedDropped, batchEvents.size(), body.toByteArray());
    }

    private byte[] prefix(ErrorEventBatch batch) {
        return ("{\"batch_id\":" + json.to(batch.batch_id()) + ",\"service\":" + json.to(batch.service())
                + ",\"environment\":" + json.to(batch.environment()) + ",\"dropped\":").getBytes(StandardCharsets.UTF_8);
    }

    private Delivery deliver(PreparedBatch batch) {
        try {
            IHttp.HttpBodyBuilder request = http.postBody(eventsUrl()).body(batch.body());
            request.headers().put("Content-Type", "application/json");
            request.connectTimeout(CONNECT_TIMEOUT);
            request.totalTimeout(TOTAL_TIMEOUT);
            request.tryCount(1);
            return request.goResponse().collect(response -> Delivery.of(response.code()), failure -> Delivery.RETRY);
        } catch (RuntimeException failure) {
            return Delivery.RETRY;
        }
    }

    private String eventsUrl() {
        String base = settings.url().strip();
        return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + "/events";
    }

    private static Duration shorter(Duration first, Duration second) {
        return first.compareTo(second) <= 0 ? first : second;
    }

    private enum Delivery {
        ACCEPTED, REJECTED, RETRY;

        static Delivery of(int httpCode) {
            if (httpCode >= 200 && httpCode < 300) {
                return ACCEPTED;
            }
            return httpCode == 408 || httpCode == 429 || httpCode >= 500 ? RETRY : REJECTED;
        }
    }

    private record PreparedBatch(long dropped, int eventCount, byte[] body) {}
}
