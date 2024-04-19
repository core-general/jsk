package jsk.vaadin.fields.binded;

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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.Scroller;
import jsk.vaadin.components.YHorizontal;
import jsk.vaadin.components.YVertical;
import lombok.extern.slf4j.Slf4j;
import org.vaadin.firitin.components.button.VButton;
import org.vaadin.firitin.components.dialog.VDialog;
import sk.utils.functional.C1;
import sk.utils.functional.R;

@Slf4j
public abstract class YBindedBufferedDialogField<T> extends YBindedBufferedButtonField<T> {
    protected VDialog dialog = new VDialog()
            .withModal(true)
            .withDialogCloseActionListener(new ComponentEventListener<>() {
                @Override
                public void onComponentEvent(Dialog.DialogCloseActionEvent event) {
                    dialog.close();
                    isInDialogue = false;
                    //rerender();
                }
            });
    protected boolean isInDialogue = false;

    public YBindedBufferedDialogField(String buttonText, String revertButtonText, C1<ValueChangeEvent<T>> onSaveButtonClicked,
            R onCancel, C1<Exception> onValidationFailed) {
        super(buttonText, revertButtonText, onSaveButtonClicked, onCancel, onValidationFailed);
    }

    public YBindedBufferedDialogField(String buttonText, String revertButtonText,
            C1<ValueChangeEvent<T>> onSaveButtonClicked,
            C1<Exception> onValidationFailed) {
        super(buttonText, revertButtonText, onSaveButtonClicked, onValidationFailed);
    }

    public YBindedBufferedDialogField(String buttonText, String revertButtonText,
            C1<ValueChangeEvent<T>> onSaveButtonClicked) {
        super(buttonText, revertButtonText, onSaveButtonClicked);
    }

    public YBindedBufferedDialogField(C1<ValueChangeEvent<T>> onSave) {
        super(onSave);
    }

    public YBindedBufferedDialogField(C1<ValueChangeEvent<T>> onSave, R onCancel) {
        super(onSave, onCancel);
    }

    @Override
    protected C1<ValueChangeEvent<T>> getOnSaveButtonClicked() {
        return tValueChangeEvent -> super.getOnSaveButtonClicked().andThen(e -> dialog.close()).accept(tValueChangeEvent);
    }

    @Override
    protected R getOnCancel() {
        return () -> {
            super.getOnCancel().run();
            dialog.close();
        };
    }

    @Override
    protected Component wrapFieldsIfNeeded(Component fields) {
        if (isInDialogue) {
            divWithButtons.removeAll();
            divWithButtons.add(fields);
            return divWithButtons;
        } else {
            return super.wrapFieldsIfNeeded(fields);
        }
    }

    @Override
    protected C1<Exception> getOnValidationFailed() {
        return exception -> super.getOnValidationFailed().andThen(e -> dialog.close()).accept(exception);
    }

    public VDialog openWithValue(T value) {
        isInDialogue = true;
        rerender();

        dialog.removeAll();
        setValue(value);

        Scroller scroller = new Scroller(this.withSizeFull());
        scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        scroller.setSizeFull();

        dialog.add(new YVertical(
                //new YHorizontal(scroller).withSizeFull(),
                scroller,
                getButtonPanel()
        ).withSizeFull());

        dialog.open();
        return dialog;
    }

    protected YHorizontal getButtonPanel() {
        VButton saveButton = createSaveButton();
        VButton cancelButton = createCancelButton();
        return (YHorizontal) new YHorizontal(saveButton, cancelButton)
                //.withSizeFull()
                .withFullWidth()
                .withAlignItems(FlexComponent.Alignment.END)
                .withJustifyContentMode(buttonsContentMode());
    }
}
