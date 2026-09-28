package edu.liceo.ugoautomate.ui.guest;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.ui.common.CampusNavigatorPanel;
import edu.liceo.ugoautomate.ui.common.DashboardFrame;
import edu.liceo.ugoautomate.ui.common.DataStructuresPanel;
import edu.liceo.ugoautomate.ui.common.HomePanel;
import edu.liceo.ugoautomate.ui.common.HomePanel.Shortcut;
import edu.liceo.ugoautomate.ui.common.ProfilePanel;

/**
 * Guest dashboard: visit registration and passes, navigator, and profile.
 */
public class GuestDashboard extends DashboardFrame {

    static final String HOME = "Home";
    static final String VISITS = "My Visits";
    static final String NAVIGATOR = "Campus Navigator";
    static final String PROFILE = "My Profile";
    static final String DATA_STRUCTURES = "Data Structures";

    public GuestDashboard(AppContext ctx) {
        super(ctx, "Guest");
        String firstName = ctx.session().currentUser().map(User::getFullName).orElse("").split(" ")[0];

        addPage(HOME, new HomePanel("Welcome to Liceo, " + firstName + "!",
                "Register your visit before entering campus, then show your visit pass at the gate.", this::showPage,
                new Shortcut(VISITS, "Register today's visit and open the visit pass QR code for the gate."),
                new Shortcut(NAVIGATOR, "Look up the office or building you are visiting and where it is."),
                new Shortcut(PROFILE, "Update your contact details and default visit information."),
                new Shortcut(DATA_STRUCTURES, "See the arrays, lists, stacks, and queues used in this app, with memory use and Big-O time complexity.")));
        addPage(VISITS, new GuestVisitsPanel(ctx));
        addPage(NAVIGATOR, new CampusNavigatorPanel(ctx));
        addPage(PROFILE, new ProfilePanel(ctx));
        addPage(DATA_STRUCTURES, new DataStructuresPanel());
        showPage(HOME);
    }
}
