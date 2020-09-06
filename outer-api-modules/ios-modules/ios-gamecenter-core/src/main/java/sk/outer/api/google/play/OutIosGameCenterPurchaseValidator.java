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

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import sk.services.bytes.IBytes;
import sk.services.http.IHttp;
import sk.services.json.IJson;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

@Log4j2
public class OutIosGameCenterPurchaseValidator {
    @Inject IJson json;
    @Inject IBytes crypt;
    @Inject IHttp http;


    public OutIosPurchaseResult verifyPurchase(String BUNDLE_ID, String PASSWORD, String transactionData, String itemId) {
        return verifyPurchase(BUNDLE_ID, PASSWORD, transactionData, itemId, Optional.empty());
    }

    public OutIosPurchaseResult verifyPurchase(String BUNDLE_ID, String PASSWORD, String transactionData, String itemId,
            Optional<String> receipt) {
        return verifyMe(BUNDLE_ID, PASSWORD, transactionData, itemId, false, receipt);
    }

    @SneakyThrows
    private OutIosPurchaseResult verifyMe(String bundle_id, String password, String transactionData, String itemId,
            boolean sandbox, Optional<String> receipt) {
        String curl;
        if (sandbox) {
            curl = "https://sandbox.itunes.apple.com/verifyReceipt";
        } else {
            curl = "https://buy.itunes.apple.com/verifyReceipt";
        }

        String rawResponse = http.postBody(curl)
                .body(json.to(Cc.m(
                        "receipt-data", transactionData,
                        "password", password
                )))
                .headers(Cc.m("Content-Type", "application/json;charset=utf-8"))
                .goAndThrow();

        IosResponse iosResponse = this.json.from(rawResponse, IosResponse.class);
        if ("21007".equals(iosResponse.getStatus())) {
            return verifyMe(bundle_id, password, transactionData, itemId, true, receipt);
        }

        return new OutIosPurchaseResult(rawResponse, isValid(bundle_id, iosResponse, itemId, receipt));
    }

    private boolean isValid(String bundle_id, IosResponse iosResponse, String itemId,
            Optional<String> receipt) {
        if (iosResponse == null
                || iosResponse.getStatus() == null
                || iosResponse.getReceipt() == null
                || iosResponse.getReceipt().getBundle_id() == null
                || iosResponse.getReceipt().getIn_app() == null
        ) {
            return false;
        }

        return receipt
                .flatMap($ -> getProductId($))
                .map($ -> itemId == null || Fu.equal($, itemId))
                .orElseGet(() -> (itemId == null ||
                        iosResponse.getReceipt().getIn_app().stream().anyMatch($ -> Fu.equal(itemId, $.getProduct_id()))))
                && Fu.equal(bundle_id, iosResponse.getReceipt().getBundle_id())
                && "0".equals(iosResponse.getStatus());
    }

    public Optional<String> getProductId(String receipt) {
        try {
            String full = iosToJson(new String(crypt.dec64(receipt), UTF_8));
            String purchInfoB64 = this.json.from(full, IosReceipt.class).getPurchaseInfo();
            String purchInfoJson = iosToJson(new String(crypt.dec64(purchInfoB64), UTF_8));
            String productid = this.json.from(purchInfoJson, IosPurchaseInfo.class).getProductid();
            return Optional.of(productid);
        } catch (Exception e) {
            log.error("", e);
            return Optional.empty();
        }
    }

    @NotNull
    private String iosToJson(String in) {
        String replace = in.replace("\" = \"", "\" : \"").replace("\";", "\",");
        return replace.substring(0, replace.lastIndexOf(',')) + "}";
    }

    @Data
    private static class IosResponse {
        String status;
        IosReceipt receipt;

        @Data
        private static class IosReceipt {
            String bundle_id;
            List<IosInapp> in_app;
        }

        @Data
        private static class IosInapp {
            String product_id;
            String transaction_id;
        }
    }

    @Data
    private static class IosReceipt {
        @SerializedName("signing-status") int signingStatus;
        @SerializedName("purchase-info") String purchaseInfo;
        String signature;
        String pod;
    }

    @Data
    public static class IosPurchaseInfo {
        @SerializedName("original-purchase-date-pst") String originalpurchasedatepst;
        @SerializedName("purchase-date-ms") String purchasedatems;
        @SerializedName("original-transaction-id") String originaltransactionid;
        @SerializedName("bvrs") String bvrs;
        @SerializedName("app-item-id") String appitemid;
        @SerializedName("transaction-id") String transactionid;
        @SerializedName("quantity") String quantity;
        @SerializedName("original-purchase-date-ms") String originalpurchasedatems;
        @SerializedName("item-id") String itemid;
        @SerializedName("version-external-identifier") String versionexternalidentifier;
        @SerializedName("product-id") String productid;
        @SerializedName("purchase-date") String purchasedate;
        @SerializedName("original-purchase-date") String originalpurchasedate;
        @SerializedName("bid") String bid;
        @SerializedName("purchase-date-pst") String purchasedatepst;
    }
}
