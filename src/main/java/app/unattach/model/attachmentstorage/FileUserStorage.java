package app.unattach.model.attachmentstorage;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileUserStorage implements UserStorage {
  @Override
  public void saveAttachment(InputStream inputStream, File targetDirectory, String targetSubPath, long targetTimestamp)
      throws IOException {
    Path targetPath = Path.of(targetDirectory.getAbsolutePath(), targetSubPath);
    //noinspection ResultOfMethodCallIgnored
    targetPath.getParent().toFile().mkdirs();
    Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
    //noinspection ResultOfMethodCallIgnored
    targetPath.toFile().setLastModified(targetTimestamp);
  }

  @Override
  public void saveMessage(MimeMessage mimeMessage, File targetDirectory, String targetSubPath)
      throws IOException, MessagingException {
    Path targetPath = Path.of(targetDirectory.getAbsolutePath(), targetSubPath);
    //noinspection ResultOfMethodCallIgnored
    targetPath.getParent().toFile().mkdirs();
    try (OutputStream os = new FileOutputStream(targetPath.toFile())) {
      mimeMessage.writeTo(os);
    }
  }
}
