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

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerHost;
import com.spotify.docker.client.auth.FixedRegistryAuthSupplier;
import com.spotify.docker.client.messages.RegistryAuth;
import lombok.SneakyThrows;
import sk.utils.statics.Cc;
import sk.utils.statics.St;
import sk.utils.tuples.X2;

import javax.inject.Inject;
import java.nio.file.Paths;
import java.util.List;

public class EcsJskDeployer {
    @Inject EcsJskDeployerProperties conf;
    @Inject EcsEcrJskClient client;

    public void deploy() {
        prepareContainer();
        deployContainerToEcs();
    }

    @SneakyThrows
    private void prepareContainer() {
        final String imageName = conf.getEcrRepoName() + ":latest";
        String remoteImageName = St.endWith(conf.getEcrUrl().split("://")[1], "/") + imageName;

        final X2<String, String> dockerPas = client.getDockerLoginAndPass().collect(w -> w, e -> {
            e.printStackTrace();
            System.exit(1);
            return null;
        });

        DockerClient docker = DefaultDockerClient.builder()
                .registryAuthSupplier(new FixedRegistryAuthSupplier(RegistryAuth.builder()
                        .username(dockerPas.i1())
                        .password(dockerPas.i2())
                        .serverAddress(conf.getEcrUrl())
                        .build(), null))
                .uri(DockerHost.defaultUnixEndpoint())
                .build();

        docker.build(Paths.get("."), conf.getEcrRepoName());
        docker.tag(imageName, remoteImageName, true);
        docker.push(remoteImageName);
    }

    private void deployContainerToEcs() {
        List<String> existingTaskDefinitions = client.getExistingTaskDefinitionsSortedAscByNumber(conf.getEcsTaskName());
        if (existingTaskDefinitions.size() == 0) {
            throw new RuntimeException("No task definitions found in container! Please create first task definition manually!");
        }

        final int howManyToDelete = Math.max(existingTaskDefinitions.size() - 3, 0);
        if (howManyToDelete > 0) {
            client.deregisterTasks(existingTaskDefinitions.subList(0, howManyToDelete));
        }
        client.reRegisterTask(Cc.last(existingTaskDefinitions).get());
        client.updateService(conf.getEcsClusterName(), conf.getEcsServiceName(), conf.getEcsTaskName());
        client.deleteOldImages(conf.getEcrRepoName());
    }
}
