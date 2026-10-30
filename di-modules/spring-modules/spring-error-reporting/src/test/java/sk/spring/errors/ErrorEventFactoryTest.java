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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import sk.test.JskMockitoTest;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static sk.spring.errors.ErrorEventFactory.MAX_FRAMES;
import static sk.spring.errors.ErrorEventFactory.*;

class ErrorEventFactoryTest extends JskMockitoTest {
    private static final String PRODUCT = "zen.";
    private static final String LIBRARY = "org.library.";

    @Mock ErrorReportingSettings settings;
    @InjectMocks ErrorEventFactory factory;

    @BeforeEach
    void settings() {
        when(settings.productPackages()).thenReturn(List.of(PRODUCT));
        when(settings.redact(anyString())).thenAnswer(invocation -> invocation.<String>getArgument(0).replace("secret", "[r]"));
    }

    @Test
    void exceptionsTakeTheRootCauseAndTheProductFramesOfTheInnermostExceptionThatHasThem() {
        SelfCaused root = new SelfCaused("root secret\nsecond line");
        root.setStackTrace(stack(LIBRARY + "Driver.connect", LIBRARY + "Pool.borrow"));
        IllegalStateException middle = new IllegalStateException("middle", root);
        middle.setStackTrace(stack(LIBRARY + "Pool.borrow", PRODUCT + "Store.load", PRODUCT + "Service.run", LIBRARY + "Thread.run"));
        RuntimeException outer = new RuntimeException("outer", middle);
        outer.setStackTrace(stack(PRODUCT + "Worker.loop"));

        ErrorEvent event = factory.create(new ErrorOccurrence.Thrown("sch.dispatch", outer, 0));

        assertEquals(ErrorEvent.EXCEPTION, event.kind());
        assertEquals(SelfCaused.class.getSimpleName(), event.type());
        assertEquals("root [r]", event.message());
        assertEquals(List.of(PRODUCT + "Store.load", PRODUCT + "Service.run"), event.frames());
        List<String> trace = event.trace().lines().toList();
        assertEquals("operation: sch.dispatch", trace.getFirst());
        assertTrue(trace.indexOf("RuntimeException: outer") < trace.indexOf("IllegalStateException: middle"));
        assertTrue(trace.indexOf("IllegalStateException: middle") < trace.indexOf("SelfCaused: root [r]"));
        assertEquals(3, trace.stream().filter(line -> line.startsWith("  at " + PRODUCT)).count());
        assertTrue(trace.stream().noneMatch(line -> line.contains(LIBRARY) || line.contains("secret")));
    }

    @Test
    void withoutAnyProductFrameTheTopFramesOfTheRootCauseAreUsed() {
        IOException root = new IOException("refused");
        root.setStackTrace(stack(IntStream.range(0, MAX_FRAMES + 2).mapToObj(index -> LIBRARY + "Net.step" + index)
                .toArray(String[]::new)));
        RuntimeException outer = new RuntimeException("wrapped", root);
        outer.setStackTrace(stack(LIBRARY + "Server.handle"));

        ErrorEvent event = factory.create(new ErrorOccurrence.Thrown(null, outer, 0));

        assertEquals(Arrays.stream(root.getStackTrace()).limit(MAX_FRAMES)
                .map(element -> element.getClassName() + "." + element.getMethodName()).toList(), event.frames());
        assertFalse(event.trace().contains("operation:"));
    }

    @Test
    void logEventsCarryTheCallSiteWithoutLineAndTheRedactedFormattedMessage() {
        StackTraceElement site = new StackTraceElement(PRODUCT + "Caller", "handle", "Caller.java", 42);
        long loggedAt = 1_761_732_000_123L;

        ErrorEvent event = factory.create(new ErrorOccurrence.Logged("zen.Caller", site, "failed {}", "failed secret", loggedAt));

        assertEquals(ErrorEvent.LOG, event.kind());
        assertEquals(site.getClassName() + "." + site.getMethodName(), event.site());
        assertEquals("failed {}", event.template());
        assertEquals("failed [r]", event.message());
        assertEquals(Instant.ofEpochMilli(loggedAt), Instant.parse(event.time()));
        assertTrue(event.time().endsWith("Z"));
    }

