package sk.test.land.testcontainers.localstack;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2024 Core General
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


import lombok.SneakyThrows;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import sk.aws.AwsUtilityHelper;
import sk.aws.AwsWithChangedPort;
import sk.aws.dynamo.DynClient;
import sk.aws.dynamo.DynConfigurator;
import sk.aws.dynamo.DynProperties;
import sk.aws.dynamo.extensions.CreatedAndUpdatedAtExtension;
import sk.aws.s3.S3JskClient;
import sk.aws.s3.S3Properties;
import sk.services.ICoreServices;
import sk.test.land.core.JskLand;
import sk.test.land.core.mixins.JskLandEmptyStateMixin;
import sk.test.land.testcontainers.JskLandContainer;
import sk.utils.files.PathWithBase;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.statics.Cc;
import sk.utils.statics.Io;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;

import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

public class JskLandLocalstack extends JskLandContainer<LocalStackContainer> implements JskLandEmptyStateMixin {
    private final AwsWithChangedPort acp;
    private final String dockerImgName;
    private final AwsUtilityHelper utilityHelper;
    private final ICoreServices core;

    public JskLandLocalstack(AwsWithChangedPort acp, String dockerImgName, AwsUtilityHelper utilityHelper, ICoreServices core) {
        super(acp.getPort());
        this.acp = acp;
        this.dockerImgName = dockerImgName;
        this.utilityHelper = utilityHelper;
        this.core = core;
    }

    private S3JskClient s3Client;
    private DynClient dynamoDbClient;

    public synchronized S3JskClient getS3Client() {
        return s3Client == null ? s3Client = initS3Client() : s3Client;
    }

    public synchronized DynClient getDynamoDbClient() {
        return dynamoDbClient == null ? dynamoDbClient = initDynamoClient() : dynamoDbClient;
    }

    @Override
    public void toEmptyState() {
        core.async().runAsyncDontWait(Cc.l(
                () -> {
                    S3JskClient s3JskClient = getS3Client();
                    s3JskClient.getBuckets().parallelStream().forEach(s -> {
                        s3JskClient.clearAll(new PathWithBase(s), O.empty());
                        s3JskClient.deleteBucket(s);
                    });
                },
                () -> {
                    DynClient dynaCli = getDynamoDbClient();
                    List<String> allTableNames = dynaCli.getAllTableNames();
                    allTableNames.parallelStream().forEach($ -> dynaCli.deleteTable($));
                }
        )).join();

    }

    @Override
    protected LocalStackContainer createContainer(int port) {
        LocalStackContainer selfPostgreSQLContainer = new LocalStackContainer(DockerImageName.parse(dockerImgName)) {
            @Override
            public String getAccessKey() {
                return "test";
            }

            @Override
            public String getSecretKey() {
                return "test";
            }

            @Override
            public String getRegion() {
                return "us-east-1";
            }
        }
                .withServices(S3, DYNAMODB)
                .withStartupTimeout(Duration.of(120, ChronoUnit.SECONDS))
                .withEnv(Io.isWWWAvailable()
                         ? Cc.m()
                         : Cc.m(
                                 "SKIP_SSL_CERT_DOWNLOAD", "1",
                                 "SKIP_INFRA_DOWNLOADS", "1",
                                 "DISABLE_EVENTS", "1"
                         ));
        selfPostgreSQLContainer.setPortBindings(Cc.l(port + ":4566"));
        return selfPostgreSQLContainer;
    }


    protected S3JskClient initS3Client() {
        LocalStackContainer container = getContainer();
        S3Properties s3Properties = new S3Properties() {
            @SneakyThrows
            @Override
            public OneOf<URI, Region> getAddress() {
                return OneOf.left(new URI("http://%s:%d".formatted(container.getHost(), getOutsidePort())));
            }

            @Override
            public AwsCredentials getCredentials() {
                return AwsBasicCredentials.create(container.getAccessKey(), container.getSecretKey());
            }

            @Override
            public boolean forcePathStyle() {
                return true;
            }
        };
        return new S3JskClient(s3Properties, core.async(), utilityHelper, core.repeat(), core.http(), core.bytes(),
                core.json(), Optional.of(acp)).init();
    }

    protected DynClient initDynamoClient() {
        LocalStackContainer container = getContainer();
        DynProperties s3Properties = new DynProperties() {
            @Override
            public String getTablePrefix() {
                return "TEST_";
            }

            @SneakyThrows
            @Override
            public OneOf<URI, Region> getAddress() {
                return OneOf.left(new URI("http://%s:%d".formatted(container.getHost(), getOutsidePort())));
            }

            @Override
            public AwsCredentials getCredentials() {
                return AwsBasicCredentials.create(container.getAccessKey(), container.getSecretKey());
            }
        };
        return new DynClient(s3Properties, new DynConfigurator(), utilityHelper,
                new CreatedAndUpdatedAtExtension(core.times()), Cc.l())
                .init();
    }

    @Override
    public Class<? extends JskLand> getId() {
        return JskLandLocalstack.class;
    }
}
