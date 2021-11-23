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

import lombok.Getter;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.statics.Cc;

import java.util.Arrays;
import java.util.Map;

@Getter
public enum LangType {
    Afrikaans("Afrikaans", "af"),
    Albanian("Shqip", "sq"),
    Amharic("አማርኛ", "am"),
    Arabic("العربية", "ar"),
    Armenian("Հայերեն", "hy"),
    Azerbaijani("Azərbaycan", "az"),
    Bengali("বাংলা", "bn"),
    Bosnian("Bosanski", "bs"),
    Bulgarian("Български език", "bg"),
    Catalan("Català", "ca"),
    Chinese("中文", "zh"),
    Croatian("Hrvatski", "hr"),
    Czech("Čeština", "cs"),
    Danish("Dansk", "da"),
    Dutch("Nederlands", "nl"),
    English("English", "en"),
    Estonian("Eesti", "et"),
    Farsi("فارسی", "fa"),
    Filipino("Pilipino", "tl"),
    Finnish("Suomi", "fi"),
    French("Français", "fr"),
    Georgian("ქართული", "ka"),
    German("Deutsch", "de"),
    Greek("Ελληνικά", "el"),
    Gujarati("ગુજરાતી", "gu"),
    Haitian("Ayisyen", "ht"),
    Hausa("هَوُسَ", "ha"),
    Hebrew("עברית", "he"),
    Hindi("हिन्दी", "hi"),
    Hungarian("Magyar", "hu"),
    Icelandic("Íslenska", "is"),
    Indonesian("Indonesia", "id"),
    Italian("Italiano", "it"),
    Japanese("日本語", "ja"),
    Kannada("ಕನ್ನಡ", "kn"),
    Kazakh("Қазақ", "kk"),
    Korean("한국어", "ko"),
    Latvian("Latviešu", "lv"),
    Lithuanian("Lietuvių", "lt"),
    Macedonian("Македонски", "mk"),
    Malay("Melayu", "ms"),
    Malayalam("മലയാളം", "ml"),
    Maltese("Malti", "mt"),
    Mongolian("Монгол", "mn"),
    Norwegian("Norsk", "no"),
    Pashto("پښتو", "ps"),
    Persian("فارسی", "fa"),
    Polish("Polski", "pl"),
    Portuguese("Português", "pt"),
    Romanian("Română", "ro"),
    Russian("Русский", "ru"),
    Serbian("Српски", "sr"),
    Sinhala("සිංහල", "si"),
    Slovak("Slovenčina", "sk"),
    Slovenian("Slovenski", "sl"),
    Somali("Soomaaliga", "so"),
    Spanish("Español", "es"),
    Swahili("Kiswahili", "sw"),
    Swedish("Svenska", "sv"),
    Tagalog("Tagalog", "tl"),
    Tamil("தமிழ்", "ta"),
    Telugu("తెలుగు", "te"),
    Thai("ไทย", "th"),
    Turkish("Türkçe", "tr"),
    Ukrainian("Українська", "uk"),
    Urdu("اردو", "ur"),
    Uzbek("Oʻzbek", "uz"),
    Vietnamese("Việt", "vi"),
    Welsh("Cymraeg", "cy");

    String originalName;
    String code;

    LangType(String originalName, String code) {
        this.originalName = originalName;
        this.code = code;
    }

    private static final Map<String, LangType> originalNames2LangType = Cc.m();
    private static final Map<String, LangType> code2LangType = Cc.m();

    public static O<LangType> getByOriginalName(String name) {
        return O.ofNull(Maps.NAME_2_LANG.map.get(name));
    }

    public static O<LangType> getByCode(String code) {
        return O.ofNull(Maps.CODE_2_LANG.map.get(code));
    }

    private enum Maps {
        NAME_2_LANG(LangType::getOriginalName),
        CODE_2_LANG(LangType::getCode);

        private Map<String, LangType> map = Cc.m();

        Maps(F1<LangType, String> getter) {
            Arrays.stream(LangType.values()).forEach($ -> {
                final String apply = getter.apply($);
                map.put(apply, $);
            });
        }
    }
}
