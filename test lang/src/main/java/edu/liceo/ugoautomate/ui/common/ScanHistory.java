package edu.liceo.ugoautomate.ui.common;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ScanHistory {
    public static final int CAPACITY = 12;

    private final Deque<String> queue = new ArrayDeque<>();
    private final List<Runnable> listeners = new ArrayList<>();

    public void offer(String entry) {
        queue.offerLast(entry);
        if (queue.size() > CAPACITY) {
            queue.pollFirst();
        }
        listeners.forEach(Runnable::run);
    }

    public List<String> oldestFirst() {
        return new ArrayList<>(queue);
    }

    public int size() {
        return queue.size();
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }
}
