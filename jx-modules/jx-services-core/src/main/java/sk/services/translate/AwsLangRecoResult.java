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
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class AwsLangRecoResult {
    List<AwsLangRecoItem> langRecoResult;
    boolean fictive;

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class AwsLangRecoItem {
        private LangType lang;
        private float val;

        public int size() {
            return lang.toString().length() * 2 + 4 + 4;
        }
    }

    public int size() {
        return langRecoResult.stream().mapToInt($ -> $.size()).sum();
    }
}
