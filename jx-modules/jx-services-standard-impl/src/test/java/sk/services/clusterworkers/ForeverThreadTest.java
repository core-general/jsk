package sk.services.clusterworkers;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2020 Core General
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

import sk.utils.async.ForeverThreadWithFinish;
import sk.utils.functional.R;
import sk.utils.statics.Ti;

public class ForeverThreadTest {
    public static void main(String[] args) {
        ForeverThreadWithFinish ftw = new ForeverThreadWithFinish(new R() {
            volatile int i = 0;

            @Override
            public void run() {
                Ti.sleep(100);
                System.out.println(i++);
            }
        }, true);

        ftw.start();
        System.out.println(ftw.isFinished());
        Ti.sleep(3000);
        ftw.finishThread()
                .thenRun(() -> System.out.println("Finished?" + ftw.isFinished()));
    }
}
