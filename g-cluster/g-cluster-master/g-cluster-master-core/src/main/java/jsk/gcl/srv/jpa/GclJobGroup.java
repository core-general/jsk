
package jsk.gcl.srv.jpa;

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

public interface GclJobGroup {
    GclJobGroupId getJgId();

    void setJgId(GclJobGroupId jgId);

    java.lang.String getJgTag();

    void setJgTag(java.lang.String jgTag);

    jsk.gcl.srv.scaling.model.GclJobStatus getJgStatus();

    void setJgStatus(jsk.gcl.srv.scaling.model.GclJobStatus jgStatus);

    jsk.gcl.srv.scaling.model.GclJobGroupInnerState getJgInnerState();

    void setJgInnerState(jsk.gcl.srv.scaling.model.GclJobGroupInnerState jgInnerState);

    java.time.ZonedDateTime getCreatedAt();

    void setCreatedAt(java.time.ZonedDateTime createdAt);

    java.time.ZonedDateTime getUpdatedAt();

    void setUpdatedAt(java.time.ZonedDateTime updatedAt);
}