    @Test
    void problemsAreIdentifiedByCodeAndApiMethod() {
        ErrorEvent event = factory.create(new ErrorOccurrence.Problem(503, "UPSTREAM_DOWN", null, "zen.Api.call", 0));

        assertEquals(ErrorEvent.PROBLEM, event.kind());
        assertEquals("UPSTREAM_DOWN", event.type());
        assertEquals(List.of("zen.Api.call"), event.frames());
        assertTrue(event.message().contains("503"));
    }

    @Test
    void preformattedTemplatesAreRedactedAlongWithMessages() {
        String content = "failed secret";
        ErrorEvent event = factory.create(new ErrorOccurrence.Logged("zen.Caller", null, content, content, 0));
        assertEquals("failed [r]", event.message());
        assertEquals(event.message(), event.template());
    }

    @Test
    void wireFieldsUseUnicodeScalarLimitsAndRepairUnpairedSurrogates() {
        String symbol = "\ud83d\ude80";
        String longText = symbol.repeat(MAX_MESSAGE_CHARS + 20);
        when(settings.service()).thenReturn(longText);
        when(settings.environment()).thenReturn(longText);
        StackTraceElement site = new StackTraceElement(longText, longText, "Caller.java", 42);
        ErrorEvent event = factory.create(new ErrorOccurrence.Logged(longText, site, longText, longText, 0));
        assertEquals(symbol.repeat(MAX_SERVICE_CHARS), event.service());
        assertEquals(symbol.repeat(MAX_ENVIRONMENT_CHARS), event.environment());
        assertEquals(symbol.repeat(MAX_LOCATION_CHARS), event.logger());
        assertEquals(symbol.repeat(MAX_LOCATION_CHARS), event.site());
        assertEquals(symbol.repeat(MAX_MESSAGE_CHARS), event.message());
        assertEquals(event.message(), event.template());
        ErrorEventBatch batch = factory.batch(0, List.of(event));
        assertEquals(event.service(), batch.service());
        assertEquals(event.environment(), batch.environment());
        assertNotNull(java.util.UUID.fromString(batch.batch_id()));

        String malformed = "left\ud800right\udc00" + symbol;
        ErrorEvent repaired = factory.create(new ErrorOccurrence.Logged(malformed, null, malformed, malformed, 0));
        String expected = "left\ufffdright\ufffd" + symbol;
        assertEquals(expected, repaired.message());
        assertEquals(expected, repaired.template());
        assertEquals(expected, repaired.logger());

        ErrorEvent problem = factory.create(new ErrorOccurrence.Problem(500, longText, longText, longText, 0));
        assertEquals(symbol.repeat(MAX_TYPE_CHARS), problem.type());
        assertEquals(List.of(symbol.repeat(MAX_LOCATION_CHARS)), problem.frames());
    }

    @Test
    void deepProductTracesAreBoundedAndTheirFramesAreBoundedSeparately() {
        var failure = new IllegalStateException("failure");
        String name = PRODUCT + "Class" + "x".repeat(MAX_LOCATION_CHARS);
        failure.setStackTrace(IntStream.range(0, MAX_TRACE_CHARS)
                .mapToObj(index -> new StackTraceElement(name, "call", "Caller.java", index)).toArray(StackTraceElement[]::new));
        ErrorEvent event = factory.create(new ErrorOccurrence.Thrown("operation", failure, 0));
        assertEquals(MAX_TRACE_CHARS, event.trace().codePointCount(0, event.trace().length()));
        assertEquals(MAX_FRAMES, event.frames().size());
        assertTrue(event.frames().stream().allMatch(frame -> frame.length() == MAX_LOCATION_CHARS));
    }

    private static StackTraceElement[] stack(String... frames) {
        return IntStream.range(0, frames.length).mapToObj(index -> {
            String frame = frames[index];
            String className = frame.substring(0, frame.lastIndexOf('.'));
            return new StackTraceElement(className, frame.substring(frame.lastIndexOf('.') + 1),
                    className.substring(className.lastIndexOf('.') + 1) + ".java", index + 1);
        }).toArray(StackTraceElement[]::new);
    }

    private static class SelfCaused extends RuntimeException {
        SelfCaused(String message) {
            super(message);
        }

        @Override
        public synchronized Throwable getCause() {
            return this;
        }
    }
}
