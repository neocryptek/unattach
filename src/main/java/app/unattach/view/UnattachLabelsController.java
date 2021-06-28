package app.unattach.view;

import app.unattach.controller.Controller;
import app.unattach.controller.ControllerFactory;
import app.unattach.model.Constants;
import app.unattach.model.GmailLabel;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static app.unattach.model.GmailLabel.NO_LABEL;

public class UnattachLabelsController {
  private Controller controller;

  @FXML
  private Label introductionLabel;
  @FXML
  private ComboBox<GmailLabel> downloadedLabelComboBox;
  @FXML
  private ComboBox<GmailLabel> removedLabelComboBox;
  @FXML
  private Label customizeLabel;
  @FXML
  private Button cancelButton;
  @FXML
  private Button okButton;

  @FXML
  public void initialize() {
    controller = ControllerFactory.getDefaultController();
    introductionLabel.setText(
        Constants.PRODUCT_NAME + " adds Gmail labels to all emails where\n" +
        "attachments have been downloaded and/or removed."
    );
    customizeLabel.setText(
        """
            You can edit the labels' settings within Gmail itself.
            You can update the label's appearance by pressing
            ⋮ next to the label's name in the list of labels."""
    );
    String downloadedLabelId = controller.getOrCreateDownloadedLabelId();
    String removedLabelId = controller.getOrCreateRemovedLabelId();
    List<GmailLabel> labels = controller.getIdToLabel().entrySet().stream()
            .map(entry -> new GmailLabel(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparing(GmailLabel::name))
            .collect(Collectors.toList());
    labels.add(0, NO_LABEL);
    downloadedLabelComboBox.getItems().setAll(labels);
    removedLabelComboBox.getItems().setAll(labels);
    selectLabel(labels, downloadedLabelId, downloadedLabelComboBox);
    selectLabel(labels, removedLabelId, removedLabelComboBox);
  }

  private void selectLabel(List<GmailLabel> labels, String labelId, ComboBox<GmailLabel> comboBox) {
    OptionalInt downloadLabelIndex = IntStream.range(0, labels.size())
        .filter(i -> labels.get(i).id().equals(labelId)).findFirst();
    if (downloadLabelIndex.isPresent()) {
      comboBox.getSelectionModel().select(downloadLabelIndex.getAsInt());
    }
  }

  @FXML
  private void onEditLabelsInGmailButtonPressed() {
    controller.openWebPage("https://mail.google.com/mail/u/0/#settings/labels");
  }

  @FXML
  private void onCancelButtonPressed() {
    cancelButton.getScene().getWindow().hide();
  }

  @FXML
  private void onOkButtonPressed() {
    GmailLabel downloadLabel = downloadedLabelComboBox.getSelectionModel().getSelectedItem();
    GmailLabel removedLabel = removedLabelComboBox.getSelectionModel().getSelectedItem();
    controller.getConfig().saveDownloadedLabelId(downloadLabel.id());
    controller.getConfig().saveRemovedLabelId(removedLabel.id());
    okButton.getScene().getWindow().hide();
  }
}
