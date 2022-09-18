package sk.services.ids;

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

import sk.services.rand.IRand;
import sk.services.rand.RandTextGenerator;
import sk.utils.statics.Cc;
import sk.utils.statics.St;

import java.util.List;

public class JskHaikunator {
    final RandTextGenerator rng;

    public final static List<String> adjs = Cc.l(
            "aged", "adorable", "adventurous", "ancient", "autumn", "billowing",
            "bitter", "black", "blue", "bold",
            "broad", "broken", "calm", "cold", "cool", "crimson", "curly", "damp",
            "dark", "dawn", "delicate", "divine", "dry", "empty", "falling", "fancy",
            "flat", "floral", "fragrant", "frosty", "funny", "gentle", "green", "helpful",
            "hidden", "hilarious", "holy",
            "icy", "jolly", "late", "lingering", "little", "lively", "long", "lucky",
            "misty", "morning", "muddy", "mute", "nameless", "noisy", "odd", "old",
            "orange", "patient", "plain", "polished", "proud", "purple", "quiet", "rapid",
            "raspy", "red", "restless", "rough", "round", "royal", "shiny", "shrill",
            "shy", "silent", "small", "snowy", "soft", "solitary", "sparkling", "spring",
            "square", "steep", "still", "summer", "super", "sweet", "throbbing", "tight",
            "tiny", "twilight", "wandering", "weathered", "white", "wild", "winter", "wispy",
            "withered", "yellow", "young");
    public final static List<String> nouns = Cc.l(
            "art", "band", "bar", "base", "bird", "block", "boat", "bonus",
            "bread", "breeze", "brook", "bush", "butterfly", "cake", "cell", "cherry",
            "cloud", "credit", "darkness", "dawn", "dew", "disk", "dream", "dust",
            "feather", "field", "fire", "firefly", "flower", "fog", "forest", "frog",
            "frost", "glade", "glitter", "grass", "hall", "hat", "haze", "heart",
            "hill", "king", "lab", "lake", "leaf", "limit", "math", "meadow",
            "mode", "moon", "morning", "mountain", "mouse", "mud", "night", "paper",
            "pine", "poetry", "pond", "queen", "rain", "recipe", "resonance", "rice",
            "river", "salad", "scene", "sea", "shadow", "shape", "silence", "sky",
            "smoke", "snow", "snowflake", "sound", "star", "sun", "sun", "sunset",
            "surf", "term", "thunder", "tooth", "tree", "truth", "union", "unit",
            "violet", "voice", "water", "water", "waterfall", "wave", "wildflower", "wind",
            "wood"
    );


    /** Default impl has ~1 Quadrillion combinations */
    public static LongAndShortHaikunator defaultHaikunators(IRand rand) {
        return new LongAndShortHaikunator(
                new JskHaikunator(rand, true, 6, St.engENGDig, adjs, nouns),//~9Q ~ 9*10^15
                new JskHaikunator(rand, false, 3, St.engENGDig, adjs, nouns)// ~2M ~2*10^9
        );
    }

    public JskHaikunator(IRand rand, String pattern, int tokenLength, String tokenChars, List<String> adjs, List<String> nouns) {
        rng = new RandTextGenerator(
                Cc.l(
                        new RandTextGenerator.VocabItemByListImpl('A', adjs),
                        new RandTextGenerator.VocabItemByListImpl('N', nouns),
                        new RandTextGenerator.VocabItemImpl('R', iRand -> iRand.rndString(tokenLength, tokenChars)))
                , Cc.l(pattern)
                , rand);
    }

    public JskHaikunator(IRand rand, boolean twoAdj, int tokenLength, String tokenChars, List<String> adjs, List<String> nouns) {
        this(rand, twoAdj ? "A-A-N-R" : "A-N-R", tokenLength, tokenChars, adjs, nouns);
    }

    public JskHaikunator(IRand rand, boolean twoAdj, int tokenLength, String tokenChars) {
        this(rand, twoAdj, tokenLength, tokenChars, adjs, nouns);
    }


    public JskHaikunator(IRand rand, String pattern, int tokenLength, String tokenChars) {
        this(rand, pattern, tokenLength, tokenChars, adjs, nouns);
    }

    public String haikunate() {
        return rng.generateNext();
    }

    public static record LongAndShortHaikunator(JskHaikunator lng, JskHaikunator shrt) {}
}
