package sk.web.swagger.mvn;

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
import lombok.Getter;
import sk.utils.functional.O;

@AllArgsConstructor
@Getter
public enum WebSwaggerMavenGeneratorTypes {
    DART("dart", O.empty()),
    DART_DIO("dart-dio", O.of("dart-dio-mod")),
    DART_DIO_NEXT("dart-dio-next", O.of("dart-dio-next-mod")),
    TYPE_SCRIPT_JQ("typescript-jquery", O.empty()),
    TYPE_SCRIPT_AXIOS("typescript-axios", O.empty()),

    ;

    String generatorName;
    O<String> templatePath;
}
