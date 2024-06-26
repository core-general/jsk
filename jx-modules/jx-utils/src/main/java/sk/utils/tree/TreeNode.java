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
public class TreeNode<K, V> {
    V value;
    @Getter K name;
    Map<K, TreeNode<K, V>> childMap;
    F0<Map<K, TreeNode<K, V>>> childCreator;

    //region Creators
    private static <K, V> TreeNode<K, V> create(K name, F0<Map<K, TreeNode<K, V>>> childCreator) {
        Objects.requireNonNull(name);
        return new TreeNode<>(name, childCreator);
    }

    @SuppressWarnings("Convert2MethodRef")
    private static <K, V> TreeNode<K, V> create(K name) {
        Objects.requireNonNull(name);
        return create(name, () -> new HashMap<>());
    }

    static <K, V> TreeNode<K, V> rootNode(F0<Map<K, TreeNode<K, V>>> childCreator) {
        return new TreeNode<>(null, childCreator);
    }

    private TreeNode(K name, F0<Map<K, TreeNode<K, V>>> childCreator) {
        this.childCreator = childCreator;
        this.childMap = childCreator.get();
        this.name = name;
    }

    private TreeNode(K name, F0<Map<K, TreeNode<K, V>>> childCreator, V value) {
        this.childCreator = childCreator;
        this.childMap = childCreator.get();
        this.name = name;
        this.value = value;
    }

    static <K, V> P1<TreeNode<K, V>> notRoot() {
        return input -> !(input.getName() == null);
    }
    //endregion

    //region Get/Set
    public TreeNode<K, V> setRelativeValue(TreePath<K> p, V value) {
        setRelativeValue(p, value, 0);
        return this;
    }

    public TreeNode<K, V> setValBetween(TreePath<K> parent, TreePath<K> child, K newNodeId, V value) {
        setValBetween(parent, child, newNodeId, value, 0);
        return this;
    }

    public TreeNode<K, V> setNodeValue(V value) {
        return setRelativeValue(emptyPath(), value);
    }

    public O<V> removeNode(TreePath<K> p, boolean clearNullBranch) {
        return removeNode(p, this, 0, clearNullBranch);
    }

    public O<V> getValueOfLastExistingParent(TreePath<K> p) {
        return getValueOfLastExistingParent(p, 0);
    }

    public O<TreeNode<K, V>> getNodeOfLastExistingParent(TreePath<K> p) {
        return getNodeOfLastExistingParent(p, this, 0);
    }

    public O<V> getValue(TreePath<K> p) {
        return getValue(p, 0);
    }

    public O<V> getValue() {
        return ofNullable(value);
    }

    public List<TreePath<K>> getAllLeafs() {
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

    private int countNodes(final boolean includeLeafs, final boolean includeNodes, final P1<TreeNode<K, V>> filter) {
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
    public <Y> List<Y> processAll(F2<TreePath<K>, TreeNode<K, V>, Y> nodeProcessor) {
        return processAll(
                (nTreePath, vnTreeNode) -> new TreeTraverseContinuator<>(nodeProcessor.apply(nTreePath, vnTreeNode), true),
                path());
    }

    public <Y> List<Y> processAllByContinue(F2<TreePath<K>, TreeNode<K, V>, TreeTraverseContinuator<Y>> nodeProcessor) {
        return processAll(nodeProcessor, path());
    }

    @SuppressWarnings("UnusedReturnValue")
    public <Y> List<Y> runAll(C2<TreePath<K>, TreeNode<K, V>> nodeRunner) {
        return processAll((a, b) -> {
            nodeRunner.accept(a, b);
            return null;
        });
    }

    public <Y> List<Y> processLeafs(final F2<TreePath<K>, TreeNode<K, V>, Y> leafProcessor) {
        return processAll((xTreePath, pxTreeNode) -> pxTreeNode.isLeaf() ? leafProcessor.apply(xTreePath, pxTreeNode) : null);
    }

    /**
     * @return null values are filtered
     */
    @SuppressWarnings("unchecked")
    protected <Y> List<Y> processAll(F2<TreePath<K>, TreeNode<K, V>, TreeTraverseContinuator<Y>> processor, TreePath<K> path) {
        List<Y> xes = Cc.l();
        TreeTraverseContinuator<Y> val = processor.apply(path, this);
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

    protected O<V> removeNode(TreePath<K> toRemove, TreeNode<K, V> parent, int index, boolean clearNullBranch) {
        if (toRemove.getSize() < index) {
            return empty();
        }

        if (toRemove.getSize() > index) {
            TreeNode<K, V> pNode = childMap.get(toRemove.getAt(index));
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

    protected void removeChild(K name) {
        childMap.remove(name);
    }

    protected O<V> getValue(TreePath<K> path, int index) {
        if (path.getSize() < index) {
            return empty();
        }

        return path.getSize() > index
               ? ofNullable(childMap.get(path.getAt(index))).flatMap($ -> $.getValue(path, index + 1))
               : getValue();
    }

    protected void setRelativeValue(TreePath<K> path, V value, int index) {
        if (path.getSize() < index) {
            return;
        }

        if (path.getSize() > index) {
            K pathPart = path.getAt(index);
            childMap.computeIfAbsent(pathPart, n -> TreeNode.create(n, childCreator))
                    .setRelativeValue(path, value, index + 1);
        } else {
            this.value = value;
        }
    }


    public void setValBetween(TreePath<K> parent, TreePath<K> child, K newNodeId, V value, int index) {
        if (!parent.isParentOf(child)) {
            throw new RuntimeException(parent + " is not parent for " + child);
        }
        if (parent.getSize() > index) {
            K pathPart = parent.getAt(index);
            childMap.get(pathPart).setValBetween(parent, child, newNodeId, value, index + 1);
        }
        if (parent.getSize() == index) {
            final K oldChild = child.getLeaf();
            final TreeNode<K, V> oldChildNode = childMap.remove(oldChild);
            final TreeNode<K, V> newNode = new TreeNode<>(newNodeId, childCreator, value);
            childMap.put(newNodeId, newNode);
            newNode.getChildMap().put(oldChild, oldChildNode);
        }
    }

    @SuppressWarnings("SameParameterValue")
    protected O<V> getValueOfLastExistingParent(TreePath<K> path, int counter) {
        return getNodeOfLastExistingParent(path, this, counter).map($ -> $.value);
    }

    protected O<TreeNode<K, V>> getNodeOfLastExistingParent(TreePath<K> path, TreeNode<K, V> parent, int index) {
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
