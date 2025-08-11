package sk.aws.cdk;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2025 Core General
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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import software.amazon.awscdk.services.ec2.IMachineImage;
import software.amazon.awscdk.services.ec2.MachineImage;
import software.amazon.awscdk.services.rds.PostgresEngineVersion;

import java.util.List;


@Getter
@With
@AllArgsConstructor
@RequiredArgsConstructor
public class JskGenericStackParams {
    private final String accountId;
    private final String region;
    private final String appPrefix;
    private final List<String> allowedIps;

    private IMachineImage ec2Img = MachineImage.genericLinux(Cc.m(
            //Verified provider ubuntu/images/hvm-ssd-gp3/ubuntu-noble-24.04-amd64-server-20250516
            "eu-west-1", "ami-0286d0aea4d6c7a34"
    ));

    private boolean enableAlb = false;
    private O<String> albDomain = O.empty();
    private O<String> healthCheckAlb = O.empty();

    boolean createPostgres = true;
    private PostgresEngineVersion pgVer = PostgresEngineVersion.VER_17;
    private boolean cantDeleteRds = true;
    private boolean rdsEncrypted = true;

    private boolean createRedisCacheCluster = false;
    private String redisVersion = "7.1";
    private int redisPort = 6379;
    private int redisCacheNodeCount = 1;
}
