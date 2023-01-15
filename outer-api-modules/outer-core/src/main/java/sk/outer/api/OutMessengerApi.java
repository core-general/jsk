package sk.outer.api;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2022 Core General
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
import sk.utils.functional.O;
import sk.utils.tuples.X2;

public interface OutMessengerApi<IMG, SPECIAL, BUTTONS, RESPONSE> {
    public RESPONSE send(String userId, O<String> text, O<IMG> image, O<SPECIAL> special,
            O<X2<String, byte[]>> document,
            O<BUTTONS> buttons);

    public default RESPONSE editMessageText(String userId, String messageId, String newText, O<BUTTONS> newButtons) {
        throw new NotImplementedException();
    }

    public default RESPONSE deleteMessage(String userId, String messageId) {
        throw new NotImplementedException();
    }
}
