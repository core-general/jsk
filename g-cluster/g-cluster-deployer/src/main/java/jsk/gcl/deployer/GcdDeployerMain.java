package jsk.gcl.deployer;

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

import jsk.gcl.agent.model.GcaMeta;
import jsk.gcl.agent.model.GcaMetaItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import sk.aws.AwsPlainPropertiesImpl;
import sk.aws.AwsUtilityHelper;
import sk.aws.s3.S3JskClient;
import sk.aws.s3.S3Properties;
import sk.services.CoreServicesRaw;
import sk.services.ICoreServices;
import sk.services.http.CrcAndSize;
import sk.utils.collections.DequeWithLimit;
import sk.utils.files.PathWithBase;
import sk.utils.functional.F0;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.javafixes.argparser.ArgParser;
import sk.utils.javafixes.argparser.ArgParserConfigProvider;
import sk.utils.statics.Cc;
import sk.utils.statics.Io;
import sk.utils.statics.St;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;

import java.io.File;
import java.net.URI;
import java.util.TreeSet;

import static sk.utils.functional.O.empty;
import static sk.utils.functional.O.of;
import static sk.utils.statics.Cc.ts;

public class GcdDeployerMain {
    final static ICoreServices core = new CoreServicesRaw();

    public static void main(String[] ___) {
        final ArgParser<ARGS> args = ArgParser.parse(___, ARGS.FILE_IN);

        S3JskClient s3Client = new S3JskClient(
                new S3Properties() {
                    final AwsPlainPropertiesImpl props = new AwsPlainPropertiesImpl(
                            args.getRequiredArg(ARGS.S3_REGION),
                            args.getRequiredArg(ARGS.S3_KEY),
                            args.getRequiredArg(ARGS.S3_SECRET)
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

        process(s3Client, args);
    }

    /**
     * 1. Calc new hash and size
     * 2. Encode data
     * 3. Upload data
     * 4. Get url to uploaded data
     * 5. Download and decode meta (if exists)
     * 6. Process meta
     * 7. Encode meta
     * 8. Upload meta
     */
    private static void process(S3JskClient s3Client, ArgParser<ARGS> a) {
        final byte[] mainPayload = Io.bRead(a.getRequiredArg(ARGS.FILE_IN)).bytes();
        final CrcAndSize newCrcAndSize = core.bytes().calcCrcAndSize(mainPayload);

        final String fileName = new File(a.getRequiredArg(ARGS.FILE_IN)).getName();


        PathWithBase pathOfVersionsFolder = new PathWithBase(a.getRequiredArg(ARGS.META_BUCKET), a.getRequiredArg(ARGS.META_PATH))
                .getParent().get()
                .addToPath("versions");
        PathWithBase newPayloadUrl = encodeAndUpload(mainPayload,
                s3Client, a.getRequiredArg(ARGS.META_BUCKET),
                pathOfVersionsFolder.getPathWithSlash() + core.times().now() + "-" + fileName);


        final PathWithBase metaPath = new PathWithBase(a.getRequiredArg(ARGS.META_BUCKET), a.getRequiredArg(ARGS.META_PATH));

        final F0<DequeWithLimit<GcaMetaItem>> defaultHistoryStorage = () -> new DequeWithLimit<>(25);

        final GcaMeta meta = s3Client.getFromS3(metaPath)
                .map($ -> core.json().from(new String($, St.UTF8), GcaMeta.class))
                .map(mm -> {

                    final DequeWithLimit<GcaMetaItem> dwl = O.ofNull(mm.history2()).orElseGet(defaultHistoryStorage);
                    dwl.addFirst(mm.currentFile());
                    return new GcaMeta(new GcaMetaItem(newPayloadUrl.getBase(), newPayloadUrl.getPathNoSlash(), newCrcAndSize,
                            O.of(core.times().nowZ())), dwl);
                }).orElseGet(() -> new GcaMeta(
                        new GcaMetaItem(newPayloadUrl.getBase(), newPayloadUrl.getPathNoSlash(), newCrcAndSize,
                                O.of(core.times().nowZ())), defaultHistoryStorage.get()));
        encodeAndUpload(core.json().to(meta).getBytes(St.UTF8), s3Client, metaPath);
    }

    private static PathWithBase encodeAndUpload(byte[] payload, S3JskClient s3, PathWithBase path) {
        s3.putPublicNoUrl(path, payload, false, empty(), empty(), Cc.m("Cache-Control", "no-store"));
        return path;
    }

    private static PathWithBase encodeAndUpload(byte[] payload, S3JskClient s3, String bucket, String path) {
        return encodeAndUpload(payload, s3, new PathWithBase(bucket, path));
    }


    @AllArgsConstructor
    @Getter
    private enum ARGS implements ArgParserConfigProvider<ARGS> {
        FILE_IN(of(ts("-fi")), true, "File to deploy"),

        S3_REGION(of(ts("-s3-region")), true, "S3 region"),
        S3_KEY(of(ts("-s3-key")), true, "S3 key"),
        S3_SECRET(of(ts("-s3-secret")), true, "S3 secret"),

        META_BUCKET(of(ts("-s3-meta-bucket")), true, "S3 meta file bucket"),
        META_PATH(of(ts("-s3-meta-path")), true, "S3 meta file path"),

        ;

        final O<TreeSet<String>> commandPrefix;
        final boolean required;
        final String description;

        @Override
        public ARGS[] getArrConfs() {return values();}
    }
}
