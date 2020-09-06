package sk.db.relational.spring.services;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 Core General
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


import sk.utils.functional.F1;
import sk.utils.tuples.*;

import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public interface RdbTransactionManager {
    <A, T extends X1<A>>
    T transactionWithSaveX1(Supplier<T> howToGet, F1<T, List<Object>> whatToSave);


    <A1, A2, T extends X2<A1, A2>>
    T transactionWithSaveX2(Supplier<T> howToGet, F1<T, List<Object>> whatToSave);


    <A1, A2, A3, T extends X3<A1, A2, A3>>
    T transactionWithSaveX3(Supplier<T> howToGet, F1<T, List<Object>> whatToSave);


    <A1, A2, A3, A4, T extends X4<A1, A2, A3, A4>>
    T transactionWithSaveX4(Supplier<T> howToGet, F1<T, List<Object>> whatToSave);


    <A1, A2, A3, A4, A5, T extends X5<A1, A2, A3, A4, A5>>
    T transactionWithSaveX5(Supplier<T> howToGet, F1<T, List<Object>> whatToSave);


    <A1, A2, A3, A4, A5, A6, T extends X6<A1, A2, A3, A4, A5, A6>>
    T transactionWithSaveX6(Supplier<T> howToGet, F1<T, List<Object>> whatToSave);


    <A1, A2, A3, A4, A5, A6, A7, T extends X7<A1, A2, A3, A4, A5, A6, A7>>
    T transactionWithSaveX7(Supplier<T> howToGet, F1<T, List<Object>> toSave);


    default <A, T extends X1<A>>
    T transactionWithSaveX1(Supplier<T> howToGet) {
        return transactionWithSaveX1(howToGet, X1::asList);
    }


    default <A1, A2, T extends X2<A1, A2>>
    T transactionWithSaveX2(Supplier<T> howToGet) {
        return transactionWithSaveX2(howToGet, X2::asList);
    }


    default <A1, A2, A3, T extends X3<A1, A2, A3>>
    T transactionWithSaveX3(Supplier<T> howToGet) {
        return transactionWithSaveX3(howToGet, X3::asList);
    }


    default <A1, A2, A3, A4, T extends X4<A1, A2, A3, A4>>
    T transactionWithSaveX4(Supplier<T> howToGet) {
        return transactionWithSaveX4(howToGet, X4::asList);
    }


    default <A1, A2, A3, A4, A5, T extends X5<A1, A2, A3, A4, A5>>
    T transactionWithSaveX5(Supplier<T> howToGet) {
        return transactionWithSaveX5(howToGet, X5::asList);
    }


    default <A1, A2, A3, A4, A5, A6, T extends X6<A1, A2, A3, A4, A5, A6>>
    T transactionWithSaveX6(Supplier<T> howToGet) {
        return transactionWithSaveX6(howToGet, X6::asList);
    }


    default <A1, A2, A3, A4, A5, A6, A7, T extends X7<A1, A2, A3, A4, A5, A6, A7>>
    T transactionWithSaveX7(Supplier<T> howToGet) {
        return transactionWithSaveX7(howToGet, X7::asList);
    }

}
