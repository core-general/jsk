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

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.GetUpdates;
import com.pengrad.telegrambot.response.GetUpdatesResponse;
import lombok.extern.log4j.Log4j2;
import sk.services.async.IAsync;
import sk.services.boot.IBoot;
import sk.utils.async.ForeverThreadWithFinish;
import sk.utils.functional.C1;
import sk.utils.statics.Ti;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.stream.Collectors;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Comparator.comparing;

@Log4j2
public class MgcPolingBootstrap implements IBoot {
    @Inject IAsync async;

    private final TelPolingConf conf;
    private final TelegramBot bot;
    private final C1<Update> updateProcessor;
    private final ForeverThreadWithFinish mainThread;

    public MgcPolingBootstrap(TelPolingConf conf, TelegramBot bot, C1<Update> updateProcessor) {
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
                GetUpdates getUpdates = new GetUpdates().limit(100).offset(lastTelegramMessageId).timeout(3)
                        .allowedUpdates(
                                "message",
                                "callback_query",
                                "pre_checkout_query"
                        );
                GetUpdatesResponse response = null;
                try {
                    try {
                        response = bot.execute(getUpdates);
                    } catch (Exception e) {
                        return;
                    }
                    response.updates().stream().parallel()
                            .collect(Collectors.groupingBy(
                                    $ -> $.message() != null
                                         ? $.message().from().id()
                                         : ($.callbackQuery() != null)
                                           ? $.callbackQuery().from().id()
                                           : $.preCheckoutQuery().from().id()))
                            .values().stream()
                            .parallel()
                            .forEach(X -> X.stream()
                                    .sorted(comparing($ -> $.message() != null ? $.message().date() : MAX_VALUE))
                                    .forEach(updateProcessor));
                    int i = 0;
                } catch (Throwable e) {
                    log.error("", e);
                } finally {
                    try {
                        if (response != null && response.updates() != null && response.updates().size() > 0) {
                            conf.setLastTelegramMessageId(response.updates().stream()
                                    .map(Update::updateId)
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
