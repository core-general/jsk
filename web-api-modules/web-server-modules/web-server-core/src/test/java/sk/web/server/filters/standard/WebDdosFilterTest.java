package sk.web.server.filters.standard;

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
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;
import sk.exceptions.JskProblem;
import sk.services.errors.ErrorReportMarkers;
import sk.test.JskMockitoTest;
import sk.utils.functional.O;
import sk.web.renders.WebFilterOutput;
import sk.web.server.context.WebRequestInnerContext;
import sk.web.server.context.WebRequestIp;
import sk.web.server.filters.WebServerFilterContext;
import sk.web.server.params.WebDdosParams;
import sk.web.utils.WebApiMethod;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebDdosFilterTest extends JskMockitoTest {
    @Mock WebDdosParams conf;
    @Mock WebRequestInnerContext request;
    @InjectMocks WebDdosFilter filter;

    @Test
    void jailedRequestsKeepTheirLocalErrorLogsWithoutRemoteReporting() {
        when(conf.isDdosCourtEnabled()).thenReturn(true);
        when(conf.getDdosPassingHeader()).thenReturn(O.empty());
        when(conf.getUserInCourtPeriod()).thenReturn(Duration.ofSeconds(5));
        when(conf.getUserRequestsAllowedInCourt()).thenReturn(1);
        when(conf.getUserInJailTime()).thenReturn(Duration.ofMinutes(5));
        when(request.getIpInfo()).thenReturn(new WebRequestIp("192.0.2.1", List.of(), O.empty()));
        filter.init();
        var context = new WebServerFilterContext<>(new WebApiMethod<>(Object.class, O.empty(), false), request,
                () -> WebFilterOutput.rawProblem(200, JskProblem.code("OK")));
        Logger logger = (Logger) LoggerFactory.getLogger(WebDdosFilter.class);
        var local = new ListAppender<ILoggingEvent>();
        local.start();
        logger.addAppender(local);
        try {
            assertEquals(200, filter.invoke(context).getCode());
            assertEquals(403, filter.invoke(context).getCode());
            assertEquals(403, filter.invoke(context).getCode());
            assertEquals(2, local.list.size());
            assertTrue(local.list.stream().allMatch(event -> event.getFormattedMessage().equals("ddoser:192.0.2.1")
                    && event.getMarkerList().contains(ErrorReportMarkers.LOCAL_ONLY)));
        } finally {
            logger.detachAppender(local);
            local.stop();
        }
    }
}
