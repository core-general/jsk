package sk.services.json;

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

import sk.services.bean.IServiceLocator;
import sk.utils.functional.O;

/**
 * IMPORTANT!!! If you created custom converter for a class, then most this class WILL NOT WORK, because e.g.
 * Gson when using GsonSerDes does not invoke it's factories.
 *
 * If you need some processing for classes with converters (e.g. they should know json instance with which properties
 * is invoking them, you could use IJson.getCurrentInvocationProps() method (it's ThreadLocal based))
 */
public interface IJsonInitialized {
    default void beforeSerialize(O<IServiceLocator> serviceProvider, IJsonInstanceProps runProps) {}

    public void afterDeserialize(O<IServiceLocator> serviceProvider, IJsonInstanceProps runProps);
}
