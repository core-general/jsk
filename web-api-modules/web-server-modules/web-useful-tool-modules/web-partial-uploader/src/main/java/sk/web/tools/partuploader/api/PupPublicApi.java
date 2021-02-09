package sk.web.tools.partuploader.api;

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

import sk.web.annotations.WebAuth;
import sk.web.annotations.WebIdempotence;
import sk.web.annotations.WebPath;
import sk.web.annotations.WebUserToken;
import sk.web.annotations.type.WebPOST;

@WebPath("jskPartUploader")
@WebUserToken
@WebAuth(srvProvider = PupIWebAuthProvider.class)
public interface PupPublicApi<META, FINISH> {
    /**
     * Allows to upload part
     *
     * @param _userToken user token
     * @param partMeta   upload metadata
     * @param content    body
     * @return
     * @throws PUP_WRONG_PART_COUNT
     * @throws PUP_WRONG_SIZE
     * @throws PUP_FINISH_SIZE_WRONG
     * @throws PUP_PARTS_GT_THAN_ALLOWED
     * @throws PUP_SIZE_GT_THAN_STATED
     * @throws PUP_UNAUTHORIZED
     * @throws PUP_SIZE_IS_NOT_AS_IN_META
     * @throws PUP_UPLOAD_ALREADY_FAILED
     * @throws PUP_UPLOAD_ALREADY_FINISHED
     * @throws PUP_PART_ALREADY_PROCESSED
     * @throws PUP_SIZE_GT_THAN_ALLOWED
     * @throws OTHER
     */
    @WebPOST(forceMultipart = true)
    @WebIdempotence
    public PupMUploadStatus<META, FINISH> uploadPart(String _userToken, PupMUploadMeta<META> partMeta, byte[] content);

    public PupMUploadStatus<META, FINISH> getUploadStatus(String _userToken, PupMUploadId id);

}
