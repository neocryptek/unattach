package app.unattach.model.service;

import app.unattach.model.Constants;
import app.unattach.model.TestStore;
import app.unattach.utils.Logger;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.apache.commons.codec.binary.Base64.decodeBase64;

public interface GmailService {
  void addLabel(String messageIds, String labelId) throws GmailServiceException;
  void batchGetMetadata(List<String> messageIds, JsonBatchCallback<Message> callback) throws GmailServiceException;
  Label createLabel(Label labelIn) throws GmailServiceException;
  void removeMessage(String messageId, boolean permanentlyRemove) throws GmailServiceException;
  String getEmailAddress() throws GmailServiceException;
  SortedMap<String, String> getIdToLabel() throws GmailServiceException;
  Message getUniqueIdAndHeaders(String messageId) throws GmailServiceException;
  Message getRawMessage(String messageId) throws GmailServiceException;
  Message insertMessage(Message message) throws GmailServiceException;
  List<Message> search(String query) throws GmailServiceException;

  static Map<String, String> getHeaderMap(Message message) {
    List<MessagePartHeader> headers = message.getPayload().getHeaders();
    Map<String, String> headerMap = new HashMap<>(headers.size());
    for (MessagePartHeader header : headers) {
      headerMap.put(header.getName().toLowerCase(), header.getValue());
    }
    return headerMap;
  }

  static SortedMap<String, String> labelsResponseToMap(ListLabelsResponse response) {
    SortedMap<String, String> labelToId = new TreeMap<>();
    for (Label label : response.getLabels()) {
      labelToId.put(label.getId(), label.getName());
    }
    return labelToId;
  }

  static MimeMessage getMimeMessage(Message message) throws MessagingException, IOException {
    String rawBefore = message.getRaw();
    if (rawBefore == null) {
      throw new IOException("Unable to extract the contents of the email.");
    }
    byte[] emailBytes = decodeBase64(rawBefore);
    try (InputStream is = new ByteArrayInputStream(emailBytes)) {
      Session session = Session.getInstance(new Properties());
      return new MimeMessage(session, is);
    }
  }

  static void trackInDebugMode(Logger logger, Message message) {
    if (Constants.DEBUG_MODE) {
      try {
        logger.info("===============================");
        logger.info(new ArrayList<>(message.keySet()).toString());
        logger.info(message.toPrettyString());
        TestStore.mergeMessage(message);
        logger.info("===============================");
      } catch (IOException e) {
        logger.error("Failed to track message.", e);
      }
    }
  }

  static void trackInDebugMode(Logger logger, ListLabelsResponse response) {
    if (Constants.DEBUG_MODE) {
      try {
        logger.info("===============================");
        logger.info(response.toPrettyString());
        TestStore.saveLabels(response);
        logger.info("===============================");
      } catch (IOException e) {
        logger.error("Failed to track labels.", e);
      }
    }
  }
}
