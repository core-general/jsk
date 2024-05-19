package sk.test.land.core;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2024 Core General
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

import sk.services.async.IAsync;
import sk.utils.functional.C1E;
import sk.utils.functional.R;

import java.util.List;


public class JskLandScapeParallel extends JskLandScape {
    private final IAsync async;

    public JskLandScapeParallel(IAsync async, List<JskLand> lands) {
        super(lands);
        this.async = async;
    }

    @Override
    protected void doInit() throws Exception {
        doWithAllLands(jskLand -> jskLand.start());
    }

    @Override
    protected void doShutdown() throws Exception {
        doWithAllLands(jskLand -> jskLand.stop());
    }

    private void doWithAllLands(C1E<JskLand> landConsumer) {
        async.runAsyncDontWait(lands.values().stream().map($ -> (R) () -> {
                    try {
                        landConsumer.accept($);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).toList())
                .join();
    }

    @Override
    public Class<? extends JskLand> getId() {
        return JskLandScapeParallel.class;
    }
}
