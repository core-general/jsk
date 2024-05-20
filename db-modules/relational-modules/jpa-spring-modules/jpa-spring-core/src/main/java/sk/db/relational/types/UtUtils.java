package sk.db.relational.types;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2024 Core General
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

import lombok.SneakyThrows;
import org.hibernate.usertype.DynamicParameterizedType;

import java.util.Properties;

public class UtUtils {
    @SneakyThrows
    public static Class<?> getType(Properties parameters, String ifNotDynamicThenThisPropertyName) {
        final DynamicParameterizedType.ParameterType reader =
                (DynamicParameterizedType.ParameterType) parameters.get(DynamicParameterizedType.PARAMETER_TYPE);
        return reader != null
               ? reader.getReturnedClass()
               : Class.forName(parameters.getProperty(ifNotDynamicThenThisPropertyName));
    }
}
