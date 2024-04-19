package sk.outer.api.fb;

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

import facebook4j.Facebook;
import facebook4j.FacebookFactory;
import facebook4j.User;
import facebook4j.auth.AccessToken;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import sk.outer.api.OutSimpleUserInfo;
import sk.utils.functional.O;
import sk.utils.statics.Ex;


@Slf4j
public class OutFbLoginService {
    private FacebookFactory facebookFactory;

    @PostConstruct
    public OutFbLoginService init() {
        facebookFactory = new FacebookFactory();
        return this;
    }

    public O<OutSimpleUserInfo> getSimpleUser(String facebookAccessToken, String faceBookUserId) {
        Facebook facebook = facebookFactory.getInstance();
        facebook.setOAuthAppId(""/*it's ok*/, ""/*it's ok*/);
        facebook.setOAuthAccessToken(new AccessToken(facebookAccessToken, null));
        User user = null;
        user = Ex.toRuntime(() -> facebook.getUser(faceBookUserId));
        return O.of(new OutSimpleUserInfo(user.getId(), user.getName()));
    }
}
