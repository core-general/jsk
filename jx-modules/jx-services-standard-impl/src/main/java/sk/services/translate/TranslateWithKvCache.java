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

import lombok.AllArgsConstructor;
import sk.services.json.IJson;
import sk.services.kv.IKvStore;
import sk.services.kv.keys.KvSimpleKeyWithName;


@AllArgsConstructor
public class TranslateWithKvCache implements ITranslate {
    IJson json;
    IKvStore kv;
    ITranslate next;

    @Override
    public LangType recognizeLanguage(Text2Translate text) {
        return kv.getAsObject(new KvSimpleKeyWithName(text.getHash() + "_lang", null) {
            @Override
            public String getDefaultValue() {
                return json.to(next.recognizeLanguage(text));
            }
        }, LangType.class);
    }

    @Override
    public TranslateInfo translate(LangType from, LangType to, Text2Translate text) {
        return kv.getAsObject(new KvSimpleKeyWithName(text.getHash() + "_" + from + "->" + to, null) {
            @Override
            public String getDefaultValue() {
                return json.to(next.translate(from, to, text));
            }
        }, TranslateInfo.class);
    }
}
