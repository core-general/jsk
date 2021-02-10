package sk.web.tools.partuploader;

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

public enum PupExc {
    PUP_WRONG_PART_COUNT,
    PUP_WRONG_SIZE,
    PUP_FINISH_SIZE_WRONG,
    PUP_PARTS_GT_THAN_ALLOWED,
    PUP_SIZE_GT_THAN_STATED,
    PUP_UNAUTHORIZED,
    PUP_SIZE_IS_NOT_AS_IN_META,
    PUP_UPLOAD_ALREADY_FAILED,
    PUP_UPLOAD_ALREADY_FINISHED,
    PUP_PART_ALREADY_PROCESSED,
    PUP_SIZE_GT_THAN_ALLOWED,
    PUP_OTHER,
    PUP_CANT_UPLOAD,
    PUP_NO_UPLOAD;
    /*PUP_PARTS_GT_THAN_STATED, is not needed since we will use PUP_UPLOAD_ALREADY_FINISHED*/

}
