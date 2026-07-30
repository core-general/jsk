package sk.web.server.spark.context;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2026 Core General
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

import jakarta.servlet.http.Part;
import org.junit.jupiter.api.Test;
import sk.web.annotations.WebInputLimit;
import sk.web.server.model.WebInputLimitExceededException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
class WebJettyContextConsumer4SparkInputLimitTest {
    @Test
    void recognizesMultipartMediaTypeCaseAndParametersWithoutPrefixConfusion() {
        assertTrue(WebJettyContextConsumer4Spark.isMultipartContentType("multipart/form-data"));
        assertTrue(WebJettyContextConsumer4Spark.isMultipartContentType(
                "  MuLtIpArT/FoRm-DaTa ; boundary=\"abc\"; charset=UTF-8"));
        assertTrue(WebJettyContextConsumer4Spark.isMultipartContentType("multipart/form-data   "));
        assertFalse(WebJettyContextConsumer4Spark.isMultipartContentType(null));
        assertFalse(WebJettyContextConsumer4Spark.isMultipartContentType("text/plain"));
        assertFalse(WebJettyContextConsumer4Spark.isMultipartContentType("multipart/form-datax; boundary=abc"));
    }

    @Test
    void validatesCountIndividualAggregateAndUnknownPartSizes() {
        WebInputLimit limits = limits(2, 8, 12);
        Part five = part(5);
        Part seven = part(7);

        assertDoesNotThrow(() -> WebJettyContextConsumer4Spark.validateMultipartParts(
                List.of(five, seven), limits));
        assertThrows(WebInputLimitExceededException.class,
                () -> WebJettyContextConsumer4Spark.validateMultipartParts(
                        List.of(part(5), part(8)), limits));
        assertThrows(WebInputLimitExceededException.class,
                () -> WebJettyContextConsumer4Spark.validateMultipartParts(
                        List.of(part(9)), limits));
        assertThrows(WebInputLimitExceededException.class,
                () -> WebJettyContextConsumer4Spark.validateMultipartParts(
                        List.of(part(-1)), limits));
        assertThrows(WebInputLimitExceededException.class,
                () -> WebJettyContextConsumer4Spark.validateMultipartParts(
                        List.of(part(1), part(1), part(1)), limits));
    }

    private static WebInputLimit limits(int maxPartCount, long maxPartBytes, long maxAggregatePartBytes) {
        return new FixedInputLimit(maxPartCount, maxPartBytes, maxAggregatePartBytes);
    }

    private static Part part(long size) {
        return new FixedPart(size);
    }

    private record FixedInputLimit(
            int maxPartCount,
            long maxPartBytes,
            long maxAggregatePartBytes) implements WebInputLimit {
        @Override
        public long maxRequestBytes() {
            return 65_536;
        }

        @Override
        public String problemCode() {
            return "too_large";
        }

        @Override
        public String problemMessage() {
            return "Too large.";
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return WebInputLimit.class;
        }
    }

    private record FixedPart(long size) implements Part {
        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public String getContentType() {
            return null;
        }

        @Override
        public String getName() {
            return "part";
        }

        @Override
        public String getSubmittedFileName() {
            return null;
        }

        @Override
        public long getSize() {
            return size;
        }

        @Override
        public void write(String fileName) throws IOException {
        }

        @Override
        public void delete() throws IOException {
        }

        @Override
        public String getHeader(String name) {
            return null;
        }

        @Override
        public Collection<String> getHeaders(String name) {
            return List.of();
        }

        @Override
        public Collection<String> getHeaderNames() {
            return List.of();
        }
    }
}
