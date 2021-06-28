package app.unattach.model;

import app.unattach.controller.LongTask;
import app.unattach.model.attachmentstorage.UserStorage;
import app.unattach.model.service.GmailService;
import app.unattach.model.service.GmailServiceException;
import app.unattach.model.service.GmailServiceManager;
import app.unattach.model.service.GmailServiceManagerException;
import app.unattach.utils.Logger;
import app.unattach.utils.MimeMessagePrettyPrinter;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.services.gmail.model.*;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static app.unattach.model.GmailLabel.NO_LABEL;
import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;

public class LiveModel implements Model {
  private static final Logger logger = Logger.get();

  private final Config config;
  private final UserStorage userStorage;
  private final GmailServiceManager gmailServiceManager;
  private GmailService service;
  private List<Email> searchResults;
  private String emailAddress;

  public LiveModel(UserStorage userStorage, GmailServiceManager gmailServiceManager) {
    this.config = new FileConfig();
    this.userStorage = userStorage;
    this.gmailServiceManager = gmailServiceManager;
    configureMimeLibrary();
    reset();
  }

  private void configureMimeLibrary() {
    // see http://docs.oracle.com/javaee/6/api/javax/mail/internet/package-summary.html
    allowEmptyPartsInEmails();
    allowNonConformingEmailHeaders();
    // see https://stackoverflow.com/a/5292975/974531
    disablePrivateFetch();
    // see https://community.oracle.com/thread/1590013?start=0&tstart=0
    enableIgnoreErrors();
  }

  private void allowEmptyPartsInEmails() {
    System.setProperty("mail.mime.multipart.allowempty", "true");
  }

  private void allowNonConformingEmailHeaders() {
    System.setProperty("mail.mime.parameters.strict", "false");
  }

  private void disablePrivateFetch() {
    System.setProperty("mail.imaps.partialfetch", "false");
  }

  private void enableIgnoreErrors() {
    System.setProperty("mail.mime.base64.ignoreerrors", "true");
  }

  private void reset() {
    service = null;
    emailAddress = null;
    clearPreviousSearchResults();
  }

  private void clearPreviousSearchResults() {
    searchResults = new ArrayList<>();
  }

  @Override
  public DefaultArtifactVersion getLatestVersion() throws IOException, InterruptedException {
    return HttpClient.getLatestVersion();
  }

  @Override
  public void signIn() throws GmailServiceManagerException {
    logger.info("Signing in...");
    configureService();
    try {
      // Test call to the service. This can fail due to token issues.
      String emailAddress = getEmailAddress();
      logger.info("Signed in as %s.", emailAddress);
    } catch (GmailServiceException e) {
      logger.warn("Initial signing in failed. Explicitly signing out and retrying...", e);
      signOut();
      configureService();
    }
  }

  private void configureService() throws GmailServiceManagerException {
    // 250 quota units / user / second
    // each set of requests should assume they start with clean quota
    service = gmailServiceManager.signIn();
  }

  @Override
  public void signOut() throws GmailServiceManagerException {
    logger.info("Signing out...");
    gmailServiceManager.signOut();
    reset();
  }

  @Override
  public void sendToServer(String contentDescription, String userEmail, String stackTraceText, String userText)
      throws IOException, InterruptedException {
    HttpClient.sendToServer(contentDescription, userEmail, stackTraceText, userText);
  }

  @Override
  public void subscribe(String emailAddress) throws IOException, InterruptedException {
    HttpClient.subscribe(emailAddress);
  }

  @Override
  public String getEmailAddress() throws GmailServiceException {
    if (emailAddress == null) {
      emailAddress = service.getEmailAddress();
    }
    return emailAddress;
  }

  @Override
  public LongTask<ProcessEmailResult> getProcessTask(Email email, ProcessSettings processSettings) {
    return new ProcessEmailTask(email, e -> processEmail(e, processSettings) /* 40 quota units */);
  }

  private ProcessEmailResult processEmail(Email email, ProcessSettings processSettings)
      throws IOException, MessagingException, GmailServiceException {
    Message message = service.getRawMessage(email.getGmailId()); // 5 quota units
    GmailService.trackInDebugMode(logger, message);
    MimeMessage mimeMessage = GmailService.getMimeMessage(message);
    logger.info("MIME structure:%n%s", MimeMessagePrettyPrinter.prettyPrint(mimeMessage));
    String newUniqueId = null;
    ProcessOption processOption = processSettings.processOption();
    if (processOption.backupEmail()) {
      backupEmail(email, processSettings, mimeMessage);
    }
    Set<String> originalAttachmentNames = new TreeSet<>();
    mimeMessage = EmailProcessor.process(userStorage, email, mimeMessage, processSettings, originalAttachmentNames);
    if (processOption.shouldDownload() && !processOption.shouldRemove() &&
        !NO_LABEL.id().equals(processOption.downloadedLabelId())) {
      service.addLabel(message.getId(), processOption.downloadedLabelId());
    }
    if (processOption.shouldRemove() && !originalAttachmentNames.isEmpty()) {
      logger.info("New MIME structure:%n%s", MimeMessagePrettyPrinter.prettyPrint(mimeMessage));
      updateRawMessage(message, mimeMessage);
      Message newMessage = service.insertMessage(message); // 25 quota units
      newMessage = service.getUniqueIdAndHeaders(newMessage.getId()); // 5 quota units
      GmailService.trackInDebugMode(logger, newMessage);
      Map<String, String> headerMap = GmailService.getHeaderMap(newMessage);
      newUniqueId = headerMap.get("message-id");
      if (processOption.shouldDownload() && !NO_LABEL.id().equals(processOption.downloadedLabelId())) {
        service.addLabel(newMessage.getId(), processOption.downloadedLabelId());
      }
      if (!NO_LABEL.id().equals(processOption.removedLabelId())) {
        service.addLabel(newMessage.getId(), processOption.removedLabelId());
      }
      // 5-10 quota units
      service.removeMessage(message.getId(), processOption.permanentlyRemoveOriginal());
    }
    return new ProcessEmailResult(newUniqueId, originalAttachmentNames);
  }

