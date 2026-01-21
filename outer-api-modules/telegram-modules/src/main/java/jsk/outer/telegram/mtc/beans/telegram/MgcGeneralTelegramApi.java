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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import sk.outer.api.OutMessengerApi;
import sk.services.ratelimits.IRateLimiter;
import sk.utils.functional.F0;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.St;
import sk.utils.tuples.X2;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.time.Duration;
import java.util.List;

@Slf4j
public class MgcGeneralTelegramApi implements OutMessengerApi<String, MgcTelegramSpecial, ReplyKeyboard, Serializable> {
    @Getter protected final TelegramClient bot;
    @Getter protected final IRateLimiter globalRateLimiter;
    private final F0<IRateLimiter> perUserRateLimiter;
    private final String botToken;

    final LoadingCache<String, IRateLimiter> perUserLimiting;

    public MgcGeneralTelegramApi(String botToken) {
        this(botToken, null, null);
    }

    public MgcGeneralTelegramApi(String botToken, IRateLimiter globalRateLimiter, F0<IRateLimiter> perUserRateLimiterProvider) {
        this.botToken = botToken;
        this.bot = new OkHttpTelegramClient(botToken);
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
    public Serializable send(String userId, O<String> text, O<String> image,
            O<MgcTelegramSpecial> mgcTelegramSpecial,
            O<X2<String, byte[]>> document,
            O<ReplyKeyboard> replyKeyboardMarkup) {

        List<BotApiMethod<?>> requests = Cc.l();
        List<Object> allRequests = Cc.l();

        image.ifPresent(s -> {
            SendPhoto photo = SendPhoto.builder()
                    .chatId(userId)
                    .photo(new InputFile(s))
                    .build();
            addCaptionIfExistsToImage(text, photo);
            allRequests.add(photo);
        });

        mgcTelegramSpecial.flatMap($ -> $.getSticker()).ifPresent(s -> {
            allRequests.add(SendSticker.builder()
                    .chatId(userId)
                    .sticker(new InputFile(s))
                    .build());
        });

        mgcTelegramSpecial.flatMap($ -> $.getPayments()).ifPresent(s -> {
            requests.add(s);
            allRequests.add(s);
        });

        mgcTelegramSpecial.flatMap($ -> $.getVideo()).ifPresent(s -> {
            allRequests.add(SendVideo.builder()
                    .chatId(userId)
                    .video(new InputFile(s))
                    .build());
        });

        mgcTelegramSpecial.flatMap($ -> $.getRawImage())
                .ifPresent(imgAndFormat -> {
                    byte[] imageBytes = imgAndFormat.i2().toBytes(imgAndFormat.i1());
                    SendPhoto photo = SendPhoto.builder()
                            .chatId(userId)
                            .photo(new InputFile(new ByteArrayInputStream(imageBytes),
                                    "image." + imgAndFormat.i2().name().toLowerCase()))
                            .build();
                    addCaptionIfExistsToImage(text, photo);
                    allRequests.add(photo);
                });

        text.filter(__ -> image.isEmpty() && mgcTelegramSpecial.map($ -> $.getRawImage().isEmpty()).orElse(true))
                .ifPresent(s -> {
                    SendMessage msg = SendMessage.builder()
                            .chatId(userId)
                            .text(s)
                            .disableWebPagePreview(true)
                            .build();
                    requests.add(msg);
                    allRequests.add(msg);
                });

        document.ifPresent(s -> {
            allRequests.add(SendDocument.builder()
                    .chatId(userId)
                    .document(new InputFile(new ByteArrayInputStream(s.i2()), s.i1()))
                    .build());
        });

        if (!allRequests.isEmpty()) {
            Object lastRequest = allRequests.get(allRequests.size() - 1);
            replyKeyboardMarkup.ifPresent(kb -> {
                if (lastRequest instanceof SendMessage msg) {
                    allRequests.set(allRequests.size() - 1, SendMessage.builder()
                            .chatId(msg.getChatId())
                            .text(msg.getText())
                            .disableWebPagePreview(msg.getDisableWebPagePreview())
                            .replyMarkup(kb)
                            .build());
                } else if (lastRequest instanceof SendPhoto photo) {
                    allRequests.set(allRequests.size() - 1, SendPhoto.builder()
                            .chatId(photo.getChatId())
                            .photo(photo.getPhoto())
                            .caption(photo.getCaption())
                            .replyMarkup(kb)
                            .build());
                } else if (lastRequest instanceof SendDocument doc) {
                    allRequests.set(allRequests.size() - 1, SendDocument.builder()
                            .chatId(doc.getChatId())
                            .document(doc.getDocument())
                            .replyMarkup(kb)
                            .build());
                } else if (lastRequest instanceof SendVideo video) {
                    allRequests.set(allRequests.size() - 1, SendVideo.builder()
                            .chatId(video.getChatId())
                            .video(video.getVideo())
                            .replyMarkup(kb)
                            .build());
                } else if (lastRequest instanceof SendSticker sticker) {
                    allRequests.set(allRequests.size() - 1, SendSticker.builder()
                            .chatId(sticker.getChatId())
                            .sticker(sticker.getSticker())
                            .replyMarkup(kb)
                            .build());
                }
            });
        }

        List<Serializable> results = allRequests.stream()
                .map(req -> {
                    F0<Serializable> toRun = () -> {
                        if (globalRateLimiter != null) {
                            globalRateLimiter.waitUntilPossible();
                        }
                        try {
                            if (req instanceof BotApiMethod<?> method) {
                                return bot.execute(method);
                            } else if (req instanceof SendPhoto photo) {
                                return bot.execute(photo);
                            } else if (req instanceof SendDocument doc) {
                                return bot.execute(doc);
                            } else if (req instanceof SendSticker sticker) {
                                return bot.execute(sticker);
                            } else if (req instanceof SendVideo video) {
                                return bot.execute(video);
                            }
                            return null;
                        } catch (Exception e) {
                            log.error("Telegram send error", e);
                            return null;
                        }
                    };

                    if (perUserRateLimiter != null) {
                        return perUserLimiting.get(userId)
                                .produceInLimit(() -> toRun.apply());
                    } else {
                        return toRun.apply();
                    }
                })
                .collect(Cc.toL());

        return Cc.last(results).orElse(null);
    }

    private void addCaptionIfExistsToImage(O<String> text, SendPhoto photo) {
        if (text.isPresent()) {
            photo.setCaption(St.raze3dots(text.get(), 990));
        }
    }

    @Override
    public Serializable editMessageText(String userId, String messageId, String newText, O<ReplyKeyboard> newButtons) {
        try {
            EditMessageText.EditMessageTextBuilder builder = EditMessageText.builder()
                    .chatId(userId)
                    .messageId(Integer.parseInt(messageId))
                    .text(newText);

            newButtons.filter($ -> $ instanceof InlineKeyboardMarkup)
                    .ifPresent($ -> builder.replyMarkup((InlineKeyboardMarkup) $));

            return bot.execute(builder.build());
        } catch (Exception e) {
            log.error("Edit message error", e);
            return null;
        }
    }

    @Override
    public Serializable deleteMessage(String userId, String messageId) {
        try {
            DeleteMessage deleteRequest = DeleteMessage.builder()
                    .chatId(userId)
                    .messageId(Integer.parseInt(messageId))
                    .build();
            return bot.execute(deleteRequest);
        } catch (Exception e) {
            log.error("Delete message error", e);
            return null;
        }
    }

    public O<byte[]> getFileByFileId(String fileId) {
        try {
            final org.telegram.telegrambots.meta.api.objects.File file = bot.execute(GetFile.builder().fileId(fileId).build());
            try (InputStream is = bot.downloadFileAsStream(file)) {
                return O.of(is.readAllBytes());
            }
        } catch (Exception e) {
            log.error("Get file error", e);
            return O.empty();
        }
    }
}
