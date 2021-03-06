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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * An immutable and sorted version of TreeNode
 *
 * Entries in the children map are sorted first by whether they have any children, and then alphabetically
 */
public class ImmutableTreeNode implements Comparable<ImmutableTreeNode> {
    private Map<String, ImmutableTreeNode> children = null;

    public ImmutableTreeNode(Map<String, ImmutableTreeNode> children) {
        if (children != null) {
            LinkedHashMap<String, ImmutableTreeNode> sortedMap = children.entrySet().stream()
                    .sorted((o1, o2) -> {
                        int childStatus = o1.getValue().compareTo(o2.getValue());
                        if (childStatus != 0) {
                            return childStatus;
                        }

                        return String.CASE_INSENSITIVE_ORDER.compare(o1.getKey(), o2.getKey());
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

            this.children = ImmutableMap.copyOf(sortedMap);
        }
    }

    public Optional<Map<String, ImmutableTreeNode>> getChildren() {
        return Optional.ofNullable(children);
    }

    public List<Map.Entry<Integer, String>> getNodeEndings() {
        if (children == null) {
            return Collections.emptyList();
        }

        List<Map.Entry<Integer, String>> results = new ArrayList<>();
        for (Map.Entry<String, ImmutableTreeNode> node : children.entrySet()) {

            // add self
            results.add(Maps.immutableEntry(0, node.getKey()));

            // add child nodes, incrementing their level & appending their prefix node
            results.addAll(node.getValue().getNodeEndings().stream()
                    .map(e -> Maps.immutableEntry(
                            e.getKey() + 1, // increment level
                            node.getKey() + "." + e.getValue())
                    )
                    .collect(Collectors.toList()));
        }
        return results;
    }

    @Override
    public int compareTo(ImmutableTreeNode o) {
        return (children != null) == o.getChildren().isPresent() ? 0 : (children != null ? 1 : -1);
    }
}
