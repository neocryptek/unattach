package app.unattach.model.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public class LiveGmailServiceManager implements GmailServiceManager {
  private static final String GOOGLE_APPLICATION_NAME = "Unattach";
  private static final File DATA_STORE_DIR = new File(System.getProperty("user.home"), ".credentials/unattach");
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  private static final List<String> SCOPES = Collections.singletonList(GmailScopes.MAIL_GOOGLE_COM);

  private FileDataStoreFactory dataStoreFactory;
  private HttpTransport httpTransport;

  @Override
  public GmailService signIn() throws GmailServiceManagerException {
    try {
      httpTransport = GoogleNetHttpTransport.newTrustedTransport();
      dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
      Credential credential = authorize();
      Gmail gmail = new Gmail.Builder(httpTransport, JSON_FACTORY, setHttpTimeout(credential))
          .setApplicationName(GOOGLE_APPLICATION_NAME)
          .build();
      return new LiveGmailService(gmail);
    } catch (GeneralSecurityException | IOException e) {
      throw new GmailServiceManagerException(e);
    }
  }

  @Override
  public void signOut() throws GmailServiceManagerException {
    try {
      FileUtils.deleteDirectory(DATA_STORE_DIR);
    } catch (IOException e) {
      throw new GmailServiceManagerException(e);
    }
  }

  private HttpRequestInitializer setHttpTimeout(final HttpRequestInitializer requestInitializer) {
    return httpRequest -> {
      requestInitializer.initialize(httpRequest);
      httpRequest.setConnectTimeout(3 * 60000);
      httpRequest.setReadTimeout(3 * 60000);
    };
  }

  private Credential authorize() throws IOException {
    try (InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("/credentials.json"))) {
      GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, reader);
      GoogleAuthorizationCodeFlow flow =
          new GoogleAuthorizationCodeFlow.Builder(
              httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
              .setDataStoreFactory(dataStoreFactory)
              .setAccessType("offline")
              .build();
      return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }
  }
}
