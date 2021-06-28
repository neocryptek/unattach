package app.unattach.controller;

import app.unattach.model.*;
import app.unattach.model.service.GmailServiceException;
import app.unattach.model.service.GmailServiceManagerException;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.io.File;
import java.util.List;
import java.util.SortedMap;

public interface Controller {
  String createLabel(String name);
  void donate(String item, int amount, String currency);
  Config getConfig();
  String getEmailAddress() throws GmailServiceException;
  SortedMap<String, String> getIdToLabel();
  DefaultArtifactVersion getLatestVersion();
  String getOrCreateDownloadedLabelId();
  String getOrCreateRemovedLabelId();
  LongTask<ProcessEmailResult> getProcessTask(Email email, ProcessSettings processSettings);
  List<Email> getSearchResults();
  GetEmailMetadataTask getSearchTask(String query) throws GmailServiceException;
  void openFile(File file);
  void openQueryLanguagePage();
  void openUnattachHomepage();
  void openTermsAndConditions();
  void openWebPage(String uriString);
  String signIn() throws GmailServiceManagerException, GmailServiceException;
  void signOut();
  void sendToServer(String contentDescription, String exceptionText, String userText);
  void subscribe(String emailAddress);
}
