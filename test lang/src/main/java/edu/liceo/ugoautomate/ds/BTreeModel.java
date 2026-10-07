package edu.liceo.ugoautomate.ds;

import java.util.ArrayList;
import java.util.List;

public final class BTreeModel {
    public record Node(List<String> keys, int childCount) {
    }

    private final List<List<Node>> levels;

    private BTreeModel(List<List<Node>> levels) {
        this.levels = levels;
    }

    public static BTreeModel build(List<String> sortedKeys, int order) {
        if (order < 3) {
            throw new IllegalArgumentException("order must be at least 3");
        }
        int maxKeys = order - 1;
        List<List<Node>> bottomUp = new ArrayList<>();
        if (sortedKeys.isEmpty()) {
            bottomUp.add(List.of(new Node(List.of(), 0)));
            return new BTreeModel(bottomUp);
        }

        List<Node> level = new ArrayList<>();
        List<String> minKeys = new ArrayList<>();
        for (int i = 0; i < sortedKeys.size(); i += maxKeys) {
            List<String> chunk = sortedKeys.subList(i, Math.min(i + maxKeys, sortedKeys.size()));
            level.add(new Node(List.copyOf(chunk), 0));
            minKeys.add(chunk.get(0));
        }
        bottomUp.add(level);

        while (level.size() > 1) {
            List<Node> parents = new ArrayList<>();
            List<String> parentMins = new ArrayList<>();
            for (int i = 0; i < level.size(); i += order) {
                int end = Math.min(i + order, level.size());
                List<String> separators = new ArrayList<>(minKeys.subList(i + 1, end));
                parents.add(new Node(separators, end - i));
                parentMins.add(minKeys.get(i));
            }
            level = parents;
            minKeys = parentMins;
            bottomUp.add(level);
        }

        List<List<Node>> topDown = new ArrayList<>();
        for (int i = bottomUp.size() - 1; i >= 0; i--) {
            topDown.add(bottomUp.get(i));
        }
        return new BTreeModel(topDown);
    }

    public List<List<Node>> levels() {
        return levels;
    }

    public int height() {
        return levels.size();
    }
}
