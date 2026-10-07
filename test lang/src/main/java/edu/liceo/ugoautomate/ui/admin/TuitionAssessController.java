package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.service.TuitionService;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Dialogs;
import edu.liceo.ugoautomate.ui.common.Views;
import edu.liceo.ugoautomate.util.Money;
import edu.liceo.ugoautomate.util.ValidationException;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.math.BigDecimal;

public class TuitionAssessController {
    private final AppContext ctx;
    private String term;
    private boolean done;

    @FXML private Label details;
    @FXML private TextField perUnit;
    @FXML private TextField miscFee;

    public TuitionAssessController(AppContext ctx) {
        this.ctx = ctx;
    }

    public void setTerm(String forTerm) {
        this.term = forTerm;
        details.setText("Term: " + forTerm);
        Async.run(perUnit, () -> ctx.tuition().rates(), rates -> {
            perUnit.setText(Money.toInput(rates.perUnitCents()));
            miscFee.setText(Money.toInput(rates.miscFeeCents()));
        });
    }

    public boolean isDone() {
        return done;
    }

    @FXML
    private void assess() {
        String rateText = perUnit.getText();
        String miscText = miscFee.getText();
        Async.run(perUnit, () -> {
            long misc = miscText == null || miscText.isBlank() || isZero(miscText) ? 0 : Money.parse(miscText, "Miscellaneous fees");
            return ctx.tuition().assess(term, new TuitionService.Rates(Money.parse(rateText, "Tuition per unit"), misc));
        }, result -> {
            done = true;
            Dialogs.info(perUnit, result.assessed() + " student(s) were charged tuition for " + term + "."
                    + (result.alreadyAssessed() > 0 ? "\n" + result.alreadyAssessed() + " already had tuition and were skipped." : "")
                    + (result.assessed() + result.alreadyAssessed() == 0 ? "\nNo students are enrolled in this term's classes yet." : ""));
            Views.close(perUnit);
        });
    }

    private static boolean isZero(String text) {
        try {
            return new BigDecimal(text.replace(",", "").trim()).signum() == 0;
        } catch (NumberFormatException e) {
            throw new ValidationException("Miscellaneous fees must be an amount such as 5,000.00.");
        }
    }

    @FXML
    private void cancel() {
        Views.close(perUnit);
    }
}
