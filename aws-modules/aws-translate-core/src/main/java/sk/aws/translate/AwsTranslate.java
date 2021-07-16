package sk.aws.translate;

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

import lombok.NoArgsConstructor;
import sk.aws.AwsUtilityHelper;
import sk.services.translate.ITranslate;
import sk.services.translate.LangType;
import sk.services.translate.Text2Translate;
import sk.services.translate.TranslateInfo;
import sk.utils.statics.St;
import software.amazon.awssdk.services.comprehend.ComprehendClient;
import software.amazon.awssdk.services.comprehend.model.DetectDominantLanguageRequest;
import software.amazon.awssdk.services.translate.TranslateClient;
import software.amazon.awssdk.services.translate.model.TranslateTextRequest;
import software.amazon.awssdk.services.translate.model.TranslateTextResponse;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Comparator;

import static sk.services.translate.LangType.*;

@NoArgsConstructor
public class AwsTranslate implements ITranslate {
    @Inject AwsTranslateProperties trConf;
    @Inject AwsComprehendProperties compConf;
    @Inject AwsUtilityHelper helper;

    private ComprehendClient comprehend;
    private TranslateClient translate;

    public AwsTranslate(AwsTranslateProperties trConf, AwsComprehendProperties compConf, AwsUtilityHelper helper) {
        this.trConf = trConf;
        this.compConf = compConf;
        this.helper = helper;
        init();
    }

    @PostConstruct
    public AwsTranslate init() {
        comprehend = helper.createSync(ComprehendClient::builder, compConf);
        translate = helper.createSync(TranslateClient::builder, trConf);
        return this;
    }

    @Override
    public LangType recognizeLanguage(Text2Translate text) {
        String langCode = comprehend.detectDominantLanguage(DetectDominantLanguageRequest.builder()
                .text(text.getTxt())
                .build()).languages().stream().max(Comparator.comparing($ -> $.score())).map($ -> $.languageCode())
                .orElseThrow();
        return lang2Code(langCode);
    }

    @Override
    public TranslateInfo translate(LangType from, LangType to, Text2Translate text) {
        final TranslateTextResponse result = translate.translateText(TranslateTextRequest.builder()
                .sourceLanguageCode(lang2Code(from))
                .targetLanguageCode(lang2Code(to))
                .text(text.getTxt())
                .build());

        return new TranslateInfo(
                lang2Code(result.sourceLanguageCode()),
                lang2Code(result.targetLanguageCode()),
                text.getTxt(),
                result.translatedText()
        );
    }

    // @formatter:off
    private static String lang2Code(LangType lt){
        switch (lt){
            case Albanian:return "sq";
            case Amharic:return "am";
            case Arabic:return "ar";
            case Armenian:return "hy";
            case Azerbaijani:return "az";
            case Bengali:return "bn";
            case Bosnian:return "bs";
            case Bulgarian:return "bg";
            case Catalan:return "ca";
            case Chinese:return "zh";
            case Croatian:return "hr";
            case Czech:return "cs";
            case Danish:return "da";
            case Dari:return "fa-AF";
            case Dutch:return "nl";
            case English:return "en";
            case Estonian:return "et";
            case Farsi:return "fa";
            case Filipino:return "tl";
            case Finnish:return "fi";
            case French:return "fr";
            case Georgian:return "ka";
            case German:return "de";
            case Greek:return "el";
            case Gujarati:return "gu";
            case Haitian:return "ht";
            case Hausa:return "ha";
            case Hebrew:return "he";
            case Hindi:return "hi";
            case Hungarian:return "hu";
            case Icelandic:return "is";
            case Indonesian:return "id";
            case Italian:return "it";
            case Japanese:return "ja";
            case Kannada:return "kn";
            case Kazakh:return "kk";
            case Korean:return "ko";
            case Latvian:return "lv";
            case Lithuanian:return "lt";
            case Macedonian:return "mk";
            case Malay:return "ms";
            case Malayalam:return "ml";
            case Maltese:return "mt";
            case Mongolian:return "mn";
            case Norwegian:return "no";
            case Pashto:return "ps";
            case Persian:return "fa";
            case Polish:return "pl";
            case Portuguese:return "pt";
            case Romanian:return "ro";
            case Russian:return "ru";
            case Serbian:return "sr";
            case Sinhala:return "si";
            case Slovak:return "sk";
            case Slovenian:return "sl";
            case Somali:return "so";
            case Spanish:return "es";
            case Swahili:return "sw";
            case Swedish:return "sv";
            case Tagalog:return "tl";
            case Tamil:return "ta";
            case Telugu:return "te";
            case Thai:return "th";
            case Turkish:return "tr";
            case Ukrainian:return "uk";
            case Urdu:return "ur";
            case Uzbek:return "uz";
            case Vietnamese:return "vi";
            case Welsh:return "cy";
            case Afrikaans:return "af";
            default: throw new RuntimeException(lt+" unknown");
        }
    }

