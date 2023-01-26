package sk.services.boot;

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

import sk.utils.statics.Io;

@SuppressWarnings("unused")
public abstract class IBootTempSafe implements IBootTemp {

    private final String triggerFilePath;

    public IBootTempSafe() {
        triggerFilePath = "/tmp/jsk_" + getClass().getSimpleName();
    }

    public IBootTempSafe(String triggerFilePath) {
        this.triggerFilePath = triggerFilePath;
    }

    protected abstract void goSafely();

    @Override
    public final void go() {
        if (!Io.exists(triggerFilePath)) {
            throw new RuntimeException("""
                                        
                    !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    OPERATION '%s' IS POTENTIALLY UNSAFE
                    PLEASE IF YOU ARE !SURE! WHAT YOU ARE DOING
                    CREATE A FILE: %s
                    IT WILL BE CHECKED AND THEN DELETED AFTER
                    goSafely METHOD RETURNS
                    !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    """.formatted(getClass().getSimpleName(), triggerFilePath));
        }
        try {
            goSafely();
        } finally {
            Io.deleteIfExists(triggerFilePath);
        }
    }

    @Override
    public final void run() {
        IBootTemp.super.run();
    }
}
