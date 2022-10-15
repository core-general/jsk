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
import sk.services.time.ITime;
import sk.utils.statics.Cc;
import sk.utils.statics.St;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.CRC32C;
import java.util.zip.Checksum;

public class JskHaikunator {
    final RandTextGenerator rng;


    /**
     * Long: ~10^19
     * Short: ~3*10^10
     * Tiny: ~6*10^5
     */
    public static LongAndShortHaikunator defaultHaikunators(IRand rand, ITime times) {
        return new LongAndShortHaikunator(
                new JskHaikunator(rand, times, true, 6, St.engENGDig, adjs, nouns),// ~ 1.7*10^19
                new JskHaikunator(rand, times, false, 3, St.engDig, adjs, nouns),// ~ 3*10^10 ~30B
                new JskHaikunator(rand, times, "A-N", 0, St.engENGDig, adjs, nouns), // ~ 6*10^5
                new JskHaikunator(rand, times, "A-N-T", 0, St.engENGDig, adjs, nouns) // ~  6*10^5 * based on current time
        );
    }

    public JskHaikunator(IRand rand, ITime times, String pattern, int tokenLength, String tokenChars, List<String> adjs,
            List<String> nouns) {
        rng = new RandTextGenerator(
                Cc.l(
                        new RandTextGenerator.VocabItemByListImpl('A', adjs),
                        new RandTextGenerator.VocabItemByListImpl('N', nouns),
                        new RandTextGenerator.VocabItemImpl('R', iRand -> iRand.rndString(tokenLength, tokenChars)),
                        new RandTextGenerator.VocabItemImpl('T', iRand -> String.valueOf(times.now())))

                , Cc.l(pattern)
                , rand);
    }

    public JskHaikunator(IRand rand, ITime times, boolean twoAdj, int tokenLength, String tokenChars, List<String> adjs,
            List<String> nouns) {
        this(rand, times, twoAdj ? "A-A-N-R" : "A-N-R", tokenLength, tokenChars, adjs, nouns);
    }

    public JskHaikunator(IRand rand, ITime times, boolean twoAdj, int tokenLength, String tokenChars) {
        this(rand, times, twoAdj, tokenLength, tokenChars, adjs, nouns);
    }


    public JskHaikunator(IRand rand, ITime times, String pattern, int tokenLength, String tokenChars) {
        this(rand, times, pattern, tokenLength, tokenChars, adjs, nouns);
    }

    public String haikunate() {
        return rng.generateNext();
    }


    public Set<String> haikunate(int sizeOfResultSet) {
        return IntStream.generate(() -> 0)
                .mapToObj($ -> haikunate())
                .distinct()
                .limit(sizeOfResultSet)
                .collect(Collectors.toSet());
    }

    public static record LongAndShortHaikunator(JskHaikunator lng,
                                                JskHaikunator shrt,
                                                JskHaikunator tiny,
                                                JskHaikunator timed) {}

    public static String toShortHaiku(String val) {
        final byte[] bytes = val.getBytes();

        Checksum checksum = new CRC32C();
        checksum.update(bytes, 0, bytes.length);
        long val1 = checksum.getValue();

        bytes[0] = (byte) (bytes[0] + (byte) 1);
        checksum = new CRC32C();
        checksum.update(bytes, 0, bytes.length);
        long val2 = checksum.getValue();

        // short 16 adjs
        // short 16 nouns
        // byte  8 engDig
        // byte  8 engDig
        // byte  8 engDig

        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putInt((int) val1).putInt((int) val2);

        final int adjsIndex = ByteBuffer.allocate(Integer.BYTES).putShort(2, buffer.getShort(1)).getInt();
        final int nounsIndex = ByteBuffer.allocate(Integer.BYTES).putShort(2, buffer.getShort(3)).getInt();
        final short engDig1 = ByteBuffer.allocate(Short.BYTES).put(1, buffer.get(5)).getShort();
        final short engDig2 = ByteBuffer.allocate(Short.BYTES).put(1, buffer.get(6)).getShort();
        final short engDig3 = ByteBuffer.allocate(Short.BYTES).put(1, buffer.get(7)).getShort();

        return adjs.get(adjsIndex % adjs.size()) + "-" + nouns.get(nounsIndex % nouns.size())
                + "-"
                + St.engDig.charAt(engDig1 % St.engDig.length())
                + St.engDig.charAt(engDig2 % St.engDig.length())
                + St.engDig.charAt(engDig3 % St.engDig.length());
    }

