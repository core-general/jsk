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
import sk.services.json.IJson;


public class TranslateWithInMemoryCache implements ITranslate {
    IJson json;

    final ITranslate next;
    final Cache<String, TranslateInfo> tranlateCache;
    final Cache<String, AwsLangRecoResult> langCache;

    public TranslateWithInMemoryCache(IJson json, ITranslate next) {
        this.json = json;
        this.next = next;
        tranlateCache = Caffeine.newBuilder()
                .maximumWeight(Runtime.getRuntime().maxMemory() / 10)
                .weigher(new Weigher<String, TranslateInfo>() {
                    @Override
                    public int weigh(String key, TranslateInfo value) {
                        return key.length() * 2 + value.approxMemory();
                    }
                })
                .softValues()
                .build();
        langCache = Caffeine.newBuilder()
                .maximumWeight(Runtime.getRuntime().maxMemory() / 10)
                .weigher(new Weigher<String, AwsLangRecoResult>() {
                    @Override
                    public int weigh(String key, AwsLangRecoResult value) {
                        return key.length() * 2 + value.size();
                    }
                })
                .softValues()
                .build();
    }

    @Override
    public AwsLangRecoResult recognizeLanguage(Text2Translate text) {
        return langCache.asMap().computeIfAbsent(text.getHash() + "_lang", k -> next.recognizeLanguage(text));
    }

    @Override
    public TranslateInfo translate(LangType from, LangType to, Text2Translate text) {
        return tranlateCache.asMap()
                .computeIfAbsent(text.getHash() + "_" + from + "->" + to, k -> next.translate(from, to, text));
    }
}
