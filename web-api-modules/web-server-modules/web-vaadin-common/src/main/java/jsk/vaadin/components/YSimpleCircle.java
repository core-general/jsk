package jsk.vaadin.components;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2023 Core General
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

import sk.services.free.IFree;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Im;

import java.awt.*;

public class YSimpleCircle extends YSvg {
    public YSimpleCircle(IFree free, int size, int margin, Color color, O<String> tooltip) {
        super(free.processByTextHtml("""
                        <svg xmlns="http://www.w3.org/2000/svg" width="${size+margin}" height="${size}"><circle cx="${size/2}" cy="${size/2}" r="${size/2}" fill="${color}"/></svg>""",
                Cc.m("size", size, "margin", margin, "color", Im.toHexColor(color, true)), true), tooltip);
    }
}
