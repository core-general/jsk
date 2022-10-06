
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

public interface GclJob {
    GclJobId getJId();

    void setJId(GclJobId jId);

    java.lang.String getJTag();

    void setJTag(java.lang.String jTag);

    GclJobGroupId getJJgId();

    GclJobGroup getJJg();

    void setJJgId(GclJobGroupId jJgId);

    jsk.gcl.srv.scaling.model.GclJobStatus getJStatus();

    void setJStatus(jsk.gcl.srv.scaling.model.GclJobStatus jStatus);

    jsk.gcl.srv.scaling.model.GclJobInnerState getJInnerState();

    void setJInnerState(jsk.gcl.srv.scaling.model.GclJobInnerState jInnerState);

    java.time.ZonedDateTime getJLifePing();

    void setJLifePing(java.time.ZonedDateTime jLifePing);

    java.time.ZonedDateTime getCreatedAt();

    void setCreatedAt(java.time.ZonedDateTime createdAt);

    java.time.ZonedDateTime getUpdatedAt();

    void setUpdatedAt(java.time.ZonedDateTime updatedAt);


}
