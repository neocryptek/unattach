package app.unattach.model.attachmentstorage;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public interface UserStorage {
  void saveAttachment(InputStream inputStream, File targetDirectory, String targetSubPath, long targetTimestamp)
      throws IOException;
  void saveMessage(MimeMessage mimeMessage, File targetDirectory, String targetSubPath)
      throws IOException, MessagingException;
}
