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

import sk.test.land.core.mixins.JskLandEmptyStateMixin;
import sk.test.land.core.mixins.JskLandStateChangerMixin;
import sk.utils.functional.C1E;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JskLandScape extends JskLand {
    protected final Map<Class<? extends JskLand>, JskLand> lands;

    public JskLandScape(List<JskLand> lands) {
        this.lands = Collections.unmodifiableMap(lands.stream()
                .collect(Collectors.toMap($ -> $.getClass(), $ -> $, Cc.throwingMerger(), () -> new LinkedHashMap<>())));
    }

    public <T extends JskLand>
    O<T> getLand(Class<T> cls) {
        return O.ofNull(lands.get(cls)).map($ -> (T) $);
    }

    public <STATE, CLS extends JskLand & JskLandStateChangerMixin<STATE>>
    void toMaybeState(O<STATE> state, Class<CLS> landCls) {
        CLS land = getLand(landCls).orElseThrow(() -> new RuntimeException("Can't find land with cls:" + landCls));
        land.toMaybeState(state);
    }

    public <STATE, CLS extends JskLand & JskLandStateChangerMixin<STATE>>
    void toState(STATE state, Class<CLS> landCls) {
        toMaybeState(O.of(state), landCls);
    }

    public <STATE, CLS extends JskLand & JskLandStateChangerMixin<STATE>>
    void toDefaultState(STATE state, Class<CLS> landCls) {
        toMaybeState(O.empty(), landCls);
    }

    public void toEmptyStateAll() throws Exception {
        doWithAllLands(changeLand -> changeLand.toEmptyState(), Fu.emptyCE(), Fu.emptyCE());
    }

    public void toDefaultStateAll() throws Exception {
        doWithAllLands(Fu.emptyCE(), changeLand -> changeLand.toDefaultState(), Fu.emptyCE());
    }

    @Override
    protected void doInit() throws Exception {
        doWithAllLands(Fu.emptyCE(), Fu.emptyCE(), land -> land.doInit());
    }

    @Override
    protected void doShutdown() throws Exception {
        doWithAllLands(Fu.emptyCE(), Fu.emptyCE(), land -> land.doShutdown());
    }

    private void doWithAllLands(C1E<JskLandEmptyStateMixin> onEmpty, C1E<JskLandStateChangerMixin<?>> onStateChanger,
            C1E<JskLand> justLand) throws Exception {
        for (JskLand land : lands.values()) {
            switch (land) {
                case JskLandStateChangerMixin<?> stateChanger -> onStateChanger.accept(stateChanger);
                case JskLandEmptyStateMixin emptyStater -> onEmpty.accept(emptyStater);
                default -> justLand.accept(land);
            }
        }
    }

    @Override
    public Class<? extends JskLand> getId() {
        return JskLandScape.class;
    }
}
