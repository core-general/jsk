package sk.db.relational.utils;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2021 Core General
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

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.internal.EntityRepresentationStrategyPojoStandard;
import org.hibernate.metamodel.spi.EntityInstantiator;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import sk.db.relational.spring.config.RdbBaseDbConfig;
import sk.spring.services.CoreServices;

import static sk.db.relational.utils.RdbIntegrator4Context.inject;

public class CtxAwarePojoTuplizer extends EntityRepresentationStrategyPojoStandard {

    private final SessionFactoryImplementor sessionFactoryImplementor;
    private final PrivateInstantiator privateInstantiator;

    public CtxAwarePojoTuplizer(PersistentClass bootDescriptor,
            EntityPersister runtimeDescriptor,
            RuntimeModelCreationContext creationContext) {
        super(bootDescriptor, runtimeDescriptor, creationContext);
        this.sessionFactoryImplementor = creationContext.getSessionFactory();
        final CoreServices ctx = (CoreServices) sessionFactoryImplementor.getProperties().get(RdbBaseDbConfig._CTX);

        privateInstantiator = new PrivateInstantiator(ctx, super.getInstantiator());
    }

    @Override
    public EntityInstantiator getInstantiator() {
        return privateInstantiator;
    }

    //@Override
    //protected Instantiator buildInstantiator(EntityMappingType entityMetamodel, PersistentClass persistentClass) {
    //
    //    final Instantiator instantiator = super.buildInstantiator(entityMetamodel, persistentClass);
    //    return new PrivateInstantiator(ctx, instantiator);
    //}

    private static class PrivateInstantiator implements EntityInstantiator {
        private final CoreServices ctx;
        private final EntityInstantiator instantiator;

        public PrivateInstantiator(CoreServices ctx, EntityInstantiator instantiator) {
            this.ctx = ctx;
            this.instantiator = instantiator;
        }

        @Override
        public boolean isInstance(Object object, SessionFactoryImplementor sessionFactory) {
            return instantiator.isInstance(object, sessionFactory);
        }

        @Override
        public boolean isSameClass(Object object, SessionFactoryImplementor sessionFactory) {
            return false;
        }

        @Override
        public Object instantiate(SessionFactoryImplementor sessionFactory) {
            return inject(ctx, instantiator.instantiate(sessionFactory));
        }
    }
}
