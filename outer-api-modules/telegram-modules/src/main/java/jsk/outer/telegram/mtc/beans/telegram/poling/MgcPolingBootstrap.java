package jsk.outer.telegram.mtc.beans.telegram.poling;

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

import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.updates.GetUpdates;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import sk.services.async.IAsync;
import sk.services.boot.IBoot;
import sk.utils.async.ForeverThreadWithFinish;
import sk.utils.functional.C1;
import sk.utils.statics.Ti;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Comparator.comparing;

@Slf4j
public class MgcPolingBootstrap implements IBoot {
    @Inject IAsync async;

    private final TelPolingConf conf;
    private final TelegramClient bot;
    private final C1<Update> updateProcessor;
    private final ForeverThreadWithFinish mainThread;

    public MgcPolingBootstrap(TelPolingConf conf, TelegramClient bot, C1<Update> updateProcessor) {
        this.conf = conf;
        this.bot = bot;
        this.updateProcessor = updateProcessor;
        mainThread = new ForeverThreadWithFinish(this::telegramPolling, true);
    }

    @Override
    public void run() {
        mainThread.start();
    }

    void telegramPolling() {
        try {
            if (conf.usePolling()) {
                int lastTelegramMessageId = conf.getLastTelegramMessageId();
                GetUpdates getUpdates = GetUpdates.builder()
                        .limit(100)
                        .offset(lastTelegramMessageId)
                        .timeout(3)
                        .allowedUpdates(List.of(
                                "message",
                                "callback_query",
                                "pre_checkout_query"
                        ))
                        .build();

                List<Update> updates = null;
                try {
                    try {
                        updates = bot.execute(getUpdates);
                    } catch (Exception e) {
                        return;
                    }
                    updates.stream().parallel()
                            .collect(Collectors.groupingBy(
                                    $ -> $.getMessage() != null
                                         ? $.getMessage().getFrom().getId()
                                         : ($.getCallbackQuery() != null)
                                           ? $.getCallbackQuery().getFrom().getId()
                                           : $.getPreCheckoutQuery().getFrom().getId()))
                            .values().stream()
                            .parallel()
                            .forEach(X -> X.stream()
                                    .sorted(comparing($ -> $.getMessage() != null ? $.getMessage().getDate() : MAX_VALUE))
                                    .forEach(updateProcessor));
                } catch (Throwable e) {
                    log.error("", e);
                } finally {
                    try {
                        if (updates != null && updates.size() > 0) {
                            conf.setLastTelegramMessageId(updates.stream()
                                                                  .map(Update::getUpdateId)
                                    .max(Comparator.comparing(u -> u)).orElse(0)
                                    + 1);
                        }
                    } catch (Throwable e) {
                        log.error("", e);
                    }
                    async.sleep(conf.getTelegramPollingPeriodSec() * Ti.second);
                }
            } else {
                async.sleep(3000);
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
