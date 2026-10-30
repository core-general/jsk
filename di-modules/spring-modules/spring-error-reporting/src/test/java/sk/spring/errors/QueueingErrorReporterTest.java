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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Spy;
import sk.services.async.IAsync;
import sk.services.bytes.BytesImpl;
import sk.services.http.IHttp;
import sk.services.http.model.CoreHttpResponse;
import sk.services.json.IJson;
import sk.services.json.JGsonImpl;
import sk.services.time.ITime;
import sk.services.time.UtcSettableTimeUtilImpl;
import sk.test.JskMockitoTest;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;

import java.io.IOException;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static sk.spring.errors.QueueingErrorReporter.*;

class QueueingErrorReporterTest extends JskMockitoTest {
    private static final String MONITOR_URL = "http://monitor.test:8193";

    @Mock ErrorReportingSettings settings;
    @Mock(answer = Answers.CALLS_REAL_METHODS) IHttp http;
    @Mock IAsync async;
    @Mock ITime time;

    @Spy private IJson json = new JGsonImpl(O.empty(), new UtcSettableTimeUtilImpl(), new BytesImpl(), false).init();
    private final Deque<OneOf<CoreHttpResponse, Exception>> outcomes = new ArrayDeque<>();
    private final List<IHttp.HttpBodyBuilder> posts = new ArrayList<>();
    private QueueingErrorReporter reporter;

