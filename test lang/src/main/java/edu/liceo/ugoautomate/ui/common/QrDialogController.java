package edu.liceo.ugoautomate.ui.common;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.security.QrToken;
import edu.liceo.ugoautomate.ui.common.Views.View;
import javafx.fxml.FXML;
import javafx.scene.Node;

public class QrDialogController {
    @FXML private QrView qrView;

    public static void show(AppContext ctx, Node owner, String title, QrToken token, String caption, String fileName) {
        View<QrDialogController> view = Views.load(ctx, "common/QrDialog.fxml");
        view.controller().qrView.show(token, caption, fileName);
        Views.showDialog(owner, title, view.root());
        view.controller().qrView.stop();
    }

    @FXML
    private void close() {
        Views.close(qrView);
    }
}
