package sk.services.nodeinfo;

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

import sk.services.nodeinfo.model.IServerInfo;
import sk.utils.functional.O;

import java.time.ZonedDateTime;

public interface INodeInfo extends INodeId {
    public String getNodeVersion();

    public ZonedDateTime getBuildTime();

    public ZonedDateTime getStartTime();

    public O<String> getPublicIp();

    public O<String> getPrivateIp();

    public IServerInfo getCurrentServerInfo();

    public boolean isShuttingDown();
}