  private void backupEmail(Email email, ProcessSettings processSettings, MimeMessage mimeMessage)
          throws IOException, MessagingException {
    String filename = email.getGmailId() + ".eml";
    userStorage.saveMessage(mimeMessage, processSettings.targetDirectory(), filename);
  }

  private void updateRawMessage(Message message, MimeMessage mimeMessage) throws IOException, MessagingException {
    try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
      mimeMessage.writeTo(buffer);
      String raw = encodeBase64URLSafeString(buffer.toByteArray());
      message.setRaw(raw);
    }
  }

  @Override
  public GetEmailMetadataTask getSearchTask(String query) throws GmailServiceException {
    logger.info("Getting email labels...");
    SortedMap<String, String> idToLabel = getIdToLabel();
    logger.info("Searching with query '%s'...", query);
    clearPreviousSearchResults();
    List<Message> messages = service.search(query);
    logger.info("Found %d results.", messages.size());
    ArrayList<String> emailIdsToProcess =
        messages.stream().map(Message::getId).collect(Collectors.toCollection(ArrayList::new));

    JsonBatchCallback<Message> perEmailCallback = new JsonBatchCallback<>() {
      @Override
      public void onFailure(GoogleJsonError googleJsonError, HttpHeaders httpHeaders) throws IOException {
        throw new IOException(googleJsonError.getMessage());
      }

      @Override
      public void onSuccess(Message message, HttpHeaders httpHeaders) {
        GmailService.trackInDebugMode(logger, message);
        Map<String, String> headerMap = GmailService.getHeaderMap(message);
        String emailId = message.getId();
        String uniqueId = headerMap.get("message-id");
        List<String> labelIds = message.getLabelIds();
        List<GmailLabel> labels = getLabelsForIds(idToLabel, labelIds);
        String from = headerMap.get("from");
        String to = headerMap.get("to");
        String subject = headerMap.get("subject");
        long timestamp = message.getInternalDate();
        List<String> attachmentNames = getAttachmentNames(message.getPayload());
        Email email = new Email(emailId, uniqueId, labels, from, to, subject, timestamp, message.getSizeEstimate(),
            attachmentNames);
        searchResults.add(email);
      }
    };

    return new GetEmailMetadataTask(emailIdsToProcess, (startIndexInclusive, endIndexExclusive) -> {
        logger.info("Getting info about emails with index [%d, %d)...", startIndexInclusive, endIndexExclusive);
        List<String> emailIds = emailIdsToProcess.subList(startIndexInclusive, endIndexExclusive);
        service.batchGetMetadata(emailIds, perEmailCallback);
      }
    );
  }

  private List<GmailLabel> getLabelsForIds(SortedMap<String, String> idToLabel, List<String> labelIds) {
    if (labelIds == null) {
      return List.of();
    }
    return labelIds.stream().map(id -> new GmailLabel(id, idToLabel.getOrDefault(id, id))).collect(Collectors.toList());
  }

  private List<String> getAttachmentNames(MessagePart part) {
    Set<String> attachmentNames = new TreeSet<>();
    getAttachmentNamesRecursive(attachmentNames, part);
    return new ArrayList<>(attachmentNames);
  }

  private void getAttachmentNamesRecursive(Set<String> attachmentNames, MessagePart part) {
    String attachmentName = part.getFilename();
    if (attachmentName != null) {
      attachmentNames.add(attachmentName);
      return;
    }
    List<MessagePart> subParts = part.getParts();
    if (subParts != null) {
      for (MessagePart subPart : subParts) {
        getAttachmentNamesRecursive(attachmentNames, subPart);
      }
    }
  }

  @Override
  public List<Email> getSearchResults() {
    return searchResults;
  }

  @Override
  public SortedMap<String, String> getIdToLabel() throws GmailServiceException {
    return service.getIdToLabel();
  }

  @Override
  public String createLabel(String name) throws GmailServiceException {
    Label labelIn = new Label();
    labelIn.setName(name);
    labelIn.setLabelListVisibility("labelShow");
    labelIn.setMessageListVisibility("show");
    LabelColor labelColor = new LabelColor();
    labelColor.setBackgroundColor("#ffffff");
    labelColor.setTextColor("#fb4c2f");
    labelIn.setColor(labelColor);
    Label labelOut = service.createLabel(labelIn);
    return labelOut.getId();
  }

  @Override
  public Config getConfig() {
    return config;
  }
}
