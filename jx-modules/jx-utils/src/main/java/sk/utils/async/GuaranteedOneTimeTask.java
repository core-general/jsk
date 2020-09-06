package sk.utils.async;

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

import lombok.Getter;
import lombok.ToString;
import sk.utils.functional.F0;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ToString
public class GuaranteedOneTimeTask<X> {
    private final CompletableFuture<X> c = new CompletableFuture<>();
    private final F0<X> itemGetTry;
    private final ScheduledExecutorService scheduler;
    private final long tryDelayMs;
    private final int maxTryCount;
    @Getter private int currentTryCount = 0;

    public GuaranteedOneTimeTask(F0<X> itemGetTry, ScheduledExecutorService scheduler, long tryDelayMs, int maxTryCount) {
        this.itemGetTry = itemGetTry;
        this.scheduler = scheduler;
        this.tryDelayMs = tryDelayMs;
        this.maxTryCount = maxTryCount;
        scheduleOnce();
    }

    private void scheduleOnce() {
        scheduler.schedule(() -> {
            try {
                c.complete(itemGetTry.apply());
            } catch (Exception e) {
                if (maxTryCount > 0 && currentTryCount >= maxTryCount) {
                    c.completeExceptionally(e);
                }
                currentTryCount++;
                scheduleOnce();
            }
        }, tryDelayMs, TimeUnit.MILLISECONDS);
    }

    public CompletableFuture<X> getFuture() {
        return c;
    }

    public static void main(String[] args) {
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

        while (true) {
            final GuaranteedOneTimeTask<String> oneTimeTasj = new GuaranteedOneTimeTask<>(() -> {
                if (Math.random() > 0.99) {
                    return "ok";
                } else {
                    throw new RuntimeException();
                }
            }, scheduler, 10, 100);

            System.out.println(oneTimeTasj.getFuture()
                    .thenApply(e -> e)
                    .exceptionally($ -> {
                        return "EXC";
                    }).join() + " " + oneTimeTasj.getCurrentTryCount());
        }
    }
}
