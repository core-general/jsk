package sk.spring.services;

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

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertSame;

class ServiceLocator4SpringImplTest {
    @Test
    void closingTheNewestContextRestoresServicesFromTheRemainingContext() {
        var original = ServiceLocator4SpringImpl.instance;
        var firstService = new TestService();
        var secondService = new TestService();
        try (var first = context(firstService)) {
            assertSame(firstService, ServiceLocator4SpringImpl.instance.getService(TestService.class).get());
            try (var second = context(secondService)) {
                assertSame(secondService, ServiceLocator4SpringImpl.instance.getService(TestService.class).get());
            }
            assertSame(firstService, ServiceLocator4SpringImpl.instance.getService(TestService.class).get());
        }
        assertSame(original, ServiceLocator4SpringImpl.instance);
    }

    @Test
    void closingContextsOutOfOrderNeverRestoresAClosedContext() {
        var original = ServiceLocator4SpringImpl.instance;
        var service = new TestService();
        try (var first = context(new TestService()); var second = context(new TestService()); var third = context(service)) {
            second.close();
            assertSame(service, ServiceLocator4SpringImpl.instance.getService(TestService.class).get());
            first.close();
            assertSame(service, ServiceLocator4SpringImpl.instance.getService(TestService.class).get());
        }
        assertSame(original, ServiceLocator4SpringImpl.instance);
    }

    private record TestService() {}

    private AnnotationConfigApplicationContext context(TestService service) {
        var context = new AnnotationConfigApplicationContext();
        context.registerBean(TestService.class, () -> service);
        context.registerBean(ServiceLocator4SpringImpl.class);
        context.refresh();
        return context;
    }
}
