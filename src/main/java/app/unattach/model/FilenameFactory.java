package app.unattach.model;

import javafx.fxml.FXML;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FilenameFactory {
  private static final Set<String> DEFAULT_LABELS = Set.of(
      "CATEGORY_FORUMS", "CATEGORY_PERSONAL", "CATEGORY_PROMOTIONS", "CATEGORY_SOCIAL", "CATEGORY_UPDATES",
      "CHAT", "DRAFT", "IMPORTANT", "INBOX", "SENT", "SPAM", "STARRED", "TRASH", "UNREAD"
  );
  static final String DEFAULT_SCHEMA = "${ID}-${BODY_PART_INDEX}-${ATTACHMENT_NAME}";

  private final Map<String, Pattern> keyToPattern;
  private final String schema;
  private final Set<String> unattachLabelIds;

  public FilenameFactory(String schema, Set<String> unattachLabelIds) {
    keyToPattern = new HashMap<>();
    this.schema = schema;
    this.unattachLabelIds = unattachLabelIds;
  }

  public String getFilename(Email email, int bodyPartIndex, String attachmentName) {
    String template = schema;
    String name_or_email = email.getFromName().isEmpty() ? email.getFromEmail() : email.getFromName();
    template = replaceRawAndNormalised(template, "FROM_EMAIL", email.getFromEmail(), FilenameFactory::simpleTrim);
    template = replaceRawAndNormalised(template, "FROM_NAME", email.getFromName(), FilenameFactory::simpleTrim);
    template = replaceRawAndNormalised(template, "FROM_NAME_OR_EMAIL", name_or_email, FilenameFactory::simpleTrim);
    template = replaceRawAndNormalised(template, "SUBJECT", email.getSubject(), FilenameFactory::simpleTrim);
    template = replaceRawAndNormalised(template, "TIMESTAMP", String.valueOf(email.getTimestamp()), FilenameFactory::simpleTrim);
    template = replaceRawAndNormalised(template, "DATE", email.getDateIso8601(), FilenameFactory::simpleTrim);
    template = replaceRawAndNormalised(template, "TIME", email.getTimeString(), FilenameFactory::simpleTrim);
    template = replaceRawAndNormalised(template, "ID", email.getGmailId(), FilenameFactory::simpleTrim);
    template = replaceRawAndNormalised(template, "BODY_PART_INDEX", String.valueOf(bodyPartIndex), FilenameFactory::simpleTrim);
    template = replaceRawAndNormalised(template, "LABELS", getLabelIdsString(email), FilenameFactory::simpleTrim);
    template = replaceRawAndNormalised(template, "LABEL_NAMES", getLabelNamesForFilenames(email), FilenameFactory::simpleTrim);
    template = replaceRawAndNormalised(template, "CUSTOM_LABEL_NAMES", getCustomLabelNamesForFilenames(email, unattachLabelIds), FilenameFactory::simpleTrim);
    template = replaceRawAndNormalised(template, "ATTACHMENT_NAME", attachmentName, FilenameFactory::basenameTrim);
    if (template.contains("${")) {
      int start = template.indexOf("${");
      int end = template.indexOf("}", start);
      String unknownPattern = template.substring(start, end == -1 ? template.length() : end + 1);
      throw new InvalidParameterException("The schema contains an unknown pattern: " + unknownPattern);
    }
    return template;
  }

  private Pattern getPattern(String key) {
    Pattern pattern;
    if (keyToPattern.containsKey(key)) {
      pattern = keyToPattern.get(key);
    } else {
      keyToPattern.put(key, pattern = Pattern.compile("\\$\\{" + key + "(:([0-9]+))?}"));
    }
    return pattern;
  }

  public String getLabelIdsString(Email email) {
    return email.getLabels().stream().map(GmailLabel::id).collect(Collectors.joining("_"));
  }

  public String getLabelNamesForFilenames(Email email) {
    return email.getLabels().stream().map(GmailLabel::name).collect(Collectors.joining(", "));
  }

  private String getCustomLabelNamesForFilenames(Email email, Set<String> unattachLabelIds) {
    return email.getLabels().stream()
        .filter(label -> !unattachLabelIds.contains(label.id())).map(GmailLabel::name)
        .filter(label -> !DEFAULT_LABELS.contains(label))
        .collect(Collectors.joining(", "));
  }

  private interface Trimmer {
    String trim(String replacement, int maxLength, int newLength);
  }

  private String replaceRawAndNormalised(String template, String patternString, String replacement, Trimmer trimmer) {
    template = replace(template, "RAW_" + patternString, replacement, trimmer);
    template = replace(template, patternString, replacement, trimmer);
    return template;
  }

  private String replace(String template, String patternString, String replacement, Trimmer trimmer) {
    Pattern pattern = getPattern(patternString);
    Matcher matcher = pattern.matcher(template);
    if (replacement == null) {
      replacement = "";
    }
    if (!patternString.startsWith("RAW_")) {
      replacement = replacement.replaceAll("[^a-zA-Z0-9-_.@]", "_");
    }
    if (matcher.find()) {
      String maxLengthGroup = matcher.group(2);
      int maxLength = maxLengthGroup == null ? Integer.MAX_VALUE : Integer.parseInt(maxLengthGroup);
      int newLength = Math.min(maxLength, replacement.length());
      String replacementTrimmed = trimmer.trim(replacement, maxLength, newLength);
      template = matcher.replaceFirst(replacementTrimmed);
    }
    return template;
  }

  private static String simpleTrim(String replacement, @SuppressWarnings("unused") int maxLength, int newLength) {
    return replacement.substring(0, newLength);
  }

  private static String basenameTrim(String replacement, int maxLength, int newLength) {
    int lastDotIndex = replacement.lastIndexOf('.');
    if (lastDotIndex == -1) {
      return simpleTrim(replacement, maxLength, newLength);
    } else {
      String basename = replacement.substring(0, lastDotIndex);
      String extension = replacement.substring(lastDotIndex + 1);
      int maxBasenameLength = Math.max(0, maxLength - extension.length() - 1);
      int newBasenameLength = Math.min(maxBasenameLength, basename.length());
      return basename.substring(0, newBasenameLength) + '.' + extension;
    }
  }
}
