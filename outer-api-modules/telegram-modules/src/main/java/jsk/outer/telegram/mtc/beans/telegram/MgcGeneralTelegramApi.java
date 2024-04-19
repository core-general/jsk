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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.File;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.request.*;
import com.pengrad.telegrambot.response.BaseResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import sk.outer.api.OutMessengerApi;
import sk.services.ratelimits.IRateLimiter;
import sk.utils.functional.F0;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.St;
import sk.utils.tuples.X2;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@Slf4j
public class MgcGeneralTelegramApi implements OutMessengerApi<String, MgcTelegramSpecial, Keyboard, BaseResponse> {
    @Getter protected final TelegramBot bot;
    @Getter protected final IRateLimiter globalRateLimiter;
    private final F0<IRateLimiter> perUserRateLimiter;

    final LoadingCache<String, IRateLimiter> perUserLimiting;

    public MgcGeneralTelegramApi(TelegramBot bot) {
        this(bot, null, null);
    }

    public MgcGeneralTelegramApi(TelegramBot bot, IRateLimiter globalRateLimiter, F0<IRateLimiter> perUserRateLimiterProvider) {
        this.bot = bot;
        this.globalRateLimiter = globalRateLimiter;
        this.perUserRateLimiter = perUserRateLimiterProvider;
        perUserLimiting = perUserRateLimiterProvider != null
                          ? Caffeine.newBuilder()
                                  .expireAfterAccess(Duration.ofMinutes(1))
                                  .maximumSize(100_000)
                                  .build(key -> perUserRateLimiterProvider.apply())
                          : null;
    }

    @Override
    public BaseResponse send(String userId, O<String> text, O<String> image,
            O<MgcTelegramSpecial> mgcTelegramSpecial,
            O<X2<String, byte[]>> document,
            O<Keyboard> replyKeyboardMarkup) {

        List<AbstractSendRequest> requests = Cc.l();

        image.ifPresent(s -> {
            addCaptionIfExistsToImage(text, requests, new SendPhoto(userId, s));
        });
        mgcTelegramSpecial.flatMap($ -> $.getSticker()).ifPresent(s -> requests.add(new SendSticker(userId, s)));
        mgcTelegramSpecial.flatMap($ -> $.getPayments()).ifPresent(s -> requests.add(s));
        mgcTelegramSpecial.flatMap($ -> $.getVideo()).ifPresent(s -> requests.add(new SendVideo(userId, s)));
        mgcTelegramSpecial.flatMap($ -> $.getRawImage())
                .ifPresent(imgAndFormat -> {
                    addCaptionIfExistsToImage(text, requests, new SendPhoto(userId, imgAndFormat.i2.toBytes(imgAndFormat.i1())));
                });
        text.filter(__ -> image.isEmpty() && mgcTelegramSpecial.map($ -> $.getRawImage().isEmpty()).orElse(true))
                .ifPresent(s -> requests.add(new SendMessage(userId, s).disableWebPagePreview(true)));
        document.ifPresent(s -> requests.add(new MgcTelegramFileRequest(userId, s.i2(), s.i1())));

        Cc.last(requests).map($ -> {
            replyKeyboardMarkup.ifPresent($::replyMarkup);
            return $;
        });
        List<BaseResponse> collect = requests.stream()
                .map($ -> {
                    F0<BaseResponse> toRun = () -> {
                        if (globalRateLimiter != null) {
                            globalRateLimiter.waitUntilPossible();
                        }
                        return bot.execute($);
                    };

                    if (perUserRateLimiter != null) {
                        return perUserLimiting.get(userId)
                                .produceInLimit(() -> toRun.apply());
                    } else {
                        return toRun.apply();
                    }
                })
                .collect(Cc.toL());
        return Cc.last(collect).get();
    }

    private void addCaptionIfExistsToImage(O<String> text, List<AbstractSendRequest> requests, SendPhoto photo) {
        requests.add(photo);
        if (text.isPresent()) {
            photo.caption(St.raze3dots(text.get(), 990));
        }
    }

    @Override
    public BaseResponse editMessageText(String __, String inlineMessageId, String newText, O<Keyboard> newButtons) {
        EditMessageText editMessageTextRequest = new EditMessageText(inlineMessageId, newText);
        newButtons.filter($ -> $ instanceof InlineKeyboardMarkup)
                .ifPresent($ -> editMessageTextRequest.replyMarkup((InlineKeyboardMarkup) $));
        return bot.execute(editMessageTextRequest);
    }

    @Override
    public BaseResponse deleteMessage(String userId, String messageId) {
        DeleteMessage deleterRequest = new DeleteMessage(userId, Integer.parseInt(messageId));
        return bot.execute(deleterRequest);
    }

    public O<byte[]> getFileByFileId(String fileId) {
        try {
            final File file = bot.execute(new GetFile(fileId)).file();
            final byte[] fileContent = bot.getFileContent(file);
            return O.of(fileContent);
        } catch (IOException e) {
            log.error("", e);
            return O.empty();
        }
    }
}
