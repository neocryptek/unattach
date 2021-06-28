package app.unattach.model;

import org.apache.commons.lang3.StringUtils;

public enum EmailStatus {
  FAILED,
  NOT_SELECTED,
  PROCESSED,
  TO_PROCESS;

  @Override
  public String toString() {
    return StringUtils.capitalize(name().toLowerCase().replace('_', ' '));
  }
}
