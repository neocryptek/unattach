package app.unattach.model;

import app.unattach.controller.LongTask;
import app.unattach.model.service.GmailServiceException;
import app.unattach.model.service.GmailServiceManagerException;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.io.IOException;
import java.util.List;
import java.util.SortedMap;

public interface Model {
  String createLabel(String name) throws GmailServiceException;
  Config getConfig();
  String getEmailAddress() throws GmailServiceException;
  SortedMap<String, String> getIdToLabel() throws GmailServiceException;
  LongTask<ProcessEmailResult> getProcessTask(Email email, ProcessSettings processSettings);
  DefaultArtifactVersion getLatestVersion() throws IOException, InterruptedException;
  List<Email> getSearchResults();
  GetEmailMetadataTask getSearchTask(String query) throws GmailServiceException;
  void signIn() throws GmailServiceManagerException;
  void signOut() throws GmailServiceManagerException;
  void sendToServer(String contentDescription, String userEmail, String stackTraceText, String userText)
      throws IOException, InterruptedException;
  void subscribe(String emailAddress) throws IOException, InterruptedException;
}
