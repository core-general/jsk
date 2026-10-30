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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import org.slf4j.Marker;
import sk.services.errors.ErrorReportMarkers;
import sk.services.errors.IErrorReportingScope;

import java.util.List;

public class ErrorReportingLogbackAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
    private static final String OWN_PACKAGE_PREFIX = ErrorReportingLogbackAppender.class.getPackageName() + ".";

    private final ILoggedErrorReporter reporter;
    private final IErrorReportingScope scopes;

    public ErrorReportingLogbackAppender(ILoggedErrorReporter reporter, IErrorReportingScope scopes) {
        this.reporter = reporter;
        this.scopes = scopes;
    }

    @Override
    protected void append(ILoggingEvent event) {
        try {
            if (isReportable(event)) {
                forward(event);
            }
        } catch (Throwable unreportable) {
        }
    }

    private boolean isReportable(ILoggingEvent event) {
        return event.getLevel().isGreaterOrEqual(Level.ERROR)
                && !isReportedSeparately(event.getMarkerList())
                && !scopes.isSuppressed(event.getLoggerName())
                && (event.getLoggerName() == null || !event.getLoggerName().startsWith(OWN_PACKAGE_PREFIX));
    }

    private boolean isReportedSeparately(List<Marker> markers) {
        return markers != null && markers.stream().anyMatch(marker ->
                marker.contains(ErrorReportMarkers.REPORTED_SEPARATELY) || marker.contains(ErrorReportMarkers.LOCAL_ONLY));
    }

    private void forward(ILoggingEvent event) {
        if (event.getThrowableProxy() instanceof ThrowableProxy thrown) {
            reporter.report(null, thrown.getThrowable());
        } else {
            StackTraceElement[] callerData = event.getCallerData();
            StackTraceElement site = callerData == null || callerData.length == 0 ? null : callerData[0];
            reporter.reportLogged(event.getLoggerName(), site, event.getMessage(), event.getFormattedMessage());
        }
    }
}
