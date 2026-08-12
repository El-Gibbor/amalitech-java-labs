package controller;

import java.time.LocalDateTime;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
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

    private static final String MODE_KEYWORD = "Title/Content";
    private static final String MODE_AUTHOR = "Author";
    private static final String MODE_TAG = "Tag";

    private static final String SORT_NEWEST = "Newest first";
    private static final String SORT_TITLE_ASC = "Title (A-Z)";
    private static final String SORT_TITLE_DESC = "Title (Z-A)";
    private static final String SORT_PUBLISHED_ASC = "Published (earliest)";
    private static final String SORT_PUBLISHED_DESC = "Published (latest)";

    @FXML private TableView<Post> postsTable;
    @FXML private TableColumn<Post, Integer> idColumn;
    @FXML private TableColumn<Post, String> titleColumn;
    @FXML private TableColumn<Post, Integer> userIdColumn;
    @FXML private TableColumn<Post, LocalDateTime> publishedAtColumn;

    @FXML private ComboBox<String> searchModeComboBox;
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button clearSearchButton;
    @FXML private ComboBox<String> sortComboBox;

    // Injected by FXMLLoader from fx:include's fx:id, following the
    // "<includeFxId>Controller" naming convention, no fx:id of its own needed.
    @FXML private CommentController commentsPanelController;
    @FXML private ReviewController reviewsPanelController;
    @FXML private TagController tagsPanelController;

    @FXML private TextField titleField;
    @FXML private TextArea contentField;
    @FXML private CheckBox publishedCheckBox;
    @FXML private Button previousButton;
    @FXML private Button nextButton;
    @FXML private Label pageLabel;
    @FXML private Label statusLabel;

    private final PostService postService = new PostService(new PostDao());
    private int currentPage = 1;

    // null means "no search active", browse all posts normally
    private String activeSearchMode;
    private String activeSearchQuery;

    @FXML
    private void initialize() {
        searchModeComboBox.setItems(FXCollections.observableArrayList(MODE_KEYWORD, MODE_AUTHOR, MODE_TAG));
        searchModeComboBox.getSelectionModel().selectFirst();

        sortComboBox.setItems(FXCollections.observableArrayList(
                SORT_NEWEST, SORT_TITLE_ASC, SORT_TITLE_DESC, SORT_PUBLISHED_ASC, SORT_PUBLISHED_DESC));
        sortComboBox.getSelectionModel().selectFirst();

        idColumn.setCellValueFactory(new PropertyValueFactory<>("postId"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        publishedAtColumn.setCellValueFactory(new PropertyValueFactory<>("publishedAt"));

        postsTable.getSelectionModel().selectedItemProperty().addListener((observable, oldSelection, newSelection) -> {
            if (newSelection != null) {
                titleField.setText(newSelection.getTitle());
                contentField.setText(newSelection.getContent());
                publishedCheckBox.setSelected(newSelection.getPublishedAt() != null);
            }
            commentsPanelController.setPost(newSelection);
            reviewsPanelController.setPost(newSelection);
            tagsPanelController.setPost(newSelection);
        });

        refreshTable();
    }

    @FXML
    private void onCreate() {
        try {
            LocalDateTime publishedAt = publishedCheckBox.isSelected() ? LocalDateTime.now() : null;
            Post post = new Post(CURRENT_USER_ID, titleField.getText(), contentField.getText(), publishedAt);
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
            if (publishedCheckBox.isSelected()) {
                if (selected.getPublishedAt() == null) {
                    selected.setPublishedAt(LocalDateTime.now());
                }
                // else: already published, keep its original publish time
            } else {
                selected.setPublishedAt(null);
            }
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
    private void onSearch() {
        activeSearchMode = searchModeComboBox.getValue();
        activeSearchQuery = searchField.getText();
        currentPage = 1;
        refreshTable();
    }

    @FXML
    private void onClearSearch() {
        activeSearchMode = null;
        activeSearchQuery = null;
        searchField.clear();
        currentPage = 1;
        refreshTable();
    }

    @FXML
    private void onSortChanged() {
        currentPage = 1;
        refreshTable();
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
            var posts = fetchPosts(currentPage);
            if (posts.isEmpty() && currentPage > 1) {
                // ran past the last page, step back rather than show an empty table
                currentPage--;
                posts = fetchPosts(currentPage);
            }
            postsTable.setItems(FXCollections.observableArrayList(posts));
            pageLabel.setText("Page " + currentPage
                    + (activeSearchMode == null ? "" : " (search: " + activeSearchMode + ")"));
            previousButton.setDisable(currentPage == 1);
            nextButton.setDisable(posts.size() < PAGE_SIZE);
        } catch (ValidationException | PostServiceException e) {
            showError(e.getMessage());
        }
    }

    // Dispatches to the active search mode, or plain browsing if no search is active
    private List<Post> fetchPosts(int pageNumber) {
        PostService.SortOption sortOption = selectedSortOption();
        if (activeSearchMode == null) {
            return postService.listPosts(pageNumber, PAGE_SIZE, sortOption);
        }
        switch (activeSearchMode) {
            case MODE_AUTHOR:
                return postService.searchByAuthor(activeSearchQuery, pageNumber, PAGE_SIZE, sortOption);
            case MODE_TAG:
                return postService.searchByTag(activeSearchQuery, pageNumber, PAGE_SIZE, sortOption);
            default:
                return postService.searchByKeyword(activeSearchQuery, pageNumber, PAGE_SIZE, sortOption);
        }
    }

    private PostService.SortOption selectedSortOption() {
        String label = sortComboBox.getValue();
        if (SORT_TITLE_ASC.equals(label)) {
            return PostService.SortOption.TITLE_A_TO_Z;
        }
        if (SORT_TITLE_DESC.equals(label)) {
            return PostService.SortOption.TITLE_Z_TO_A;
        }
        if (SORT_PUBLISHED_ASC.equals(label)) {
            return PostService.SortOption.PUBLISHED_EARLIEST_FIRST;
        }
        if (SORT_PUBLISHED_DESC.equals(label)) {
            return PostService.SortOption.PUBLISHED_LATEST_FIRST;
        }
        return PostService.SortOption.NEWEST_FIRST;
    }

    private void clearForm() {
        titleField.clear();
        contentField.clear();
        publishedCheckBox.setSelected(false);
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
