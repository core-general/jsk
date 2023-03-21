package jsk.vaadin.components;

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

import com.vaadin.flow.component.button.ButtonVariant;
import org.vaadin.firitin.components.button.VButton;
import sk.utils.functional.R;

public class YOpenedButton extends VButton {
    private final R openAction;
    private final R closeAction;
    boolean opened = false;

    public YOpenedButton(String text, R openAction, R closeAction) {
        super(text);
        this.openAction = openAction;
        this.closeAction = closeAction;
        addClickListener((BasicClickListener) () -> swapOpenedStatus());
    }

    public void close() {
        if (opened) {
            swapOpenedStatus();
        }
    }

    private void swapOpenedStatus() {
        if (opened) {
            opened = false;
            removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
            closeAction.run();
        } else {
            opened = true;
            addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            openAction.run();
        }
    }
}
