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

import sk.utils.functional.O;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.SortedMap;

public interface INodeInfo extends INodeId {
    public String getNodeVersion();

    public ZonedDateTime getBuildTime();

    public O<String> getPublicIp();

    public O<String> getPrivateIp();

    /**
     * Obtain information about current server state as the sum of states of all his beans.
     * The information should be gathered automatically from beans, which implement IBeanInfo interface
     *
     * @param filter if empty, then all, if not empty, then just a subset of all list
     */
    public SortedMap<String, Object> getCurrentServerInfo(O<List<String>> filter);

    public boolean isShuttingDown();
}
