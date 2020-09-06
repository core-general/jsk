package sk.utils.tree;

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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import sk.utils.statics.Cc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static sk.utils.statics.Cc.*;

@SuppressWarnings({"WeakerAccess", "unused"})
@EqualsAndHashCode(exclude = "pathS")
public class TreePath<X> {
    private final static String D = "/";
    @Getter
    private String path;
    private List<X> pathS;

    public static <X> TreePath<X> emptyPath() {
        return new TreePath<>(Collections.emptyList());
    }

    @SuppressWarnings("unchecked")
    public static <X> TreePath<X> path(X... path) {
        return path.length == 0 ? emptyPath() : new TreePath<>(Arrays.asList(path));
    }

    private TreePath(List<X> pth) {
        pathS = new ArrayList<>(pth);
        path = join(D, pathS);
    }

    public TreePath<X> merge(TreePath<X> toAdd) {
        ArrayList<X> path = addAll(new ArrayList<>(pathS), toAdd.pathS);
        return new TreePath<>(path);
    }

    public TreePath<X> getParent() {
        return pathS.size() > 1 ? new TreePath<>(pathS.subList(0, pathS.size() - 1)) : emptyPath();
    }

    public boolean isParentOf(TreePath<X> p) {
        return p.isChildOf(this);
    }

    public boolean isChildOf(TreePath<X> p) {
        return path.indexOf(p.getPath()) == 0;
    }

    public int getSize() {
        return pathS.size();
    }

    public X getLeaf() {
        return last(pathS).orElse(null);
    }

    public X getRoot() {
        return first(pathS).orElse(null);
    }

    public X getAt(int index) {
        return Cc.getAt(pathS, index).orElse(null);
    }

    @Override
    public String toString() {
        return path;
    }

    public String toStringWith(String delimeter) {
        return join(delimeter, pathS);
    }

}
