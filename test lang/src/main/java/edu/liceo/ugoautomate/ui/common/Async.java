package edu.liceo.ugoautomate.ui.common;

import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class Async {
    private static final ExecutorService WORKERS = Executors.newCachedThreadPool(task -> {
        Thread thread = new Thread(task, "ugo-worker");
        thread.setDaemon(true);
        return thread;
    });

    private Async() {
    }

    public static <T> void run(Node owner, Callable<T> task, Consumer<T> onSuccess) {
        run(owner, task, onSuccess, null);
    }

    public static <T> void run(Node owner, Callable<T> task, Consumer<T> onSuccess, Runnable onDone) {
        Scene scene = owner == null ? null : owner.getScene();
        if (scene != null) {
            scene.setCursor(Cursor.WAIT);
        }
        WORKERS.execute(() -> {
            T result = null;
            Throwable failure = null;
            try {
                result = task.call();
            } catch (Throwable t) {
                failure = t;
            }
            T value = result;
            Throwable error = failure;
            Platform.runLater(() -> {
                if (scene != null) {
                    scene.setCursor(Cursor.DEFAULT);
                }
                try {
                    if (error == null) {
                        onSuccess.accept(value);
                    } else {
                        Dialogs.error(owner, error);
                    }
                } finally {
                    if (onDone != null) {
                        onDone.run();
                    }
                }
            });
        });
    }
}
