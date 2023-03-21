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

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.data.binder.*;
import com.vaadin.flow.data.value.HasValueChangeMode;
import com.vaadin.flow.data.value.ValueChangeMode;
import jsk.vaadin.fields.FieldWithGeneratedValue;
import lombok.Getter;
import lombok.Setter;
import org.vaadin.firitin.components.customfield.VCustomField;
import sk.utils.statics.Cc;
import sk.utils.tuples.X2;

import javax.validation.ValidationException;
import java.util.List;

public abstract class YAbstractFieldBase<MODEL,
        FIELD extends AbstractField<FIELD, String> & HasValueChangeMode & HasValidation,
        SUBCLASS extends YAbstractFieldBase<MODEL, FIELD, SUBCLASS>>
        extends VCustomField<MODEL>
        implements FieldWithConverter<MODEL>,
                   FieldWithValidator<MODEL>,
                   HasValueChangeMode,
                   FieldWithGeneratedValue<MODEL>, HasBinder {

    protected FIELD _rawDataField;
    private List<Validator<MODEL>> validators = Cc.l();

    @Getter
    @Setter
    private X2<Binder<?>, Binder.Binding<?, ?>> binder;

    public YAbstractFieldBase(FIELD field) {
        _rawDataField = field;
        add(_rawDataField);
    }

    @Override
    public List<Validator<MODEL>> getValidators() {
        return validators;
    }

    @Override
    public SUBCLASS withValidator(Validator<MODEL> validator) {
        validators.add((value, context) -> {
            final ValidationResult result = validator.apply(value, context);
            if (result.isError()) {
                YAbstractFieldBase.this.forceInvalid(true);
            }
            return result;
        });
        return (SUBCLASS) this;
    }

    @Override
    public SUBCLASS withNullBadValidator(String textForNull) {
        return withValidator((value, context) -> value == null
                                                 ? ValidationResult.error(textForNull)
                                                 : ValidationResult.ok());
    }

    @Override
    protected boolean valueEquals(MODEL value1, MODEL value2) {
        return false;
    }

    @Override
    public ValueChangeMode getValueChangeMode() {
        return _rawDataField.getValueChangeMode();
    }

    @Override
    public void setValueChangeMode(ValueChangeMode valueChangeMode) {
        _rawDataField.setValueChangeMode(valueChangeMode);
    }

    protected void updateValue() {
        try {
            final MODEL newModelValue = generateModelValue();
            setModelValue(newModelValue, true);
        } catch (Exception e) {
            //validation error
        }
    }

    @Override
    protected void setPresentationValue(MODEL newPresentationValue) {
        _rawDataField.setValue(getConverter().convertToPresentation(newPresentationValue, new ValueContext(this)));
    }

    @Override
    public MODEL generateModelValue() {
        final Result<MODEL> modelResult = getConverter().convertToModel(_rawDataField.getValue(), new ValueContext(this));
        if (modelResult.isError()) {
            forceInvalid(true);
            if (this.getBinder() != null) {
                this.getBinder().i1().getValidationStatusHandler().statusChange(
                        new BinderValidationStatus(
                                getBinder().i1(),
                                Cc.<BindingValidationStatus<?>>l(new BindingValidationStatus(modelResult,
                                        this.getBinder().i2())),
                                Cc.lEmpty()
                        )
                );
            }
            throw new ValidationException("Wrong format");
        } else {
            final MODEL model = modelResult.getOrThrow(s -> new RuntimeException());
            forceInvalid(false);
            return model;
        }
    }

    private void forceInvalid(boolean invalid) {
        _rawDataField.setInvalid(invalid);
        setInvalid(invalid);
    }
}
