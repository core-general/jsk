package sk.aws.s3.comparetool;

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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import sk.aws.AwsUtilityHelper;
import sk.aws.s3.S3ItemMeta;
import sk.aws.s3.S3JskClient;
import sk.aws.s3.S3ListObject;
import sk.aws.s3.S3Properties;
import sk.aws.s3.comparetool.model.S3CompareInput;
import sk.aws.s3.comparetool.model.S3CompareMeta;
import sk.services.bytes.IBytes;
import sk.services.comparer.CompareItem;
import sk.services.comparer.CompareTool;
import sk.services.comparer.model.CompareResult;
import sk.services.http.IHttp;
import sk.services.json.IJson;
import sk.services.retry.IRepeat;
import sk.utils.async.AtomicNotifier;
import sk.utils.files.PathWithBase;
import sk.utils.functional.F1;
import sk.utils.functional.F2;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.statics.Cc;
import sk.utils.statics.St;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;

import javax.inject.Inject;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@NoArgsConstructor
@AllArgsConstructor
public class S3CompareTool extends CompareTool<S3ItemMeta, S3CompareMeta> {
    @Inject IRepeat repeat;
    @Inject IHttp http;
    @Inject IBytes bytes;
    @Inject IJson json;
    @Inject AwsUtilityHelper helper;

    public CompareResult<S3ItemMeta, S3CompareMeta> compare(S3CompareInput i1, S3CompareInput i2, boolean filesMustBePublic) {
        log.info("Starting compare:\n" + json.to(i1, true) + "\n and \n" + json.to(i2, true));
        return innerCompare(
                () -> getData.apply(new CompareProcessorAux(createClient(i1), i1, true, filesMustBePublic)),
                () -> getData.apply(new CompareProcessorAux(createClient(i2), i2, false, filesMustBePublic)),
                lst -> new S3CompareMeta(
                        lst.size(),
                        lst.stream().mapToLong($ -> $.getSize()).sum(),
                        St.bytesToHex(bytes.sha256(lst.stream()
                                .sorted(Comparator.comparing($ -> $.getId()))
                                .map($ -> $.getHash())
                                .collect(Collectors.joining())
                                .getBytes(StandardCharsets.UTF_8)))
                ));
    }

    public S3JskClient createClient(S3CompareInput in) {
        return new S3JskClient(new S3Properties() {
            @Override
            public O<PathWithBase> get4everStorage() {
                return O.empty();
            }

            @Override
            public O<PathWithBase> getTempStorage() {
                return O.empty();
            }

            @Override
            public OneOf<URI, Region> getAddress() {
                return in.getProps().getAddress();
            }

            @Override
            public AwsCredentials getCredentials() {
                return in.getProps().getCredentials();
            }
        }, async.get(), helper, repeat, http).init();
    }

    final F2<S3JskClient, S3CompareInput, List<S3ListObject>> getResponse =
            (cli, in) -> cli.getAllItems(in.getRoot(), in.getMsBetweenPages())
                    .parallelStream()
                    .collect(Cc.toL());

    final F1<CompareProcessorAux, List<CompareItem<S3ItemMeta>>> getData = (conf) -> {
        final List<S3ListObject> response = getResponse.apply(conf.client, conf.in);
        AtomicNotifier al =
                new AtomicNotifier(response.size(), 100, $ -> log.info(((conf.isFirst) ? "First: " : "Second: ") + $));
        return response.parallelStream()
                .map(item -> new CompareItem<S3ItemMeta>() {
                    @Override
                    public String getId() {
                        return St.isNullOrEmpty(conf.in.getRoot().getPathNoSlash())
                               ? item.getKey()
                               : item.getKey().replace(conf.in.getRoot().getPathWithSlash(), "");
                    }

                    @Override
                    public S3ItemMeta getItemInfo() {
                        final S3ItemMeta mmeta = repeat.repeat(() -> conf.client
                                .getMeta(conf.in.getRoot(), item.getKey(), conf.isFilesMustBePublic())
                                .orElseThrow(() -> new RuntimeException(
                                        "Problem with:" + conf.in.getRoot() + ":" + item.getKey())), 50, 2000);
                        al.incrementAndNotify();
                        return mmeta;
                    }
                }).collect(Cc.toL());
    };

    @Data
    @AllArgsConstructor
    private static class CompareProcessorAux {
        S3JskClient client;
        S3CompareInput in;
        boolean isFirst;
        boolean filesMustBePublic;
    }
}
