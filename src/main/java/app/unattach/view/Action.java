package app.unattach.view;

public enum Action {
  DOWNLOAD("download"),
  REMOVE("remove"),
  DOWNLOAD_AND_REMOVE("download and remove");

  private final String name;

  Action(String name) {
    this.name = name;
  }

  public String toString() {
    return name;
  }
}
