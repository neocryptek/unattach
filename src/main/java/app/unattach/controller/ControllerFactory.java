package app.unattach.controller;

import app.unattach.model.attachmentstorage.UserStorage;
import app.unattach.model.attachmentstorage.FileUserStorage;
import app.unattach.model.service.GmailServiceManager;
import app.unattach.model.service.LiveGmailServiceManager;
import app.unattach.model.LiveModel;
import app.unattach.model.Model;

public class ControllerFactory {
  private static Controller defaultController;

  public static synchronized Controller getDefaultController() {
    if (defaultController == null) {
      UserStorage userStorage = new FileUserStorage();
      GmailServiceManager gmailServiceManager = new LiveGmailServiceManager();
      Model model = new LiveModel(userStorage, gmailServiceManager);
      defaultController = new DefaultController(model);
    }
    return defaultController;
  }
}
