package jsk.gcl.agent;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2022 Core General
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

import jsk.gcl.agent.model.GcaNodeId;
import jsk.gcl.agent.model.GcaProperties;
import jsk.gcl.agent.model.GcaUpdateFileProps;
import jsk.gcl.agent.services.GcaFileUpdaterTask;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import sk.aws.AwsPlainPropertiesImpl;
import sk.aws.AwsUtilityHelper;
import sk.aws.s3.S3JskClient;
import sk.aws.s3.S3Properties;
import sk.services.CoreServicesRaw;
import sk.services.ICoreServices;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.javafixes.argparser.ArgParser;
import sk.utils.javafixes.argparser.ArgParserConfigProvider;
import sk.utils.statics.Io;
import sk.utils.statics.Ti;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;
import spark.Spark;

import java.net.URI;
import java.util.List;
import java.util.TreeSet;

import static jsk.gcl.agent.model.GcaUpdateFileProps.GcaRollingUpdateConfig;
import static sk.utils.functional.O.of;
import static sk.utils.statics.Cc.ts;

/**
 * To enable agent what is needed:
 * 1. Meta file and config file with GcaProperties
 * 2. In config file there should be at least configuration for agent jar
 * 3. Also there should be at least one config payload
 */
@Slf4j
public class GcaAgentMain {
    static final ICoreServices core = new CoreServicesRaw();
    public final static String configFile = "/tmp/jsk_agent_config.json";
    public final static GcaNodeId nodeId = new GcaNodeId(core.ids().timedHaiku());

    static S3JskClient s3Client;

    List<GcaFileUpdaterTask> payloadUpdaters;

    public static void main(String[] args) throws Exception {
        Spark.port(8079);
        Spark.get("/agent/ping", (request, response) -> "ok");

        log.info("Starting agent");
        final ArgParser<ARGS> arguments = ArgParser.parse(args, ARGS.S3_KEY);

        s3Client = new S3JskClient(
                new S3Properties() {
                    final AwsPlainPropertiesImpl props = new AwsPlainPropertiesImpl(
                            arguments.getRequiredArg(ARGS.S3_REGION),
                            arguments.getRequiredArg(ARGS.S3_KEY),
                            arguments.getRequiredArg(ARGS.S3_SECRET)
                    );

                    @Override
                    public OneOf<URI, Region> getAddress() {
                        return props.getAddress();
                    }

                    @Override
                    public AwsCredentials getCredentials() {
                        return props.getCredentials();
                    }
                },
                core.async(), new AwsUtilityHelper(), core.repeat(), core.http(), core.bytes(), core.json()
        ).init();

        new GcaAgentMain().run(arguments);
    }

    public void run(ArgParser<ARGS> args) {
        //one time execution to force load meta for config. No bg thread is created
        new GcaFileUpdaterTask(s3Client, core,
                new GcaUpdateFileProps(args.getRequiredArg(ARGS.META_BUCKET), args.getRequiredArg(ARGS.META_PATH),
                        configFile, O.empty(), O.empty(), new GcaRollingUpdateConfig(false, "", 0l)), false);

        //now config must exist
        final GcaProperties configs = core.json().from(Io.sRead(configFile).string(), GcaProperties.class);
        log.info("Configs:\n" + core.json().to(configs, true));


        payloadUpdaters = configs.updateFileTasks().stream()
                .map($ -> new GcaFileUpdaterTask(s3Client, core, $, true))
                .toList();

        log.info("Init started!");
        try {
            Ti.sleep(Long.MAX_VALUE);
        } catch (Exception e) {
            log.error("", e);
            log.info("Finished...");
        }
    }

    @AllArgsConstructor
    @Getter
    private enum ARGS implements ArgParserConfigProvider<ARGS> {
        S3_REGION(of(ts("-s3-region")), true, "S3 region for data"),
        S3_KEY(of(ts("-s3-key")), true, "S3 key for data"),
        S3_SECRET(of(ts("-s3-secret")), true, "S3 secret for data"),

        META_BUCKET(of(ts("-s3-meta-bucket")), true, "S3 meta file for config bucket"),
        META_PATH(of(ts("-s3-meta-path")), true, "S3 meta file for config path"),
        ;

        final O<TreeSet<String>> commandPrefix;
        final boolean required;
        final String description;

        @Override
        public ARGS[] getArrConfs() {return values();}
    }
}
