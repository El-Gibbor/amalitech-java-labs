package controller;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;

import dao.CommentDao;
import model.Comment;
import model.Post;
import service.CommentService;
import service.CommentServiceException;
import service.ValidationException;

/**
 * Shows and manages the comments belonging to whichever post is currently
 * selected on the Posts screen. PostController drives this class through
 * setPost, this class never reaches into PostController on its own.
 */
public class CommentController {
    // No login screen exists in this stage, so new comments are attributed to
    // this fixed seeded user rather than to whoever is "logged in."
    private static final int CURRENT_USER_ID = 1;

    @FXML private TableView<Comment> commentsTable;
    @FXML private TableColumn<Comment, Integer> commentIdColumn;
    @FXML private TableColumn<Comment, Integer> commentUserIdColumn;
    @FXML private TableColumn<Comment, String> commentContentColumn;
    @FXML private TextArea commentContentField;
    @FXML private Button addCommentButton;
    @FXML private Button updateCommentButton;
    @FXML private Button deleteCommentButton;
    @FXML private Label commentStatusLabel;

    private final CommentService commentService = new CommentService(new CommentDao());
    private Post selectedPost;

    @FXML
    private void initialize() {
        commentIdColumn.setCellValueFactory(new PropertyValueFactory<>("commentId"));
        commentUserIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        commentContentColumn.setCellValueFactory(new PropertyValueFactory<>("content"));

        commentsTable.getSelectionModel().selectedItemProperty().addListener((observable, oldSelection, newSelection) -> {
            if (newSelection != null) {
                commentContentField.setText(newSelection.getContent());
            }
        });

        setPost(null);
    }

    /** Called by PostController whenever the selected post changes, including to null. */
    public void setPost(Post post) {
        this.selectedPost = post;
        commentContentField.clear();
        commentsTable.getSelectionModel().clearSelection();
        commentStatusLabel.setText("");

        if (post == null) {
            commentsTable.setItems(FXCollections.observableArrayList());
            setControlsDisabled(true);
            return;
        }
        setControlsDisabled(false);
        refreshComments();
    }

    private void refreshComments() {
        try {
            List<Comment> comments = commentService.getCommentsForPost(selectedPost.getPostId());
            commentsTable.setItems(FXCollections.observableArrayList(comments));
        } catch (CommentServiceException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onAddComment() {
        if (selectedPost == null) {
            return;
        }
        try {
            Comment comment = new Comment(selectedPost.getPostId(), CURRENT_USER_ID, commentContentField.getText());
            commentService.addComment(comment);
            commentContentField.clear();
            refreshComments();
            showStatus("Comment added.");
        } catch (ValidationException | CommentServiceException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onUpdateComment() {
        Comment selected = commentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a comment in the table first.");
            return;
        }
        try {
            selected.setContent(commentContentField.getText());
            commentService.updateComment(selected);
            refreshComments();
            showStatus("Comment updated.");
        } catch (ValidationException | CommentServiceException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onDeleteComment() {
        Comment selected = commentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a comment in the table first.");
            return;
        }
        try {
            commentService.deleteComment(selected.getCommentId());
            commentContentField.clear();
            refreshComments();
            showStatus("Comment deleted.");
        } catch (ValidationException | CommentServiceException e) {
            showError(e.getMessage());
        }
    }

    private void setControlsDisabled(boolean disabled) {
        commentContentField.setDisable(disabled);
        addCommentButton.setDisable(disabled);
        updateCommentButton.setDisable(disabled);
        deleteCommentButton.setDisable(disabled);
    }

    private void showStatus(String message) {
        commentStatusLabel.setTextFill(javafx.scene.paint.Color.web("#1b5e20"));
        commentStatusLabel.setText(message);
    }

    private void showError(String message) {
        commentStatusLabel.setTextFill(javafx.scene.paint.Color.web("#b00020"));
        commentStatusLabel.setText(message);
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.showAndWait();
    }
}
