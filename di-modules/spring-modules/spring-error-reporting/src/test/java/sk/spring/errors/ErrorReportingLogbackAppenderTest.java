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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import sk.services.errors.ErrorReportMarkers;
import sk.test.JskMockitoTest;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Set;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ErrorReportingLogbackAppenderTest extends JskMockitoTest {
    @Mock ILoggedErrorReporter reporter;
    @Spy ThreadErrorReportingScope scopes;
    @InjectMocks ErrorReportingLogbackAppender appender;

    private final LoggerContext context = new LoggerContext();
    private Logger logger;

    @BeforeEach
    void attach() {
        appender.setContext(context);
        appender.start();
        context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).addAppender(appender);
        logger = context.getLogger("test.errors");
    }

    @AfterEach
    void detach() {
        context.stop();
    }

    @Test
    void errorsWithAThrowableAreReportedAsThatThrowable() {
        IllegalStateException failure = new IllegalStateException("broken");

        logger.error("Request failed", failure);

        verify(reporter).report(null, failure);
        verifyNoMoreInteractions(reporter);
    }

    @Test
    void errorsWithoutAThrowableAreReportedWithCallSiteTemplateAndText() {
        logger.error("Request {} failed", "r-1");

        verify(reporter).reportLogged(eq(logger.getName()),
                argThat(site -> site != null && site.getClassName().equals(getClass().getName())),
                eq("Request {} failed"), eq("Request r-1 failed"));
        verifyNoMoreInteractions(reporter);
    }

    @Test
    void separatelyReportedOwnPackageAndBelowErrorEventsAreSkipped() {
        logger.error(ErrorReportMarkers.REPORTED_SEPARATELY, "Problem reply already reported");
        logger.error(ErrorReportMarkers.LOCAL_ONLY, "Routine rejection");
        context.getLogger(QueueingErrorReporter.class).error("Reporter failure", new IllegalStateException("send"));
        logger.warn("Degraded", new IllegalStateException("slow"));

        verifyNoInteractions(reporter);
    }

    @Test
    void scopedSqlLogsAndRoutineRejectionsRemainLocalWhileTerminalErrorsAreReported() {
        var local = new ListAppender<ILoggingEvent>();
        local.start();
        context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).addAppender(local);
        String sqlLogger = "org.hibernate.engine.jdbc.spi.SqlExceptionHelper";
        try (var outer = scopes.suppressLoggers(Set.of(sqlLogger))) {
            context.getLogger(sqlLogger).error("duplicate key first attempt");
            try (var inner = scopes.suppressLoggers(Set.of(sqlLogger))) {
                context.getLogger(sqlLogger).error("nested attempt", new IllegalStateException("database"));
            }
            context.getLogger(sqlLogger).error("commit conflict");
            logger.error(ErrorReportMarkers.LOCAL_ONLY, "ddoser:192.0.2.1");
            logger.error("independent error");
        }
        var terminal = new IllegalStateException("terminal");
        logger.error("request failed", terminal);
        verify(reporter).reportLogged(eq(logger.getName()), any(), eq("independent error"), eq("independent error"));
        verify(reporter).report(null, terminal);
        verifyNoMoreInteractions(reporter);
        org.junit.jupiter.api.Assertions.assertEquals(6, local.list.size());
    }
}
