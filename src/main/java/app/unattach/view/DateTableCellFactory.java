package app.unattach.view;

import app.unattach.controller.Controller;
import app.unattach.controller.ControllerFactory;
import app.unattach.model.DateFormat;
import app.unattach.model.Email;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

import java.util.Date;

public class DateTableCellFactory
    implements Callback<TableColumn.CellDataFeatures<Email, DateCellValue>, ObservableValue<DateCellValue>> {
  private final Controller controller;

  public DateTableCellFactory() {
    controller = ControllerFactory.getDefaultController();
  }

  @Override
  public ObservableValue<DateCellValue> call(TableColumn.CellDataFeatures<Email, DateCellValue> cellDataFeatures) {
    Email email = cellDataFeatures.getValue();
    Date date = email.getDate();
    String pattern = controller.getConfig().getDateFormat();
    DateFormat dateFormat = DateFormat.fromPattern(pattern);
    DateCellValue dateCellValue = new DateCellValue(date, dateFormat);
    return new SimpleObjectProperty<>(dateCellValue);
  }
}