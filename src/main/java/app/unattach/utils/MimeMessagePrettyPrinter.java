package app.unattach.utils;

import org.apache.commons.lang3.StringUtils;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import java.io.IOException;

public class MimeMessagePrettyPrinter {
  public static String prettyPrint(Part part) throws MessagingException, IOException {
    StringBuilder sb = new StringBuilder();
    prettyPrint(sb, part, 0, true);
    return sb.toString();
  }

  private static void prettyPrint(StringBuilder sb, Part parent, int depth, boolean lastChild)
      throws MessagingException, IOException {
    if (depth > 0) {
      sb.append("\n");
    }
    sb.append(StringUtils.repeat("|   ", depth - 1));
    if (depth > 0) {
      sb.append(lastChild ? "`-- " : "|-- ");
    }
    sb.append(parent.getContentType());
    sb.append(" (disposition: ").append(parent.getDisposition()).append(")");
    Object content = parent.getContent();
    if (content instanceof Multipart multipart) {
      for (int i = 0; i < multipart.getCount(); ++i) {
        BodyPart child = multipart.getBodyPart(i);
        prettyPrint(sb, child, depth + 1, i == multipart.getCount() - 1);
      }
    }
  }
}
