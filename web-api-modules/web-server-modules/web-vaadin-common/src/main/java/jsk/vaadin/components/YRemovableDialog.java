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

import com.vaadin.flow.component.HasComponents;
import org.vaadin.firitin.components.dialog.VDialog;

public class YRemovableDialog extends VDialog {
    private final HasComponents rootPanel;

    public <T extends HasComponents> YRemovableDialog(T rootPanel) {
        super();
        this.rootPanel = rootPanel;

        this.setModal(true);

        withDialogCloseActionListener((e) -> {
            close();
            rootPanel.remove(this);
        });
    }

    @Override
    public void setOpened(boolean opened) {
        if (opened) {
            rootPanel.add(this);
        }
        super.setOpened(opened);
    }
}


