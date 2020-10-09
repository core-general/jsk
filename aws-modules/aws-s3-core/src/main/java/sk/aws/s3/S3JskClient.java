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

import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import sk.aws.AwsUtilityHelper;
import sk.services.async.IAsync;
import sk.utils.files.PathWithBase;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Io;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.*;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.InputStream;
import java.util.List;

import static sk.utils.functional.O.empty;
import static sk.utils.functional.O.ofNullable;


@Log4j2
@NoArgsConstructor
public class S3JskClient {
    @Inject S3Properties conf;
    @Inject IAsync async;
    @Inject AwsUtilityHelper helper;

    public S3JskClient(S3Properties conf, IAsync async, AwsUtilityHelper helper) {
        this.conf = conf;
        this.async = async;
        this.helper = helper;
    }

    private S3Client s3;
    private F1<PathWithBase, String> urlGetter;

    @PostConstruct
    public void init() {
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
    }

    /**
     * Puts data to S3 and returns nothing
     */
    public void putPublicNoUrl(PathWithBase base, byte[] body) {
        putPublicNoUrl(base, body, true, empty(), empty());
    }

    /**
     * Puts data to S3 and returns nothing
     */
    public void putPublicNoUrl(PathWithBase base, byte[] body,
            O<String> contentType, O<String> contentEncoding) {
        putPublicNoUrl(base, body, true, contentType, contentEncoding);
    }

    /**
     * Puts data to S3 and returns URL
     */
    public String putPublic(PathWithBase base, byte[] body) {
        return putPublic(base, body, true, empty(), empty());
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
        putPublicNoUrl(base, body, allRead, contentType, contentEncoding);
        return urlGetter.apply(base);
    }

    /**
     * Puts data to S3 and returns URL
     */
    public void putPublicNoUrl(PathWithBase base, byte[] body, boolean allRead, O<String> contentType,
            O<String> contentEncoding) {
        PutObjectRequest.Builder builder = PutObjectRequest.builder();
        contentType.ifPresent(builder::contentType);
        contentEncoding.ifPresent(builder::contentEncoding);
        builder.bucket(base.getBase());
        builder.key(base.getPathNoSlash());

        if (allRead) {
            builder = builder.acl(ObjectCannedACL.PUBLIC_READ);
        }

        PutObjectRequest putObjectRequest = builder.build();

        s3.putObject(putObjectRequest, RequestBody.fromBytes(body));
    }

    public int deleteByKeys(PathWithBase base, String... keys) {
        try {
            base = base.replacePath("");
            PathWithBase finalBase = base;
            ObjectIdentifier[] objectIdentifiers = Cc.stream(keys)
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

    public void deleteOne(PathWithBase path) {
        try {
            DeleteObjectRequest req = DeleteObjectRequest.builder().bucket(path.getBase())
                    .key(path.getPathNoSlash())
                    .build();

            s3.deleteObject(req);
        } catch (Exception e) {
            log.warn("", e);
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
        return new S3ListResponse(O.ofNull(response.nextMarker()),
                response.contents().stream().map($ -> new S3ListObject($.key())).collect(Cc.toL()));
    }

    public List<S3ListResponse> getAllItems(PathWithBase path, O<Long> msBetweenPageRequests) {
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
        return responses;
    }

    public O<byte[]> getFromS3(PathWithBase path) {
        final GetObjectRequest builder = GetObjectRequest.builder()
                .bucket(path.getBase())
                .key(path.getPathNoSlash())
                .build();

        try (InputStream stream = s3.getObject(builder)) {
            return ofNullable(Io.streamToBytes(stream));
        } catch (Exception e) {
        }
        return empty();
    }

    public void clearAll(PathWithBase base, O<Long> msBetweenPageRequests) {
        getAllItems(base, msBetweenPageRequests).stream().forEach($ -> {
            deleteByKeys(base, $.getObjects().stream().map($$ -> $$.getKey()).collect(Cc.toL()).toArray(new String[0]));
        });
    }
}
