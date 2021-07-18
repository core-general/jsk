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
import software.amazon.awssdk.services.comprehend.ComprehendClient;
import software.amazon.awssdk.services.comprehend.model.DetectDominantLanguageRequest;
import software.amazon.awssdk.services.translate.TranslateClient;
import software.amazon.awssdk.services.translate.model.TranslateTextRequest;
import software.amazon.awssdk.services.translate.model.TranslateTextResponse;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Comparator;

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
        return LangType.getByCode(langCode);
    }

    @Override
    public TranslateInfo translate(LangType from, LangType to, Text2Translate text) {
        final TranslateTextResponse result = translate.translateText(TranslateTextRequest.builder()
                .sourceLanguageCode(from.getCode())
                .targetLanguageCode(to.getCode())
                .text(text.getTxt())
                .build());

        return new TranslateInfo(
                LangType.getByCode(result.sourceLanguageCode()),
                LangType.getByCode(result.targetLanguageCode()),
                text.getTxt(),
                result.translatedText()
        );
    }

    //public static void main(String[] args) {
    //    final List<LangType> collect = Arrays.stream(values())
    //            .filter($ -> !Fu.equal($.getCode(), $.getCode()))
    //            .collect(Cc.toL());
    //    int i = 0;
    //
    //    //var langs = "Afrikaans\taf\n" +
    //    //        "                Albanian\tsq\n" +
    //    //        "                Amharic\tam\n" +
    //    //        "                Arabic\tar\n" +
    //    //        "                Armenian\thy\n" +
    //    //        "                Azerbaijani\taz\n" +
    //    //        "                Bengali\tbn\n" +
    //    //        "                Bosnian\tbs\n" +
    //    //        "                Bulgarian\tbg\n" +
    //    //        "                Catalan\tca\n" +
    //    //        "                Chinese (Simplified)\tzh\n" +
    //    //        "                Chinese (Traditional)\tzh-TW\n" +
    //    //        "                Croatian\thr\n" +
    //    //        "                Czech\tcs\n" +
    //    //        "                Danish\tda\n" +
    //    //        "                Dari\tfa-AF\n" +
    //    //        "                Dutch\tnl\n" +
    //    //        "                English\ten\n" +
    //    //        "                Estonian\tet\n" +
    //    //        "                Farsi (Persian)\tfa\n" +
    //    //        "                Filipino Tagalog\ttl\n" +
    //    //        "                Finnish\tfi\n" +
    //    //        "                French\tfr\n" +
    //    //        "                French (Canada)\tfr-CA\n" +
    //    //        "                Georgian\tka\n" +
    //    //        "                German\tde\n" +
    //    //        "                Greek\tel\n" +
    //    //        "                Gujarati\tgu\n" +
    //    //        "                Haitian Creole\tht\n" +
    //    //        "                Hausa\tha\n" +
    //    //        "                Hebrew\the\n" +
    //    //        "                Hindi\thi\n" +
    //    //        "                Hungarian\thu\n" +
    //    //        "                Icelandic\tis\n" +
    //    //        "                Indonesian\tid\n" +
    //    //        "                Italian\tit\n" +
    //    //        "                Japanese\tja\n" +
    //    //        "                Kannada\tkn\n" +
    //    //        "                Kazakh\tkk\n" +
    //    //        "                Korean\tko\n" +
    //    //        "                Latvian\tlv\n" +
    //    //        "                Lithuanian\tlt\n" +
    //    //        "                Macedonian\tmk\n" +
    //    //        "                Malay\tms\n" +
    //    //        "                Malayalam\tml\n" +
    //    //        "                Maltese\tmt\n" +
    //    //        "                Mongolian\tmn\n" +
    //    //        "                Norwegian\tno\n" +
    //    //        "                Persian\tfa\n" +
    //    //        "                Pashto\tps\n" +
    //    //        "                Polish\tpl\n" +
    //    //        "                Portuguese\tpt\n" +
    //    //        "                Romanian\tro\n" +
    //    //        "                Russian\tru\n" +
    //    //        "                Serbian\tsr\n" +
    //    //        "                Sinhala\tsi\n" +
    //    //        "                Slovak\tsk\n" +
    //    //        "                Slovenian\tsl\n" +
    //    //        "                Somali\tso\n" +
    //    //        "                Spanish\tes\n" +
    //    //        "                Spanish (Mexico)\tes-MX\n" +
    //    //        "                Swahili\tsw\n" +
    //    //        "                Swedish\tsv\n" +
    //    //        "                Tagalog\ttl\n" +
    //    //        "                Tamil\tta\n" +
    //    //        "                Telugu\tte\n" +
    //    //        "                Thai\tth\n" +
    //    //        "                Turkish\ttr\n" +
    //    //        "                Ukrainian\tuk\n" +
    //    //        "                Urdu\tur\n" +
    //    //        "                Uzbek\tuz\n" +
    //    //        "                Vietnamese\tvi\n" +
    //    //        "                Welsh\tcy";
    //    //
    //    //Cc.stream(langs.split("\n"))
    //    //        .filter(St::isNotNullOrEmpty)
    //    //        .map($ -> X.x($.split("	")[0], $.split("	")[1]))
    //    //        .sorted(Comparator.comparing($ -> $.i1().trim()))
    //    //        //.filter($->!$.i1().contains(" ") && !$.i1().contains("("))
    //    //        .map($ -> "                case " + "\"" + $.i2() + "\"" + ":return " + $.i1().trim() + ";")
    //    //        .forEach($ -> System.out.println($));
    //}
}
