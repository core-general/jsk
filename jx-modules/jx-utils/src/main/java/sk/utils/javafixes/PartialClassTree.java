package sk.utils.javafixes;

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

import sk.utils.functional.O;
import sk.utils.statics.St;
import sk.utils.tree.Tree;
import sk.utils.tree.TreePath;
import sk.utils.tree.TreeTraverseContinuator;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class PartialClassTree {
    private Tree<Class<?>, String> tree = Tree.create();

    private <A extends Class<?>, B extends Class<?>> ClsCompare compareClasses(A t1, B t2) {
        if (t1 == t2) {
            return ClsCompare.SAME;
        } else if (t1.isAssignableFrom(t2)) {
            return ClsCompare.FIRST_IS_PARENT;
        } else if (t2.isAssignableFrom(t1)) {
            return ClsCompare.FIRST_IS_CHILD;
        } else {
            return ClsCompare.DIFFERENT_BRANCHES;
        }
    }

    public O<Class<?>> getNearestParentTo(Class<?> clsToCheck) {
        final List<TreeCheck> treeChecks = tree.getRoot().processAllByContinue((path, node) -> {
            if (node.getValue().isPresent()) {
                final Class<?> val = node.getValue().get();
                if (val.isAssignableFrom(clsToCheck)) {
                    return new TreeTraverseContinuator<>(new TreeCheck(val, path, ClsCompare.FIRST_IS_PARENT), true);
                } else {
                    return new TreeTraverseContinuator<>(null, false);
                }
            } else {
                return new TreeTraverseContinuator<>(null, true);
            }
        });

        return O.of(treeChecks.stream().max(Comparator.comparing($ -> $.path().getSize())))
                .map($ -> $.cls);
    }

    public void add(Class<?> newCls) {
        if (tree.getRoot().getChildMap().isEmpty()) {
            tree.setVal(TreePath.path(newCls.getName()), newCls);
            return;
        }

        final List<TreeCheck> treeChecks = tree.getRoot().processAllByContinue((path, node) -> {
            if (St.isNullOrEmpty(path.getPath())) {
                return new TreeTraverseContinuator<>(null, true);
            }
            if (node.getValue().isPresent()) {
                final ClsCompare compare = compareClasses(node.getValue().get(), newCls);
                return switch (compare) {
                    case SAME -> new TreeTraverseContinuator<>(null, true);
                    case FIRST_IS_PARENT -> new TreeTraverseContinuator<>(new TreeCheck(newCls, path, compare), true);
                    case FIRST_IS_CHILD -> new TreeTraverseContinuator<>(new TreeCheck(newCls, path, compare), false);
                    case DIFFERENT_BRANCHES -> new TreeTraverseContinuator<>(null, false);
                };
            } else {
                return new TreeTraverseContinuator<>(null, false);
            }
        });

        final Optional<TreeCheck> clsIsParent = treeChecks.stream().filter($ -> $.compareWith == ClsCompare.FIRST_IS_CHILD)
                .min(Comparator.comparing($ -> $.path.getSize()));
        final Optional<TreeCheck> clsIsChild = treeChecks.stream().filter($ -> $.compareWith == ClsCompare.FIRST_IS_PARENT)
                .max(Comparator.comparing($ -> $.path.getSize()));

        if (clsIsParent.isEmpty() && clsIsChild.isEmpty()) {
            tree.setVal(TreePath.path(newCls.getName()), newCls);
        } else if (clsIsChild.isPresent() && clsIsParent.isPresent()) {
            final TreeCheck parentItem = clsIsChild.get();
            final TreeCheck childItem = clsIsParent.get();
            tree.setValBetween(parentItem.path(), childItem.path(), newCls.getName(), newCls);
        } else if (clsIsChild.isPresent() && clsIsParent.isEmpty()) {
            final TreeCheck parent = clsIsChild.get();
            tree.setVal(parent.path().merge(newCls.getName()), newCls);
        } else if (clsIsChild.isEmpty() && clsIsParent.isPresent()) {
            boolean firstIsDone = false;
            for (TreeCheck treeCheck : treeChecks) {
                if (!firstIsDone) {
                    tree.setValBetween(TreePath.emptyPath(), treeCheck.path(), newCls.getName(), newCls);
                    firstIsDone = true;
                } else {
                    tree.setNewParent(TreePath.path(newCls.getName()), treeCheck.path());
                }
            }
        }
    }

    @Override
    public String toString() {
        return tree.toString().trim();
    }

    private enum ClsCompare {
        SAME, FIRST_IS_PARENT, FIRST_IS_CHILD, DIFFERENT_BRANCHES
    }

    private record TreeCheck(Class<?> cls, TreePath<String> path, ClsCompare compareWith) {}
}
