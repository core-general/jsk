package jsk.outer.fb.msger;

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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import sk.utils.functional.O;

import static sk.utils.functional.O.of;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MgcFbSpecial {
    O<String> video;

    public O<String> getVideo() {
        return video;
    }

    public static MgcFbSpecial select(O<String> video) {
        return new MgcFbSpecial(video);
    }

    public static MgcFbSpecial video(String video) {
        return new MgcFbSpecial(of(video));
    }
}
