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
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.vaadin.firitin.components.button.VButton;
import org.vaadin.firitin.components.orderedlayout.VHorizontalLayout;
import sk.utils.functional.C1;
import sk.utils.functional.R;

@Getter(AccessLevel.PROTECTED)
@Log4j2
public abstract class YBindedBufferedButtonField<T> extends YBindedBufferedField<T> {
    @Setter private String saveButtonText;
    @Setter private String revertButtonText;
    @Setter private C1<ValueChangeEvent<T>> onSaveButtonClicked;
    private final R onCancel;
    private final C1<Exception> onValidationFailed;
    protected Div divWithButtons;

    public YBindedBufferedButtonField(String saveButtonText, String revertButtonText, C1<ValueChangeEvent<T>> onSave,
            C1<Exception> onValidFail) {
        this(saveButtonText, revertButtonText, onSave, () -> {}, onValidFail);
    }

    public YBindedBufferedButtonField(String buttonText, String revertButtonText, C1<ValueChangeEvent<T>> onSave) {
        this(buttonText, revertButtonText, onSave, () -> {}, e -> {});
    }


    public YBindedBufferedButtonField(C1<ValueChangeEvent<T>> onSave) {
        this("Save", "Cancel", onSave, () -> {}, e -> {});
    }

    public YBindedBufferedButtonField(C1<ValueChangeEvent<T>> onSave, R onCancel) {
        this("Save", "Cancel", onSave, onCancel, e -> {});
    }

    public YBindedBufferedButtonField(String saveButton) {
        this(saveButton, "Cancel", v -> {}, () -> {}, e -> log.error("", e));
    }

    public YBindedBufferedButtonField(String saveButtonText, String revertButtonText,
            C1<ValueChangeEvent<T>> onSaveButtonClicked, R onCancel, C1<Exception> onValidationFailed) {
        this.saveButtonText = saveButtonText;
        this.revertButtonText = revertButtonText;
        this.onSaveButtonClicked = onSaveButtonClicked;
        this.onCancel = onCancel;
        this.onValidationFailed = onValidationFailed;
        divWithButtons = new Div();
    }

    @Override
    protected Component wrapFieldsIfNeeded(Component fields) {
        divWithButtons.removeAll();
        VButton saveButton = createSaveButton();
        VButton cancelButton = createCancelButton();
        divWithButtons.add(fields,
                new VHorizontalLayout(saveButton, cancelButton)
                        .withJustifyContentMode(buttonsContentMode()));
        return divWithButtons;
    }

    @Override
    public void setSizeFull() {
        super.setSizeFull();
        divWithButtons.setSizeFull();
    }

    @Override
    public void setWidthFull() {
        super.setWidthFull();
        divWithButtons.setWidthFull();
    }

    @Override
    public void setHeightFull() {
        super.setHeightFull();
        divWithButtons.setHeightFull();
    }

    protected VButton createCancelButton() {
        return new VButton(revertButtonText).onClick(() -> {
            try {
                fireCancelEvent();
                getOnCancel().run();
            } catch (Throwable e) {
                log.error("", e);
                throw new RuntimeException(e);
            }
        });
    }

    protected JustifyContentMode buttonsContentMode() {
        return JustifyContentMode.END;
    }

    protected VButton createSaveButton() {
        return new VButton(saveButtonText).onClick(() -> {
            try {
                final T oldValue = getValue();
                fireSaveEvent();
                final T newValue = getValue();
                getOnSaveButtonClicked().accept(new ValueChangeEvent<T>() {
                    @Override
                    public HasValue<?, T> getHasValue() {
                        return YBindedBufferedButtonField.this;
                    }

                    @Override
                    public boolean isFromClient() {
                        return false;
                    }

                    @Override
                    public T getOldValue() {
                        return oldValue;
                    }

                    @Override
                    public T getValue() {
                        return newValue;
                    }
                });
            } catch (YValidationException e) {
                getOnValidationFailed().accept(e);
            }
        });
    }
}
