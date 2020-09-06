package sk.services.oauth;

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

import lombok.AllArgsConstructor;
import lombok.Data;
import sk.utils.functional.GSet;
import sk.utils.functional.O;
import sk.utils.functional.Sett;

/**
 * Idea:
 * 1. We have code and try to change it to access and refresh token
 * 2. After succesfull change we can execute requests
 * 3. If request fails because access token is old - we try to revoke it using refresh token
 * 4. If revoking using refresh token fails - we throw exception, which signalizes that we have to ask user to get then new code
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class IOAuthClient<CLIENT> {
    protected abstract TokensResponse exchangeCodeToRefreshAndAccess(String code) throws IOAuthCantExchangeCode;

    protected abstract TokensResponse exchangeRefreshToAccessAndPossiblyUpdateRefresh(String refresh)
            throws IOAuthCantExchangeRefreshToken;

    protected abstract CLIENT getClientByAccessToken(String accessToken) throws IOAuthCantInstantiateClient;

    @SuppressWarnings("ConstantConditions")
    public void processCode(String code, Sett<String> setRefresh, Sett<String> setAccess) throws IOAuthCantExchangeCode {
        TokensResponse refreshAndAccessTokens = exchangeCodeToRefreshAndAccess(code);
        setRefresh.accept(refreshAndAccessTokens.getRefreshToken());
        O.ofNullable(refreshAndAccessTokens.getAccessToken())
                .or(() -> {
                    String accessToken = null;
                    try {
                        final TokensResponse newToken =
                                updateRefreshAndUpdate(setRefresh, setAccess, refreshAndAccessTokens.getRefreshToken());
                        accessToken = newToken.getAccessToken();
                    } catch (IOAuthCantExchangeRefreshToken ignored) { }
                    return O.ofNullable(accessToken);
                })
                .ifPresent(setAccess);
    }

    public <T> T executeRequest(IOAuthExecutor<T, CLIENT> executor, GSet<String> gSetRefreshToken, GSet<String> gSetAccessToken)
            throws IOAuthCantExchangeRefreshToken {
        int problemCreatingClient = 0;
        int problemExchangingToken = 0;
        while (true) {
            CLIENT client;
            try {
                client = getClientByAccessToken(gSetAccessToken.get());
                problemCreatingClient = 0;
            } catch (Exception exception) {
                if (problemCreatingClient == 0) {
                    updateRefreshAndUpdate(gSetRefreshToken, gSetAccessToken, gSetRefreshToken.get());
                    problemCreatingClient++;
                } else {
                    throw new IOAuthCantExchangeRefreshToken(exception);
                }
                continue;
            }

            try {
                return executor.execute(client);
            } catch (Exception except) {
                if (problemExchangingToken == 0) {
                    updateRefreshAndUpdate(gSetRefreshToken, gSetAccessToken, gSetRefreshToken.get());
                    problemExchangingToken++;
                } else {
                    throw new IOAuthCantExchangeRefreshToken(except);
                }
            }
        }
    }

    private TokensResponse updateRefreshAndUpdate(Sett<String> setRefresh, Sett<String> setAccess,
            String refreshToken) throws IOAuthCantExchangeRefreshToken {
        TokensResponse accessAndRefresh = exchangeRefreshToAccessAndPossiblyUpdateRefresh(refreshToken);
        setAccess.setIfNotNull(accessAndRefresh.getAccessToken());
        setRefresh.setIfNotNull(accessAndRefresh.getRefreshToken());
        return accessAndRefresh;
    }

    @Data
    @AllArgsConstructor
    public static class TokensResponse {
        private String accessToken;
        private String refreshToken;
    }

    public interface IOAuthExecutor<T, CLIENT> {
        T execute(CLIENT client) throws IOAuthCantUseAccessToken;
    }


    public static abstract class IOAuthException extends Exception {
        public IOAuthException() {
        }

        public IOAuthException(Throwable cause) {
            super(cause);
        }

        public IOAuthException(String message) {
            super(message);
        }

        public IOAuthException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class IOAuthCantExchangeCode extends IOAuthException {
        public IOAuthCantExchangeCode() {
        }

        public IOAuthCantExchangeCode(Throwable cause) {
            super(cause);
        }

        public IOAuthCantExchangeCode(String message) {
            super(message);
        }

        public IOAuthCantExchangeCode(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class IOAuthCantExchangeRefreshToken extends IOAuthException {
        public IOAuthCantExchangeRefreshToken() {
        }

        public IOAuthCantExchangeRefreshToken(Throwable cause) {
            super(cause);
        }

        public IOAuthCantExchangeRefreshToken(String message) {
            super(message);
        }

        public IOAuthCantExchangeRefreshToken(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class IOAuthCantUseAccessToken extends IOAuthException {
        public IOAuthCantUseAccessToken() {
        }

        public IOAuthCantUseAccessToken(Throwable cause) {
            super(cause);
        }

        public IOAuthCantUseAccessToken(String message) {
            super(message);
        }

        public IOAuthCantUseAccessToken(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class IOAuthCantInstantiateClient extends IOAuthException {
        public IOAuthCantInstantiateClient() {
        }

        public IOAuthCantInstantiateClient(Throwable cause) {
            super(cause);
        }

        public IOAuthCantInstantiateClient(String message) {
            super(message);
        }

        public IOAuthCantInstantiateClient(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
