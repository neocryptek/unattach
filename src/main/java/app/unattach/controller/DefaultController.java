package app.unattach.controller;

import app.unattach.model.*;
import app.unattach.model.service.GmailServiceException;
import app.unattach.model.service.GmailServiceManagerException;
import app.unattach.utils.Logger;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.function.Consumer;

public record DefaultController(Model model) implements Controller {
  private static final Logger logger = Logger.get();
  private static final String DEFAULT_DOWNLOADED_LABEL_NAME = "Unattach - Downloaded";
  private static final String DEFAULT_REMOVED_LABEL_NAME = "Unattach - Removed";

  @Override
  public String createLabel(String name) {
    try {
      logger.info("Creating label " + name + "...");
      String id = model.createLabel(name);
      logger.info("Creating label " + name + "... successful.");
      return id;
    } catch (Throwable t) {
      logger.error("Creating label " + name + "... failed.", t);
      return null;
    }
  }

  @Override
  public void donate(String item, int amount, String currency) {
    String uriString = Constants.DONATE_URL;
    uriString += "&coffee_type=" + item.replace(" ", "%20") + "&coffee_price=" + amount +
        "&currency=" + currency;
    openWebPage(uriString);
  }

  @Override
  public Config getConfig() {
    return model.getConfig();
  }

  @Override
  public String getOrCreateDownloadedLabelId() {
    return getOrCreateLabelId(getConfig().getDownloadedLabelId(), DEFAULT_DOWNLOADED_LABEL_NAME,
        getConfig()::saveDownloadedLabelId);
  }

  @Override
  public String getOrCreateRemovedLabelId() {
    return getOrCreateLabelId(getConfig().getRemovedLabelId(), DEFAULT_REMOVED_LABEL_NAME,
        getConfig()::saveRemovedLabelId);
  }

  private String getOrCreateLabelId(String labelId, String defaultLabelName, Consumer<String> saveLabelId) {
    // If no label has been set, don't try to match it to Gmail labels.
    if (GmailLabel.NO_LABEL.id().equals(labelId)) {
      return labelId;
    }
    // If the passed-in labelId is set and exists within Gmail labels, use it.
    SortedMap<String, String> idToLabel = getIdToLabel();
    if (labelId != null) {
      if (idToLabel.containsKey(labelId)) {
        return labelId;
      }
      logger.error("Couldn't find the label ID in the user config within Gmail label IDs: " + labelId);
    }
    // Otherwise, use an existing label with the passed-in defaultLabelName name.
    for (Map.Entry<String, String> entry : idToLabel.entrySet()) {
      String id = entry.getKey();
      String name = entry.getValue();
      if (name.equals(defaultLabelName)) {
        saveLabelId.accept(id);
        return id;
      }
    }
    // If not such label exists, create it.
    String id = createLabel(defaultLabelName);
    saveLabelId.accept(id);
    return id;
  }

  @Override
  public LongTask<ProcessEmailResult> getProcessTask(Email email, ProcessSettings processSettings) {
    return model.getProcessTask(email, processSettings);
  }

  @Override
  public List<Email> getSearchResults() {
    return model.getSearchResults();
  }

  @Override
  public GetEmailMetadataTask getSearchTask(String query) throws GmailServiceException {
    return model.getSearchTask(query);
  }

  @Override
  public void openUnattachHomepage() {
    openWebPage(Constants.HOMEPAGE);
  }

  @Override
  public void openTermsAndConditions() {
    openWebPage(Constants.TERMS_AND_CONDITIONS_URL);
  }

  @Override
  public void openQueryLanguagePage() {
    openWebPage("https://support.google.com/mail/answer/7190");
  }

  @Override
  public String signIn() throws GmailServiceManagerException, GmailServiceException {
    model.signIn();
    return model.getEmailAddress();
  }

  @Override
  public String getEmailAddress() throws GmailServiceException {
    return model.getEmailAddress();
  }

  @Override
  public SortedMap<String, String> getIdToLabel() {
    try {
      logger.info("Getting email labels...");
      SortedMap<String, String> idToLabel = model.getIdToLabel();
      logger.info("Getting email labels... successful.");
      return idToLabel;
    } catch (Throwable t) {
      logger.error("Getting email labels... failed.", t);
      return Collections.emptySortedMap();
    }
  }

  @Override
  public DefaultArtifactVersion getLatestVersion() {
    try {
      logger.info("Getting latest version...");
      DefaultArtifactVersion latestVersion = model.getLatestVersion();
      if (latestVersion == null) {
        logger.error("Getting latest version... failed.");
      } else {
        logger.info("Getting latest version... successful.");
      }
      return latestVersion;
    } catch (Throwable t) {
      logger.error("Getting latest version... failed.", t);
      return null;
    }
  }

  @Override
  public void signOut() {
    try {
      logger.info("Signing out...");
      model.signOut();
      logger.info("Signing out... successful.");
    } catch (Throwable t) {
      logger.error("Signing out... failed.", t);
    }
  }

  @Override
  public void sendToServer(String contentDescription, String stackTraceText, String userText) {
    try {
      logger.info("Sending " + contentDescription + "...");
      String userEmail = model.getEmailAddress();
      model.sendToServer(contentDescription, userEmail, stackTraceText, userText);
      logger.info("Sending " + contentDescription + "... successful. Thanks!");
    } catch (Throwable t) {
      String logMessage = "Failed to send " + contentDescription + " to the server. " +
          "Please consider sending an email to " + Constants.CONTACT_EMAIL + " instead.";
      logger.error(logMessage, t);
    }
  }

  @Override
  public void subscribe(String emailAddress) {
    try {
      logger.info("Subscribing with " + emailAddress + "...");
      model.subscribe(emailAddress);
      logger.info("Subscription successful.");
    } catch (Throwable t) {
      logger.error("Failed to subscribe.", t);
    }
  }

  @Override
  public void openFile(File file) {
    try {
      if (SystemUtils.IS_OS_LINUX) {
        // Desktop.getDesktop().browse() only works on Linux with libgnome installed.
        if (hasXdgOpen()) {
          Runtime.getRuntime().exec(new String[]{"xdg-open", file.getAbsolutePath()});
        } else {
          logger.error("Unable to open a file on this operating system.");
        }
      } else {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
          Desktop.getDesktop().open(file);
        } else {
          logger.error("Unable to open a file on this operating system.");
        }
      }
    } catch (Throwable t) {
      logger.error("Failed to open a file.", t);
    }
  }

  @Override
  public void openWebPage(String uriString) {
    String manualInstructions = "Please visit " + uriString + " manually.";
    try {
      if (SystemUtils.IS_OS_LINUX) {
        // Desktop.getDesktop().browse() only works on Linux with libgnome installed.
        if (hasXdgOpen()) {
          Runtime.getRuntime().exec(new String[]{"xdg-open", uriString});
        } else {
          logger.error("Unable to open a web page on this operating system. " + manualInstructions);
        }
      } else {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
          Desktop.getDesktop().browse(URI.create(uriString));
        } else {
          logger.error("Unable to open a web page on this operating system. " + manualInstructions);
        }
      }
    } catch (Throwable t) {
      logger.info("Unable to open a web page from within the application. " + manualInstructions);
    }
  }

  private boolean hasXdgOpen() {
    try {
      Process process = Runtime.getRuntime().exec(new String[]{"which", "xdg-open"});
      try (InputStream is = process.getInputStream()) {
        return is.read() != -1;
      }
    } catch (Throwable t) {
      return false;
    }
  }
}
