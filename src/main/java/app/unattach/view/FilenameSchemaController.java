package app.unattach.view;

import app.unattach.controller.Controller;
import app.unattach.controller.ControllerFactory;
import app.unattach.model.Email;
import app.unattach.model.FilenameFactory;
import app.unattach.model.GmailLabel;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class FilenameSchemaController {
  private static final List<GmailLabel> labels = Arrays.asList(
      new GmailLabel("IMPORTANT", "IMPORTANT"), new GmailLabel("LABEL_42", "Custom Label"));
  private static final Email email = new Email("17b4aed892cc3f0b", "17b4aed892cc3f0b",
          labels, "\"John Doe\" <john.doe@example.com>",
          "\"Jane Doe\" <jane.doe@example.com>", "Re: Holiday plans",
          1501597962321L, 1234567, Collections.singletonList("data.zip"));
  private Controller controller;

  @FXML
  private Text introductionText;
  @FXML
  private TextField filenameSchemaTextField;
  @FXML
  private Label filenameExampleLabel;
  @FXML
  private Label errorLabel;
  @FXML
  private Button cancelButton;
  @FXML
  private Button okButton;

  @FXML
  public void initialize() {
    controller = ControllerFactory.getDefaultController();
    introductionText.setText(
        "Here, you can configure the file name schema for the downloaded attachments.\n\n" +
            "For example, to configure that every downloaded attachment should first contain Google's email\n" +
            "ID, followed by the index of the attachment within the email, followed by a normalized name of\n" +
            "the attachment file itself, with dashes as separators, you would write the following schema below:\n\n" +
            "    ${ID}-${BODY_PART_INDEX}-${ATTACHMENT_NAME}\n\n" +
            "The available schema variables are:\n" +
            "- FROM_EMAIL, e.g. " + getSchemaVariableExample("FROM_EMAIL") + "\n" +
            "- FROM_NAME, e.g. " + getSchemaVariableExample("FROM_NAME") + "\n" +
            "- FROM_NAME_OR_EMAIL, i.e. FROM_NAME if non-empty, otherwise FROM_EMAIL\n" +
            "- SUBJECT, e.g. " + getSchemaVariableExample("SUBJECT") + "\n" +
            "- TIMESTAMP, e.g. " + getSchemaVariableExample("TIMESTAMP") + "\n" +
            "- DATE, e.g. " + getSchemaVariableExample("DATE") + "\n" +
            "- TIME, e.g. " + getSchemaVariableExample("TIME") + "\n" +
            "- ID, e.g. " + getSchemaVariableExample("ID") + "\n" +
            "- BODY_PART_INDEX, e.g. " + getSchemaVariableExample("BODY_PART_INDEX") + "\n" +
            "- ATTACHMENT_NAME, e.g. " + getSchemaVariableExample("ATTACHMENT_NAME") + "\n" +
            "- LABELS, e.g. " + getSchemaVariableExample("LABELS") + "\n" +
            "- LABEL_NAMES, e.g. " + getSchemaVariableExample("LABEL_NAMES") + "\n" +
            "- CUSTOM_LABEL_NAMES, e.g. " + getSchemaVariableExample("CUSTOM_LABEL_NAMES") + "\n\n" +
            "There are also RAW_ (e.g. RAW_SUBJECT) variants of the above variables, but they are not recommended,\n" +
            "since they may contain symbols not suitable for file names.\n\n" +
            "We highly recommend ending your schema with ${ATTACHMENT_NAME}, so that the operating system correctly\n" +
            "recognises the attachment type.\n\n" +
            "For each occurrence of '/' in the schema, the app will create a sub-directory.\n\n" +
            "You can set the maximum length for each variable's expansion. For example, to use up to 5 characters\n" +
            "of the SUBJECT, use ${SUBJECT:5}. For attachment name variables, the extension of the file is preserved."
    );
    filenameSchemaTextField.textProperty().addListener(observable -> {
      String schema = filenameSchemaTextField.getText();
      FilenameFactory filenameFactory = new FilenameFactory(schema, Set.of());
      try {
        String filename = filenameFactory.getFilename(email, 3, "the beach.jpg");
        filenameExampleLabel.setText(filename);
        if (schema.endsWith("${ATTACHMENT_NAME}")) {
          errorLabel.setText("");
        } else {
          errorLabel.setText("The schema doesn't end with ${ATTACHMENT_NAME}.");
        }
        okButton.setDisable(false);
      } catch (Throwable t) {
        filenameExampleLabel.setText("");
        errorLabel.setText(t.getMessage());
        okButton.setDisable(true);
      }
    });
  }

  void setSchema(String schema) {
    filenameSchemaTextField.setText(schema);
  }

  @FXML
  private void onCancelButtonPressed() {
    cancelButton.getScene().getWindow().hide();
  }

  @FXML
  private void onOkButtonPressed() {
    controller.getConfig().saveFilenameSchema(filenameSchemaTextField.getText());
    okButton.getScene().getWindow().hide();
  }

  private String getSchemaVariableExample(String variable) {
    FilenameFactory filenameFactory = new FilenameFactory("${" + variable + "}", Set.of());
    return filenameFactory.getFilename(email, 3, "the beach.jpg");
  }
}
