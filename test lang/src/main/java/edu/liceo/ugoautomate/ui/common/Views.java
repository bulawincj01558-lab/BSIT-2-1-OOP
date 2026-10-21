package edu.liceo.ugoautomate.ui.common;

import edu.liceo.ugoautomate.AppContext;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.net.URL;

public final class Views {
    private static final String ROOT = "/edu/liceo/ugoautomate/ui/";

    public record View<C>(Parent root, C controller) {
    }

    private Views() {
    }

    public static <C> View<C> load(AppContext ctx, String fxml) {
        FXMLLoader loader = new FXMLLoader(resource(fxml));
        loader.setControllerFactory(type -> createController(type, ctx));
        try {
            Parent root = loader.load();
            return new View<>(root, loader.getController());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load view " + fxml + ": " + e.getMessage(), e);
        }
    }

    public static void loadInto(Object component, String fxml) {
        FXMLLoader loader = new FXMLLoader(resource(fxml));
        loader.setRoot(component);
        loader.setController(component);
        try {
            loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load view " + fxml + ": " + e.getMessage(), e);
        }
    }

    public static Scene scene(Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(stylesheet());
        return scene;
    }

    public static void showDialog(Node owner, String title, Parent root) {
        Stage dialog = new Stage();
        dialog.setTitle(title);
        dialog.getIcons().add(appIcon());
        dialog.initModality(Modality.APPLICATION_MODAL);
        Window window = windowOf(owner);
        if (window != null) {
            dialog.initOwner(window);
        }
        Scene scene = new Scene(root);
        scene.getStylesheets().add(stylesheet());
        dialog.setScene(scene);
        dialog.setMinWidth(320);
        dialog.showAndWait();
    }

    public static void close(Node node) {
        if (windowOf(node) instanceof Stage stage) {
            stage.close();
        }
    }

    public static Window windowOf(Node node) {
        return node == null || node.getScene() == null ? null : node.getScene().getWindow();
    }

    public static Image appIcon() {
        return new Image(resource("images/liceo-logo.png").toExternalForm());
    }

    public static String stylesheet() {
        return resource("app.css").toExternalForm();
    }

    private static URL resource(String name) {
        URL url = Views.class.getResource(ROOT + name);
        if (url == null) {
            throw new IllegalStateException("Missing UI resource " + ROOT + name);
        }
        return url;
    }

    private static Object createController(Class<?> type, AppContext ctx) {
        try {
            for (Constructor<?> constructor : type.getConstructors()) {
                Class<?>[] params = constructor.getParameterTypes();
                if (params.length == 1 && params[0] == AppContext.class) {
                    return constructor.newInstance(ctx);
                }
            }
            return type.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot create controller " + type.getName(), e);
        }
    }
}
