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
import jakarta.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import sk.services.bytes.IBytes;
import sk.services.json.IJson;
import sk.services.nodeinfo.model.ApiBuildInfo;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Ex;
import sk.utils.statics.Io;
import sk.utils.statics.St;
import sk.utils.tuples.X;
import sk.utils.tuples.X2;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
public class EcsJskDeployer {
    @Inject EcsJskDeployerProperties conf;
    @Inject EcsEcrJskClient client;
    @Inject IBytes bytes;
    @Inject IJson json;

    public void deploy() {
        String fullRemoteImageName = prepareContainer();
        deployContainerToEcs(fullRemoteImageName);
    }

    private String prepareContainer() {
        final String tag = prepareTag(".");
        final String imageNameLatestTag = conf.getEcrRepoName() + ":latest";
        final String imageNameRemoteWithTag = conf.getEcrRepoName() + ":" + tag;
        String fullRemoteImageNameLatest = St.endWith(conf.getEcrUrl().split("://")[1], "/") + imageNameLatestTag;
        String fullRemoteImageNameWithTag = St.endWith(conf.getEcrUrl().split("://")[1], "/") + imageNameRemoteWithTag;

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

        try {
            docker.build(Paths.get("."), conf.getEcrRepoName());
            docker.tag(imageNameLatestTag, fullRemoteImageNameWithTag, true);
            docker.push(fullRemoteImageNameWithTag);
            docker.tag(imageNameLatestTag, fullRemoteImageNameLatest, true);
            docker.push(fullRemoteImageNameLatest);
        } catch (Exception e) {
            Ex.thRow(e);
        }

        return fullRemoteImageNameLatest;
    }

    private String prepareTag(String folderPath) {
        final File file1 = new File(folderPath);
        System.out.println("TRY TO PREPARE TAG FOLDER: " + file1.getAbsolutePath());
        final List<X2<String, ApiBuildInfo>> buildInfos = Arrays.stream(file1.listFiles())
                .filter($ -> $.getName().endsWith(".jar"))
                .map($ -> {
                    O<byte[]> oJar = Io.bRead($.getAbsolutePath()).oBytes();
                    final O<Map<String, byte[]>> mapO = oJar.flatMap(jar -> bytes.unZipArchive(jar));
                    oJar = null;
                    final O<byte[]> buildInfo = mapO.flatMap(files -> O.ofNull(files.get("__jsk_util/web_api/__buildInfo.json")));
                    final O<ApiBuildInfo> from = buildInfo.map(file -> json.from(new String(file), ApiBuildInfo.class));
                    return X.x($.getName(), from);
                })
                .filter($ -> $.i2().isPresent())
                .map($ -> X.x($.i1(), $.i2().get()))
                .toList();
        if (buildInfos.size() == 0) {
            throw new RuntimeException("No valid jars with valid __jsk_util/web_api/__buildInfo.json inside found!");
        }
        if (buildInfos.size() > 1) {
            throw new RuntimeException("More than one jar found with __jsk_util/web_api/__buildInfo.json inside: !");
        }
        return "prefix4policy-" + buildInfos.get(0).i2().toString();
    }

    private void deployContainerToEcs(String fullRemoteImageName) {
        List<String> existingTaskDefinitions = client.getExistingTaskDefinitionsSortedAscByNumber(conf.getEcsTaskName());
        if (existingTaskDefinitions.size() == 0) {
            throw new RuntimeException("No task definitions found in container! Please create first task definition manually!");
        }

        final int howManyToDelete = Math.max(existingTaskDefinitions.size() - 3, 0);
        if (howManyToDelete > 0) {
            client.deregisterTasks(existingTaskDefinitions.subList(0, howManyToDelete));
        }
        client.reRegisterTask(Cc.last(existingTaskDefinitions).get(), fullRemoteImageName);
        client.updateService(conf.getEcsClusterName(), conf.getEcsServiceName(), conf.getEcsTaskName());
        client.deleteOldImages(conf.getEcrRepoName());
    }
}
