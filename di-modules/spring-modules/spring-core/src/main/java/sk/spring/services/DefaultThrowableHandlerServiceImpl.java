package sk.spring.services;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2022 Core General
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

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import sk.spring.utils.DefaultThrowableHandler;
import sk.utils.functional.O;
import sk.utils.javafixes.PartialClassTree;
import sk.utils.statics.Cc;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Log4j2
public class DefaultThrowableHandlerServiceImpl {
    private @Inject List<DefaultThrowableHandler<?>> handlers;

    private final PartialClassTree classHierarchy = new PartialClassTree();
    private final Map<Class<?>, List<DefaultThrowableHandler<?>>> clsCache = Cc.m();

    @PostConstruct
    public DefaultThrowableHandlerServiceImpl init() {
        handlers.stream().filter($ -> $.getThrowableClass() != null).forEach(handler -> {
            classHierarchy.add(handler.getThrowableClass());
            Cc.computeAndApply(clsCache, handler.getThrowableClass(), (k, lst) -> Cc.add(lst, handler), Cc::l);
        });

        Thread.setDefaultUncaughtExceptionHandler(this::processError);

        return this;
    }

    private <T extends Throwable> void processError(Thread thread, T throwable) {
        final O<Class<?>> nearestParent = classHierarchy.getNearestParentTo(throwable.getClass());
        if (nearestParent.isPresent()) {
            for (DefaultThrowableHandler<?> hnd : clsCache.get(nearestParent.get())) {
                try {
                    ((DefaultThrowableHandler<T>) hnd).process(throwable, thread);
                } catch (Exception e) {
                    log.error("", e);
                }
            }
        }
    }
}
