package sk.outer.api.ios.signin;

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

import lombok.Data;

@Data
public class OutIosTokenPayload {
    private String iss;
    private String aud;
    private Long exp;
    private Long iat;
    private String sub;//users unique id
    private String at_hash;
    private Long auth_time;
    private Boolean nonce_supported;
    private Boolean email_verified;
    private String email;
}
