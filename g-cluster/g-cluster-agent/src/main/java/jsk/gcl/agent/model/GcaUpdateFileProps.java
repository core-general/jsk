package jsk.gcl.agent.model;

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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import sk.utils.files.PathWithBase;
import sk.utils.functional.O;
import sk.utils.statics.St;

import java.io.File;
import java.time.Duration;

@AllArgsConstructor
@EqualsAndHashCode

public final class GcaUpdateFileProps {
    private final String metaFileBucket;
    private final String metaFilePathInBucket;
    private final String localFilePath;
    private final O<String> serviceToReboot;
    private final O<Duration> recheckDuration;
    private final GcaRollingUpdateConfig rollingUpdate;

    public O<String> getFileExtension() {
        return localFilePath.contains(".") ? O.of(St.subLL(localFilePath(), ".")) : O.empty();
    }

    public String getInternalFsPath() {
        return St.endWith(new File(localFilePath()).getParentFile().getAbsolutePath(), "/") + ".internal_fs/";
    }

    public PathWithBase getS3LockPathFile() {
        return new PathWithBase(metaFileBucket, metaFilePathInBucket).getParent().get().addToPath("lock.json");
    }

    public PathWithBase getPathToMetaFile() {return new PathWithBase(metaFileBucket, metaFilePathInBucket);}

    public String getInternalFileName() {
        return St.subLL(localFilePath, "/");
    }

    public String localFilePath() {return localFilePath;}

    public O<String> serviceToReboot() {return serviceToReboot;}

    public O<Duration> recheckDuration() {return recheckDuration;}

    public GcaRollingUpdateConfig getRollingUpdate() {
        return rollingUpdate == null ? new GcaRollingUpdateConfig(false, "", 0) : rollingUpdate;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GcaRollingUpdateConfig {
        boolean enabled;
        String url;
        long okAfterMs;
    }
}
