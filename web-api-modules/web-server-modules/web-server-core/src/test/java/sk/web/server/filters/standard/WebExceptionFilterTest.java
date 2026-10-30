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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import sk.exceptions.JskProblem;
import sk.exceptions.JskProblemException;
import sk.services.errors.IErrorReporter;
import sk.test.JskMockitoTest;
import sk.utils.functional.O;
import sk.web.exceptions.JskProblemExceptionWithHttpCode;
import sk.web.renders.WebFilterOutput;
import sk.web.server.context.WebRequestInnerContext;
import sk.web.server.filters.WebServerFilterContext;
import sk.web.server.params.WebExceptionParams;
import sk.web.utils.WebApiMethod;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WebExceptionFilterTest extends JskMockitoTest {
    @Mock WebExceptionParams conf;
    @Mock IErrorReporter reporter;
    @Mock WebRequestInnerContext request;

    private final WebExceptionFilter filter = new WebExceptionFilter();
    private Method apiMethod;

    @BeforeEach
    void filter() throws NoSuchMethodException {
        filter.conf = conf;
        filter.errorReporter = Optional.of(reporter);
        when(conf.shouldLog(any())).thenReturn(true);
        apiMethod = TestApi.class.getMethod("call");
    }

    @Test
    void serverProblemRepliesAreReportedWithTheirApiMethod() {
        when(conf.getUnhandledJskExceptionHttpCode()).thenReturn(500);

        WebFilterOutput withCode = invoke(new JskProblemExceptionWithHttpCode(503, JskProblem.code("UPSTREAM_DOWN"), false));
        WebFilterOutput withoutCode = invoke(new JskProblemException(JskProblem.description("database unavailable"), false));

        String apiMethodName = TestApi.class.getName() + "." + apiMethod.getName();
        verify(reporter).reportProblem(withCode.getCode(), "UPSTREAM_DOWN", null, apiMethodName);
        verify(reporter).reportProblem(withoutCode.getCode(), null, "database unavailable", apiMethodName);
        verifyNoMoreInteractions(reporter);
    }

    @Test
    void clientProblemRepliesAreNotReported() {
        when(conf.getUnhandledJskExceptionHttpCode()).thenReturn(422);

        WebFilterOutput withCode = invoke(new JskProblemExceptionWithHttpCode(404, JskProblem.code("NOT_FOUND"), false));
        WebFilterOutput withoutCode = invoke(new JskProblemException(JskProblem.code("INVALID"), false));

        assertTrue(withCode.getCode() < 500 && withoutCode.getCode() < 500);
        verifyNoInteractions(reporter);
    }

    private WebFilterOutput invoke(RuntimeException failure) {
        return filter.invoke(new WebServerFilterContext<>(new WebApiMethod<>(TestApi.class, O.of(apiMethod), false), request,
                () -> {
                    throw failure;
                }));
    }

    interface TestApi {
        void call();
    }
}
