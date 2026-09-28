package edu.liceo.ugoautomate.ui.common;

import javax.swing.SwingWorker;
import java.awt.Component;
import java.awt.Cursor;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Runs service calls off the Swing event thread so the UI stays responsive,
 * then delivers the result (or shows the error) back on the event thread.
 */
public final class Async {

    private Async() {
    }

    public static <T> void run(Component parent, Callable<T> task, Consumer<T> onSuccess) {
        run(parent, task, onSuccess, null);
    }

    /**
     * @param parent    component used for the wait cursor and error dialogs (may be null)
     * @param task      work to run in the background
     * @param onSuccess called on the event thread with the task's result
     * @param onFinally called on the event thread after success or failure (may be null)
     */
    public static <T> void run(Component parent, Callable<T> task, Consumer<T> onSuccess, Runnable onFinally) {
        if (parent != null) {
            parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                if (parent != null) {
                    parent.setCursor(Cursor.getDefaultCursor());
                }
                try {
                    onSuccess.accept(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Dialogs.error(parent, e.getCause());
                } catch (RuntimeException e) {
                    Dialogs.error(parent, e);
                } finally {
                    if (onFinally != null) {
                        onFinally.run();
                    }
                }
            }
        }.execute();
    }
}
