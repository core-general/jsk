package jsk.outer.telegram.mtc.beans.telegram;

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

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.request.AbstractSendRequest;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.request.SendSticker;
import com.pengrad.telegrambot.response.BaseResponse;
import lombok.AllArgsConstructor;
import sk.outer.api.OutMessengerApi;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.tuples.X2;

import java.util.List;

@AllArgsConstructor
public class MgcGeneralTelegramApi implements OutMessengerApi<String, MgcTelegramSpecial, Keyboard, BaseResponse> {
    final TelegramBot bot;

    @Override
    public BaseResponse send(String userId, O<String> text, O<String> image,
            O<MgcTelegramSpecial> mgcTelegramSpecial,
            O<X2<String, byte[]>> document,
            O<Keyboard> replyKeyboardMarkup) {
        List<AbstractSendRequest> requests = Cc.l();

        image.ifPresent(s -> requests.add(new SendPhoto(userId, s)));
        mgcTelegramSpecial.flatMap($ -> $.getSticker()).ifPresent(s -> requests.add(new SendSticker(userId, s)));
        mgcTelegramSpecial.flatMap($ -> $.getPayments()).ifPresent(s -> requests.add(s));
        text.ifPresent(s -> requests.add(new SendMessage(userId, s)));
        document.ifPresent(s -> requests.add(new MgcTelegramFileRequest(userId, s.i2(), s.i1())));

        Cc.last(requests).map($ -> {
            replyKeyboardMarkup.ifPresent($::replyMarkup);
            return $;
        });
        List<BaseResponse> collect = requests.stream().map($ -> bot.execute($)).collect(Cc.toL());
        return Cc.last(collect).get();
    }
}
