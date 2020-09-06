package sk.spring;

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

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import sk.services.profile.IAppProfileType;
import sk.spring.config.SpringCoreConfig;
import sk.spring.services.AppProfileImpl;

public class SpringAppTest {
    public static void main(String[] args) {
        //SpringApp.create(new Input(args), Config.class);
    }

    //@SuppressWarnings({"unused"})
    //@RequiredArgsConstructor
    //public static class Input implements R {
    //    final String[] args;
    //
    //    @Inject AppProfileImpl<Profile> profile;
    //
    //    @Override
    //    public void run() {
    //        //noinspection InfiniteLoopStatement
    //        while (true) {
    //            val i = new AtomicInteger(0);
    //            String format = Ti.yyyyMMddHHmmssSSS.format(times.nowZ());
    //            System.out.println(format + " " + bytes.enc64(bytes.sha256(format.getBytes())));
    //            System.out.println(json.from(http.getS("http://worldtimeapi.org/api/ip"), Map.class).get("utc_datetime"));
    //            System.out.println(ids.customId(25) + " " + ids.shortIdS() + " " + ids.uniqueFrom(format));
    //            System.out.println(rand.rndDist(Cc.m("Lol!", 0.5, "Wow!", 0.25, "Kiss!", 0.125, "Love!", 0.125)));
    //            System.out.println(bytes.enc64(resCache.getResourceBytes("sk/spring/SpringAppTest.class").get()));
    //            //System.out.println(repeat.repeat(() -> {
    //            //    if (rand.rndBool(0.5)) {
    //            //        System.out.println("Exception!");
    //            //        throw new RuntimeException();
    //            //    }
    //            //    return i.incrementAndGet();
    //            //}, 100, Cc.s(RuntimeException.class)));
    //
    //
    //            async.sleep(1000);
    //        }
    //    }
    //}

    @SuppressWarnings("unused")
    @AllArgsConstructor
    public enum Profile implements IAppProfileType {
        DEV(false, true), PROD(true, true);
        @Getter boolean forProductionUsage;
        @Getter boolean forDefaultTesting;
    }

    @Configuration
    @Import(SpringCoreConfig.class)
    public static class Config {
        @Bean
        AppProfileImpl<Profile> AppProfileImpl() {
            return new AppProfileImpl<>(Profile.class);
        }
    }
}
