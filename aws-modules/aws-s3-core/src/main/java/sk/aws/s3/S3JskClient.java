package sk.aws.s3;

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

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import sk.aws.AwsUtilityHelper;
import sk.services.async.IAsync;
import sk.services.bytes.IBytes;
import sk.services.http.CrcAndSize;
import sk.services.http.IHttp;
import sk.services.http.model.CoreHttpResponse;
import sk.services.json.IJson;
import sk.services.rand.IRand;
import sk.services.retry.IRepeat;
import sk.utils.async.AtomicNotifier;
import sk.utils.files.PathWithBase;
import sk.utils.functional.C1;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.javafixes.TypeWrap;
import sk.utils.statics.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.*;

import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static sk.utils.functional.O.empty;
import static sk.utils.functional.O.ofNullable;


@Slf4j
@NoArgsConstructor
public class S3JskClient {
    @Inject S3Properties conf;

    @Inject IJson json;
    @Inject IAsync async;
    @Inject IRepeat repeat;
    @Inject IRand rand;
    @Inject IHttp http;
    @Inject AwsUtilityHelper helper;
    @Inject IBytes bytes;

    public S3JskClient(S3Properties conf, IAsync async, AwsUtilityHelper helper, IRepeat repeat, IHttp http, IBytes bytes,
            IJson json) {
        this.conf = conf;
        this.async = async;
        this.helper = helper;
        this.repeat = repeat;
        this.http = http;
        this.bytes = bytes;
        this.json = json;
    }

    private S3Client s3;
    private F1<PathWithBase, String> urlGetter;

    @PostConstruct
    public S3JskClient init() {
        s3 = helper.createSync(S3Client::builder, conf);

        //region urlGetter
        S3Utilities s3Util = s3.utilities();

        urlGetter = pathWithBase -> {
            GetUrlRequest.Builder reqBuilder = GetUrlRequest.builder()
                    .bucket(pathWithBase.getBase())
                    .key(pathWithBase.getPathNoSlash());
            conf.getAddress().collect(
                    reqBuilder::endpoint,
                    reqBuilder::region
            );
            final GetUrlRequest build = reqBuilder.build();
            return s3Util.getUrl(build).toString();
        };
        //endregion

        return this;
    }

    public List<String> getBuckets() {
        return s3.listBuckets().buckets().stream().map(Bucket::name).sorted().toList();
    }

    public void deleteBucket(String bucket) {
        s3.deleteBucket(DeleteBucketRequest.builder().bucket(bucket).build());
    }

    /**
     * Puts data to S3 and returns nothing
     */
    public void putPublicNoUrl(PathWithBase base, byte[] body) {
        putPublicNoUrl(base, body, true, empty(), empty(), null);
    }

    /**
     * Puts data to S3 and returns nothing
     */
    public void putPublicNoUrl(PathWithBase base, byte[] body,
            O<String> contentType, O<String> contentEncoding) {
        putPublicNoUrl(base, body, true, contentType, contentEncoding, null);
    }

    /**
     * Puts data to S3 and returns URL
     */
    public String putPublic(PathWithBase base, byte[] body) {
        return putPublic(base, body, null);
    }

    /**
     * Puts data to S3 and returns URL
     */
    public String putPublic(PathWithBase base, byte[] body, Map<String, String> metadata) {
        return putPublic(base, body, true, empty(), empty(), metadata);
    }

    /**
     * Puts data to S3 and returns URL
     */
    public String putPublic(PathWithBase base, byte[] body,
            O<String> contentType, O<String> contentEncoding) {
        return putPublic(base, body, true, contentType, contentEncoding);
    }

    /**
     * Puts data to S3 and returns URL
     */
    public String putPublic(PathWithBase base, byte[] body, boolean allRead, O<String> contentType,
            O<String> contentEncoding) {
        return putPublic(base, body, allRead, contentType, contentEncoding, null);
    }

    /**
     * Puts data to S3 and returns URL
     */
    public String putPublic(PathWithBase base, byte[] body, boolean allRead, O<String> contentType,
            O<String> contentEncoding, Map<String, String> metadata) {
        putPublicNoUrl(base, body, allRead, contentType, contentEncoding, metadata);
        return getUrl(base);
    }

    public String getUrl(PathWithBase base) {
        return urlGetter.apply(base);
    }

    public HeadObjectResponse headObject(PathWithBase path) {
        HeadObjectRequest hor = HeadObjectRequest.builder()
                .bucket(path.getBase())
                .key(path.getPathNoSlash())
                .build();
        final HeadObjectResponse headObjectResponse = s3.headObject(hor);
        return headObjectResponse;
    }

