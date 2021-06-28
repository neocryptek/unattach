package app.unattach.model;

import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.Message;

import java.io.*;
import java.util.Map;

public class TestStore {
  private static final String DIRECTORY = "test-store";
  private static final File LABELS_FILE = new File(DIRECTORY, "labels.json");

  public static ListLabelsResponse loadLabels(JsonFactory factory) throws IOException {
    return load(factory, LABELS_FILE, ListLabelsResponse.class);
  }

  public static Message loadMessage(JsonFactory factory, String messageId) throws IOException {
    File file = new File(DIRECTORY, messageId + ".json");
    return load(factory, file, Message.class);
  }

  public synchronized static void saveLabels(ListLabelsResponse listLabelsResponse) throws IOException {
    save(LABELS_FILE, listLabelsResponse);
  }

  public synchronized static void mergeMessage(Message message) throws IOException {
    File file = new File(DIRECTORY, message.getId() + ".json");
    if (file.exists()) {
      Message previousMessage = load(message.getFactory(), file, Message.class);
      previousMessage.putAll(message);
      message = previousMessage;
    }
    save(file, message);
  }

  private static <T> T load(JsonFactory factory, File file, Class<T> destinationClass) throws IOException {
    try (InputStream inputStream = new FileInputStream(file)) {
      return factory.fromInputStream(inputStream, destinationClass);
    }
  }

  private static <T extends GenericJson> void save(File file, T t) throws IOException {
    //noinspection ResultOfMethodCallIgnored
    file.getParentFile().mkdirs();
    try (Writer writer = new BufferedWriter(new FileWriter(file))) {
      writer.write(t.toPrettyString());
    }
  }

  public static void main(String[] args) throws IOException {
    JsonFactory factory = JacksonFactory.getDefaultInstance();
    Message message = loadMessage(factory, "1774618de184163d");
    for (Map.Entry<String, Object> entry : message.entrySet()) {
      System.out.println(entry.getKey());
      System.out.println(entry.getValue());
    }
  }
}
