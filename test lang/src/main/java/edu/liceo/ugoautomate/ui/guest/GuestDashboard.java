package edu.liceo.ugoautomate.ui.guest;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.ui.common.DashboardController;
import edu.liceo.ugoautomate.ui.common.DataStructuresController;
import edu.liceo.ugoautomate.ui.common.HomeController;
import edu.liceo.ugoautomate.ui.common.HomeController.Shortcut;
import edu.liceo.ugoautomate.ui.common.ProfileController;
import edu.liceo.ugoautomate.ui.common.Views;
import edu.liceo.ugoautomate.ui.common.Views.View;

public final class GuestDashboard {
    private static final String HOME = "Home";
    private static final String VISITS = "My Visits";
    private static final String DATA_STRUCTURES = "Data Structures";
    private static final String PROFILE = "My Profile";

    private GuestDashboard() {
    }

    public static View<DashboardController> create(AppContext ctx, User guest) {
        View<DashboardController> view = Views.load(ctx, "common/Dashboard.fxml");
        DashboardController d = view.controller();
        d.setRole("Guest");

        d.addPage(HOME, HomeController.create(ctx, "Welcome, " + guest.getFirstName() + "!", guest.getSummary(), d::showPage,
                new Shortcut(VISITS, "Register your visit before coming to campus and get your guest pass QR."),
                new Shortcut(DATA_STRUCTURES, "See the data structures behind the app, with memory allocation and time complexity."),
                new Shortcut(PROFILE, "Update your contact details or change your password.")));
        d.addPage(VISITS, Views.load(ctx, "guest/GuestVisits.fxml"));
        d.addPage(DATA_STRUCTURES, DataStructuresController.create(ctx, d));
        View<ProfileController> profile = Views.load(ctx, "common/Profile.fxml");
        profile.controller().setOnSaved(d::refreshHeader);
        d.addPage(PROFILE, profile);
        d.showFirstPage();
        return view;
    }
}