    /**
     * Puts data to S3 and returns URL
     */
    public void putPublicNoUrl(PathWithBase base, byte[] body, boolean allRead, O<String> contentType,
            O<String> contentEncoding, Map<String, String> metadata) {
        PutObjectRequest.Builder builder = PutObjectRequest.builder();
        contentType.ifPresent(builder::contentType);
        contentEncoding.ifPresent(builder::contentEncoding);
        builder.bucket(base.getBase())
                .key(base.getPathNoSlash());

        if (allRead) {
            builder.acl(ObjectCannedACL.PUBLIC_READ);
        }

        if (metadata != null && !metadata.isEmpty()) {
            getAndDelete(metadata, "Content-Type", builder::contentType);
            getAndDelete(metadata, "Content-Encoding", builder::contentEncoding);
            getAndDelete(metadata, "Content-Disposition", builder::contentDisposition);
            getAndDelete(metadata, "Cache-Control", builder::cacheControl);

            builder.metadata(metadata);
        }

        PutObjectRequest putObjectRequest = builder.build();

        s3.putObject(putObjectRequest, RequestBody.fromBytes(body));
    }


    public int deleteByKeys(PathWithBase base, String... keysFromRoot) {
        try {
            base = base.replacePath("");
            PathWithBase finalBase = base;
            ObjectIdentifier[] objectIdentifiers = Cc.stream(keysFromRoot)
                    .map($ -> ObjectIdentifier.builder().key($).build())
                    .toArray(ObjectIdentifier[]::new);

            DeleteObjectsRequest req = DeleteObjectsRequest.builder().bucket(base.getBase())
                    .delete(Delete.builder().objects(objectIdentifiers).build())
                    .build();

            return s3.deleteObjects(req).deleted().size();
        } catch (Exception e) {
            log.warn("", e);
            return 0;
        }
    }

    public boolean deleteOne(PathWithBase path) {
        try {
            DeleteObjectRequest req = DeleteObjectRequest.builder().bucket(path.getBase())
                    .key(path.getPathNoSlash())
                    .build();

            s3.deleteObject(req);
            return true;
        } catch (Exception e) {
            log.warn("", e);
            return false;
        }
    }

    public Set<String> makeAllFilesPublicAndReturnWhichFailed(PathWithBase target) {
        Set<String> failedItems = Collections.newSetFromMap(new ConcurrentHashMap<>());
        final List<S3ListObject> allItems = getAllItems(target, empty());
        AtomicNotifier okChanged = new AtomicNotifier(allItems.size(), 100,
                s -> log.info(s + " from " + target + " are now public"));

        async.coldTaskFJP().submit(() -> allItems.parallelStream().forEach($ -> {
            try {
                repeat.repeat(() -> {
                    final PutObjectAclRequest request = PutObjectAclRequest.builder().bucket(target.getBase())
                            .key($.getKey())
                            .acl(ObjectCannedACL.PUBLIC_READ)
                            .build();
                    s3.putObjectAcl(request);
                }, 10);
                okChanged.incrementAndNotify();
            } catch (Exception e) {
                log.error("", e);
                failedItems.add($.getKey());
            }
        }));

        return failedItems;
    }

    public boolean makeFilePublic(PathWithBase target) {
        try {
            repeat.repeat(() -> {
                final PutObjectAclRequest build = PutObjectAclRequest.builder().bucket(target.getBase())
                        .key(target.getPathNoSlash())
                        .acl(ObjectCannedACL.PUBLIC_READ)
                        .build();

                s3.putObjectAcl(build);
            }, 10);
            return true;
        } catch (Exception e) {
            log.error("", e);
            return false;
        }
    }

    public void copyPublic(PathWithBase source, PathWithBase destination) {
        CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
                .copySource(source.getEncodedUrl())
                .destinationBucket(destination.getBase())
                .destinationKey(destination.getPathNoSlash())
                .acl(ObjectCannedACL.PUBLIC_READ)
                .build();
        s3.copyObject(copyObjectRequest);
    }

    public S3ListResponse getItemPage(PathWithBase path, int count, O<String> nextMarker) {
        final ListObjectsRequest.Builder builder = ListObjectsRequest.builder();
        builder
                .bucket(path.getBase())
                .prefix(path.getPathWithSlash())
                .maxKeys(count)
                .build();
        nextMarker.ifPresent(builder::marker);
        ListObjectsRequest request = builder.build();
        final ListObjectsResponse response = s3.listObjects(request);

        O<String> marker = response.isTruncated()
                           ? O.ofNull(response.nextMarker()).or(() -> Cc.last(response.contents()).map($ -> $.key()))
                           : empty();

        return new S3ListResponse(marker, response.contents().stream().map($ -> new S3ListObject($.key())).collect(Cc.toL()));
    }

