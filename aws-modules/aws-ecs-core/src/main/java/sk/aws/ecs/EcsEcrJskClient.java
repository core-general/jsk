package sk.aws.ecs;

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

import sk.aws.AwsUtilityHelper;
import sk.services.bytes.IBytes;
import sk.utils.functional.OneOf;
import sk.utils.statics.Cc;
import sk.utils.statics.Ma;
import sk.utils.statics.St;
import sk.utils.tuples.X;
import sk.utils.tuples.X2;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.model.BatchDeleteImageRequest;
import software.amazon.awssdk.services.ecr.model.GetAuthorizationTokenResponse;
import software.amazon.awssdk.services.ecr.model.ImageIdentifier;
import software.amazon.awssdk.services.ecr.model.ListImagesRequest;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.*;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

public class EcsEcrJskClient {
    @Inject AwsUtilityHelper helper;
    @Inject IBytes bytes;
    @Inject EcsJskDeployerProperties conf;

    EcsClient ecs;
    EcrClient ecr;

    public EcsEcrJskClient(EcsJskDeployerProperties conf, AwsUtilityHelper helper) {
        this.conf = conf;
        this.helper = helper;
    }

    @PostConstruct
    public void init() {
        ecs = helper.createSync(EcsClient::builder, conf);
        ecr = helper.createSync(EcrClient::builder, conf);
    }

    public OneOf<X2<String, String>, Exception> getDockerLoginAndPass() {
        try {
            final GetAuthorizationTokenResponse resp = ecr.getAuthorizationToken();
            final String encodedToken = resp.authorizationData().get(0).authorizationToken();
            final String token = new String(bytes.dec64(encodedToken), StandardCharsets.UTF_8);
            final String[] authData = token.split(":");
            return OneOf.left(X.x(authData[0], authData[1]));
        } catch (Exception e) {
            return OneOf.right(e);
        }
    }

    public List<String> getExistingTaskDefinitionsSortedAscByNumber(String ecsTaskName) {
        final ListTaskDefinitionsRequest request = ListTaskDefinitionsRequest.builder()
                .familyPrefix(ecsTaskName)
                .build();
        return ecs.listTaskDefinitions(request).taskDefinitionArns().stream()
                .map($ -> St.subLL($, "/"))
                .sorted(Comparator.comparingInt($ -> Ma.pi(St.subLL($, ":"))))
                .collect(Cc.toL());
    }

    public void deregisterTasks(List<String> toDeregister) {
        toDeregister.forEach($ ->
                ecs.deregisterTaskDefinition(DeregisterTaskDefinitionRequest.builder()
                        .taskDefinition($)
                        .build()));
    }

    public void reRegisterTask(String lastTask) {
        DescribeTaskDefinitionRequest describeTaskDefinitionRequest = DescribeTaskDefinitionRequest.builder()
                .taskDefinition(lastTask)
                .build();

        final DescribeTaskDefinitionResponse described =
                ecs.describeTaskDefinition(describeTaskDefinitionRequest);

        TaskDefinition revisionedTask = described.taskDefinition();

        RegisterTaskDefinitionRequest registerTaskDefinitionRequest = RegisterTaskDefinitionRequest.builder()
                .family(revisionedTask.family())
                .volumes(revisionedTask.volumes())
                .containerDefinitions(revisionedTask.containerDefinitions())
                .build();

        ecs.registerTaskDefinition(registerTaskDefinitionRequest);
    }

    public void updateService(String clusterName, String serviceName, String taskDefinition) {
        UpdateServiceRequest updateServiceRequest = UpdateServiceRequest.builder()
                .cluster(clusterName)
                .service(serviceName)
                .taskDefinition(taskDefinition)
                .build();

        ecs.updateService(updateServiceRequest);
    }

    public void deleteOldImages(String ecrRepoName) {
        final List<ImageIdentifier> toDelete = ecr
                .listImages(ListImagesRequest.builder().repositoryName(ecrRepoName).build())
                .imageIds().stream()
                .filter(i -> i.imageTag() == null)
                .collect(Cc.toL());

        if (toDelete.size() > 0) {
            BatchDeleteImageRequest batchDeleteImageRequest = BatchDeleteImageRequest.builder()
                    .repositoryName(ecrRepoName)
                    .imageIds(toDelete)
                    .build();
            ecr.batchDeleteImage(batchDeleteImageRequest);
        }
    }
}
