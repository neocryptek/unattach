package app.unattach.model;

import app.unattach.controller.LongTask;
import app.unattach.utils.Logger;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.io.IOException;
import java.util.*;

public class MockModel implements Model {
  private static final Logger logger = Logger.get();

  private final Config config = new BaseConfig();
  private final Random random = new Random(1337);
  private ArrayList<Email> emails = new ArrayList<>();

  @Override
  public String createLabel(String name) {
    return "removed-label-id";
  }

  @Override
  public Config getConfig() {
    return config;
  }

  @Override
  public GetEmailMetadataTask getSearchTask(String query) {
    int minEmailSizeInMb = 1;
    List<String> emailIds = new ArrayList<>();
    emails = new ArrayList<>();
    int minEmailSizeInBytes = minEmailSizeInMb * (int) Math.pow(2, 20);
    int maxEmailId = 15;
    for (int i = minEmailSizeInBytes / 1000 / 1000; i < maxEmailId; ++i) {
      String emailId = String.valueOf(i);
      emailIds.add(emailId);
    }
    return new GetEmailMetadataTask(emailIds, (startIndexInclusive, endIndexExclusive) -> {
      for (int i = minEmailSizeInBytes / 1000 / 1000; i < maxEmailId; ++i) {
        if (startIndexInclusive <= i && i < endIndexExclusive) {
          String emailId = String.valueOf(i);
          List<GmailLabel> labels =
              Arrays.asList(new GmailLabel("INBOX", "INBOX"), new GmailLabel("IMPORTANT", "IMPORTANT"));
          emails.add(new Email(emailId, emailId, labels,
              "john.doe@example.com", "jane.doe@example.com",
              "Subject " + i, System.currentTimeMillis(),
              i * (int) Math.pow(2, 20), Collections.singletonList("data.zip")));
        }
      }
    });
  }

  @Override
  public String getEmailAddress() {
    return "user@mock.com";
  }

  @Override
  public List<Email> getSearchResults() {
    return emails;
  }

  @Override
  public SortedMap<String, String> getIdToLabel() {
    SortedMap<String, String> idToLabel = new TreeMap<>();
    for (int i = 0; i < 10; ++i) {
      idToLabel.put(String.valueOf(i), "Label Name " + i);
    }
    return idToLabel;
  }

  @Override
  public LongTask<ProcessEmailResult> getProcessTask(Email email, ProcessSettings processSettings) {
    return new ProcessEmailTask(email, e -> {
      if (random.nextBoolean()) {
        return new ProcessEmailResult("mock-new-unique-id", Collections.singleton(e.getGmailId()));
      } else {
        throw new IOException("Something went wrong.");
      }
    });
  }

  @Override
  public DefaultArtifactVersion getLatestVersion() {
    return new DefaultArtifactVersion(Constants.VERSION);
  }

  @Override
  public void signIn() {
    logger.info("signIn");
  }

  @Override
  public void signOut() {
    logger.info("signOut");
  }

  @Override
  public void sendToServer(String contentDescription, String userEmail, String stackTraceText, String userText) {
    logger.info("========== sendToServer ==========");
    logger.info(userEmail);
    logger.info(stackTraceText);
    logger.info(userText);
    logger.info("========== sendToServer ==========");
  }

  @Override
  public void subscribe(String emailAddress) {
    logger.info("subscribe: " + emailAddress);
  }
}
