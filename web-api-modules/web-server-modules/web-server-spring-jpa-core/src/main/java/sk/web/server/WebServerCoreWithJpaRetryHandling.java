package sk.web.server;

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

import org.hibernate.StaleObjectStateException;
import org.hibernate.dialect.lock.OptimisticEntityLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import sk.exceptions.JskProblem;
import sk.utils.functional.F1;
import sk.utils.functional.F2;
import sk.utils.functional.O;
import sk.web.renders.WebFilterOutput;
import sk.web.server.context.WebRequestInnerContext;

import javax.persistence.OptimisticLockException;
import java.lang.reflect.Method;

public class WebServerCoreWithJpaRetryHandling<T> extends WebServerCoreWithPings<T> {
    private final F2<Exception, WebRequestInnerContext, WebFilterOutput> retryProblem =
            (e, ctx) -> WebFilterOutput.rawProblem(503, JskProblem.substatus("must_retry", "contention"));


    public WebServerCoreWithJpaRetryHandling(Class<T> tClass, T impl) {
        super(tClass, impl);
    }

    public WebServerCoreWithJpaRetryHandling(Class<T> tClass, T impl, String basePath) {
        super(tClass, impl, basePath);
    }

    @Override
    protected O<F1<Class<? extends Exception>, O<F2<Exception, WebRequestInnerContext, WebFilterOutput>>>>
    getExceptionProcessors(O<Method> methodOrAll) {
        return O.of(aClass -> {
            if (ObjectOptimisticLockingFailureException.class.isAssignableFrom(aClass) ||
                    OptimisticLockException.class.isAssignableFrom(aClass) ||
                    OptimisticEntityLockException.class.isAssignableFrom(aClass) ||
                    StaleObjectStateException.class.isAssignableFrom(aClass)) {
                return O.of(retryProblem);
            } else {
                return O.empty();
            }
        });
    }
}
