package jsk.vaadin.fields.pofi;

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

import com.vaadin.flow.component.textfield.TextField;
import org.vaadin.firitin.components.textfield.VTextField;

public abstract class YTextFieldBase<MODEL, SUBCLASS extends YTextFieldBase<MODEL, SUBCLASS>>
        extends YAbstractFieldBase<MODEL, TextField, SUBCLASS> {

    public YTextFieldBase(String caption) {
        super(new VTextField(caption));
    }

    public YTextFieldBase() {
        super(new VTextField());
    }

    public void setWdth(String width) {
        _rawDataField.getStyle().set("width", width);
    }

    public String getRawValueWithoutValidation() {
        return _rawDataField.getValue();
    }

    public String getLabel() {
        return _rawDataField.getLabel();
    }

    public void setLabel(String label) {
        _rawDataField.setLabel(label);
    }
}
