package net.bull.javamelody.internal.model;

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

import lombok.extern.log4j.Log4j2;
import net.bull.javamelody.internal.common.Parameters;

import java.io.*;
import java.net.URL;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Log4j2
public class MelRemoteCollectorPatch {
    public List<Serializable> getRemoteOrLoadLocal(URL url, RemoteCall remoteCall, Collector collector,
            String applicationPath)
            throws IOException {
        List<Serializable> serialized;
        try {
            serialized = remoteCall.collectData();
            saveData(url, serialized, applicationPath);
        } catch (IOException e) {
            if (collector != null) {
                throw e;
            } else {
                return loadData(url, applicationPath);
            }
        }
        return serialized;
    }

    private void saveData(URL url, List<Serializable> serialized, String applicationPath) {
        try {
            File serFile = getFileForCurrentUrl(url, applicationPath);
            try (ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(serFile)))) {
                oos.writeObject(serialized);
            }
        } catch (Exception e) {
            log.error("E", e);
        }
    }

    private List<Serializable> loadData(URL url, String applicationPath) {
        List<Serializable> result;
        try {
            File serFile = getFileForCurrentUrl(url, applicationPath);
            if (!serFile.exists()) {
                throw new RuntimeException("No file for:" + url);
            }
            try (ObjectInputStream e = new ObjectInputStream(new GZIPInputStream(new FileInputStream(serFile)))) {
                result = (List<Serializable>) e.readObject();
            }
        } catch (Exception e) {
            throw new RuntimeException("Problem with:" + url, e);
        }
        return result;
    }

    private File getFileForCurrentUrl(URL url, String applicationPath) {
        return new File(Parameters.getStorageDirectory(applicationPath), url.getHost() + "_" + url.getPort() + ".ser");
    }
}
