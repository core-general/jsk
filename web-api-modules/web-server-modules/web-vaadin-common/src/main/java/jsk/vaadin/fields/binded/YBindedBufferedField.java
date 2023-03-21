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

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Log4j2
public abstract class YBindedBufferedField<T> extends YBindedField<T> {
    @Getter T oldValue;

    public void fireSaveEvent() throws YValidationException {
        T currentValues = generateModelValue();
        clearAndSetWithValue(currentValues);
        oldValue = currentValues;
    }

    public void fireCancelEvent() {
        if (oldValue == null) {
            oldValue = createEmptyModelInstance();
        }
        clearAndSetPresentation(oldValue);

    }

    @Override
    @SneakyThrows
    public void setValue(T value) {
        super.setValue(value);
        try {
            fireSaveEvent();
        } catch (YValidationException e) {}
    }


    @Override
    public T getValue() {
        return oldGetValue.apply();
    }

    protected void updateValue() {
        //intentionally - value is updated only when fireSaveEvent is invoked
    }

    @SneakyThrows
    private void clearAndSetWithValue(T currentValues) {
        super.setValue(currentValues);
        clearAndSetPresentation(currentValues);
    }

    @SneakyThrows
    private void clearAndSetPresentation(T newPresentationValue) {
        setPresentationValue(newPresentationValue);
    }
}
