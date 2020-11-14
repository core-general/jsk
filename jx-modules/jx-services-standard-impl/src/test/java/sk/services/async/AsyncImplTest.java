package sk.services.async;

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

import org.junit.Test;
import sk.utils.statics.Cc;

import static org.junit.Assert.fail;

public class AsyncImplTest {
    IAsync async = new AsyncImpl();


    @Test
    public void runParallelTest() {
        try {
            async.runParallel(Cc.l(
                    () -> {},
                    () -> {throw new RuntimeException();},
                    () -> {}
            ));
            fail();
        } catch (Exception e) { }
    }

    @Test
    public void supplyParallelTest() {
        try {
            async.supplyParallel(Cc.l(
                    () -> 1,
                    () -> {throw new RuntimeException();},
                    () -> 2
            ));
            fail();
        } catch (Exception e) { }
    }
}
