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

import com.google.gson.annotations.JsonAdapter;
import lombok.Data;
import sk.outer.api.ios.purchases.iossub.JwsJsonAdapter;

@Data
public class OutIosSignInTokenResponse {
    private String access_token;
    private String token_type;
    private Long expires_in;
    private String refresh_token;
    @JsonAdapter(JwsJsonAdapter.class)
    private OutIosTokenPayload id_token;
}
