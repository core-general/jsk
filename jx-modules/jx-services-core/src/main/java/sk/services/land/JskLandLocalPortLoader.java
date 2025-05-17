package sk.services.land;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2025 Core General
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
import sk.services.json.IJson;
import sk.services.shutdown.AppStopListener;
import sk.utils.functional.O;
import sk.utils.javafixes.TypeWrap;
import sk.utils.land.JskWithChangedPortType;
import sk.utils.statics.Io;

import java.util.Map;


@AllArgsConstructor
public class JskLandLocalPortLoader implements AppStopListener {
    public static final String PARENT = "/tmp/jsk/local";
    public static final String PATH = PARENT + "/.portdata";

    private final IJson json;
    private final boolean deleteFile;

    public O<Map<JskWithChangedPortType, Integer>> load() {
        return Io.sRead(PATH).oString()
                .map($ -> json.from($, TypeWrap.getMap(JskWithChangedPortType.class, Integer.class)));
    }

    public void save(Map<JskWithChangedPortType, Integer> ports) {
        Io.reWrite(PATH, w -> w.append(json.to(ports, true)));
    }

    @Override
    public void onStop() {
        if (deleteFile) {
            Io.deleteIfExists(PATH);
        }
    }

    @Override
    public long waitBeforeStopMs() {
        return 0;
    }
}
