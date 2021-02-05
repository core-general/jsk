package jsk.s3;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2021 Core General
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

import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import sk.aws.AwsPlainPropertiesImpl;
import sk.aws.s3.comparetool.S3CompareTool;
import sk.aws.s3.comparetool.S3SyncTool;
import sk.aws.s3.comparetool.model.S3CompareInput;
import sk.aws.spring.AwsBeanConfig;
import sk.services.json.IJson;
import sk.services.time.ITime;
import sk.spring.SpringApp;
import sk.spring.SpringAppEntryPoint;
import sk.spring.config.SpringCoreConfig;
import sk.utils.files.PathWithBase;
import sk.utils.statics.Io;
import sk.utils.statics.Ti;

import javax.inject.Inject;

import static sk.utils.functional.O.empty;
import static sk.utils.functional.O.of;

public class S3CompareToolTest implements SpringAppEntryPoint {
    public static void main(String[] args) {
        SpringApp.createWithWelcomeAndLogAndInit("Hi!", "tst_logger", new S3CompareToolTest(), Config.class);
    }

    @Import(AwsBeanConfig.class)
    public static class Config extends SpringCoreConfig {
        @Bean
        S3CompareTool S3CompareTool() { return new S3CompareTool(); }

        @Bean
        S3SyncTool S3SyncTool() { return new S3SyncTool(); }
    }

    @Inject S3CompareTool tool;
    @Inject S3SyncTool sync;
    @Inject IJson json;
    @Inject ITime times;

    @Override
    public void run() {
        final S3CompareInput i1 = new S3CompareInput(
                new AwsPlainPropertiesImpl(
                        "https://sfo2.digitaloceanspaces.com",
                        "DQ45JS4MNLHLQIH7M524",
                        "0NbyRzITLVungTm0lpN2DN3OObB+8M1qaNfTbsCT1L8"
                ),
                new PathWithBase("bma-code", of("dev")), empty()
        );
        final S3CompareInput i2 = new S3CompareInput(
                new AwsPlainPropertiesImpl(
                        "https://storage.googleapis.com",
                        "GOOG1EMKQKJUHZEDPAJAQGCNMCPO2BFHSSHUSJFTLW6GFSF4KFWACAKEI4BLA",
                        "wBckzg7j/xDU1uWQQge5xdbr/9idD3SnHWLbWqYg"
                ),
                new PathWithBase("bma-code", of("rc")), empty()
        );

        val result = sync.sync(3, 100_000_000, i1, i2, true);

        if (result.hasDifferences()) {
            Io.reWrite(
                    "/tmp/compare_tool/" + (i1.getShortDescription() + "_VS_" + i2.getShortDescription()).replace("/", "_") +
                            "__" +
                            Ti.yyyyMMddHHmmss.format(times.nowZ()) + ".json",
                    w -> w.append(json.to(result, true)));

            throw new RuntimeException("Difs:" + result.getShortInfo());
        }


    }
}
