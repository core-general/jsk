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
public class Tree<V, N> {
    @Getter
    TreeNode<V, N> root;
    V nullValue;

    public static <V, N> Tree<V, N> create(V nullValue, F0<Map<N, TreeNode<V, N>>> sup) {
        return new Tree<>(nullValue, sup);
    }

    @SuppressWarnings("Convert2MethodRef")
    public static <V, N> Tree<V, N> create(V nullValue) {
        return create(nullValue, () -> new HashMap<>());
    }

    public static <V, N> Tree<V, N> create() {
        return create(null);
    }

    private Tree(V nv, F0<Map<N, TreeNode<V, N>>> sup) {
        root = TreeNode.rootNode(sup);
        nullValue = nv;
    }

    public Tree<V, N> setVal(TreePath<N> p, V value) {
        getRoot().setRelativeValue(p, value);
        return this;
    }

    public Tree<V, N> setValBetween(TreePath<N> parent, TreePath<N> child, N newNodeId, V value) {
        getRoot().setValBetween(parent, child, newNodeId, value);
        return this;
    }

    public V getVal(TreePath<N> p) {
        return getRoot().getValue(p).orElse(nullValue);
    }

    public V removeNode(TreePath<N> path) {
        return getRoot().removeNode(path, false).orElse(nullValue);
    }

    public V removeNodeClearBranchWithNulls(TreePath<N> path) {
        return getRoot().removeNode(path, true).orElse(nullValue);
    }

    public O<TreeNode<V, N>> getNode(TreePath<N> p) {
        return getRoot().getNodeOfLastExistingParent(p);
    }

    public void setNewParent(TreePath<N> newParent, TreePath<N> oldPath) {
        final TreeNode<V, N> newParentNode = getNode(newParent).get();
        final TreeNode<V, N> oldParent = getNode(oldPath.getParent()).get();

        final TreeNode<V, N> nodeToRelink = oldParent.getChildMap().remove(oldPath.getLeaf());
        newParentNode.getChildMap().put(nodeToRelink.getName(), nodeToRelink);
    }

    @Override
    public String toString() {
        return getRoot().toString().trim();
    }

}
