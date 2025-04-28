package sk.db.relational.spring.services.impl;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2025 Core General
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
import sk.db.relational.utils.ReadWriteRepo;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RdbTransactionManagerHolder {
    @Inject private List<ReadWriteRepo> readWriteRepos;
    private final Map<Type, ReadWriteRepo> repoJpaMap = new HashMap<>();


    @PostConstruct
    public void initJpaRepoMap() {
        readWriteRepos.forEach(repository -> {
            Arrays.stream(repository.getClass().getGenericInterfaces())
                    .filter(ParameterizedType.class::isInstance)
                    .map(ParameterizedType.class::cast)
                    .filter($ -> ReadWriteRepo.class.equals($.getRawType()))
                    .findAny()
                    .ifPresent(parameterizedType ->
                            repoJpaMap.put(parameterizedType.getActualTypeArguments()[0], repository));
        });
    }

    public ReadWriteRepo<Object, ?> get(Class<?> aClass) {
        return repoJpaMap.get(aClass);
    }
}
