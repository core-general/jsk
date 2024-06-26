package jsk.vaadin.fields.pofi;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2023 Core General
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

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import sk.utils.statics.St;

public interface FieldWithConverter<MODEL> {

    default Converter<String, MODEL> getConverter() {
        return new Converter<>() {
            @Override
            public Result<MODEL> convertToModel(String value, ValueContext context) {
                try {
                    return Result.ok(St.isNullOrEmpty(value) ? null : convertTo(value));
                } catch (Exception e) {
                    return Result.error(getConversionErrorText(e));
                }
            }

            @Override
            public String convertToPresentation(MODEL value, ValueContext context) {
                return value == null ? "" : convertFrom(value);
            }
        };
    }

    MODEL convertTo(String value);

    default String convertFrom(MODEL value) {
        return value.toString();
    }

    String getConversionErrorText(Exception e);
}
