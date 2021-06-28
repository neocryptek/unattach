package app.unattach.model;

public record GmailLabel(String id, String name) implements Comparable<GmailLabel> {
  public static GmailLabel NO_LABEL = new GmailLabel("UNATTACH_NO_LABEL", "(no label)");

  @Override
  public String toString() {
    return name;
  }

  @Override
  public int compareTo(GmailLabel o) {
    return name.compareTo(o.name);
  }
}
