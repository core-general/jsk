package sk.utils.computation.chained;

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

import sk.utils.computation.chained.values.SomeValue;
import sk.utils.javafixes.TypeWrap;
import sk.utils.statics.Re;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.stream.Stream;

public abstract class ChainedMappedInput {
    public ChainedMappedInput(ChainedComputation context) {
        final Set<Field> publicFields = Re.getNonStaticPublicFields(this.getClass());
        Stream<Field> stream = publicFields.stream();
        stream = (context.isParallelComputation()) ? stream.parallel() : stream;
        stream.forEach((publicField) -> initField(context, publicField));
    }

    private void initField(ChainedComputation context, Field field) {
        final String name = field.getName();
        final SomeValue<?> some = context.getDataHolder().get(name);
        if (some == null) {
            throw new RuntimeException(String.format("Can't find input \"%s\" for %s", field, this.getClass().getName()));
        }
        checkTypedParameter(field, some);
        final Object data = some.get(context);
        try {
            field.set(this, data);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Cant set field \"%s\" for %s", field, this.getClass().getSimpleName()), e);
        }
    }

    private void checkTypedParameter(Field field, SomeValue<?> some) {
        final TypeWrap<?> typeParameter = some.getValueClass();
        final Type fieldGenericType = field.getGenericType();
        if (!typeParameter.equalsTo(fieldGenericType)) {
            try {
                if (((Class) fieldGenericType).isAssignableFrom((Class<?>) typeParameter.getType())) {
                    return;
                }
            } catch (Exception e) {}

            throw new RuntimeException(
                    String.format("Type parameters are different type \"%s  %s!=%s\" for %s", field,
                            typeParameter.getType().getTypeName(),
                            fieldGenericType.getTypeName(), this.getClass().getSimpleName()));
        }
    }
}