    private static LangType lang2Code(String code){
        switch (St.subRF(code, "-")){
            case "af":return Afrikaans;
            case "sq":return Albanian;
            case "am":return Amharic;
            case "ar":return Arabic;
            case "hy":return Armenian;
            case "az":return Azerbaijani;
            case "bn":return Bengali;
            case "bs":return Bosnian;
            case "bg":return Bulgarian;
            case "ca":return Catalan;
            case "zh":return Chinese;
            case "hr":return Croatian;
            case "cs":return Czech;
            case "da":return Danish;
            case "fa-AF":return Dari;
            case "nl":return Dutch;
            case "en":return English;
            case "et":return Estonian;
            case "fa":return Farsi;
            case "tl":return Filipino;
            case "fi":return Finnish;
            case "fr":return French;
            case "ka":return Georgian;
            case "de":return German;
            case "el":return Greek;
            case "gu":return Gujarati;
            case "ht":return Haitian;
            case "ha":return Hausa;
            case "he":return Hebrew;
            case "hi":return Hindi;
            case "hu":return Hungarian;
            case "is":return Icelandic;
            case "id":return Indonesian;
            case "it":return Italian;
            case "ja":return Japanese;
            case "kn":return Kannada;
            case "kk":return Kazakh;
            case "ko":return Korean;
            case "lv":return Latvian;
            case "lt":return Lithuanian;
            case "mk":return Macedonian;
            case "ms":return Malay;
            case "ml":return Malayalam;
            case "mt":return Maltese;
            case "mn":return Mongolian;
            case "no":return Norwegian;
            case "ps":return Pashto;
            case "pl":return Polish;
            case "pt":return Portuguese;
            case "ro":return Romanian;
            case "ru":return Russian;
            case "sr":return Serbian;
            case "si":return Sinhala;

            case "sk":return Slovak;
            case "sl":return Slovenian;
            case "so":return Somali;
            case "es":return Spanish;
            case "sw":return Swahili;
            case "sv":return Swedish;
            case "ta":return Tamil;
            case "te":return Telugu;
            case "th":return Thai;
            case "tr":return Turkish;
            case "uk":return Ukrainian;
            case "ur":return Urdu;
            case "uz":return Uzbek;
            case "vi":return Vietnamese;
            case "cy":return Welsh;
            default: throw new RuntimeException(code+" unknown");
        }
    }
    // @formatter:on

    //public static void main(String[] args) {
    //    var langs = "Afrikaans\taf\n" +
    //            "                Albanian\tsq\n" +
    //            "                Amharic\tam\n" +
    //            "                Arabic\tar\n" +
    //            "                Armenian\thy\n" +
    //            "                Azerbaijani\taz\n" +
    //            "                Bengali\tbn\n" +
    //            "                Bosnian\tbs\n" +
    //            "                Bulgarian\tbg\n" +
    //            "                Catalan\tca\n" +
    //            "                Chinese (Simplified)\tzh\n" +
    //            "                Chinese (Traditional)\tzh-TW\n" +
    //            "                Croatian\thr\n" +
    //            "                Czech\tcs\n" +
    //            "                Danish\tda\n" +
    //            "                Dari\tfa-AF\n" +
    //            "                Dutch\tnl\n" +
    //            "                English\ten\n" +
    //            "                Estonian\tet\n" +
    //            "                Farsi (Persian)\tfa\n" +
    //            "                Filipino Tagalog\ttl\n" +
    //            "                Finnish\tfi\n" +
    //            "                French\tfr\n" +
    //            "                French (Canada)\tfr-CA\n" +
    //            "                Georgian\tka\n" +
    //            "                German\tde\n" +
    //            "                Greek\tel\n" +
    //            "                Gujarati\tgu\n" +
    //            "                Haitian Creole\tht\n" +
    //            "                Hausa\tha\n" +
    //            "                Hebrew\the\n" +
    //            "                Hindi\thi\n" +
    //            "                Hungarian\thu\n" +
    //            "                Icelandic\tis\n" +
    //            "                Indonesian\tid\n" +
    //            "                Italian\tit\n" +
    //            "                Japanese\tja\n" +
    //            "                Kannada\tkn\n" +
    //            "                Kazakh\tkk\n" +
    //            "                Korean\tko\n" +
    //            "                Latvian\tlv\n" +
    //            "                Lithuanian\tlt\n" +
    //            "                Macedonian\tmk\n" +
    //            "                Malay\tms\n" +
    //            "                Malayalam\tml\n" +
    //            "                Maltese\tmt\n" +
    //            "                Mongolian\tmn\n" +
    //            "                Norwegian\tno\n" +
    //            "                Persian\tfa\n" +
    //            "                Pashto\tps\n" +
    //            "                Polish\tpl\n" +
    //            "                Portuguese\tpt\n" +
    //            "                Romanian\tro\n" +
    //            "                Russian\tru\n" +
    //            "                Serbian\tsr\n" +
    //            "                Sinhala\tsi\n" +
    //            "                Slovak\tsk\n" +
    //            "                Slovenian\tsl\n" +
    //            "                Somali\tso\n" +
    //            "                Spanish\tes\n" +
    //            "                Spanish (Mexico)\tes-MX\n" +
    //            "                Swahili\tsw\n" +
    //            "                Swedish\tsv\n" +
    //            "                Tagalog\ttl\n" +
    //            "                Tamil\tta\n" +
    //            "                Telugu\tte\n" +
    //            "                Thai\tth\n" +
    //            "                Turkish\ttr\n" +
    //            "                Ukrainian\tuk\n" +
    //            "                Urdu\tur\n" +
    //            "                Uzbek\tuz\n" +
    //            "                Vietnamese\tvi\n" +
    //            "                Welsh\tcy";
    //
    //    Cc.stream(langs.split("\n"))
    //            .filter(St::isNotNullOrEmpty)
    //            .map($ -> X.x($.split("	")[0], $.split("	")[1]))
    //            .sorted(Comparator.comparing($ -> $.i1().trim()))
    //            //.filter($->!$.i1().contains(" ") && !$.i1().contains("("))
    //            .map($ -> "                case " + "\"" + $.i2() + "\"" + ":return " + $.i1().trim() + ";")
    //            .forEach($ -> System.out.println($));
    //}
}
