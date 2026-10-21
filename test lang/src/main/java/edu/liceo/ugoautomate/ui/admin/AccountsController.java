package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.Role;
import edu.liceo.ugoautomate.model.Student;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Choice;
import edu.liceo.ugoautomate.ui.common.Dialogs;
import edu.liceo.ugoautomate.ui.common.DisplayTime;
import edu.liceo.ugoautomate.ui.common.Page;
import edu.liceo.ugoautomate.ui.common.Tables;
import edu.liceo.ugoautomate.ui.common.Views;
import edu.liceo.ugoautomate.ui.common.Views.View;
import edu.liceo.ugoautomate.util.AppConfig;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.List;

public class AccountsController implements Page {
    private static final String SELECT_HINT = "Select an account in the list first.";

    private final AppContext ctx;

    @FXML private ComboBox<Choice<Role>> roleFilter;
    @FXML private TextField search;
    @FXML private Label count;
    @FXML private TableView<User> table;

    public AccountsController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        Choice.fill(roleFilter, "All account types", List.of(Role.STUDENT, Role.GUEST, Role.ADMIN));

        Tables.setup(table, "No accounts match the filter.");
        Tables.column(table, "Student ID", u -> u instanceof Student s ? s.getStudentNumber() : "");
        Tables.column(table, "Name", u -> u.getLastName() + ", " + u.getFirstName());
        Tables.column(table, "Email", User::getEmail);
        Tables.column(table, "Type", User::getRole);
        Tables.column(table, "Details", User::getSummary);
        Tables.column(table, "Contact", User::getContactNumber);
        Tables.column(table, "Status", u -> u.isActive() ? "Active" : "Deactivated");
        Tables.column(table, "Registered", User::getCreatedAt, DisplayTime::format);
        Tables.onDoubleClick(table, this::edit);
    }

    @Override
    public void onShow() {
        reload();
    }

    @FXML
    private void reload() {
        Role role = Choice.selectedValue(roleFilter);
        String text = search.getText();
        Async.run(table, () -> ctx.accounts().listAccounts(role, text), users -> {
            Tables.setRows(table, users);
            count.setText(users.size() + " account(s)");
        });
    }

    @FXML
    private void addStudent() {
        View<StudentAddController> view = Views.load(ctx, "admin/StudentAdd.fxml");
        Views.showDialog(table, "Add Student", view.root());
        view.controller().added().ifPresent(student -> {
            reload();
            Dialogs.info(table, student.getFullName() + " (" + student.getStudentNumber() + ") was added. "
                    + "Give them their email and password so they can log in.");
        });
    }

    @FXML
    private void edit() {
        Tables.selected(table, SELECT_HINT).ifPresent(this::edit);
    }

    private void edit(User user) {
        View<AccountEditController> view = Views.load(ctx, "admin/AccountEdit.fxml");
        view.controller().setUser(user);
        Views.showDialog(table, "Edit Account", view.root());
        if (view.controller().isSaved()) {
            reload();
        }
    }

    @FXML
    private void toggleActive() {
        Tables.selected(table, SELECT_HINT).ifPresent(user -> {
            boolean activate = !user.isActive();
            String verb = activate ? "Activate" : "Deactivate";
            if (!Dialogs.confirm(table, verb + " the account of " + user.getFullName() + "?")) {
                return;
            }
            Async.run(table, () -> {
                ctx.accounts().setActive(user.getId(), activate);
                return Boolean.TRUE;
            }, ok -> reload());
        });
    }

    @FXML
    private void resetPassword() {
        Tables.selected(table, SELECT_HINT).ifPresent(user -> {
            PasswordField field = new PasswordField();
            Label hint = new Label("At least 8 characters with a letter and a number.");
            hint.getStyleClass().add("muted");
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(AppConfig.APP_NAME);
            dialog.setHeaderText("New password for " + user.getFullName());
            dialog.getDialogPane().setContent(new VBox(8, field, hint));
            dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
            dialog.getDialogPane().getStylesheets().add(Views.stylesheet());
            dialog.initOwner(Views.windowOf(table));
            if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }
            String password = field.getText();
            Async.run(table, () -> {
                ctx.accounts().resetPassword(user.getId(), password);
                return Boolean.TRUE;
            }, ok -> Dialogs.info(table, "Password reset. Give the new password to " + user.getFullName() + "."));
        });
    }

    @FXML
    private void delete() {
        Tables.selected(table, SELECT_HINT).ifPresent(user -> {
            if (!Dialogs.confirm(table, "Permanently delete the account of " + user.getFullName() + "?\n"
                    + "Their attendance records and visits will also be deleted. Entry logs are kept.")) {
                return;
            }
            Async.run(table, () -> {
                ctx.accounts().deleteAccount(user.getId());
                return Boolean.TRUE;
            }, ok -> reload());
        });
    }
}
