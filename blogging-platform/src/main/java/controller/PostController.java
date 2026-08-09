package controller;

import java.time.LocalDateTime;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import dao.PostDao;
import model.Post;
import service.PostService;
import service.PostServiceException;
import service.ValidationException;

/**
 * Wires the posts screen to PostService. Contains no SQL, PostService is the
 * only thing this class talks to.
 */
public class PostController {
    private static final int PAGE_SIZE = 5;

    // No login screen exists in this stage, so new posts are attributed to
    // this fixed seeded user rather than to whoever is "logged in."
    private static final int CURRENT_USER_ID = 1;

    @FXML private TableView<Post> postsTable;
    @FXML private TableColumn<Post, Integer> idColumn;
    @FXML private TableColumn<Post, String> titleColumn;
    @FXML private TableColumn<Post, Integer> userIdColumn;
    @FXML private TableColumn<Post, LocalDateTime> publishedAtColumn;

    @FXML private TextField titleField;
    @FXML private TextArea contentField;
    @FXML private Button previousButton;
    @FXML private Button nextButton;
    @FXML private Label pageLabel;
    @FXML private Label statusLabel;

    private final PostService postService = new PostService(new PostDao());
    private int currentPage = 1;

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("postId"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        publishedAtColumn.setCellValueFactory(new PropertyValueFactory<>("publishedAt"));

        postsTable.getSelectionModel().selectedItemProperty().addListener((observable, oldSelection, newSelection) -> {
            if (newSelection != null) {
                titleField.setText(newSelection.getTitle());
                contentField.setText(newSelection.getContent());
            }
        });

        refreshTable();
    }

    @FXML
    private void onCreate() {
        try {
            Post post = new Post(CURRENT_USER_ID, titleField.getText(), contentField.getText(), null);
            postService.createPost(post);
            clearForm();
            refreshTable();
            showStatus("Post created.");
        } catch (ValidationException | PostServiceException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onUpdate() {
        Post selected = postsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a post in the table first.");
            return;
        }
        try {
            selected.setTitle(titleField.getText());
            selected.setContent(contentField.getText());
            postService.updatePost(selected);
            refreshTable();
            showStatus("Post updated.");
        } catch (ValidationException | PostServiceException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        Post selected = postsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a post in the table first.");
            return;
        }
        try {
            postService.deletePost(selected.getPostId());
            clearForm();
            refreshTable();
            showStatus("Post deleted.");
        } catch (ValidationException | PostServiceException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onPreviousPage() {
        if (currentPage > 1) {
            currentPage--;
            refreshTable();
        }
    }

    @FXML
    private void onNextPage() {
        currentPage++;
        refreshTable();
    }

    private void refreshTable() {
        try {
            var posts = postService.listPosts(currentPage, PAGE_SIZE);
            if (posts.isEmpty() && currentPage > 1) {
                // ran past the last page, step back rather than show an empty table
                currentPage--;
                posts = postService.listPosts(currentPage, PAGE_SIZE);
            }
            postsTable.setItems(FXCollections.observableArrayList(posts));
            pageLabel.setText("Page " + currentPage);
            previousButton.setDisable(currentPage == 1);
            nextButton.setDisable(posts.size() < PAGE_SIZE);
        } catch (PostServiceException e) {
            showError(e.getMessage());
        }
    }

    private void clearForm() {
        titleField.clear();
        contentField.clear();
        postsTable.getSelectionModel().clearSelection();
    }

    private void showStatus(String message) {
        statusLabel.setTextFill(javafx.scene.paint.Color.web("#1b5e20"));
        statusLabel.setText(message);
    }

    private void showError(String message) {
        statusLabel.setTextFill(javafx.scene.paint.Color.web("#b00020"));
        statusLabel.setText(message);
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.showAndWait();
    }
}
