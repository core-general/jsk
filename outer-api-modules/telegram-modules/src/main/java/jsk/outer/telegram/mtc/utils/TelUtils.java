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

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import sk.utils.statics.Cc;

import java.util.List;

public class TelUtils {
    public static ReplyKeyboard toKeyboard(List<String> buttons, int columns) {
        return toKeyboard(buttons, columns, Cc.l());
    }

    public static ReplyKeyboard toKeyboard(List<String> buttons, int columns, List<String> singleColumnButtons) {
        singleColumnButtons = singleColumnButtons == null ? Cc.l() : singleColumnButtons;

        List<KeyboardRow> rows = Cc.l();
        for (int i = 0; i < buttons.size(); i = i + columns) {
            int elementsLeft = Math.min(buttons.size() - i, columns);
            KeyboardRow row = new KeyboardRow();
            for (int j = 0; j < elementsLeft; j++) {
                row.add(buttons.get(i + j));
            }
            rows.add(row);
        }

        for (String singleColumnButton : singleColumnButtons) {
            KeyboardRow row = new KeyboardRow();
            row.add(singleColumnButton);
            rows.add(row);
        }

        if (rows.stream().mapToLong(KeyboardRow::size).sum() == 0) {
            return ReplyKeyboardRemove.builder().removeKeyboard(true).build();
        } else {
            return ReplyKeyboardMarkup.builder()
                    .keyboard(rows)
                    .resizeKeyboard(true)
                    .build();
        }
    }

    public static InlineKeyboardMarkup toInlineKeyboard(List<InlineKeyboardButton> buttons, int columns) {
        return toInlineKeyboard(buttons, columns, Cc.l());
    }

    public static InlineKeyboardMarkup toInlineKeyboard(List<InlineKeyboardButton> buttons, int columns,
            List<InlineKeyboardButton> singleColumnButtons) {
        singleColumnButtons = singleColumnButtons == null ? Cc.l() : singleColumnButtons;

        List<InlineKeyboardRow> rows = Cc.l();
        for (int i = 0; i < buttons.size(); i = i + columns) {
            int elementsLeft = Math.min(buttons.size() - i, columns);
            InlineKeyboardRow row = new InlineKeyboardRow();
            for (int j = 0; j < elementsLeft; j++) {
                row.add(buttons.get(i + j));
            }
            rows.add(row);
        }

        for (InlineKeyboardButton singleColumnButton : singleColumnButtons) {
            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(singleColumnButton);
            rows.add(row);
        }

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }
}
