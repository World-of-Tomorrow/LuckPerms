/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.common.treeview;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Represents one "branch" of the node tree
 */
public class TreeNode {
    private Map<String, TreeNode> children = null;

    // lazy init
    public synchronized Map<String, TreeNode> getChildMap() {
        if (children == null) {
            children = new ConcurrentHashMap<>();
        }
        return children;
    }

    public Optional<Map<String, TreeNode>> getChildren() {
        return Optional.ofNullable(children);
    }

    public int getDeepSize() {
        if (children == null) {
            return 1;
        } else {
            return children.values().stream().mapToInt(TreeNode::getDeepSize).sum();
        }
    }

    public ImmutableTreeNode makeImmutableCopy() {
        if (children == null) {
            return new ImmutableTreeNode(null);
        } else {
            return new ImmutableTreeNode(children.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().makeImmutableCopy())));
        }
    }
}
