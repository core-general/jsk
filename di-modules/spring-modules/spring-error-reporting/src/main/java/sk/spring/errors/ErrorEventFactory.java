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

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

public class ErrorEventFactory {
    public static final int MAX_CAUSE_DEPTH = 8;
    public static final int MAX_FRAMES = 5;
    public static final int MAX_ID_CHARS = 64;
    public static final int MAX_ENVIRONMENT_CHARS = 64;
    public static final int MAX_SERVICE_CHARS = 100;
    public static final int MAX_KIND_CHARS = 32;
    public static final int MAX_TYPE_CHARS = 200;
    public static final int MAX_LOCATION_CHARS = 512;
    public static final int MAX_MESSAGE_CHARS = 4096;
    public static final int MAX_TRACE_CHARS = 16384;
    private static final DateTimeFormatter RFC_3339_UTC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private final ErrorReportingSettings settings;

    public ErrorEventFactory(ErrorReportingSettings settings) {
        this.settings = settings;
    }

    public ErrorEventBatch batch(long dropped, List<ErrorEvent> events) {
        return new ErrorEventBatch(UUID.randomUUID().toString(), limit(settings.service(), MAX_SERVICE_CHARS),
                limit(settings.environment(), MAX_ENVIRONMENT_CHARS), dropped, List.copyOf(events));
    }

    public ErrorEvent create(ErrorOccurrence occurrence) {
        return switch (occurrence) {
            case ErrorOccurrence.Thrown thrown -> exception(thrown);
            case ErrorOccurrence.Logged logged -> log(logged);
            case ErrorOccurrence.Problem problem -> problem(problem);
        };
    }

    private ErrorEvent exception(ErrorOccurrence.Thrown thrown) {
        List<Throwable> chain = causeChain(thrown.error());
        Throwable root = chain.getLast();
        Predicate<StackTraceElement> product = productFrames();
        return event(thrown, ErrorEvent.EXCEPTION, typeName(root), firstLine(redact(root.getMessage())),
                frames(chain, product), "", "", "", trace(thrown.operation(), chain, product));
    }

    private ErrorEvent log(ErrorOccurrence.Logged logged) {
        return event(logged, ErrorEvent.LOG, "", limit(redact(logged.message()), MAX_MESSAGE_CHARS), List.of(),
                text(logged.logger()), logged.site() == null ? "" : frame(logged.site()),
                limit(redact(logged.template()), MAX_MESSAGE_CHARS), "");
    }

    private ErrorEvent problem(ErrorOccurrence.Problem problem) {
        String detail = limit(redact(problem.detail()), MAX_MESSAGE_CHARS);
        return event(problem, ErrorEvent.PROBLEM, limit(text(problem.code()), MAX_TYPE_CHARS),
                detail.isBlank() ? "HTTP " + problem.httpCode() : detail,
                problem.apiMethod() == null ? List.of() : List.of(problem.apiMethod()), "", "", "", "");
    }

    private ErrorEvent event(ErrorOccurrence occurrence, String kind, String type, String message, List<String> frames,
            String logger, String site, String template, String trace) {
        return new ErrorEvent(limit(UUID.randomUUID().toString(), MAX_ID_CHARS), limit(settings.service(), MAX_SERVICE_CHARS),
                limit(settings.environment(), MAX_ENVIRONMENT_CHARS),
                RFC_3339_UTC.format(Instant.ofEpochMilli(occurrence.epochMillis())), limit(kind, MAX_KIND_CHARS),
                limit(type, MAX_TYPE_CHARS), limit(message, MAX_MESSAGE_CHARS),
                frames.stream().limit(MAX_FRAMES).map(value -> limit(value, MAX_LOCATION_CHARS)).toList(),
                limit(logger, MAX_LOCATION_CHARS), limit(site, MAX_LOCATION_CHARS),
                limit(template, MAX_MESSAGE_CHARS), limit(trace, MAX_TRACE_CHARS));
    }

    private List<Throwable> causeChain(Throwable error) {
        List<Throwable> chain = new ArrayList<>();
        chain.add(error);
        Throwable current = error;
        for (int depth = 0; depth < MAX_CAUSE_DEPTH && current.getCause() != null && current.getCause() != current; depth++) {
            current = current.getCause();
            chain.add(current);
        }
        return chain;
    }

    private List<String> frames(List<Throwable> chain, Predicate<StackTraceElement> product) {
        for (Throwable error : chain.reversed()) {
            List<String> productFrames = Arrays.stream(error.getStackTrace()).filter(product).limit(MAX_FRAMES)
                    .map(this::frame).toList();
            if (!productFrames.isEmpty()) {
                return productFrames;
            }
        }
        return Arrays.stream(chain.getLast().getStackTrace()).limit(MAX_FRAMES).map(this::frame).toList();
    }

    private String trace(String operation, List<Throwable> chain, Predicate<StackTraceElement> product) {
        StringBuilder trace = new StringBuilder();
        if (operation != null) {
            trace.append("operation: ").append(firstLine(redact(operation))).append('\n');
        }
        for (Throwable error : chain) {
            String message = firstLine(redact(error.getMessage()));
            trace.append(typeName(error)).append(message.isEmpty() ? "" : ": " + message).append('\n');
            for (StackTraceElement element : error.getStackTrace()) {
                if (product.test(element)) {
                    trace.append("  at ").append(frame(element)).append('(').append(location(element)).append(")\n");
                    if (trace.codePointCount(0, trace.length()) >= MAX_TRACE_CHARS) {
                        return limit(trace.toString(), MAX_TRACE_CHARS);
                    }
                }
            }
        }
        return limit(trace.toString().stripTrailing(), MAX_TRACE_CHARS);
    }

    private Predicate<StackTraceElement> productFrames() {
        List<String> packages = List.copyOf(settings.productPackages());
        return element -> packages.stream().anyMatch(element.getClassName()::startsWith);
    }

    private String frame(StackTraceElement element) {
        return limit(element.getClassName() + "." + element.getMethodName(), MAX_LOCATION_CHARS);
    }

    private String location(StackTraceElement element) {
        if (element.isNativeMethod()) {
            return "Native Method";
        }
        if (element.getFileName() == null) {
            return "Unknown Source";
        }
        return limit(element.getLineNumber() >= 0 ? element.getFileName() + ":" + element.getLineNumber()
                : element.getFileName(), MAX_LOCATION_CHARS);
    }

    private String typeName(Throwable error) {
        String simpleName = error.getClass().getSimpleName();
        return limit(simpleName.isEmpty() ? error.getClass().getName() : simpleName, MAX_TYPE_CHARS);
    }

    private String redact(String value) {
        return value == null ? "" : Objects.requireNonNullElse(settings.redact(value), "");
    }

    private String firstLine(String value) {
        return value.lines().map(String::strip).filter(line -> !line.isEmpty()).findFirst()
                .map(line -> limit(line, MAX_MESSAGE_CHARS)).orElse("");
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private String limit(String value, int maxChars) {
        if (value == null) return "";
        StringBuilder safe = new StringBuilder(Math.min(value.length(), maxChars * 2));
        for (int index = 0, count = 0; index < value.length() && count < maxChars; count++) {
            char current = value.charAt(index++);
            if (Character.isHighSurrogate(current)) {
                if (index < value.length() && Character.isLowSurrogate(value.charAt(index))) {
                    safe.append(current).append(value.charAt(index++));
                } else {
                    safe.append('\ufffd');
                }
            } else {
                safe.append(Character.isLowSurrogate(current) ? '\ufffd' : current);
            }
        }
        return safe.toString();
    }
}
