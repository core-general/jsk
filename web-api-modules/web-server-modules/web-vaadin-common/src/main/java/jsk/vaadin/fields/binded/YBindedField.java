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

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.data.binder.*;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.converter.ConverterFactory;
import com.vaadin.flow.data.value.HasValueChangeMode;
import com.vaadin.flow.data.value.ValueChangeMode;
import jsk.vaadin.fields.FieldWithGeneratedValue;
import jsk.vaadin.fields.pofi.HasBinder;
import jsk.vaadin.fields.pofi.YAbstractFieldBase;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.vaadin.firitin.components.customfield.VCustomField;
import sk.utils.functional.C2;
import sk.utils.functional.F0;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;
import sk.utils.statics.Re;
import sk.utils.tuples.X;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
public abstract class YBindedField<OUTER_MODEL> extends VCustomField<OUTER_MODEL>
        implements FieldWithGeneratedValue<OUTER_MODEL> {
    protected __Binder<OUTER_MODEL> binder;
    protected Class<OUTER_MODEL> cls;
    protected boolean inited;

    protected Component fieldContainer;

    protected abstract Component putFieldsToContainer();

    protected F0<OUTER_MODEL> oldGetValue;

    public YBindedField() {
        oldGetValue = () -> super.getValue();
    }

    @Override
    public OUTER_MODEL generateModelValue() {
        try {
            privateInit();
            OUTER_MODEL newInstance = createEmptyModelInstance();
            binder.writeBeanRecursivelyWithActualFieldValues(newInstance);
            return newInstance;
        } catch (ValidationException e) {
            //todo add more info about validation (it seems it need som hacks)
            throw new YValidationException(e);
        }
    }

    @Override
    public OUTER_MODEL getValue() {
        return generateModelValue();
    }

    @Override
    @SneakyThrows
    public void setPresentationValue(OUTER_MODEL newPresentationValue) {
        privateInit();
        binder.readBean(newPresentationValue);
    }

    public Binder<OUTER_MODEL> getBinder() {
        privateInit();
        return binder;
    }

    @Override
    protected boolean valueEquals(OUTER_MODEL value1, OUTER_MODEL value2) {
        return false;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        privateInit();
        super.onAttach(attachEvent);
    }

    @NotNull
    @SneakyThrows
    protected OUTER_MODEL createEmptyModelInstance() {
        return cls.getConstructor().newInstance();
    }

    protected Component wrapFieldsIfNeeded(Component fields) {return fields;}

    protected void afterInitFinished() {}

    protected void rerender() {
        privateInit();
        getChildren().forEach($ -> remove($));
        add(wrapFieldsIfNeeded(fieldContainer));
    }

    @SneakyThrows
    protected void privateInit() {
        if (!inited) {
            inited = true;

            autoInitFieldsIfNeeded();
            fieldContainer = putFieldsToContainer();
            rerender();
            setAllFieldsDefaultValueChangeMode();

            {

                this.cls = (Class<OUTER_MODEL>) Re.getParentParameters(this.getClass())
                        .map($ -> $[0])
                        .filter($ -> $ instanceof Class || $ instanceof ParameterizedType)
                        .map($ -> ($ instanceof Class)
                                  ? (Class<?>) $
                                  : (Class<?>) ((ParameterizedType) $).getRawType()).get();
            }
            this.binder = new __Binder<>(cls);
            bindOurself(binder, cls);
            try {
                setValue(createEmptyModelInstance());
            } catch (YValidationException e) {
                log.error("", e);
            }

            //this.binder.bindInstanceFields(this);
        }
    }

    private void bindOurself(Binder<OUTER_MODEL> binder, Class<OUTER_MODEL> outerCls) {
        Re.getAllNonStaticFields(this.getClass()).stream()
                .filter($ -> HasValue.class.isAssignableFrom($.getType()))
                .forEach($ -> bindOneField(binder, $, outerCls));
    }

    private <FIELD_TYPE, NEW_TYPE> void bindOneField(Binder<OUTER_MODEL> binder, Field fld, Class<OUTER_MODEL> outerCls) {
        final F1<Object, Object> oGetter = Re.getter(fld);
        final HasValue<?, FIELD_TYPE> field = (HasValue<?, FIELD_TYPE>) oGetter.apply(this);

        Binder.BindingBuilder finalBuilder;
        if (field instanceof YAbstractFieldBase) {
            YAbstractFieldBase<NEW_TYPE, ?, ?>
                    typedField = (YAbstractFieldBase<NEW_TYPE, ?, ?>) field;

            //converter
            Binder.BindingBuilder<OUTER_MODEL, NEW_TYPE> builder = binder.forField(typedField);

            //validators
            if (typedField.getValidators().size() > 0) {
                final List<Validator<NEW_TYPE>> validators = typedField.getValidators();
                for (Validator<NEW_TYPE> validator : validators) {
                    final Object o = validators.get(0);
                    builder = builder.withValidator(validator);
                }
            }

            finalBuilder = builder;
        } else {
            finalBuilder = binder.forField((HasValue<?, String>) field);
        }

        //binding to outer properties
        final String name = fld.getName();
        try {
            final Field targetField =
                    Re.getAllNonStaticFields(outerCls).stream().filter($ -> $.getName().equals(name)).findAny().get();
            final F1<OUTER_MODEL, NEW_TYPE> oTargetFieldGetter =
                    (F1<OUTER_MODEL, NEW_TYPE>) (Object) Re.getter(targetField);
            final O<C2<OUTER_MODEL, NEW_TYPE>> oTargetFieldSetter =
                    (O<C2<OUTER_MODEL, NEW_TYPE>>) (Object) Re.setter(targetField);
            if (oTargetFieldSetter.isEmpty()) {
                throw new RuntimeException(
                        "Setter is final for field: %s.%s".formatted(outerCls.getName(), fld.getName()));
            }

            final Binder.Binding<?, ?> bind = finalBuilder.bind(
                    outer_model -> oTargetFieldGetter.apply((OUTER_MODEL) outer_model),
                    (outer_model, new_type) -> oTargetFieldSetter.get().accept((OUTER_MODEL) outer_model, (NEW_TYPE) new_type)
            );
            if (field instanceof HasBinder hb) {
                hb.setBinder(X.x(binder, bind));
            }

        } catch (Exception e) {
            log.error(outerCls + "", e);
            throw new RuntimeException(e);
        }
    }

    private void setAllFieldsDefaultValueChangeMode() {
        // workaround since Viritin components set ValueChangeMode to Lazy and it makes problems when changing fields
        Re.getAllNonStaticFields(this.getClass()).stream()
                .filter($ -> HasValueChangeMode.class.isAssignableFrom($.getType()))
                .forEach($ -> {
                    final F1<Object, Object> oGetter = Re.getter($);
                    final HasValueChangeMode apply = (HasValueChangeMode) oGetter.apply(this);
                    apply.setValueChangeMode(ValueChangeMode.ON_CHANGE);
                });
    }

    protected void autoInitFieldsIfNeeded() {
        //auto field initialization
        Re.getAllNonStaticFields(this.getClass()).stream().filter($ -> AbstractField.class.isAssignableFrom($.getType()))
                .filter($ -> {
                    //check that the field is not inited yet
                    final F1<Object, Object> getter = Re.getter($);
                    final Object value = getter.apply(this);
                    return value == null;
                }).forEach($ -> {
                    //init field value with the default constructor
                    final O<C2<Object, Object>> setter = Re.setter($);
                    if (setter.isEmpty()) {
                        throw new RuntimeException("No public setter for field: %s".formatted($.getName()));
                    }
                    try {
                        Object newInstance = $.getType().getConstructor().newInstance();
                        setter.get().accept(this, newInstance);
                    } catch (Exception e) {
                        throw new RuntimeException(
                                "No default constructor for %s %s (please init it manually in assignFields())".formatted(
                                        $.getType().getSimpleName(), $.getName()), e);
                    }
                });
    }

    private static class __Binder<T> extends Binder<T> {

        //region THIS REGION NEEDED FOR VALIDATORS WORK CORRECTLY IN CASE WE SAVE REQUEST
        public __Binder(Class<T> cls) {
            super(cls);
        }

        /** To force bean use generateModelValue instead of getValue on the validation step */
        public void writeBeanRecursivelyWithActualFieldValues(T bean) throws ValidationException {
            BinderValidationStatus<T> status = doValidateActualFields(bean, getBindings());
            if (status.hasErrors()) {
                throw new ValidationException(status.getFieldValidationErrors(), status.getBeanValidationErrors());
            } else {
                super.writeBean(bean);
            }
        }

        /** FOR writeBeanRecursivelyWithActualFieldValues */
        private BinderValidationStatus<T> doValidateActualFields(T bean, Collection<BindingImpl<T, ?, ?>> bindings)
                throws ValidationException {
            final List<BindingValidationStatus<?>> bindingValidationStatuses =
                    (List<BindingValidationStatus<?>>) (Object) bindings.stream()
                            .map(b -> {
                                if (b.getField() instanceof FieldWithGeneratedValue<?> cf) {
                                    Object fieldValue = cf.generateModelValue();
                                    Converter converter = fieldsToConverters.get(cf);
                                    Result<?> result = converter
                                            .convertToModel(fieldValue, new ValueContext((Component) cf, (HasValue<?, ?>) cf));
                                    return new BindingValidationStatus(result, b);
                                } else {
                                    return null;
                                }
                            })
                            .filter(Fu.notNull())
                            .filter($ -> $.isError())
                            .collect(Collectors.toList());
            return new BinderValidationStatus<T>(this, bindingValidationStatuses, Cc.l());
        }

        /** To allow our custom validation to take place for writeBeanRecursivelyWithActualFieldValues */
        IdentityHashMap<FieldWithGeneratedValue<?>, Converter<?, ?>> fieldsToConverters = new IdentityHashMap<>();

        /**
         * To allow writeBeanRecursivelyWithActualFieldValues work, so that we can start the chain of converters ourself (they
         * are private in Vaadin)
         */
        @Override
        protected <FIELDVALUE, TARGET> BindingBuilder<T, TARGET> doCreateBinding(
                HasValue<?, FIELDVALUE> field,
                Converter<FIELDVALUE, TARGET> converter,
                BindingValidationStatusHandler handler) {
            if (field instanceof FieldWithGeneratedValue<?> cf) {
                fieldsToConverters.put(cf, converter);
            }
            return new __BindingBuilderImplThis<>(this, field, converter, handler);
        }

        /** Only to make doCreateBinding work */
        protected static class __BindingBuilderImplThis<BEAN, FIELDVALUE, TARGET>
                extends BindingBuilderImpl<BEAN, FIELDVALUE, TARGET> {
            protected __BindingBuilderImplThis(Binder<BEAN> binder, HasValue<?, FIELDVALUE> field,
                    Converter<FIELDVALUE, TARGET> converterValidatorChain, BindingValidationStatusHandler statusHandler) {
                super(binder, field, converterValidatorChain, statusHandler);
            }
        }
        //endregion


        /** TO FORCE ONLY OUR CONVERTERS */
        @Override
        protected ConverterFactory getConverterFactory() {
            return new ConverterFactory() {
                @Override
                public <P, M> Optional<Converter<P, M>> newInstance(Class<P> presentationType, Class<M> modelType) {
                    return Optional.empty();
                }
            };
        }
    }
}
