package sk.web.renders.inst;

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

import sk.exceptions.NotImplementedException;
import sk.utils.functional.OneOf;
import sk.web.renders.WebRender;

public abstract class WebByteRenderBase implements WebRender {
    protected String ifNotByteThenThisContentType() {
        throw new NotImplementedException();
    }

    protected String stringValueProvider(Object val) {
        throw new NotImplementedException();
    }

    protected boolean forceDeflate() {
        return false;
    }

    @Override
    public String contentHeaderProvider(Object val, OneOf<String, byte[]> bs) {
        return bs.isRight() ? "application/octet-stream" : ifNotByteThenThisContentType();
    }

    @Override
    public boolean allowDeflation(Object val, OneOf<String, byte[]> bs) {
        return forceDeflate() || bs.isRight() ? false : true;
    }

    @Override
    public OneOf<String, byte[]> valueProvider(Object val) {
        return (val instanceof byte[])
                ? OneOf.right((byte[]) val)
                : OneOf.left(stringValueProvider(val));
    }
}
