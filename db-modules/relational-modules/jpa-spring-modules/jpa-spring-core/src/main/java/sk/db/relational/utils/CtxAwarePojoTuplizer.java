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

import org.hibernate.mapping.PersistentClass;
import org.hibernate.tuple.Instantiator;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.tuple.entity.PojoEntityTuplizer;
import sk.db.relational.spring.config.RdbBaseDbConfig;
import sk.spring.services.CoreServices;

import java.io.Serializable;

import static sk.db.relational.utils.RdbIntegrator4Context.inject;

public class CtxAwarePojoTuplizer extends PojoEntityTuplizer {
    public CtxAwarePojoTuplizer(EntityMetamodel entityMetamodel, PersistentClass mappedEntity) {
        super(entityMetamodel, mappedEntity);
    }

    @Override
    protected Instantiator buildInstantiator(EntityMetamodel entityMetamodel, PersistentClass persistentClass) {
        final CoreServices ctx = (CoreServices) entityMetamodel.getSessionFactory().getProperties().get(RdbBaseDbConfig._CTX);

        final Instantiator instantiator = super.buildInstantiator(entityMetamodel, persistentClass);
        return new PrivateInstantiator(ctx, instantiator);
    }

    private static class PrivateInstantiator implements Instantiator {
        private final CoreServices ctx;
        private final Instantiator instantiator;

        public PrivateInstantiator(CoreServices ctx, Instantiator instantiator) {
            this.ctx = ctx;
            this.instantiator = instantiator;
        }

        @Override
        public Object instantiate(Serializable id) {
            return inject(ctx, instantiator.instantiate(id));
        }

        @Override
        public Object instantiate() {
            return inject(ctx, instantiator.instantiate());
        }

        @Override
        public boolean isInstance(Object object) {
            return instantiator.isInstance(object);
        }
    }
}
