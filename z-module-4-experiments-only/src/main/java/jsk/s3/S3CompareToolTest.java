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

public class S3CompareToolTest implements SpringAppEntryPoint {
    public static void main(String[] args) {
        SpringApp.createWithWelcomeAndLogAndInit("Hi!!", "tst_logger", new S3CompareToolTest(), Config.class);
    }

    @Import(AwsBeanConfig.class)
    public static class Config extends SpringCoreConfig {
        @Bean
        S3CompareTool S3CompareTool() { return new S3CompareTool(); }
    }

    @Inject S3CompareTool tool;
    @Inject IJson json;
    @Inject ITime times;

    @Override
    public void run() {
        final S3CompareInput i1 = new S3CompareInput(
                new AwsPlainPropertiesImpl(
                        "",
                        "",
                        ""
                ),
                new PathWithBase("", empty()), empty()
        );
        final S3CompareInput i2 = new S3CompareInput(
                new AwsPlainPropertiesImpl(
                        "",
                        "",
                        ""
                ),
                new PathWithBase("", empty()), empty()
        );

        val result = tool.compare(i1, i2, true);

        Io.reWrite(
                "/tmp/compare_tool/" + (i1.getShortDescription() + "_VS_" + i2.getShortDescription()).replace("/", "_") + "__" +
                        Ti.yyyyMMddHHmmss.format(times.nowZ()) + ".json",
                w -> w.append(json.to(result, true)));
    }
}
