package sk.services.translate;

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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Weigher;
import lombok.AllArgsConstructor;
import org.checkerframework.checker.index.qual.NonNegative;
import sk.services.json.IJson;

import javax.inject.Inject;


@AllArgsConstructor
public class TranslateWithInMemoryCache implements ITranslate {
    @Inject IJson json;

    final ITranslate next;
    final Cache<String, TranslateInfo> tranlateCache;
    final Cache<String, LangType> langCache;

    public TranslateWithInMemoryCache(ITranslate next) {
        this.next = next;
        tranlateCache = Caffeine.newBuilder()
                .maximumWeight(Runtime.getRuntime().maxMemory() / 10)
                .weigher(new Weigher<String, TranslateInfo>() {
                    @Override
                    public @NonNegative int weigh(String key, TranslateInfo value) {
                        return key.length() * 2 + value.approxMemory();
                    }
                })
                .softValues()
                .build();
        langCache = Caffeine.newBuilder()
                .maximumWeight(Runtime.getRuntime().maxMemory() / 10)
                .weigher(new Weigher<String, LangType>() {
                    @Override
                    public @NonNegative int weigh(String key, LangType value) {
                        return key.length() * 2 + value.name().length() * 2;
                    }
                })
                .softValues()
                .build();
    }

    @Override
    public LangType recognizeLanguage(Text2Translate text) {
        return langCache.asMap().computeIfAbsent(text.getHash() + "_lang", k -> next.recognizeLanguage(text));
    }

    @Override
    public TranslateInfo translate(LangType from, LangType to, Text2Translate text) {
        return tranlateCache.asMap()
                .computeIfAbsent(text.getHash() + "_" + from + "->" + to, k -> next.translate(from, to, text));
    }
}
