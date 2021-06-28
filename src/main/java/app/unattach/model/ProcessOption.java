package app.unattach.model;

import app.unattach.view.Action;

public record ProcessOption(Action action, boolean processEmbedded, boolean backupEmail,
                            boolean permanentlyRemoveOriginal, String downloadedLabelId, String removedLabelId) {
  public boolean shouldDownload() {
    return action == Action.DOWNLOAD || action == Action.DOWNLOAD_AND_REMOVE;
  }

  public boolean shouldRemove() {
    return action == Action.REMOVE || action == Action.DOWNLOAD_AND_REMOVE;
  }

  public boolean shouldProcessEmbedded() {
    return processEmbedded;
  }
}
