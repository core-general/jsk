package jsk.gcl.agent.services;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2024 Core General
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

import jsk.gcl.agent.model.GcaUpdateFileProps;
import jsk.gcl.agent.model.GcaVersionId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import sk.services.bytes.IBytes;
import sk.services.http.CrcAndSize;
import sk.services.json.IJson;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;
import sk.utils.statics.Io;

import java.io.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class GcaFileStorage {
    private final GcaUpdateFileProps props;
    private final GcaInternalSyncedMeta<Versions> meta;
    private final String localPathToRepo;
    private final O<String> fileExtension;
    private final IBytes bytes;

    public GcaFileStorage(GcaUpdateFileProps props, IJson json, IBytes bytes) {
        this.props = props;
        fileExtension = props.getFileExtension();
        this.bytes = bytes;
        String rootPath = props.getInternalFsPath();
        meta = new GcaInternalSyncedMeta<>(json, rootPath + "meta.json", Versions.class, new Versions()) {
            @Override
            protected void beforeSave(Versions newVersioons) {
                super.beforeSave(newVersioons);
                clearVersionsIfNeeded(newVersioons);
            }
        };
        localPathToRepo = rootPath + "versions/";
        new File(localPathToRepo).mkdirs();
    }

    /** After save is done, moves old version to backup storage and sets new version to needed place */
    public OutputStream setNewVersionAndSetItAsCurrent(GcaVersionId version) throws Exception {
        log.debug("Trying to update to new version:" + version);
        String newFile = localPathToRepo + version.toString() + fileExtension.map($ -> "." + $).orElse("");
        log.debug("File for new version:" + newFile);
        return new FileOutputStream(newFile) {
            @Override
            @SneakyThrows
            public void close() throws IOException {
                super.close();
                try (FileInputStream is = new FileInputStream(newFile)) {
                    CrcAndSize crcAndSize = bytes.crc32(is, Io.NONE(), 16 * 1024, Io.NONE);
                    log.debug("Caclulated CrcAndSize of new file:" + crcAndSize);
                    meta.readOrUpdateObject(versions -> {
                        VersionInfo newVersion = new VersionInfo(version, newFile, VersionStatus.UNKNOWN, crcAndSize);
                        versions.getVersions().add(newVersion);
                        versions.setCurrentActual(O.of(newVersion));
                        log.debug("New version state:\n" + versions);
                        copyToActual(newVersion);
                    });
                }
            }
        };
    }

    /** Marks current version as OK, deletes all bad versions */
    public GcaVersionId setCurrentVersionIsOk() throws Exception {
        GcaVersionId[] ver = new GcaVersionId[1];
        meta.readOrUpdateObject(versions -> {
            log.debug("Current version is OK: " + versions.getCurrentActual().map($ -> $.toString()).orElse("X"));
            setStatusToCurrentVersion(versions, VersionStatus.GOOD);
            versions.setLastGoodVersion(versions.getCurrentActual());
            ver[0] = versions.getCurrentActual().get().getVersion();
            log.debug("New version state:\n" + versions);
        });
        return ver[0];
    }

    /** Marks current version as bad, tries to return to last good version */
    public GcaVersionId setCurrentVersionIsBadSwitchToLastGood() throws Exception {
        GcaVersionId[] ver = new GcaVersionId[1];
        meta.readOrUpdateObject(versions -> {
            log.debug("Current version is BAD: " + versions.getCurrentActual().map($ -> $.toString()).orElse("X"));
            setStatusToCurrentVersion(versions, VersionStatus.BAD);
            O<VersionInfo> lastGoodVersion = versions.getLastGoodVersion();
            versions.setCurrentActual(lastGoodVersion);
            if (lastGoodVersion.isEmpty()) {
                throw new RuntimeException("No good versions found!");
            }
            log.debug("New version state:\n" + versions);
            copyToActual(lastGoodVersion.get());
            ver[0] = lastGoodVersion.get().getVersion();
        });
        return ver[0];
    }

    public VersionStatus getVersionStatus(GcaVersionId actualFileInCloudVersionId) throws Exception {
        VersionStatus[] status = new VersionStatus[1];
        meta.readOrUpdateObject(v -> {
            status[0] = v.getVersions().stream().filter($ -> Fu.equal($.getVersion(), actualFileInCloudVersionId))
                    .findAny()
                    .map($ -> $.versionType)
                    .orElse(VersionStatus.NEW);
        });
        log.debug("Version status for '%s' = '%s'".formatted(actualFileInCloudVersionId, status[0]));
        return status[0];
    }

    private static void setStatusToCurrentVersion(Versions versions, VersionStatus status) {
        VersionInfo currentActual = versions.getCurrentActual().get();
        log.debug("Setting '%s' for '%s'".formatted(status, currentActual));
        currentActual.setVersionType(status);
        versions.getVersions().stream()
                .filter($ -> Fu.equal($.getVersion(), currentActual.getVersion()))
                .forEach($ -> $.setVersionType(status));
    }

    private void copyToActual(VersionInfo info) {
        log.debug("Copy from '%s' to '%s'".formatted(info.pathToContents, props.localFilePath()));
        Io.copy(info.pathToContents, props.localFilePath());
    }

    private void clearVersionsIfNeeded(Versions newVersions) {
        Set<VersionInfo> toDelete = newVersions.getVersions().stream()
                .skip(10)
                .filter($ ->
                        Fu.notEqual($, newVersions.getCurrentActual().orElse(null))
                        && Fu.notEqual($, newVersions.getLastGoodVersion().orElse(null)))
                .collect(Collectors.toSet());

        log.debug("Deleting: %s".formatted(toDelete.stream().map($ -> $.toString()).collect(Collectors.joining("||"))));

        newVersions.setVersions(
                newVersions.getVersions().stream().filter($ -> !toDelete.contains($)).collect(Cc.toL()));

        toDelete.parallelStream().forEach($ -> Io.deleteIfExists($.getPathToContents()));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static final class Versions {
        O<VersionInfo> lastGoodVersion = O.empty();
        O<VersionInfo> currentActual = O.empty();
        List<VersionInfo> versions = Cc.l();

        @Override
        public String toString() {
            return """
                    lastGood: %s
                    Current : %s
                    Versions: [%d]""".formatted(lastGoodVersion.map($ -> $.toString()).orElse("X"),
                    currentActual.map($ -> $.toString()).orElse("X"), versions.size());
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static final class VersionInfo {
        private GcaVersionId version;
        private String pathToContents;
        private VersionStatus versionType;
        private CrcAndSize crcAndSize;

        @Override
        public String toString() {
            return "%s %s %s %s".formatted(versionType, version, pathToContents, crcAndSize);
        }
    }

    public enum VersionStatus {GOOD, BAD, UNKNOWN, NEW}
}
