package sk.db.relational.utils;

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

import org.hibernate.HibernateException;
import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.*;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import sk.db.relational.model.JpaWithContext;
import sk.db.relational.spring.config.RdbBaseDbConfig;
import sk.spring.services.CoreServices;

import java.util.Map;

public class RdbIntegrator4Context implements Integrator {
    @Override
    public void integrate(Metadata metadata,
            SessionFactoryImplementor sessionFactory,
            SessionFactoryServiceRegistry serviceRegistry) {
        final CoreServices ctx = (CoreServices) sessionFactory.getProperties().get(RdbBaseDbConfig._CTX);
        final EventListenerRegistry eventListenerRegistry = serviceRegistry.getService(EventListenerRegistry.class);

        eventListenerRegistry.prependListeners(EventType.PRE_LOAD, event -> {
            inject(ctx, event.getEntity());
        });
        eventListenerRegistry.prependListeners(EventType.PRE_INSERT, (PreInsertEventListener) event -> {
            inject(ctx, event.getEntity());
            return false;
        });
        eventListenerRegistry.prependListeners(EventType.PRE_UPDATE, (PreUpdateEventListener) event -> {
            inject(ctx, event.getEntity());
            return false;
        });
        eventListenerRegistry.prependListeners(EventType.PERSIST, new PersistEventListener() {
            @Override
            public void onPersist(PersistEvent event) throws HibernateException {
                inject(ctx, event.getObject());
            }

            @Override
            public void onPersist(PersistEvent event, Map createdAlready) throws HibernateException {
                inject(ctx, event.getObject());
            }
        });
        eventListenerRegistry.prependListeners(EventType.SAVE, (SaveOrUpdateEventListener) event -> {
            inject(ctx, event.getEntity());
        });
        eventListenerRegistry.prependListeners(EventType.MERGE, new MergeEventListener() {
            @Override
            public void onMerge(MergeEvent event) throws HibernateException {
                inject(ctx, event.getEntity());
            }

            @Override
            public void onMerge(MergeEvent event, Map copiedAlready) throws HibernateException {
                inject(ctx, event.getEntity());
            }
        });
        eventListenerRegistry.prependListeners(EventType.UPDATE, (SaveOrUpdateEventListener) event -> {
            inject(ctx, event.getEntity());
        });
    }

    public static Object inject(CoreServices ctx, Object entity) {
        if (entity instanceof JpaWithContext && ((JpaWithContext) entity).getCtx() == null) {
            ((JpaWithContext) entity).setCtx(ctx);
        }
        return entity;
    }

    @Override
    public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) { }
}
