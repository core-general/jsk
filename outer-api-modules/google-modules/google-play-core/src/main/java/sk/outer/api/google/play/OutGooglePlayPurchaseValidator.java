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
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.model.ProductPurchase;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.log4j.Log4j2;
import sk.services.json.IJson;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;

import static com.google.auth.oauth2.GoogleCredentials.fromStream;
import static java.nio.charset.StandardCharsets.UTF_8;

@Log4j2
public class OutGooglePlayPurchaseValidator {
    private HttpTransport transport;
    private JsonFactory jsonFactory;
    @Inject IJson json;

    @PostConstruct
    void init() {
        transport = new NetHttpTransport();
        jsonFactory = new JacksonFactory();
    }

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
            ProductPurchase purchase = new AndroidPublisher.Builder(transport, jsonFactory, requestInitializer)
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
}