    public List<S3ListObject> getAllItems(PathWithBase path, O<Long> msBetweenPageRequests) {
        O<String> nextMarker = O.empty();
        List<S3ListResponse> responses = Cc.l();
        do {
            if (nextMarker.isPresent()) {
                msBetweenPageRequests.ifPresent($ -> async.sleep($));
            }
            S3ListResponse objectListing = getItemPage(path, 1000, nextMarker);
            responses.add(objectListing);
            nextMarker = objectListing.getNextMarker();
        } while (nextMarker.isPresent());

        return responses.stream().flatMap($ -> $.getObjects().stream())
                .filter($ -> !$.getKey().endsWith("/"))
                .collect(Cc.toL());
    }

    public O<byte[]> getFromS3(PathWithBase path) {
        final GetObjectRequest builder = GetObjectRequest.builder()
                .bucket(path.getBase())
                .key(path.getPathNoSlash())
                .build();

        try (InputStream stream = s3.getObject(builder)) {
            return ofNullable(Io.streamPump(stream));
        } catch (Exception e) {
        }
        return empty();
    }

    public <T> O<T> getObjectFromS3(PathWithBase path, TypeWrap<T> type) {
        return getFromS3(path).map($ -> json.from(new String($, St.UTF8), type));
    }

    public <T> O<T> getObjectFromS3(PathWithBase path, Class<T> type) {
        return getObjectFromS3(path, TypeWrap.simple(type));
    }

    @SneakyThrows
    public CrcAndSize downloadAsLocalFile(PathWithBase s3Path, String pcPath) {
        new File(pcPath).getParentFile().mkdirs();
        return downloadToStream(s3Path, new FileOutputStream(pcPath));
    }

    public CrcAndSize downloadToStream(PathWithBase s3Path, OutputStream os) {
        GetObjectRequest getObjReq = GetObjectRequest.builder().bucket(s3Path.getBase()).key(s3Path.getPathNoSlash()).build();
        return repeat.repeat(() -> {
            try {

                try (var in = new BufferedInputStream(s3.getObject(getObjReq));
                     var out = new BufferedOutputStream(os)) {
                    return bytes.crc32(in, out, 8 * 1024, Io.NONE);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, 10, 500);
    }

    public O<S3ItemMeta> getMeta(PathWithBase base, String pathFromRoot, boolean mustBePublic) {
        return getMeta(base.replacePath(pathFromRoot), mustBePublic);
    }

    public O<S3ItemMeta> getMeta(PathWithBase fullPath, boolean mustBePublic) {
        try {
            CoreHttpResponse rsp = null;
            HeadObjectResponse head = null;
            final String pathFromRoot = fullPath.getPathNoSlash();

            try {
                head = headObject(fullPath);
            } catch (NoSuchKeyException e) {
                return empty();
            }
            if (mustBePublic) {
                head = null;
                String url = getUrl(fullPath);
                rsp = http.headResp(url);
                if (rsp.code() == 403) {
                    makeFilePublic(fullPath);
                    rsp = http.headResp(url);
                }
            }

            HeadObjectResponse finalHead = head;
            return O.ofNull(rsp)
                    .filter($ -> $.getHeader("Content-Length").isPresent() && $.getHeader("ETag").isPresent())
                    .map($ -> new S3ItemMeta(pathFromRoot,
                            $.getHeader("Content-Length").map(Ma::pl).get(), $.getHeader("ETag").get(), false))
                    .or(() -> O.ofNull(finalHead)
                            .map($ -> new S3ItemMeta(pathFromRoot, $.contentLength(), $.eTag(), false)));
        } catch (Exception e) {
            return empty();
        }
    }

    public void clearAll(PathWithBase base, O<Long> msBetweenPageRequests) {
        Cc.splitCollectionRandomly(getAllItems(base, msBetweenPageRequests), 1000, () -> rand.rndLong())
                .values().parallelStream().forEach($ -> {
                    repeat.repeat(() -> deleteByKeys(base,
                            $.stream().map($$ -> $$.getKey()).collect(Cc.toL()).toArray(new String[0])), 10);
                });
    }

    public void clearAllByOneParallel(PathWithBase base, O<Long> msBetweenPageRequests) {
        Ex.toRuntime(() -> async.coldTaskFJP().submit(() -> getAllItems(base, msBetweenPageRequests)
                .parallelStream()
                .forEach($ -> repeat.repeat(() -> deleteOne(base.replacePath($.getKey())), 10))).get());
    }

    void getAndDelete(Map<String, String> metadata, String key, C1<String> ifPresent) {
        final String val = metadata.remove(key);
        if (val != null) {
            ifPresent.accept(val);
        }
    }
}
