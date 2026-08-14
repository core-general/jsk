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

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.junit.jupiter.api.Test;
import sk.web.annotations.WebInputLimit;
import sk.web.server.model.WebInputLimitExceededException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void writesBinaryResponseAsCompleteGzipStreamWithoutRawContentLength() throws Exception {
        byte[] body = "repeated-response-content-".repeat(100).getBytes();
        ByteArrayOutputStream written = new ByteArrayOutputStream();
        AtomicInteger contentLength = new AtomicInteger(-1);
        HttpServletResponse response = responseWritingTo(written, contentLength);

        WebJettyContextConsumer4Spark.writeResponseBytes(response, body, true);

        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(written.toByteArray()))) {
            assertArrayEquals(body, gzip.readAllBytes());
        }
        assertEquals(-1, contentLength.get());
    }

    @Test
    void writesUncompressedBinaryResponseWithItsContentLength() throws Exception {
        byte[] body = new byte[]{1, 2, 3, 4};
        ByteArrayOutputStream written = new ByteArrayOutputStream();
        AtomicInteger contentLength = new AtomicInteger(-1);
        HttpServletResponse response = responseWritingTo(written, contentLength);

        WebJettyContextConsumer4Spark.writeResponseBytes(response, body, false);

        assertArrayEquals(body, written.toByteArray());
        assertEquals(body.length, contentLength.get());
    }

    private static HttpServletResponse responseWritingTo(ByteArrayOutputStream output, AtomicInteger contentLength) {
        ServletOutputStream stream = new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
            }

            @Override
            public void write(int value) {
                output.write(value);
            }
        };
        return (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getOutputStream" -> stream;
                    case "setContentLength" -> {
                        contentLength.set((Integer) args[0]);
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
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
