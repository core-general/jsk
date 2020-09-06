package sk.web.utils;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2020 Core General
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
import lombok.Getter;
import sk.utils.functional.O;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@AllArgsConstructor
@Getter
public class WebApiMethod<T> {
    Class<T> cls;

    /**
     * We can use this without a method at all
     */
    O<Method> oMethod;

    /**
     * If this method is auxiliary, some filters could ignore it
     */
    boolean auxiliaryMethod;

    public <X extends Annotation, Y extends Annotation> O<X> getAnnotation(Class<X> annotationClass) {
        return getAnnotation(annotationClass, O.empty());
    }

    public <X extends Annotation, Y extends Annotation> O<X> getAnnotation(Class<X> annotationClass, Class<Y> opposite) {
        return getAnnotation(annotationClass, O.ofNull(opposite));
    }

    private <X extends Annotation, Y extends Annotation> O<X> getAnnotation(Class<X> annotationClass, O<Class<Y>> opposite) {
        final O<X> annotation = oMethod.flatMap($ -> O.ofNull($.getAnnotation(annotationClass)))
                .or(() -> O.ofNull(cls.getAnnotation(annotationClass)));

        if (annotation.isPresent() && opposite.isPresent() &&
                oMethod.flatMap($ -> O.ofNull($.getAnnotation(opposite.get()))).isPresent()) {
            return O.empty();
        } else {
            return annotation;
        }
    }
}
