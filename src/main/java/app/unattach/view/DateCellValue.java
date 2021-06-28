package app.unattach.view;

import app.unattach.model.DateFormat;

import java.util.Date;

public record DateCellValue(Date date,
                            DateFormat dateFormat) implements Comparable<DateCellValue> {
  @Override
  public int compareTo(DateCellValue other) {
    return date.compareTo(other.date);
  }

  @Override
  public String toString() {
    return dateFormat().format(date);
  }
}
