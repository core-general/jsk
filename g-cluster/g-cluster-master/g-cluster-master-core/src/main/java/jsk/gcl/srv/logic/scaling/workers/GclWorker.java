package jsk.gcl.srv.logic.scaling.workers;

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

import jsk.gcl.srv.logic.scaling.GclOOMManager;
import lombok.extern.log4j.Log4j2;
import sk.utils.async.ForeverThreadWithFinish;
import sk.utils.functional.F0;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.functional.R;

@Log4j2
public class GclWorker extends ForeverThreadWithFinish {
    public GclWorker(F0<O<R>> taskGetter, F1<O<Exception>, Boolean> onFinishAndCheckContinue, GclOOMManager oomManager) {
        super(cancelation -> {
            Exception result = null;
            try {
                final O<R> task = taskGetter.get();
                task.ifPresent($ -> $.run());
            } catch (Exception e) {
                result = e;
            }

            final Boolean continueSpinning = onFinishAndCheckContinue.apply(O.ofNull(result));

            if (!continueSpinning) {
                cancelation.setCancelled(true);
            }
        }, true, (throwable, thread) -> {
            if (throwable instanceof OutOfMemoryError) {
                oomManager.onOOM((OutOfMemoryError) throwable, thread);
            } else {
                log.error("", throwable);
            }
        });
    }
}
