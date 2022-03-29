package sk.outer.api.ios.purchases;

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

import sk.outer.api.ios.OutGeneralIosRequester;
import sk.outer.api.ios.purchases.iossub.model.StatusResponse;
import sk.services.http.IHttp;
import sk.services.ids.IIds;
import sk.utils.functional.OneOf;
import sk.utils.statics.Cc;

import javax.inject.Inject;

public class OutIosSubscriptionGatherer extends OutGeneralIosRequester<StatusResponse> {
    @Inject IHttp http;
    @Inject IIds ids;

    public OneOf<StatusResponse, Exception> getSubscriptionInfo(
            byte[] pkcs8PemFile,
            String issuerId,
            String keyID,
            String appBundleId,
            String transactionId,
            boolean sandbox) {

        return executeRequest(pkcs8PemFile, issuerId, keyID, "appstoreconnect-v1", sandbox,
                token -> http.get(String.format("https://api.storekit%s.itunes.apple.com/inApps/v1/subscriptions/%s",
                                sandbox ? "-sandbox" : "", transactionId))
                        .headers(Cc.m("Authorization", "Bearer " + token)),
                StatusResponse.class,
                builder -> builder.withPayload(Cc.m(
                        "nonce", ids.shortIdS(),
                        "bid", appBundleId))
        );
    }
}
