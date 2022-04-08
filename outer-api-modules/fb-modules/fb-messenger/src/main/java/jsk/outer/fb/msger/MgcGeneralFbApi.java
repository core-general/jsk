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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.messenger4j.Messenger;
import com.github.messenger4j.send.MessagePayload;
import com.github.messenger4j.send.MessageResponse;
import com.github.messenger4j.send.MessagingType;
import com.github.messenger4j.send.Payload;
import com.github.messenger4j.send.message.RichMediaMessage;
import com.github.messenger4j.send.message.TemplateMessage;
import com.github.messenger4j.send.message.TextMessage;
import com.github.messenger4j.send.message.quickreply.QuickReply;
import com.github.messenger4j.send.message.quickreply.TextQuickReply;
import com.github.messenger4j.send.message.richmedia.ReusableRichMediaAsset;
import com.github.messenger4j.send.message.richmedia.RichMediaAsset;
import com.github.messenger4j.send.message.richmedia.UrlRichMediaAsset;
import com.github.messenger4j.send.message.template.ButtonTemplate;
import com.github.messenger4j.send.message.template.button.Button;
import com.github.messenger4j.send.message.template.button.PostbackButton;
import com.github.messenger4j.webhook.Event;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import sk.outer.api.OutMessengerApi;
import sk.services.ids.IIds;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.St;
import sk.utils.tuples.X;
import sk.utils.tuples.X2;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.github.messenger4j.send.message.richmedia.RichMediaAsset.Type.IMAGE;
import static sk.utils.functional.O.of;
import static sk.utils.functional.O.ofNullable;

@Log4j2
public class MgcGeneralFbApi implements OutMessengerApi<String, Void, List<String>, MessageResponse> {
    @Inject Messenger bot;
    @Inject IIds ids;

    ConcurrentHashMap<String, String> imageCache;
    @Getter Cache<String, X2<String, byte[]>> cache;

    @PostConstruct
    void initMgcGeneralFbApi() {
        imageCache = new ConcurrentHashMap<>();
        cache = Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .build();
    }


    @Override
    public MessageResponse send(String userId, O<String> text,
            O<String> image, O<Void> aVoid,
            O<X2<String, byte[]>> document,
            O<List<String>> qr) {

        List<Payload> requests = Cc.l();

        image.ifPresent($ -> {
            RichMediaAsset richMediaAsset = imageCache.containsKey($)
                                            ? ReusableRichMediaAsset.create(IMAGE, imageCache.get($))
                                            : UrlRichMediaAsset.create(IMAGE, url($), of(true).toOpt());

            final RichMediaMessage richMediaMessage = RichMediaMessage.create(richMediaAsset);
            final MessagePayload payload = MessagePayload.create(userId, MessagingType.RESPONSE, richMediaMessage);
            requests.add(payload);

        });

        document.ifPresent($ -> {
            String key = ids.shortIdS();
            cache.put(key, $);
            RichMediaAsset rma =
                    UrlRichMediaAsset.create(IMAGE, url("http://try-db.com/?_binaryAssetId_" + key + "_"), of(false).toOpt());
            final RichMediaMessage richMediaMessage = RichMediaMessage.create(rma);
            final MessagePayload payload = MessagePayload.create(userId, MessagingType.RESPONSE, richMediaMessage);
            requests.add(payload);
        });

        if (qr.isPresent() && qr.get().size() > 0 && qr.get().size() <= 3 && (!text.isPresent() || text.get().length() < 639)) {
            addButtonReply(userId, text, qr, requests);
        } else {
            addQuickReply(userId, text, qr, requests);
        }

        List<MessageResponse> collect = requests.stream().map($ -> {
            boolean needCache = false;
            if (((MessagePayload) $).message() instanceof RichMediaMessage) {
                RichMediaMessage rmm = (RichMediaMessage) ((MessagePayload) $).message();
                if (rmm.richMediaAsset() instanceof UrlRichMediaAsset) {
                    needCache = true;
                }
            }
            MessageResponse messageResponse = botSend($);
            if (needCache) {
                messageResponse.attachmentId()
                        .flatMap(x -> image.map($$ -> X.x($$, x)).toOpt())
                        .ifPresent(x -> imageCache.put(x.i1(), x.i2()));
            }

            return messageResponse;
        }).collect(Cc.toL());
        return Cc.last(collect).get();

    }

    private void addButtonReply(String userId, O<String> text, O<List<String>> qr, List<Payload> requests) {
        if (text.isPresent() || qr.isPresent()) {
            List<Button> buttons = qr.orElse(Cc.l())
                    .stream().map($ -> PostbackButton.create($, $))
                    .collect(Cc.toL());

            ButtonTemplate genericTemplate = ButtonTemplate.create(text.orElse(""), buttons);

            requests.add(MessagePayload.create(
                    userId, MessagingType.RESPONSE, TemplateMessage.create(genericTemplate)));
        }
    }

    protected void addQuickReply(String userId, O<String> text, O<List<String>> qr, List<Payload> requests) {
        if (text.isPresent() || qr.isPresent()) {
            List<QuickReply> quickReplies = qr.orElseGet(() -> Cc.l())
                    .stream().map($ -> TextQuickReply.create($, $, Optional.empty())).collect(Cc.toL());

            quickReplies = quickReplies.size() > 0 ? quickReplies : null;


            String txt = text.orElse("Choices:\n");
            List<TextMessage> collect = St.splitBySize(txt, 1997).stream()
                    .map($ -> TextMessage.create($, Optional.empty(), Optional.empty()))
                    .collect(Cc.toL());

            collect.set(collect.size() - 1,
                    TextMessage.create(collect.get(0).text(), ofNullable(quickReplies).toOpt(), Optional.empty()));

            collect.forEach($ -> requests.add(MessagePayload.create(userId, MessagingType.RESPONSE, $)));
        }
    }

    @SneakyThrows
    public void subscribeOnEvent(String payload, Consumer<Event> event) {
        bot.onReceiveEvents(
                payload,
                Optional.empty(),
                event);
    }

    @SneakyThrows
    private URL url(String $) {
        return new URL($);
    }

    @SneakyThrows
    private MessageResponse botSend(Payload $) {
        log.debug($ + "");
        return bot.send($);
    }
}
