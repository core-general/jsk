package jsk.spark;

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

import jsk.spark.testmodel.SomeClass1;
import jsk.spark.testmodel.SomeClass2;
import jsk.spark.testmodel.SomeEnum;
import sk.web.annotations.WebPath;
import sk.web.annotations.type.WebGET;

/**
 * Just test class which we use 4 test Just test class which we use 4 test Just test class which we use 4 test Just test class
 * which we use 4 test Just test class which we use 4 test Just test class which we use 4 test Just test class which we use 4
 * test Just test class which we use 4 test Just test class which we use 4 test Just test class which we use 4 test Just test
 * class which we use 4 test Just test class which we use 4 test Just test class which we use 4 test Just test class which we
 * use 4 test Just test class which we use 4 test Just test class which we use 4 test Just test class which we use 4 test Just
 * test class which we use 4 test Just test class which we use 4 test Just test class which we use 4 test
 */
@WebPath("test")
public interface TestApi1 {

    /**
     * Just test method which we use 4 test Just test method which we use 4 test Just test method which we use 4 test Just test
     * method
     * which we use 4 test Just test method which we use 4 test Just test method which we use 4 test Just test method which we
     * use 4
     * test Just test method which we use 4 test Just test method which we use 4 test Just test method which we use 4 test Just
     * test
     * method which we use 4 test Just test method which we use 4 test Just test method which we use 4 test Just test method
     * which we
     * use 4 test Just test method which we use 4 test Just test method which we use 4 test Just test method which we use 4
     * test Just
     * test method which we use 4 test Just test method which we use 4 test Just test method which we use 4 test
     * <p>
     * "New line!"
     * Very New Line
     *
     * @param abc nice comment about parameter
     * @return returns something very strange
     * @throws ABC_ERR when this error occurs, we will not be happy
     * @throws CRB_ESK when this error occurs, we will not be happy twice
     * @throws CRB_ESK when this error occurs, we will not be happy twice
     */
    @WebGET
    @WebPath(value = "ajk/:abc", appendMethodName = false)
    String a(String abc);

    /**
     * @throws CRB_ESK when this error occurs, we will not be happy twice
     */
    @WebGET
    @WebPath(value = "less", appendMethodName = false)
    SomeClass1 b(SomeClass2 abc, SomeEnum x);

    //
    //@WebPOST
    //@WebAuth
    //@WebIdempotence(force = false)
    //Map<String, Integer> testWebUserToken(Map<String, String> a);
}
