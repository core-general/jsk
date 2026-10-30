package sk.web.server.filters.standard;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2020 Core General
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

import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import sk.exceptions.JskProblem;
import sk.exceptions.JskProblemException;
import sk.services.errors.IErrorReporter;
import sk.web.exceptions.JskProblemExceptionWithHttpCode;
import sk.web.renders.WebFilterOutput;
import sk.web.server.WebServerCore;
import sk.web.server.filters.WebServerFilter;
import sk.web.server.filters.WebServerFilterContext;
import sk.web.server.model.WebProblemWithRequestBodyException;
import sk.web.server.params.WebExceptionParams;
import sk.web.utils.WebApiMethod;

import java.lang.reflect.Method;
import java.util.Optional;

import static sk.services.errors.ErrorReportMarkers.REPORTED_SEPARATELY;

@Slf4j
public class WebExceptionFilter implements WebServerFilter {
    public static final int PRIORITY = WebIdempotenceFilter.PRIORITY + PRIORITY_STEP;

    @Inject WebExceptionParams conf;
    @Inject Optional<IErrorReporter> errorReporter = Optional.empty();

    @Override
    public int getFilterPriority() {
        return PRIORITY;
    }

    @Override
    public <API> WebFilterOutput invoke(WebServerFilterContext<API> requestContext) {
        try {
            return requestContext.getNextInChain().invokeNext();
        } catch (JskProblemExceptionWithHttpCode exc) {
            final WebFilterOutput webFilterOutput = WebFilterOutput.rawProblem(exc.getHttpCode(), exc.getProblem());
            if (conf.shouldLog(exc)) {
                log.error(REPORTED_SEPARATELY,
                        String.format("Error on request with code: %d %s", exc.getHttpCode(), exc.getProblem()));
                reportServerProblem(exc.getHttpCode(), exc, requestContext);
            }
            return webFilterOutput;
        } catch (JskProblemException exc) {
            final WebFilterOutput webFilterOutput =
                    WebFilterOutput.rawProblem(conf.getUnhandledJskExceptionHttpCode(), exc.getProblem());
            if (conf.shouldLog(exc)) {
                log.error(REPORTED_SEPARATELY,
                        "Error on request no code: " + conf.getUnhandledJskExceptionHttpCode() + " " + exc.getProblem());
                reportServerProblem(conf.getUnhandledJskExceptionHttpCode(), exc, requestContext);
            }
            return webFilterOutput;
        } catch (WebProblemWithRequestBodyException exc) {
            if (conf.shouldLog(exc)) {
                log.warn("WebProblemWithRequestBodyException");
            }
            return WebFilterOutput.rawProblem(503, JskProblem.code(WebServerCore.IO_PROBLEM_WHILE_READ_BODY));
        } catch (Exception unknownExc) {
            final WebFilterOutput webFilterOutput = requestContext.getRequestContext().getExceptionProcessors()
                    .flatMap($ -> $.apply(unknownExc.getClass()))
                    .map($ -> $.apply(unknownExc, requestContext.getRequestContext()))
                    .orElseGet(() -> WebFilterOutput.rawProblem(conf.getUnknownExceptionHttpCode(),
                            JskProblem.code(WebServerCore.INTERNAL_ERROR)));

            if (conf.shouldLog(unknownExc)) {
                log.error("Error on request unknown: " + webFilterOutput.getCode() + " " +
                                webFilterOutput.getRawOrRenderedAsString(),
                        unknownExc);
            }
            return webFilterOutput;
        }
    }

    private void reportServerProblem(int httpCode, JskProblemException exc, WebServerFilterContext<?> requestContext) {
        if (httpCode >= 500) {
            final JskProblem problem = exc.getProblem();
            errorReporter.ifPresent(reporter -> reporter.reportProblem(httpCode,
                    problem == null ? null : problem.getCode(),
                    problem == null ? null : problem.getDescription(),
                    apiMethodName(requestContext.getApiMethod())));
        }
    }

    private static String apiMethodName(WebApiMethod<?> apiMethod) {
        return apiMethod.getCls().getName() + apiMethod.getOMethod().map(Method::getName).map($ -> "." + $).orElse("");
    }
}
