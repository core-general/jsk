package sk.spring.services;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 Core General
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

import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import sk.services.boot.IBoot;
import sk.services.time.ITime;
import sk.utils.statics.Ti;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Log4j2
@Order(10)
public class BootServiceImpl implements ApplicationListener<ContextRefreshedEvent> {
    @Inject List<IBoot> boots = new ArrayList<>();
    @Inject ITime times;

    private volatile boolean initFinished;

    @Override
    public synchronized void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        if (!initFinished) {
            initFinished = true;
            StringBuilder sb = new StringBuilder("\nIBoots\n");
            boots.forEach(bootstrap -> {
                sb.append("     ")
                        .append(Ti.yyyyMMddHHmmssSSS.format(times.nowZ()))
                        .append(" ").append(bootstrap.getClass())
                        .append("\n");
                bootstrap.run();
            });
            log.info(sb.toString());
        }
    }

}
