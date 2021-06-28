package app.unattach.view;

record SchedulePeriod(String name, int seconds) {
  @Override
  public String toString() {
    return name;
  }
}
