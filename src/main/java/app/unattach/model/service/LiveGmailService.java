package app.unattach.model.service;

import app.unattach.utils.Logger;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;

import java.io.IOException;
import java.lang.Thread;
import java.util.*;

public record LiveGmailService(Gmail gmail) implements GmailService {
  private static final Logger logger = Logger.get();
  private static final String USER = "me";

  @Override
  public void addLabel(String messageIds, String labelId) throws GmailServiceException {
    if (labelId == null) {
      logger.warn("Cannot add a label, because it was not specified.");
      return;
    }
    try {
      ModifyMessageRequest modifyMessageRequest = new ModifyMessageRequest();
      modifyMessageRequest.setAddLabelIds(Collections.singletonList(labelId));
      // 1 messages.modify == 5 quota units
      gmail.users().messages().modify(USER, messageIds, modifyMessageRequest).execute();
    } catch (IOException e) {
      throw new GmailServiceException(e);
    }
  }

  @Override
  public void batchGetMetadata(List<String> messageIds, JsonBatchCallback<Message> callback)
      throws GmailServiceException {
    try {
      BatchRequest batch = gmail.batch();
      for (String emailId : messageIds) {
        // 1 messages.get == 5 quota units
        String fields = "id,labelIds,internalDate,payload/filename,payload/headers,payload/parts/filename,sizeEstimate";
        gmail.users().messages().get(USER, emailId).setFields(fields).queue(batch, callback);
      }
      batch.execute();
    } catch (IOException e) {
      throw new GmailServiceException(e);
    }
  }

  @Override
  public Label createLabel(Label labelIn) throws GmailServiceException {
    try {
      return gmail.users().labels().create(USER, labelIn).execute();
    } catch (IOException e) {
      throw new GmailServiceException(e);
    }
  }

  @Override
  public void removeMessage(String messageId, boolean permanentlyRemove) throws GmailServiceException {
    try {
      if (permanentlyRemove) {
        // 1 messages.delete == 10 quota units
        gmail.users().messages().delete(USER, messageId).execute();
      } else {
        // 1 messages.trash == 5 quota units
        gmail.users().messages().trash(USER, messageId).execute();
      }
    } catch (IOException e) {
      throw new GmailServiceException(e);
    }
  }

  @Override
  public String getEmailAddress() throws GmailServiceException {
    try {
      // unknown quota units
      Profile profile = gmail.users().getProfile(USER).setFields("emailAddress").execute();
      return profile.getEmailAddress();
    } catch (IOException e) {
      throw new GmailServiceException(e);
    }
  }

  @Override
  public SortedMap<String, String> getIdToLabel() throws GmailServiceException {
    try {
      // 1 labels.get == 1 quota unit
      ListLabelsResponse response = gmail.users().labels().list(USER).setFields("labels/id,labels/name").execute();
      GmailService.trackInDebugMode(logger, response);
      return GmailService.labelsResponseToMap(response);
    } catch (IOException e) {
      throw new GmailServiceException(e);
    }
  }

  @Override
  public Message getUniqueIdAndHeaders(String messageId) throws GmailServiceException {
    try {
      // 1 messages.get == 5 quota units
      return gmail.users().messages().get(USER, messageId).setFields("id,payload/headers").execute();
    } catch (IOException e) {
      throw new GmailServiceException(e);
    }
  }

  @Override
  public Message getRawMessage(String messageId) throws GmailServiceException {
    try {
      // 1 messages.get == 5 quota units
      // download limit = 2500 MB / day / user
      return gmail.users().messages().get(USER, messageId).setFormat("raw").execute();
    } catch (IOException e) {
      throw new GmailServiceException(e);
    }
  }

  @Override
  public Message insertMessage(Message message) throws GmailServiceException {
    try {
      // 1 messages.insert == 25 quota units
      // upload limit = 500 MB / day / user
      return gmail.users().messages().insert(USER, message).setInternalDateSource("dateHeader").execute();
    } catch (IOException e) {
      throw new GmailServiceException(e);
    }
  }

  @Override
  public List<Message> search(String query) throws GmailServiceException {
    try {
      List<Message> messages = new ArrayList<>();
      String pageToken = null;
      do {
        // 1 messages.list == 5 quota units
        Gmail.Users.Messages.List request = gmail.users().messages().list(USER).setFields("messages/id,nextPageToken")
            .setQ(query).setMaxResults(100000L).setPageToken(pageToken);
        ListMessagesResponse response = request.execute();
        if (response == null) {
          break;
        }
        List<Message> responseMessages = response.getMessages();
        if (responseMessages == null) {
          break;
        }
        messages.addAll(responseMessages);
        pageToken = response.getNextPageToken();
        Thread.sleep(25);
      } while (pageToken != null);
      return messages;
    } catch (IOException | InterruptedException e) {
      throw new GmailServiceException(e);
    }
  }
}
