package app.unattach.view;

import app.unattach.model.EmailStatus;
import app.unattach.model.Email;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.util.Callback;
import org.apache.commons.lang3.StringUtils;

public class SelectedCheckBoxTableCellFactory
    implements Callback<TableColumn.CellDataFeatures<Email, CheckBox>, ObservableValue<CheckBox>> {
  @Override
  public ObservableValue<CheckBox> call(TableColumn.CellDataFeatures<Email, CheckBox> cellDataFeatures) {
    Email email = cellDataFeatures.getValue();
    CheckBox checkBox = new CheckBox();
    TableView<Email> tableView = cellDataFeatures.getTableView();
    checkBox.selectedProperty().setValue(email.isSelected());
    checkBox.getStyleClass().removeAll();
    if (email.getStatus() == EmailStatus.PROCESSED) {
      checkBox.getStyleClass().add("checkbox-processed");
    } else if (email.getStatus() == EmailStatus.FAILED) {
      checkBox.getStyleClass().add("checkbox-failed");
    }
    checkBox.tooltipProperty().setValue(new Tooltip(email.getStatus().toString()));
    checkBox.selectedProperty().addListener((observableValue, oldValue, newValue) -> {
      EmailStatus targetStatus = newValue ? EmailStatus.TO_PROCESS : EmailStatus.NOT_SELECTED;
      ObservableList<Email> selectedEmails = tableView.getSelectionModel().getSelectedItems();
      if (selectedEmails.contains(email)) {
        for (Email selectedEmail : selectedEmails) {
          selectedEmail.setStatus(targetStatus);
        }
      } else {
        email.setStatus(targetStatus);
      }
      tableView.refresh();
    });
    return new SimpleObjectProperty<>(checkBox);
  }
}