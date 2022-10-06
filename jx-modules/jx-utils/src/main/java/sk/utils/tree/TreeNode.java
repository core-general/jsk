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

import lombok.Data;
import lombok.Getter;
import sk.utils.functional.*;
import sk.utils.statics.Cc;
import sk.utils.statics.St;
import sk.utils.tuples.X;
import sk.utils.tuples.X1;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static sk.utils.functional.O.*;
import static sk.utils.tree.TreePath.emptyPath;
import static sk.utils.tree.TreePath.path;

/**
 * Created by kivan on 6/26/15
 */
@SuppressWarnings({"WeakerAccess", "unused"})
@Data
public class TreeNode<V, N> {
    V value;
    @Getter N name;
    Map<N, TreeNode<V, N>> childMap;
    F0<Map<N, TreeNode<V, N>>> childCreator;

    //region Creators
    private static <V, N> TreeNode<V, N> create(N name, F0<Map<N, TreeNode<V, N>>> childCreator) {
        Objects.requireNonNull(name);
        return new TreeNode<>(name, childCreator);
    }

    @SuppressWarnings("Convert2MethodRef")
    private static <V, N> TreeNode<V, N> create(N name) {
        Objects.requireNonNull(name);
        return create(name, () -> new HashMap<>());
    }

    static <V, N> TreeNode<V, N> rootNode(F0<Map<N, TreeNode<V, N>>> childCreator) {
        return new TreeNode<>(null, childCreator);
    }

    private TreeNode(N name, F0<Map<N, TreeNode<V, N>>> childCreator) {
        this.childCreator = childCreator;
        this.childMap = childCreator.get();
        this.name = name;
    }

    private TreeNode(N name, F0<Map<N, TreeNode<V, N>>> childCreator, V value) {
        this.childCreator = childCreator;
        this.childMap = childCreator.get();
        this.name = name;
        this.value = value;
    }

    static <V, N> P1<TreeNode<V, N>> notRoot() {
        return input -> !(input.getName() == null);
    }
    //endregion

    //region Get/Set
    public TreeNode<V, N> setRelativeValue(TreePath<N> p, V value) {
        setRelativeValue(p, value, 0);
        return this;
    }

    public TreeNode<V, N> setValBetween(TreePath<N> parent, TreePath<N> child, N newNodeId, V value) {
        setValBetween(parent, child, newNodeId, value, 0);
        return this;
    }

    public TreeNode<V, N> setNodeValue(V value) {
        return setRelativeValue(emptyPath(), value);
    }

    public O<V> removeNode(TreePath<N> p, boolean clearNullBranch) {
        return removeNode(p, this, 0, clearNullBranch);
    }

    public O<V> getValueOfLastExistingParent(TreePath<N> p) {
        return getValueOfLastExistingParent(p, 0);
    }

    public O<TreeNode<V, N>> getNodeOfLastExistingParent(TreePath<N> p) {
        return getNodeOfLastExistingParent(p, this, 0);
    }

    public O<V> getValue(TreePath<N> p) {
        return getValue(p, 0);
    }

    public O<V> getValue() {
        return ofNullable(value);
    }

    public List<TreePath<N>> getAllLeafs() {
        return processLeafs((path, pNode) -> path);
    }

    public boolean isLeaf() {
        return childMap.size() == 0;
    }
    //endregion

    //region Counts
    public int getLeafCount() {
        return countNodes(true, false, notRoot());
    }

    public int getNonLeafCount() {
        return countNodes(false, true, notRoot());
    }

    public int getAllCount() {
        return countNodes(true, true, notRoot());
    }

    private int countNodes(final boolean includeLeafs, final boolean includeNodes, final P1<TreeNode<V, N>> filter) {
        X1<Integer> count = X.x(0);
        runAll((p, n) -> {
            if (filter.test(n) && ((n.isLeaf() && includeLeafs) || (!n.isLeaf() && includeNodes))) {
                count.set(count.get() + 1);
            }
        });
        return count.get();
    }
    //endregion

    @SuppressWarnings("unchecked")
    public <Y> List<Y> processAll(F2<TreePath<N>, TreeNode<V, N>, Y> nodeProcessor) {
        return processAll((nTreePath, vnTreeNode) -> new Continuator<>(nodeProcessor.apply(nTreePath, vnTreeNode), true), path());
    }

    public <Y> List<Y> processAllByContinue(F2<TreePath<N>, TreeNode<V, N>, Continuator<Y>> nodeProcessor) {
        return processAll(nodeProcessor, path());
    }

