package sk.spring.services;

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

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.AbstractApplicationContext;
import sk.services.bean.IServiceLocator;
import sk.utils.functional.O;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ServiceLocator4SpringImpl implements IServiceLocator {
    public static volatile IServiceLocator instance;

    @Inject AbstractApplicationContext context;

    @PostConstruct
    public void init() {
        instance = this;
    }

    @Override
    public <K> O<K> getService(Class<K> cls) {
        try {
            return O.ofNull(context.getBean(cls));
        } catch (Exception e) {
            log.error("", e);
            return O.empty();
        }
    }

    @Override
    public <K> List<K> getServices(Class<K> cls) {
        return new ArrayList<>(context.getBeansOfType(cls).values());
    }

    @Override
    public <K> O<K> injectServicesInto(K someObject) {
        try {
            context.getBeanFactory().autowireBean(someObject);
            return O.ofNull(someObject);
        } catch (Exception e) {
            log.error("", e);
            return O.empty();
        }
    }
}