    //region ADJS + NOUNS
    public final static List<String> adjs = /*~500*/Collections.unmodifiableList(
            Cc.l("able", "accurate", "action", "active", "actual", "adorable", "adult", "afraid", "after", "aged", "agent", "ago",
                    "airline", "alive", "all", "alone", "amazing", "ancient", "angry", "animal", "annual", "another", "anxious",
                    "any", "apart", "asleep", "autumn", "aware", "away", "basic", "best", "better", "big", "bitter", "black",
                    "blue", "bold", "boring", "born", "both", "brave", "brief", "bright", "broad", "broken", "brown", "budget",
                    "business", "busy", "calm", "capable", "capital", "car", "careful", "certain", "chance", "cheap", "chemical",
                    "chicken", "choice", "civil", "classic", "clean", "clear", "close", "cold", "common", "complete", "complex",
                    "connect", "constant", "content", "cool", "corner", "correct", "crazy", "creative", "crimson", "critical",
                    "cultural", "curious", "curly", "current", "cute", "damp", "dark", "daughter", "dawn", "day", "dead", "dear",
                    "decent", "deep", "delicate", "designer", "direct", "dirty", "distinct", "divine", "double", "downtown",
                    "dramatic", "dress", "drunk", "dry", "due", "each", "east", "eastern", "easy", "economy", "either", "empty",
                    "enough", "entire", "equal", "even", "evening", "every", "exact", "exciting", "existing", "expert", "express",
                    "external", "extra", "extreme", "fair", "falling", "false", "familiar", "famous", "fancy", "far", "fast",
                    "fat", "federal", "feeling", "female", "few", "final", "fine", "firm", "first", "fit", "flat", "floral",
                    "foreign", "formal", "former", "forward", "fragrant", "free", "frequent", "fresh", "friendly", "front",
                    "frosty", "full", "fun", "funny", "future", "game", "general", "gentle", "glad", "glass", "global", "gold",
                    "good", "grand", "great", "green", "gross", "guilty", "happy", "hard", "head", "healthy", "heavy", "helpful",
                    "hidden", "high", "his", "holiday", "holy", "home", "honest", "horror", "hot", "hour", "house", "huge",
                    "human", "hungry", "icy", "ideal", "ill", "illegal", "incident", "informal", "initial", "inner", "inside",
                    "internal", "joint", "jolly", "junior", "just", "key", "kind", "kitchen", "known", "large", "last", "late",
                    "latter", "leading", "least", "leather", "left", "legal", "less", "level", "life", "little", "live", "lively",
                    "living", "local", "logical", "lonely", "long", "loose", "lost", "loud", "low", "lower", "lucky", "mad",
                    "main", "major", "male", "many", "massive", "master", "material", "maximum", "mean", "medical", "medium",
                    "mental", "middle", "minimum", "minor", "minute", "mission", "misty", "mobile", "money", "more", "morning",
                    "most", "mother", "motor", "mountain", "much", "muddy", "mute", "nameless", "narrow", "nasty", "national",
                    "native", "natural", "nearby", "neat", "negative", "neither", "nervous", "new", "next", "nice", "no", "noisy",
                    "normal", "north", "novel", "numerous", "obvious", "odd", "official", "ok", "old", "one", "only", "open",
                    "opening", "opposite", "orange", "ordinary", "original", "other", "outside", "over", "overall", "own",
                    "parking", "party", "past", "patient", "perfect", "period", "personal", "physical", "plain", "plane",
                    "plastic", "pleasant", "plenty", "plus", "polished", "poor", "popular", "positive", "possible", "powerful",
                    "pregnant", "present", "pretend", "pretty", "previous", "primary", "prior", "private", "prize", "proof",
                    "proper", "proud", "public", "pure", "purple", "quick", "quiet", "rapid", "rare", "raspy", "raw", "ready",
                    "real", "recent", "red", "regular", "relative", "relevant", "remote", "resident", "restless", "rich", "right",
                    "rough", "round", "routine", "royal", "sad", "safe", "salt", "same", "savings", "scared", "sea", "secret",
                    "secure", "select", "senior", "separate", "serious", "several", "severe", "sharp", "shiny", "short",
                    "shot", "shrill", "shy", "sick", "signal", "silent", "silly", "silver", "similar", "simple", "single",
                    "slight", "slow", "small", "smart", "smooth", "snowy", "soft", "solid", "solitary", "some", "sorry", "south",
                    "southern", "spare", "special", "specific", "spring", "square", "standard", "status", "steep", "still",
                    "stock", "straight", "strange", "street", "strict", "strong", "stupid", "subject", "such", "sudden",
                    "suitable", "summer", "super", "sure", "sweet", "swimming", "tall", "terrible", "that", "then", "these",
                    "thick", "thin", "think", "this", "tight", "time", "tiny", "top", "total", "tough", "training", "trick",
                    "true", "twilight", "typical", "ugly", "unable", "unfair", "unhappy", "unique", "united", "unlikely",
                    "unusual", "upper", "upset", "upstairs", "used", "useful", "usual", "valuable", "various", "vast", "visible",
                    "visual", "warm", "waste", "weak", "weekly", "weird", "west", "western", "what", "which", "white", "whole",
                    "wide", "wild", "willing", "wine", "winter", "wise", "wispy", "withered", "wooden", "work", "working",
                    "worth", "wrong", "yellow", "young"));
    public final static List<String> nouns = Collections.unmodifiableList( /*~1200*/
            Cc.l("ability", "abroad", "abuse", "access", "account", "act", "action", "active", "actor", "address", "adult",
                    "advance", "advice", "affair", "affect", "age", "agency", "agent", "air", "airline", "airport", "alarm",
                    "alcohol", "amount", "analyst", "anger", "angle", "animal", "annual", "answer", "anxiety", "anybody",
                    "appeal", "apple", "area", "arm", "army", "arrival", "art", "article", "aside", "ask", "aspect", "assist",
                    "attack", "attempt", "author", "average", "award", "baby", "back", "bad", "bag", "bake", "balance", "ball",
                    "band", "bank", "bar", "base", "basis", "basket", "bat", "bath", "battle", "beach", "bear", "beat", "bed",
                    "bedroom", "beer", "being", "bell", "belt", "bench", "bend", "benefit", "bet", "beyond", "bicycle", "bid",
                    "big", "bike", "bill", "bird", "birth", "bit", "bite", "bitter", "black", "blame", "blank", "blind", "block",
                    "blood", "blow", "blue", "board", "boat", "body", "bone", "bonus", "book", "boot", "border", "boss", "bother",
                    "bottle", "bottom", "bowl", "box", "boy", "brain", "branch", "brave", "bread", "break", "breast", "breath",
                    "breeze", "brick", "bridge", "brief", "broad", "brook", "brother", "brown", "brush", "buddy", "budget", "bug",
                    "bunch", "burn", "bus", "bush", "button", "buy", "buyer", "cabinet", "cable", "cake", "call", "calm",
                    "camera", "camp", "can", "cancel", "cancer", "candle", "candy", "cap", "capital", "car", "card", "care",
                    "career", "carpet", "carry", "case", "cash", "cat", "catch", "cause", "cell", "chain", "chair", "chance",
                    "change", "channel", "chapter", "charge", "charity", "chart", "check", "cheek", "cherry", "chest", "chicken",
                    "child", "chip", "choice", "church", "city", "claim", "class", "classic", "clerk", "click", "client",
                    "climate", "clock", "closet", "clothes", "cloud", "club", "clue", "coach", "coast", "coat", "code", "coffee",
                    "cold", "collar", "college", "combine", "comfort", "command", "comment", "common", "company", "complex",
                    "concept", "concern", "concert", "consist", "contact", "contest", "context", "control", "convert", "cook",
                    "cookie", "copy", "corner", "cost", "count", "counter", "country", "county", "couple", "courage", "course",
                    "court", "cousin", "cover", "cow", "crack", "craft", "crash", "crazy", "cream", "credit", "crew", "cross",
                    "cry", "culture", "cup", "current", "curve", "cut", "cycle", "dad", "damage", "dance", "dare", "dark", "data",
                    "date", "dawn", "day", "dead", "deal", "dealer", "dear", "death", "debate", "debt", "deep", "degree", "delay",
                    "demand", "deposit", "depth", "design", "desire", "desk", "detail", "device", "devil", "dew", "diamond",
                    "diet", "dig", "dinner", "dirt", "disease", "dish", "disk", "display", "divide", "doctor", "dog", "door",
                    "dot", "double", "doubt", "draft", "drag", "drama", "draw", "drawer", "drawing", "dream", "dress", "drink",
                    "drive", "driver", "drop", "drunk", "due", "dump", "dust", "duty", "ear", "earth", "ease", "east", "eat",
                    "economy", "edge", "editor", "effect", "effort", "egg", "emotion", "employ", "end", "energy", "engine",
                    "entry", "equal", "error", "escape", "essay", "estate", "evening", "event", "exam", "example", "excuse",
                    "exit", "expert", "extent", "extreme", "eye", "face", "fact", "factor", "fail", "failure", "fall", "family",
                    "fan", "farm", "farmer", "fat", "father", "fault", "fear", "feather", "feature", "fee", "feed", "feel",
                    "feeling", "female", "few", "field", "fight", "figure", "file", "fill", "film", "final", "finance", "finding",
                    "finger", "finish", "fire", "firefly", "fish", "fishing", "fix", "flight", "floor", "flow", "flower", "fly",
                    "focus", "fog", "fold", "food", "foot", "force", "forest", "forever", "form", "formal", "fortune", "frame",
                    "freedom", "friend", "frog", "front", "frost", "fruit", "fuel", "fun", "funeral", "funny", "future", "gain",
                    "game", "gap", "garage", "garbage", "garden", "gas", "gate", "gather", "gear", "gene", "general", "gift",
                    "girl", "give", "glad", "glade", "glass", "glitter", "glove", "goal", "god", "gold", "golf", "good", "grab",
                    "grade", "grand", "grass", "great", "green", "grocery", "ground", "group", "growth", "guard", "guess",
                    "guest", "guide", "guitar", "guy", "habit", "hair", "half", "hall", "hand", "handle", "hang", "harm", "hat",
                    "hate", "haze", "head", "health", "hearing", "heart", "heat", "heavy", "height", "hell", "hello", "help",
                    "hide", "high", "highway", "hill", "hire", "history", "hit", "hold", "hole", "holiday", "home", "honey",
                    "hook", "hope", "horror", "horse", "host", "hotel", "hour", "house", "housing", "human", "hunt", "hurry",
                    "hurt", "husband", "ice", "idea", "ideal", "illegal", "image", "impact", "impress", "income", "initial",
                    "injury", "insect", "inside", "invite", "iron", "island", "issue", "item", "jacket", "job", "join", "joint",
                    "joke", "judge", "juice", "jump", "junior", "jury", "keep", "key", "kick", "kid", "kill", "kind", "king",
                    "kiss", "kitchen", "knee", "knife", "lab", "lack", "ladder", "lady", "lake", "land", "laugh", "law", "lawyer",
                    "lay", "layer", "lead", "leader", "leading", "leaf", "league", "leather", "leave", "lecture", "leg", "length",
                    "lesson", "let", "letter", "level", "library", "lie", "life", "lift", "light", "limit", "line", "link", "lip",
                    "list", "listen", "living", "load", "loan", "local", "lock", "log", "long", "look", "loss", "love", "low",
                    "luck", "lunch", "machine", "mail", "main", "major", "make", "male", "mall", "man", "manager", "manner",
                    "many", "map", "march", "mark", "market", "master", "match", "mate", "math", "matter", "maximum", "maybe",
                    "meadow", "meal", "meaning", "meat", "media", "medium", "meet", "meeting", "member", "memory", "mention",
                    "menu", "mess", "message", "metal", "method", "middle", "might", "milk", "mind", "mine", "minimum", "minor",
                    "minute", "mirror", "miss", "mission", "mistake", "mix", "mixture", "mobile", "mode", "model", "mom",
                    "moment", "money", "monitor", "month", "mood", "moon", "morning", "most", "mother", "motor", "mouse", "mouth",
                    "move", "movie", "mud", "muscle", "music", "nail", "name", "nasty", "nation", "native", "natural", "nature",
                    "neat", "neck", "nerve", "net", "network", "news", "night", "nobody", "noise", "normal", "north", "nose",
                    "note", "nothing", "notice", "novel", "number", "nurse", "object", "offer", "office", "officer", "oil", "one",
                    "opening", "opinion", "option", "orange", "order", "other", "outcome", "outside", "oven", "owner", "pace",
                    "pack", "package", "page", "pain", "paint", "pair", "panic", "paper", "parent", "park", "parking", "part",
                    "partner", "party", "pass", "passage", "passion", "past", "path", "patient", "pattern", "pause", "pay",
                    "payment", "peace", "peak", "pen", "penalty", "pension", "people", "period", "permit", "person", "phase",
                    "phone", "photo", "phrase", "physics", "piano", "pick", "picture", "pie", "piece", "pin", "pine", "pipe",
                    "pitch", "pizza", "place", "plan", "plane", "plant", "plastic", "plate", "play", "player", "plenty", "poem",
                    "poet", "poetry", "point", "police", "policy", "pond", "pool", "pop", "post", "pot", "potato", "pound",
                    "power", "present", "press", "price", "pride", "priest", "primary", "print", "prior", "private", "prize",
                    "problem", "process", "produce", "product", "profile", "profit", "program", "project", "promise", "prompt",
                    "proof", "public", "pull", "punch", "purple", "purpose", "push", "put", "quality", "quarter", "queen",
                    "quiet", "quit", "quote", "race", "radio", "rain", "raise", "range", "rate", "ratio", "raw", "reach", "read",
                    "reading", "reality", "reason", "recipe", "record", "recover", "red", "refuse", "region", "regret", "regular",
                    "release", "relief", "remote", "remove", "rent", "repair", "repeat", "reply", "report", "request", "reserve",
                    "resist", "resolve", "resort", "respect", "respond", "rest", "result", "return", "reveal", "revenue",
                    "review", "reward", "rice", "rich", "ride", "ring", "rip", "rise", "risk", "river", "road", "rock", "role",
                    "roll", "roof", "room", "rope", "rough", "round", "routine", "row", "royal", "rub", "ruin", "rule", "run",
                    "rush", "sad", "safe", "safety", "sail", "salad", "salary", "sale", "salt", "sample", "sand", "save",
                    "savings", "scale", "scene", "scheme", "school", "science", "score", "scratch", "screen", "screw", "script",
                    "sea", "search", "season", "seat", "second", "secret", "section", "sector", "self", "sell", "senior", "sense",
                    "series", "serve", "service", "session", "set", "setting", "shadow", "shake", "shame", "shape",
                    "share", "she", "shelter", "shift", "shine", "ship", "shirt", "shock", "shoe", "shoot", "shop", "shot",
                    "show", "shower", "sick", "side", "sign", "signal", "silence", "silly", "silver", "simple", "sing", "singer",
                    "single", "sink", "sir", "sister", "site", "size", "skill", "skin", "skirt", "sky", "sleep", "slice", "slide",
                    "slip", "smell", "smile", "smoke", "snow", "society", "sock", "soft", "soil", "solid", "son", "song", "sort",
                    "sound", "soup", "source", "south", "space", "spare", "speaker", "special", "speech", "speed", "spell",
                    "spend", "spirit", "spite", "split", "sport", "spot", "spray", "spread", "spring", "square", "stable",
                    "staff", "stage", "stand", "star", "start", "state", "station", "status", "stay", "steak", "steal", "step",
                    "stick", "still", "stock", "stomach", "stop", "storage", "store", "storm", "story", "strain", "street",
                    "stress", "stretch", "strike", "string", "strip", "stroke", "student", "studio", "study", "stuff", "stupid",
                    "style", "subject", "success", "suck", "sugar", "suit", "summer", "sun", "sunset", "support", "surf",
                    "surgery", "survey", "suspect", "sweet", "swim", "swing", "switch", "system", "table", "tackle", "tale",
                    "talk", "tank", "tap", "target", "task", "taste", "tax", "tea", "teach", "teacher", "team", "tear", "tell",
                    "tennis", "tension", "term", "test", "text", "thanks", "theme", "theory", "thing", "thought", "throat",
                    "thunder", "ticket", "tie", "till", "time", "tip", "title", "today", "toe", "tone", "tongue", "tonight",
                    "tool", "tooth", "top", "topic", "total", "touch", "tough", "tour", "tourist", "towel", "tower", "town",
                    "track", "trade", "traffic", "train", "trainer", "trash", "travel", "treat", "tree", "trick", "trip",
                    "trouble", "truck", "trust", "truth", "try", "tune", "turn", "twist", "two", "type", "uncle", "union",
                    "unique", "unit", "upper", "use", "user", "usual", "value", "variety", "vast", "vehicle", "version", "video",
                    "view", "village", "violet", "virus", "visit", "visual", "voice", "volume", "wait", "wake", "walk", "wall",
                    "war", "warning", "wash", "watch", "water", "wave", "way", "wealth", "wear", "weather", "web", "wedding",
                    "week", "weekend", "weight", "weird", "welcome", "west", "western", "wheel", "whereas", "while", "white",
                    "whole", "wife", "will", "win", "wind", "window", "wine", "wing", "winner", "winter", "wish", "witness",
                    "woman", "wonder", "wood", "word", "work", "worker", "working", "world", "worry", "worth", "wrap", "writer",
                    "writing", "yard", "year", "yellow", "you", "young", "youth", "zone"));
    //endregion
}
