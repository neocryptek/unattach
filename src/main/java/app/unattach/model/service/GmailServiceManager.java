package app.unattach.model.service;

public interface GmailServiceManager {
  GmailService signIn() throws GmailServiceManagerException;
  void signOut() throws GmailServiceManagerException;
}
