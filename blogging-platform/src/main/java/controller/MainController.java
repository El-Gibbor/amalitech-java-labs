package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Tab;

/**
 * The application's top level window: a tab for the Posts screen (which
 * itself hosts Tags, Comments, and Reviews) and a tab for Analytics.
 */
public class MainController {
    @FXML private Tab analyticsTab;

    // Injected by FXMLLoader from fx:include's fx:id, following the
    // "<includeFxId>Controller" naming convention, no fx:id of its own needed.
    @FXML private AnalyticsController analyticsPanelController;

    @FXML
    private void initialize() {
        // Analytics holds no cache of its own, so it refreshes itself every
        // time it becomes the visible tab rather than only once at startup.
        analyticsTab.selectedProperty().addListener((observable, wasSelected, isSelected) -> {
            if (isSelected) {
                analyticsPanelController.refresh();
            }
        });
    }
}
