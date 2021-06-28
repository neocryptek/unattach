package app.unattach.model.service;

import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import org.json.JSONObject;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class FakeGmailService implements GmailService {
  private final String emailAddress;
  private final SortedMap<String, String> idToLabel;
  private final SortedMap<String, Message> idToMessage = new TreeMap<>();
  private final Map<String, String> beforeIdToAfterId;

  public FakeGmailService(String emailAddress, SortedMap<String, String> idToLabel, List<Message> messages,
                          Map<String, String> beforeIdToAfterId) {
    this.emailAddress = emailAddress;
    this.idToLabel = idToLabel;
    for (Message message : messages) {
      idToMessage.put(message.getId(), message);
    }
    this.beforeIdToAfterId = beforeIdToAfterId;
  }

  @Override
  public void addLabel(String messageIds, String labelId) {
    Message message = idToMessage.get(messageIds);
    List<String> labelIds = message.getLabelIds();
    if (labelIds == null) {
      labelIds = new ArrayList<>();
      message.setLabelIds(labelIds);
    }
    labelIds.add(labelId);
  }

  @Override
  public void batchGetMetadata(List<String> messageIds, JsonBatchCallback<Message> callback)
      throws GmailServiceException {
    for (String messageId : messageIds) {
      try {
        Message message = idToMessage.get(messageId);
        Message result = filterKeys(message, "id", "internalDate", "labelIds", "payload", "sizeEstimate");
        callback.onSuccess(result, null);
      } catch (IOException e) {
        throw new GmailServiceException(e);
      }
    }
  }

  @Override
  public Label createLabel(Label labelIn) {
    String id = "LABEL_" + idToLabel.size();
    Label labelOut = labelIn.clone().setId(id);
    idToLabel.put(id, labelOut.getName());
    return labelOut;
  }

  @Override
  public void removeMessage(String messageId, boolean permanentlyRemove) {
    // TODO: implement 'trash'
    idToMessage.remove(messageId);
  }

  @Override
  public String getEmailAddress() {
    return emailAddress;
  }

  @Override
  public SortedMap<String, String> getIdToLabel() {
    return new TreeMap<>(idToLabel);
  }

  @Override
  public Message getUniqueIdAndHeaders(String messageId) throws GmailServiceException {
    return filterKeys(idToMessage.get(messageId), "id", "payload");
  }

  @Override
  public Message getRawMessage(String messageId) throws GmailServiceException {
    return filterKeys(idToMessage.get(messageId),
        "historyId", "id", "internalDate", "raw", "sizeEstimate", "snippet", "threadId");
  }

  @Override
  public Message insertMessage(Message message) throws GmailServiceException {
    String afterId = beforeIdToAfterId.get(message.getId());
    Message afterMessage = message.clone().setId(afterId);
    try {
      MimeMessage mimeMessage = GmailService.getMimeMessage(afterMessage);
      List<MessagePartHeader> messagePartHeaders = Collections.list(mimeMessage.getAllHeaders()).stream()
          .map(this::headerToMessagePartHeader).collect(Collectors.toList());
      MessagePart payload = new MessagePart();
      payload.setHeaders(messagePartHeaders);
      MessagePart part = new MessagePart();
      part.setFilename("");
      payload.setParts(Collections.singletonList(part));
      afterMessage.setPayload(payload);
    } catch (MessagingException | IOException e) {
      throw new GmailServiceException(e);
    }
    idToMessage.put(afterId, afterMessage);
    return filterKeys(afterMessage, "id", "labelIds", "threadId");
  }

  private MessagePartHeader headerToMessagePartHeader(Header header) {
    MessagePartHeader messagePartHeader = new MessagePartHeader();
    messagePartHeader.setName(header.getName());
    messagePartHeader.setValue(header.getValue());
    return messagePartHeader;
  }

  @Override
  public List<Message> search(String query) throws GmailServiceException {
    List<Message> result = new ArrayList<>();
    for (Message message : idToMessage.values()) {
      Map<String, String> headerMap = GmailService.getHeaderMap(message);
      if (headerMap.get("subject").toLowerCase().contains(query.toLowerCase())) {
        result.add(filterKeys(message, "id", "internalDate", "labelIds", "payload", "sizeEstimate"));
      }
    }
    return result;
  }

  /**
   * Creates a new message with only the specified keys.
   *
   * The implementation goes via JSON, since Message doesn't support removal of valid keys.
   *
   * @param message The message to clone and filter.
   * @param keysToKeep The keys to keep in the new message.
   * @return The new message.
   * @throws GmailServiceException Thrown if print or parsing fails.
   */
  private Message filterKeys(Message message, String... keysToKeep) throws GmailServiceException {
    String jsonString = message.toString();
    JSONObject jsonObject = new JSONObject(jsonString);
    Set<String> keysToKeepSet = Set.of(keysToKeep);
    for (String key : message.keySet()) {
      if (!keysToKeepSet.contains(key)) {
        jsonObject.remove(key);
      }
    }
    try {
      return message.getFactory().fromString(jsonObject.toString(), Message.class);
    } catch (IOException e) {
      throw new GmailServiceException(e);
    }
  }
}
