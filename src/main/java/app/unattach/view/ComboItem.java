package app.unattach.view;

public record ComboItem<T>(String caption, T value) {
  @Override
  public String toString() {
    return caption;
  }
}