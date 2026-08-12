package controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import dao.AnalyticsDao;
import model.AuthorPostCount;
import model.PostCommentCount;
import model.PostRatingSummary;
import model.TagUsageCount;
import service.AnalyticsService;
import service.AnalyticsServiceException;

/**
 * Read only aggregate reports over posts, comments, tags, reviews, and
 * users. Unlike the Posts screen this holds no cache of its own, since a
 * report screen showing stale numbers is worse than one that costs a query
 * to refresh; MainController calls refresh() every time this tab is selected.
 */
public class AnalyticsController {
    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 50;

    @FXML private Spinner<Integer> limitSpinner;
    @FXML private Button refreshButton;

    @FXML private TableView<PostCommentCount> mostCommentedTable;
    @FXML private TableColumn<PostCommentCount, String> commentedPostTitleColumn;
    @FXML private TableColumn<PostCommentCount, Integer> commentedCountColumn;

    @FXML private TableView<PostRatingSummary> highestRatedTable;
    @FXML private TableColumn<PostRatingSummary, String> ratedPostTitleColumn;
    @FXML private TableColumn<PostRatingSummary, Double> averageRatingColumn;
    @FXML private TableColumn<PostRatingSummary, Integer> reviewCountColumn;

    @FXML private TableView<TagUsageCount> mostUsedTagsTable;
    @FXML private TableColumn<TagUsageCount, String> tagNameColumn;
    @FXML private TableColumn<TagUsageCount, Integer> tagPostCountColumn;

    @FXML private TableView<AuthorPostCount> mostActiveAuthorsTable;
    @FXML private TableColumn<AuthorPostCount, String> authorUsernameColumn;
    @FXML private TableColumn<AuthorPostCount, Integer> authorPostCountColumn;

    @FXML private Label statusLabel;

    private final AnalyticsService analyticsService = new AnalyticsService(new AnalyticsDao());

    @FXML
    private void initialize() {
        limitSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, MAX_LIMIT, DEFAULT_LIMIT));

        commentedPostTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        commentedCountColumn.setCellValueFactory(new PropertyValueFactory<>("commentCount"));

        ratedPostTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        averageRatingColumn.setCellValueFactory(new PropertyValueFactory<>("averageRating"));
        averageRatingColumn.setCellFactory(column -> new TableCell<PostRatingSummary, Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : String.format("%.1f", value));
            }
        });
        reviewCountColumn.setCellValueFactory(new PropertyValueFactory<>("reviewCount"));

        tagNameColumn.setCellValueFactory(new PropertyValueFactory<>("tagName"));
        tagPostCountColumn.setCellValueFactory(new PropertyValueFactory<>("postCount"));

        authorUsernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        authorPostCountColumn.setCellValueFactory(new PropertyValueFactory<>("postCount"));

        refresh();
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    /** Called on load and whenever MainController detects this tab becoming selected. */
    public void refresh() {
        int limit = limitSpinner.getValue();
        try {
            mostCommentedTable.setItems(FXCollections.observableArrayList(analyticsService.mostCommentedPosts(limit)));
            highestRatedTable.setItems(FXCollections.observableArrayList(analyticsService.highestRatedPosts(limit)));
            mostUsedTagsTable.setItems(FXCollections.observableArrayList(analyticsService.mostUsedTags(limit)));
            mostActiveAuthorsTable.setItems(
                    FXCollections.observableArrayList(analyticsService.mostActiveAuthors(limit)));
            statusLabel.setText("");
        } catch (AnalyticsServiceException e) {
            showError(e.getMessage());
        }
    }

    private void showError(String message) {
        statusLabel.setTextFill(javafx.scene.paint.Color.web("#b00020"));
        statusLabel.setText(message);
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.showAndWait();
    }
}