    @BeforeEach
    void reporter() {
        when(settings.url()).thenReturn(MONITOR_URL);
        when(settings.service()).thenReturn("Test service");
        when(settings.environment()).thenReturn("prd");
        when(settings.productPackages()).thenReturn(List.of("sk.spring.errors."));
        when(settings.redact(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        F1<IHttp.HttpBodyBuilder, OneOf<CoreHttpResponse, Exception>> monitor = request -> {
            posts.add(request);
            return outcomes.removeFirst();
        };
        doReturn(monitor).when(http).<IHttp.HttpBodyBuilder>howToPost();
        reporter = new QueueingErrorReporter(settings, new ErrorEventFactory(settings), http, json, async, time);
    }

    @Test
    void fullQueueDropsWithoutBlockingAndOnlyTheSentDropCountIsCleared() {
        int overflow = 3;
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> reportDistinct(QUEUE_CAPACITY + overflow));
        respond(failure(), status(202), status(202));

        reporter.sendNext();
        int laterOverflow = 2;
        reportDistinct(QUEUE_CAPACITY + laterOverflow);
        reporter.sendNext();
        reporter.sendNext();

        Map<String, Object> first = batch(0);
        assertEquals(MONITOR_URL + "/events", posts.getFirst().url());
        assertEquals("application/json", posts.getFirst().headers().get("Content-Type"));
        assertEquals(Set.of("batch_id", "service", "environment", "dropped", "events"), first.keySet());
        assertEquals(Set.of("id", "service", "environment", "time", "kind", "type", "message", "frames", "logger", "site",
                "template", "trace"), events(first).getFirst().keySet());
        assertEquals(QUEUE_CAPACITY, events(first).size());
        assertTrue(events(first).size() <= MAX_BATCH_EVENTS);
        assertEquals(overflow, dropped(first));
        assertEquals(ids(first), ids(batch(1)));
        assertEquals(laterOverflow, dropped(batch(2)));
        assertEquals(QUEUE_CAPACITY, events(batch(2)).size());
    }

    @Test
    void failedPostsRetryTheSameBatchWithGrowingCappedDelaysUntilAccepted() {
        reportDistinct(2);
        int failures = 8;
        IntStream.range(0, failures).forEach(attempt -> respond(switch (attempt % 4) {
            case 0 -> failure();
            case 1 -> status(408);
            case 2 -> status(429);
            default -> status(503);
        }));
        respond(status(202), status(202));

        List<Duration> delays = new ArrayList<>();
        for (int attempt = 0; attempt < failures; attempt++) {
            delays.add(reporter.sendNext());
        }
        reportDistinct(1);
        reporter.sendNext();
        reporter.sendNext();

        assertEquals(FIRST_RETRY_DELAY, delays.getFirst());
        assertEquals(delays.getFirst().multipliedBy(2), delays.get(1));
        assertEquals(MAX_RETRY_DELAY, delays.getLast());
        for (int attempt = 1; attempt < failures; attempt++) {
            assertTrue(delays.get(attempt).compareTo(delays.get(attempt - 1)) >= 0);
        }
        for (int attempt = 1; attempt <= failures; attempt++) {
            assertEquals(ids(batch(0)), ids(batch(attempt)));
            assertEquals(batch(0).get("batch_id"), batch(attempt).get("batch_id"));
            assertArrayEquals(encodedBody(0), encodedBody(attempt));
        }
        assertEquals(1, events(batch(failures + 1)).size());
        assertTrue(Collections.disjoint(ids(batch(0)), ids(batch(failures + 1))));
        assertNotEquals(batch(0).get("batch_id"), batch(failures + 1).get("batch_id"));
        verify(json, times(3)).to(isA(ErrorEvent.class));
    }

    @Test
    void rejectedBatchIsDroppedAndNeverRetried() {
        reportDistinct(1);
        respond(status(400), status(202));

        reporter.sendNext();
        reportDistinct(1);
        reporter.sendNext();

        assertEquals(2, posts.size());
        assertEquals(1, events(batch(1)).size());
        assertTrue(Collections.disjoint(ids(batch(0)), ids(batch(1))));
        assertEquals(1, dropped(batch(1)));
    }

    @Test
    void reusingAThrowableDoesNotSuppressIndependentOccurrences() {
        RuntimeException repeated = new RuntimeException("repeated");
        reporter.report("first", repeated);
        reporter.report("second", repeated);
        reporter.report(null, repeated);
        reporter.report("first", new RuntimeException("other"));
        respond(status(202));

        reporter.sendNext();

        assertEquals(4, events(batch(0)).size());
        assertEquals(4, ids(batch(0)).stream().distinct().count());
    }

    @Test
    void errorsRaisedWhileSendingAreNotReportedBack() {
        reportDistinct(1);
        F1<IHttp.HttpBodyBuilder, OneOf<CoreHttpResponse, Exception>> failingMonitor = request -> {
            posts.add(request);
            reporter.report("sending", new IllegalStateException("send failed"));
            reporter.reportLogged("sender.logger", null, "send failed", "send failed");
            return status(202);
        };
        doReturn(failingMonitor).when(http).<IHttp.HttpBodyBuilder>howToPost();

        reporter.sendNext();
        reporter.sendNext();

        assertEquals(1, posts.size());
    }

    @Test
    void utf8BytePackingIncludesEnvelopeAndKeepsOverflowForTheNextBatch() {
        String large = "\ud83d\ude80\"\\".repeat(ErrorEventFactory.MAX_MESSAGE_CHARS);
        for (int index = 0; index < MAX_BATCH_EVENTS; index++) {
            reporter.reportLogged("logger", null, large, large);
        }
        IntStream.range(0, MAX_BATCH_EVENTS).forEach(index -> respond(status(202)));
        for (int index = 0; index < MAX_BATCH_EVENTS; index++) reporter.sendNext();

        assertTrue(posts.size() > 1);
        List<Object> sent = new ArrayList<>();
        for (int index = 0; index < posts.size(); index++) {
            assertTrue(encodedBody(index).length <= MAX_BATCH_BYTES);
            assertTrue(events(batch(index)).size() <= MAX_BATCH_EVENTS);
            assertEquals(0, dropped(batch(index)));
            sent.addAll(ids(batch(index)));
        }
        assertEquals(MAX_BATCH_EVENTS, sent.size());
        assertEquals(sent.size(), sent.stream().distinct().count());
    }

    @Test
    void snapshotAndSerializationFailuresAreCountedAlongsideSurvivingEvents() {
        when(settings.redact(anyString())).thenAnswer(invocation -> {
            String value = invocation.getArgument(0);
            if (value.equals("unbuildable")) throw new IllegalArgumentException("redaction failed");
            return value;
        });
        reporter.report("operation", new IllegalStateException("unbuildable"));
        reporter.report("operation", new IllegalStateException("serializable later"));
        reporter.report("operation", new IllegalStateException("survivor"));
        doThrow(new IllegalArgumentException("serialization failed")).doCallRealMethod().when(json).to(isA(ErrorEvent.class));
        respond(status(202));

        reporter.sendNext();

        assertEquals(1, events(batch(0)).size());
        assertEquals(2, dropped(batch(0)));
        reporter.sendNext();
        assertEquals(1, posts.size());
    }

    @Test
    void permanentRejectionAddsLostEventsAndRetainsEarlierDropsUntilAcknowledged() {
        int overflow = 7;
        reportDistinct(QUEUE_CAPACITY + overflow);
        respond(status(413), failure(), status(202));

        reporter.sendNext();
        reporter.sendNext();
        reporter.sendNext();
        reporter.sendNext();

        assertEquals(3, posts.size());
        assertEquals(overflow, dropped(batch(0)));
        assertTrue(events(batch(1)).isEmpty());
        assertEquals(overflow + QUEUE_CAPACITY, dropped(batch(1)));
        assertNotEquals(batch(0).get("batch_id"), batch(1).get("batch_id"));
        assertArrayEquals(encodedBody(1), encodedBody(2));
    }

    private void reportDistinct(int count) {
        for (int index = 0; index < count; index++) {
            reporter.report("operation", new IllegalStateException("failure " + index));
        }
    }

    @SafeVarargs
    private void respond(OneOf<CoreHttpResponse, Exception>... responses) {
        outcomes.addAll(List.of(responses));
    }

    private OneOf<CoreHttpResponse, Exception> status(int code) {
        CoreHttpResponse response = mock(CoreHttpResponse.class);
        when(response.code()).thenReturn(code);
        return OneOf.left(response);
    }

    private OneOf<CoreHttpResponse, Exception> failure() {
        return OneOf.right(new IOException("connection refused"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> batch(int index) {
        return json.from(new String(encodedBody(index), StandardCharsets.UTF_8), Map.class);
    }

    private byte[] encodedBody(int index) {
        return posts.get(index).body().collect(value -> value.getBytes(StandardCharsets.UTF_8), value -> value);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> events(Map<String, Object> batch) {
        return (List<Map<String, Object>>) batch.get("events");
    }

    private long dropped(Map<String, Object> batch) {
        return ((Number) batch.get("dropped")).longValue();
    }

    private List<Object> ids(Map<String, Object> batch) {
        return events(batch).stream().map(event -> event.get("id")).toList();
    }
}
