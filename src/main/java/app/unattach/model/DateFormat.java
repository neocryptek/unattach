package app.unattach.model;

import java.text.SimpleDateFormat;
import java.util.Date;

public enum DateFormat {
  ISO_8601_DATE("yyyy-MM-dd"),
  ISO_8601_DATE_TIME("yyyy-MM-dd'T'HH-mm-ss"),
  ISO_8601_DATE_TIME_TZ("yyyy-MM-dd'T'HH:mm:ss.SSSZ"),
  DD_MM_YYYY("dd-MM-yyyy"),
  MM_DD_YYYY("MM-dd-yyyy"),
  DD_MMMM_YYYY("dd MMMM yyyy"),
  DATE_DEFAULT("EEE MMM dd HH:mm:ss z yyyy");

  private final String pattern;
  private final SimpleDateFormat simpleDateFormat;

  DateFormat(String pattern) {
    this.pattern = pattern;
    simpleDateFormat = new SimpleDateFormat(pattern);
  }

  public String getPattern() {
    return pattern;
  }

  public String format(Date date) {
    return simpleDateFormat.format(date);
  }

  public static DateFormat fromPattern(String pattern) {
    for (DateFormat dateFormat : values()) {
      if (dateFormat.pattern.equals(pattern)) {
        return dateFormat;
      }
    }
    throw new IllegalArgumentException("Unknown pattern: " + pattern);
  }
}
