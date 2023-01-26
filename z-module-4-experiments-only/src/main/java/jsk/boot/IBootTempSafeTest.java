package jsk.boot;

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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import sk.services.boot.IBoot;
import sk.services.boot.IBootTempSafe;
import sk.spring.SpringApp;
import sk.spring.config.SpringCoreConfig;
import sk.utils.statics.Ti;

public class IBootTempSafeTest {
    public static void main(String[] args) {
        SpringApp.createSimple(() -> {}, IBootTempSafeTestConfig.class);

        Ti.sleep(1000 * Ti.day);
    }
}

@Order(0)
class IBootTempSafeBean extends IBootTempSafe {
    @Override
    protected void goSafely() {
        System.out.println("OOOOOOOOOOOOOOK");
    }
}

class IBootTempSafeBeanSomeOtherBean implements IBoot {
    @Override
    public void run() {
        System.out.println("!!!!!!!!!!!!!!!");
    }
}

@Configuration
@Import(SpringCoreConfig.class)
class IBootTempSafeTestConfig {
    @Bean
    IBootTempSafeBeanSomeOtherBean IBootTempSafeBeanSomeOtherBean() {
        return new IBootTempSafeBeanSomeOtherBean();
    }

    @Bean
    IBootTempSafeBean IBootTempSafeBean() {
        return new IBootTempSafeBean();
    }
}
