package controller;

import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

import dao.TagDao;
import model.Post;
import model.Tag;
import service.TagService;
import service.TagServiceException;
import service.ValidationException;

/**
 * Shows every tag that exists, and manages which of them are assigned to
 * whichever post is currently selected on the Posts screen. PostController
 * drives this class through setPost, this class never reaches into
 * PostController on its own.
 */
public class TagController {
    @FXML private TableView<Tag> allTagsTable;
    @FXML private TableColumn<Tag, Integer> tagIdColumn;
    @FXML private TableColumn<Tag, String> tagNameColumn;
    @FXML private TextField newTagNameField;
    @FXML private Button createTagButton;
    @FXML private Button deleteTagButton;

    @FXML private TableView<Tag> postTagsTable;
    @FXML private TableColumn<Tag, String> postTagNameColumn;
    @FXML private ComboBox<Tag> availableTagsComboBox;
    @FXML private Button assignTagButton;
    @FXML private Button removeTagButton;
    @FXML private Label statusLabel;

    private final TagService tagService = new TagService(new TagDao());
    private Post selectedPost;

    @FXML
    private void initialize() {
        tagIdColumn.setCellValueFactory(new PropertyValueFactory<>("tagId"));
        tagNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        postTagNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        availableTagsComboBox.setConverter(new StringConverter<Tag>() {
            @Override
            public String toString(Tag tag) {
                return tag == null ? "" : tag.getName();
            }

            @Override
            public Tag fromString(String string) {
                return null;
            }
        });

        refreshAllTags();
        setPost(null);
    }

    /** Called by PostController whenever the selected post changes, including to null. */
    public void setPost(Post post) {
        this.selectedPost = post;
        statusLabel.setText("");

        if (post == null) {
            postTagsTable.setItems(FXCollections.observableArrayList());
            availableTagsComboBox.setItems(FXCollections.observableArrayList());
            setPostControlsDisabled(true);
            return;
        }
        setPostControlsDisabled(false);
        refreshPostTags();
    }

    @FXML
    private void onCreateTag() {
        try {
            tagService.createTag(newTagNameField.getText());
            newTagNameField.clear();
            refreshAllTags();
            if (selectedPost != null) {
                refreshPostTags();
            }
            showStatus("Tag created.");
        } catch (ValidationException | TagServiceException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onDeleteTag() {
        Tag selected = allTagsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a tag in the table first.");
            return;
        }
        try {
            tagService.deleteTag(selected.getTagId());
            refreshAllTags();
            if (selectedPost != null) {
                refreshPostTags();
            }
            showStatus("Tag deleted.");
        } catch (ValidationException | TagServiceException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onAssignTag() {
        if (selectedPost == null) {
            return;
        }
        Tag selected = availableTagsComboBox.getValue();
        if (selected == null) {
            showError("Choose a tag to assign first.");
            return;
        }
        try {
            tagService.assignTagToPost(selectedPost.getPostId(), selected.getTagId());
            refreshPostTags();
            showStatus("Tag assigned.");
        } catch (ValidationException | TagServiceException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onRemoveTag() {
        Tag selected = postTagsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a tag in the post's tag list first.");
            return;
        }
        try {
            tagService.removeTagFromPost(selectedPost.getPostId(), selected.getTagId());
            refreshPostTags();
            showStatus("Tag removed from post.");
        } catch (ValidationException | TagServiceException e) {
            showError(e.getMessage());
        }
    }

    private void refreshAllTags() {
        try {
            List<Tag> tags = tagService.listAllTags();
            allTagsTable.setItems(FXCollections.observableArrayList(tags));
        } catch (TagServiceException e) {
            showError(e.getMessage());
        }
    }

    private void refreshPostTags() {
        try {
            List<Tag> tags = tagService.getTagsForPost(selectedPost.getPostId());
            postTagsTable.setItems(FXCollections.observableArrayList(tags));
            refreshAvailableTags(tags);
        } catch (TagServiceException e) {
            showError(e.getMessage());
        }
    }

    // Only tags not already assigned to this post make sense to offer for assignment.
    private void refreshAvailableTags(List<Tag> assignedTags) {
        List<Tag> available = allTagsTable.getItems().stream()
                .filter(tag -> assignedTags.stream().noneMatch(assigned -> assigned.getTagId().equals(tag.getTagId())))
                .collect(Collectors.toList());
        availableTagsComboBox.setItems(FXCollections.observableArrayList(available));
        availableTagsComboBox.getSelectionModel().clearSelection();
    }

    private void setPostControlsDisabled(boolean disabled) {
        availableTagsComboBox.setDisable(disabled);
        assignTagButton.setDisable(disabled);
        removeTagButton.setDisable(disabled);
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
