package sk.outer.api.google.play;

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

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.model.ProductPurchase;
import com.google.api.services.androidpublisher.model.SubscriptionPurchase;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import sk.services.json.IJson;
import sk.utils.functional.OneOf;

import java.io.ByteArrayInputStream;

import static com.google.auth.oauth2.GoogleCredentials.fromStream;
import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class OutGooglePlayPurchaseValidator {
    private HttpTransport transport;
    private GsonFactory gsonFactory;
    @Inject IJson json;

    @PostConstruct
    void init() {
        transport = new NetHttpTransport();
        gsonFactory = new GsonFactory();
    }

    //https://developers.google.com/identity/protocols/oauth2/service-account
    public OutGooglePurchaseResult verifyPurchase_v_3_0(
            String jsonFileSecret,
            String GOOGLE_PRODUCT_NAME,
            String GOOGLE_PACKAGE_NAME,
            String productId,
            String purchaseToken
    ) {
        try {
            GoogleCredentials googleCredentials = fromStream(new ByteArrayInputStream(jsonFileSecret.getBytes(UTF_8)));
            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(googleCredentials);
            ProductPurchase purchase = new AndroidPublisher.Builder(transport, gsonFactory, requestInitializer)
                    .setApplicationName(GOOGLE_PRODUCT_NAME)
                    .build().purchases().products()
                    .get(GOOGLE_PACKAGE_NAME, productId, purchaseToken).execute();
            if (purchase != null && purchase.getPurchaseState() == 0) {
                if (purchase.getConsumptionState() == 0) {
                    return new OutGooglePurchaseResult(json.to(purchase), OutGooglePurchaseState.OWNED);
                } else {
                    return new OutGooglePurchaseResult(json.to(purchase), OutGooglePurchaseState.CONSUMED);
                }
            } else {
                return new OutGooglePurchaseResult(json.to(purchase), OutGooglePurchaseState.BAD);
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new OutGooglePurchaseResult(e.getMessage(), OutGooglePurchaseState.BAD);
        }
    }

    //https://developers.google.com/identity/protocols/oauth2/service-account
    public OneOf<SubscriptionPurchase, Exception> getSubscription_v_3_0_raw(
            String jsonFileSecret,
            String GOOGLE_PRODUCT_NAME,
            String GOOGLE_PACKAGE_NAME,
            String productId,
            String purchaseToken
    ) {
        try {
            GoogleCredentials googleCredentials = fromStream(new ByteArrayInputStream(jsonFileSecret.getBytes(UTF_8)));
            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(googleCredentials);

            return OneOf.left(new AndroidPublisher.Builder(transport, gsonFactory, requestInitializer)
                    .setApplicationName(GOOGLE_PRODUCT_NAME)
                    .build().purchases().subscriptions()
                    .get(GOOGLE_PACKAGE_NAME, productId, purchaseToken).execute());

        } catch (Exception e) {
            return OneOf.right(e);
        }
    }
}
