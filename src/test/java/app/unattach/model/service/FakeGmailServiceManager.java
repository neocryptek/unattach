package app.unattach.model.service;

import com.google.api.services.gmail.model.Message;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public record FakeGmailServiceManager(String emailAddress,
                                      SortedMap<String, String> idToLabel,
                                      List<Message> messages,
                                      Map<String, String> beforeIdToAfterId) implements GmailServiceManager {
  @Override
  public GmailService signIn() {
    return new FakeGmailService(emailAddress, idToLabel, messages, beforeIdToAfterId);
  }

  @Override
  public void signOut() {}
}
