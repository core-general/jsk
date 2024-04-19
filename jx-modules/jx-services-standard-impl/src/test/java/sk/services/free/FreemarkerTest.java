package sk.services.free;

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

import org.junit.jupiter.api.Test;
import sk.services.CoreServicesRaw;
import sk.utils.statics.Cc;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FreemarkerTest {

    @Test
    public void processByText() {
        CoreServicesRaw icore = new CoreServicesRaw();
        Freemarker free = (Freemarker) icore.free();

        final String templateText1 = "${abc}1";
        final String templateText2 = "${abc}3";

        assertEquals(free.processByText(templateText1, Cc.m("abc", "5"), true), "51");
        assertEquals(free.processByText(templateText1, Cc.m("abc", "7"), true), "71");
        assertEquals(free.processByText("${abc}2", Cc.m("abc", "7"), false), "72");
        assertEquals(free.processByText(templateText2, Cc.m("abc", "7"), true), "73");


        String id1 = icore.ids().unique(templateText1);
        String id2 = icore.ids().unique(templateText2);
        assertEquals(Cc.join(free.templateCache.keySet().stream().sorted()), id1 + "," + id2);
    }
}
