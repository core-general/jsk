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
import sk.utils.functional.F0;
import sk.utils.functional.O;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kivan on 6/27/15
 */
@SuppressWarnings({"WeakerAccess", "unused"})
@EqualsAndHashCode
public class Tree<K, V> {
    @Getter
    TreeNode<K, V> root;
    V nullValue;

    public static <K, V> Tree<K, V> create(V nullValue, F0<Map<K, TreeNode<K, V>>> sup) {
        return new Tree<>(nullValue, sup);
    }

    @SuppressWarnings("Convert2MethodRef")
    public static <K, V> Tree<K, V> create(V nullValue) {
        return create(nullValue, () -> new HashMap<>());
    }

    public static <K, V> Tree<K, V> create() {
        return create(null);
    }

    private Tree(V nv, F0<Map<K, TreeNode<K, V>>> sup) {
        root = TreeNode.rootNode(sup);
        nullValue = nv;
    }

    public Tree<K, V> setVal(TreePath<K> p, V value) {
        getRoot().setRelativeValue(p, value);
        return this;
    }

    public Tree<K, V> setValBetween(TreePath<K> parent, TreePath<K> child, K newNodeId, V value) {
        getRoot().setValBetween(parent, child, newNodeId, value);
        return this;
    }

    public V getVal(TreePath<K> p) {
        return getRoot().getValue(p).orElse(nullValue);
    }

    public V removeNode(TreePath<K> path) {
        return getRoot().removeNode(path, false).orElse(nullValue);
    }

    public V removeNodeClearBranchWithNulls(TreePath<K> path) {
        return getRoot().removeNode(path, true).orElse(nullValue);
    }

    public O<TreeNode<K, V>> getNode(TreePath<K> p) {
        //todo fix the issue that it doesn't return empty if there is no such path
        return getRoot().getNodeOfLastExistingParent(p);
    }

    public void setNewParent(TreePath<K> newParent, TreePath<K> oldPath) {
        final TreeNode<K, V> newParentNode = getNode(newParent).get();
        final TreeNode<K, V> oldParent = getNode(oldPath.getParent()).get();

        final TreeNode<K, V> nodeToRelink = oldParent.getChildMap().remove(oldPath.getLeaf());
        newParentNode.getChildMap().put(nodeToRelink.getName(), nodeToRelink);
    }

    @Override
    public String toString() {
        return getRoot().toString().trim();
    }

}
