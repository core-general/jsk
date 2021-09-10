package sk.utils.statics;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2021 Core General
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

import java.nio.ByteBuffer;

public class Ar {
    public static byte[] intToByteArray(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    public static byte[] longToByteArray(long value) {
        return ByteBuffer.allocate(8).putLong(value).array();
    }

    public static int bArrToInt(byte[] value) {
        return ByteBuffer.wrap(value).getInt();
    }

    public static long bArrToLong(byte[] value) {
        return ByteBuffer.wrap(value).getLong();
    }

    //public static void main(String[] args) {
    //    var v = Cc.l(
    //            longToByteArray(Long.MAX_VALUE),
    //            longToByteArray(0l),
    //            longToByteArray(Long.MIN_VALUE),
    //            bArrToLong(longToByteArray(Long.MAX_VALUE)),
    //            bArrToLong(longToByteArray(0l)),
    //            bArrToLong(longToByteArray(Long.MIN_VALUE)),
    //
    //            intToByteArray(Integer.MAX_VALUE),
    //            intToByteArray(0),
    //            intToByteArray(Integer.MIN_VALUE),
    //            bArrToInt(intToByteArray(Integer.MAX_VALUE)),
    //            bArrToInt(intToByteArray(0)),
    //            bArrToInt(intToByteArray(Integer.MIN_VALUE))
    //    );
    //}
}
