package controller;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;

import dao.ReviewDao;
import model.Post;
import model.Review;
import service.ReviewService;
import service.ReviewServiceException;
import service.ValidationException;

/**
 * Shows and manages the reviews left on whichever post is currently selected
 * on the Posts screen. PostController drives this class through setPost,
 * this class never reaches into PostController on its own.
 */
public class ReviewController {
    // No login screen exists in this stage, so new reviews are attributed to
    // this fixed seeded user rather than to whoever is "logged in."
    private static final int CURRENT_USER_ID = 1;

    @FXML private TableView<Review> reviewsTable;
    @FXML private TableColumn<Review, Integer> reviewIdColumn;
    @FXML private TableColumn<Review, Integer> reviewUserIdColumn;
    @FXML private TableColumn<Review, Integer> reviewRatingColumn;
    @FXML private TableColumn<Review, String> reviewTextColumn;
    @FXML private ComboBox<Integer> ratingComboBox;
    @FXML private TextArea reviewTextField;
    @FXML private Button addReviewButton;
    @FXML private Button updateReviewButton;
    @FXML private Button deleteReviewButton;
    @FXML private Label averageRatingLabel;
    @FXML private Label reviewStatusLabel;

    private final ReviewService reviewService = new ReviewService(new ReviewDao());
    private Post selectedPost;

    @FXML
    private void initialize() {
        reviewIdColumn.setCellValueFactory(new PropertyValueFactory<>("reviewId"));
        reviewUserIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        reviewRatingColumn.setCellValueFactory(new PropertyValueFactory<>("rating"));
        reviewTextColumn.setCellValueFactory(new PropertyValueFactory<>("reviewText"));

        ratingComboBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        ratingComboBox.getSelectionModel().selectLast();

        reviewsTable.getSelectionModel().selectedItemProperty().addListener((observable, oldSelection, newSelection) -> {
            if (newSelection != null) {
                ratingComboBox.getSelectionModel().select(Integer.valueOf(newSelection.getRating()));
                reviewTextField.setText(newSelection.getReviewText());
            }
        });

        setPost(null);
    }

    /** Called by PostController whenever the selected post changes, including to null. */
    public void setPost(Post post) {
        this.selectedPost = post;
        reviewTextField.clear();
        reviewsTable.getSelectionModel().clearSelection();
        reviewStatusLabel.setText("");

        if (post == null) {
            reviewsTable.setItems(FXCollections.observableArrayList());
            averageRatingLabel.setText("");
            setControlsDisabled(true);
            return;
        }
        setControlsDisabled(false);
        refreshReviews();
    }

    private void refreshReviews() {
        try {
            List<Review> reviews = reviewService.getReviewsForPost(selectedPost.getPostId());
            reviewsTable.setItems(FXCollections.observableArrayList(reviews));
            reviewService.getAverageRating(selectedPost.getPostId()).ifPresentOrElse(
                    average -> averageRatingLabel.setText(String.format("Average rating: %.1f / 5 (%d review%s)",
                            average, reviews.size(), reviews.size() == 1 ? "" : "s")),
                    () -> averageRatingLabel.setText("No reviews yet."));
        } catch (ReviewServiceException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onAddReview() {
        if (selectedPost == null) {
            return;
        }
        try {
            Review review = new Review(selectedPost.getPostId(), CURRENT_USER_ID,
                    ratingComboBox.getValue(), blankToNull(reviewTextField.getText()));
            reviewService.addReview(review);
            reviewTextField.clear();
            refreshReviews();
            showStatus("Review added.");
        } catch (ValidationException | ReviewServiceException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onUpdateReview() {
        Review selected = reviewsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a review in the table first.");
            return;
        }
        try {
            selected.setRating(ratingComboBox.getValue());
            selected.setReviewText(blankToNull(reviewTextField.getText()));
            reviewService.updateReview(selected);
            refreshReviews();
            showStatus("Review updated.");
        } catch (ValidationException | ReviewServiceException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onDeleteReview() {
        Review selected = reviewsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a review in the table first.");
            return;
        }
        try {
            reviewService.deleteReview(selected.getReviewId());
            reviewTextField.clear();
            refreshReviews();
            showStatus("Review deleted.");
        } catch (ValidationException | ReviewServiceException e) {
            showError(e.getMessage());
        }
    }

    private String blankToNull(String text) {
        return (text == null || text.isBlank()) ? null : text;
    }

    private void setControlsDisabled(boolean disabled) {
        ratingComboBox.setDisable(disabled);
        reviewTextField.setDisable(disabled);
        addReviewButton.setDisable(disabled);
        updateReviewButton.setDisable(disabled);
        deleteReviewButton.setDisable(disabled);
    }

    private void showStatus(String message) {
        reviewStatusLabel.setTextFill(javafx.scene.paint.Color.web("#1b5e20"));
        reviewStatusLabel.setText(message);
    }

    private void showError(String message) {
        reviewStatusLabel.setTextFill(javafx.scene.paint.Color.web("#b00020"));
        reviewStatusLabel.setText(message);
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.showAndWait();
    }
}
