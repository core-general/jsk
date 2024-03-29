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

import jsk.spark.testmodel.SomeClass4;
import sk.web.WebMethodType;
import sk.web.annotations.WebPath;
import sk.web.annotations.WebRender;
import sk.web.annotations.type.WebGET;
import sk.web.annotations.type.WebMethod;
import sk.web.annotations.type.WebPOST;
import sk.web.renders.WebRenderType;

import java.util.List;

@SuppressWarnings("JavadocReference")
@WebPath("test3")
public interface TestApiSwaggerTest3 {
    @WebGET
    int testInt(int a);

    /**
     * Test method
     *
     * @param a test parameter
     * @return test stuff
     * @throws BAD      exception
     * @throws VERY_BAD exception
     * @throws AWFUL    exception
     */
    @WebPOST
    @WebPath("level1")
    byte[] testIntPost(int a);

    @WebRender(WebRenderType.RAW_STRING)
    @WebMethod(method = WebMethodType.POST_BODY)
    String testIntPostMultiForce(byte[] bd);

    @WebRender(WebRenderType.RAW_STRING)
    @WebMethod(method = WebMethodType.POST_MULTI)
    String testIntPostMultiForce2(byte[] bd, String x);

    @WebRender(WebRenderType.RAW_STRING)
    @WebMethod(method = WebMethodType.POST_MULTI_SURE)
    String testIntPostMultiForce3(byte[] bd, int z);

    SomeClass4 testClass(SomeClass4 cls);

    List<String> testClassXX(List<String> cls);
}