    @SuppressWarnings("UnusedReturnValue")
    public <Y> List<Y> runAll(C2<TreePath<N>, TreeNode<V, N>> nodeRunner) {
        return processAll((a, b) -> {
            nodeRunner.accept(a, b);
            return null;
        });
    }

    public <Y> List<Y> processLeafs(final F2<TreePath<N>, TreeNode<V, N>, Y> leafProcessor) {
        return processAll((xTreePath, pxTreeNode) -> pxTreeNode.isLeaf() ? leafProcessor.apply(xTreePath, pxTreeNode) : null);
    }

    /**
     * @return null values are filtered
     */
    @SuppressWarnings("unchecked")
    protected <Y> List<Y> processAll(F2<TreePath<N>, TreeNode<V, N>, Continuator<Y>> processor, TreePath<N> path) {
        List<Y> xes = Cc.l();
        Continuator<Y> val = processor.apply(path, this);
        if (val.value() != null) {
            xes.add(val.value());
        }
        if (val.processChildren()) {
            childMap.entrySet().stream()
                    .map($ -> $.getValue().processAll(processor, path.merge(path($.getKey()))))
                    .filter($ -> $.size() > 0)
                    .forEach(xes::addAll);
        }
        return xes;
    }

    protected O<V> removeNode(TreePath<N> toRemove, TreeNode<V, N> parent, int index, boolean clearNullBranch) {
        if (toRemove.getSize() < index) {
            return empty();
        }

        if (toRemove.getSize() > index) {
            TreeNode<V, N> pNode = childMap.get(toRemove.getAt(index));
            if (pNode != null) {
                O<V> remove = pNode.removeNode(toRemove, this, index + 1, clearNullBranch);
                if (clearNullBranch && value == null && isLeaf() && parent != null) {
                    parent.removeChild(toRemove.getAt(index - 1));
                }
                return remove;
            } else {
                return empty();
            }
        } else {
            O<V> toRet = ofNullable(value);
            value = null;
            if (isLeaf() && parent != null) {
                parent.removeChild(toRemove.getAt(index - 1));
            }
            return toRet;
        }
    }

    protected void removeChild(N name) {
        childMap.remove(name);
    }

    protected O<V> getValue(TreePath<N> path, int index) {
        if (path.getSize() < index) {
            return empty();
        }

        return path.getSize() > index
               ? ofNullable(childMap.get(path.getAt(index))).flatMap($ -> $.getValue(path, index + 1))
               : getValue();
    }

    protected void setRelativeValue(TreePath<N> path, V value, int index) {
        if (path.getSize() < index) {
            return;
        }

        if (path.getSize() > index) {
            N pathPart = path.getAt(index);
            childMap.computeIfAbsent(pathPart, n -> TreeNode.create(n, childCreator))
                    .setRelativeValue(path, value, index + 1);
        } else {
            this.value = value;
        }
    }


    public void setValBetween(TreePath<N> parent, TreePath<N> child, N newNodeId, V value, int index) {
        if (!parent.isParentOf(child)) {
            throw new RuntimeException(parent + " is not parent for " + child);
        }
        if (parent.getSize() > index) {
            N pathPart = parent.getAt(index);
            childMap.get(pathPart).setValBetween(parent, child, newNodeId, value, index + 1);
        }
        if (parent.getSize() == index) {
            final N oldChild = child.getLeaf();
            final TreeNode<V, N> oldChildNode = childMap.remove(oldChild);
            final TreeNode<V, N> newNode = new TreeNode<>(newNodeId, childCreator, value);
            childMap.put(newNodeId, newNode);
            newNode.getChildMap().put(oldChild, oldChildNode);
        }
    }

    @SuppressWarnings("SameParameterValue")
    protected O<V> getValueOfLastExistingParent(TreePath<N> path, int counter) {
        return getNodeOfLastExistingParent(path, this, counter).map($ -> $.value);
    }

    protected O<TreeNode<V, N>> getNodeOfLastExistingParent(TreePath<N> path, TreeNode<V, N> parent, int index) {
        if (path.getSize() < index) {
            return empty();
        }

        return path.getSize() > index
               ? ofNull(childMap.get(path.getAt(index)))
                       .map($ -> $.getNodeOfLastExistingParent(path, this, index + 1))
                       .orElseGet(() -> this.value == null ? of(parent) : of(this))
               : of(this);
    }

    @Override
    public String toString() {
        return toString(0).trim();
    }

    protected String toString(int level) {
        return childMap.values().stream()
                .map($ -> St.repeat("   ", level) + "|- " + $.toString(level + 1))
                .collect(() -> new StringBuilder((name == null ? "ROOT" : name) + "\n"),
                        StringBuilder::append, StringBuilder::append)
                .toString();
    }
}
