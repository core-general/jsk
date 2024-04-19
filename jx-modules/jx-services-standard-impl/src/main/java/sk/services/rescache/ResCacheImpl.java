package sk.services.rescache;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 Core General
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

import org.apache.commons.vfs2.*;
import sk.utils.functional.O;
import sk.utils.statics.Io;
import sk.utils.statics.St;
import sk.utils.tree.Tree;
import sk.utils.tree.TreePath;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public class ResCacheImpl implements IResCache {
    private Map<String, O<String>> cache = new ConcurrentHashMap<>();
    private Map<String, O<byte[]>> byteCache = new ConcurrentHashMap<>();

    @Override
    public O<String> getResource(String path) {
        return cache.computeIfAbsent(path, s -> Io.getResource(path));
    }

    @Override
    public O<byte[]> getResourceBytes(String path) {
        return byteCache.computeIfAbsent(path, s -> Io.getResourceBytes(path));
    }

    @Override
    public O<Tree<String, byte[]>> getResourcesInFolder(String folderInResources) {
        try {
            FileSystemManager fsManager = VFS.getManager();
            FileObject folder = fsManager.resolveFile("res:" + folderInResources);

            FileObject[] files = folder.findFiles(new FileSelector() {
                public boolean includeFile(FileSelectInfo fileInfo) throws Exception {
                    return fileInfo.getFile().getType() == FileType.FILE;
                }

                public boolean traverseDescendents(FileSelectInfo fileInfo) throws Exception {
                    return true;
                }
            });

            Tree<String, byte[]> tree = Tree.create();
            for (FileObject file : files) {
                tree.setVal(TreePath.path(
                                St.notStartWith(St.sub(file.getURL().toString()).leftFirst(folderInResources).get(), "/").split("/")),
                        Io.streamToBytes(file.getContent().getInputStream()));
            }
            return tree.getRoot().getLeafCount() == 0 ? O.empty() : O.of(tree);
        } catch (Exception e) {
            return O.empty();
        }
    }
}
