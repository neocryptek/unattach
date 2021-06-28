package app.unattach.utils;

import app.unattach.model.TestStore;
import app.unattach.model.service.GmailService;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.model.Message;
import org.junit.jupiter.api.Test;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class MimeMessagePrettyPrinterTest {
  @Test
  public void test_prettyPrint_SHOULD_print_all_parts_WHEN_given_test_store_input()
      throws IOException, MessagingException {
    JsonFactory factory = JacksonFactory.getDefaultInstance();
    Message message = TestStore.loadMessage(factory, "1-simple-before");
    MimeMessage mimeMessage = GmailService.getMimeMessage(message);
    String output = MimeMessagePrettyPrinter.prettyPrint(mimeMessage);
    assertEquals("""
        multipart/mixed; boundary="000000000000e3d7ec05ba5de359" (disposition: null)
        |-- multipart/alternative; boundary="000000000000e3d7e905ba5de357" (disposition: null)
        |   |-- text/plain; charset="UTF-8" (disposition: null)
        |   `-- text/html; charset="UTF-8" (disposition: null)
        `-- image/png; name="logo-256.png" (disposition: attachment)
        """.trim(), output);
  }
}