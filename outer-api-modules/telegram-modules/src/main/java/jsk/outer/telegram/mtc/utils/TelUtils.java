package jsk.outer.telegram.mtc.utils;

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

import com.pengrad.telegrambot.model.request.*;
import sk.utils.statics.Cc;

import java.util.List;

public class TelUtils {
    public static Keyboard toKeyboard(List<String> buttons, int columns) {
        return toKeyboard(buttons, columns, Cc.l());
    }

    public static Keyboard toKeyboard(List<String> buttons, int columns, List<String> singleColumnButtons) {
        singleColumnButtons = singleColumnButtons == null ? Cc.l() : singleColumnButtons;

        List<String[]> collect = Cc.l();
        for (int i = 0; i < buttons.size(); i = i + columns) {
            int elementsLeft = Math.min(buttons.size() - i, columns);
            String[] val = new String[elementsLeft];
            for (int j = 0; j < elementsLeft; j++) {
                val[j] = buttons.get(i + j);
            }
            collect.add(val);
        }

        collect.addAll(singleColumnButtons.stream().map(x -> new String[]{x}).collect(Cc.toL()));
        if (collect.stream().flatMap($ -> Cc.stream($)).count() == 0) {
            return new ReplyKeyboardRemove();
        } else {
            return new ReplyKeyboardMarkup(collect.toArray(new String[collect.size()][])).resizeKeyboard(true);
        }
    }

    public static InlineKeyboardMarkup toInlineKeyboard(List<InlineKeyboardButton> buttons, int columns) {
        return toInlineKeyboard(buttons, columns, Cc.l());
    }

    public static InlineKeyboardMarkup toInlineKeyboard(List<InlineKeyboardButton> buttons, int columns,
            List<InlineKeyboardButton> singleColumnButtons) {
        singleColumnButtons = singleColumnButtons == null ? Cc.l() : singleColumnButtons;

        List<InlineKeyboardButton[]> collect = Cc.l();
        for (int i = 0; i < buttons.size(); i = i + columns) {
            int elementsLeft = Math.min(buttons.size() - i, columns);
            InlineKeyboardButton[] val = new InlineKeyboardButton[elementsLeft];
            for (int j = 0; j < elementsLeft; j++) {
                val[j] = buttons.get(i + j);
            }
            collect.add(val);
        }

        collect.addAll(singleColumnButtons.stream().map(x -> new InlineKeyboardButton[]{x}).collect(Cc.toL()));
        return new InlineKeyboardMarkup(collect.toArray(new InlineKeyboardButton[collect.size()][]));
    }
}
