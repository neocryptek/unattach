package app.unattach.view;

import app.unattach.controller.Controller;
import app.unattach.controller.ControllerFactory;
import app.unattach.controller.LongTask;
import app.unattach.model.*;
import app.unattach.model.service.GmailServiceException;
import app.unattach.utils.Logger;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static app.unattach.view.Action.*;

public class MainViewController {
  private static final Logger logger = Logger.get();

  private Controller controller;
  @FXML
  private VBox root;

  // Menu
  @FXML
  private MenuItem emailMenuItem;
  @FXML
  private CheckMenuItem signInAutomaticallyCheckMenuItem;
  @FXML
  private MenuItem signOutMenuItem;
  @FXML
  private CheckMenuItem addMetadataCheckMenuItem;
  @FXML
  private Menu viewColumnMenu;
  @FXML
  private Menu dateFormatMenu;
  @FXML
  private CheckMenuItem processEmbeddedCheckMenuItem;
  @FXML
  private CheckMenuItem permanentlyRemoveOriginalMenuItem;
  @FXML
  private CheckMenuItem trashOriginalMenuItem;
  @FXML
  private Menu donationCurrencyMenu;
  @FXML
  private Menu donateMenu;
  @FXML
  private MenuItem donateTwo;
  @FXML
  private MenuItem donateFive;
  @FXML
  private MenuItem donateTen;
  @FXML
  private MenuItem donateTwentyFive;
  @FXML
  private MenuItem donateFifty;
  @FXML
  private MenuItem donateCustom;

  // Search view
  @FXML
  private Tab basicSearchTab;
  @FXML
  private ComboBox<ComboItem<Integer>> emailSizeComboBox;
  @FXML
  private Label labelsListViewLabel;
  @FXML
  private ListView<GmailLabel> labelsListView;
  @FXML
  private TextField searchQueryTextField;
  @FXML
  private Button searchButton;
  @FXML
  private ProgressBarWithText searchProgressBarWithText;
  @FXML
  private CheckBox backupCheckBox;
  @FXML
  private Button stopSearchButton;
  private boolean stopSearchButtonPressed;

  // Results view
  private static final String DESELECT_ALL_CAPTION = "Deselect all";
  private static final String SELECT_ALL_CAPTION = "Select all";
  @FXML
  private SubView resultsSubView;
  @FXML
  private TableView<Email> resultsTable;
  @FXML
  private TableColumn<Email, CheckBox> selectedTableColumn;
  @FXML
  private CheckBox toggleAllEmailsCheckBox;

  // Download view
  @FXML
  private TextField targetDirectoryTextField;
  @FXML
  private Button browseButton;
  @FXML
  private Button downloadButton;
  @FXML
  private Button downloadAndRemoveButton;
  @FXML
  private Button removeButton;
  @FXML
  private Button stopProcessingButton;
  @FXML
  private ProgressBarWithText processingProgressBarWithText;

  // Schedule view
  @FXML
  private CheckBox enableScheduleCheckBox;
  @FXML
  private Label schedulePeriodPrefixLabel;
  @FXML
  private ComboBox<SchedulePeriod> schedulePeriodComboBox;
  @FXML
  private Label scheduleTimeLabel;
  @FXML
  private Button stopScheduleButton;

  private long bytesProcessed = 0;
  private long allBytesToProcess = 0;
  private boolean stopProcessingButtonPressed = false;
  private Timeline timeline;

  @FXML
  private void initialize() throws GmailServiceException {
    controller = ControllerFactory.getDefaultController();
    emailMenuItem.setText("Signed in as " + controller.getEmailAddress() + ".");
    signInAutomaticallyCheckMenuItem.setSelected(controller.getConfig().getSignInAutomatically());
    addMenuForHidingColumns();
    addMenuForDateFormats();
    processEmbeddedCheckMenuItem.setSelected(controller.getConfig().getProcessEmbedded());
    if (!controller.getConfig().getRemoveOriginal()) {
      onTrashOriginalMenuItemPressed();
    }
    List<CheckMenuItem> currencyMenuItems =
            Arrays.stream(Constants.CURRENCIES).map(CheckMenuItem::new).collect(Collectors.toList());
    currencyMenuItems.forEach(menuItem -> menuItem.setOnAction(this::onDonationCurrencySelected));
    donationCurrencyMenu.getItems().addAll(currencyMenuItems);
    //noinspection CodeBlock2Expr
    Platform.runLater(() -> {
      currencyMenuItems.stream().filter(menuItem -> menuItem.getText().equals(Constants.DEFAULT_CURRENCY))
              .forEach(menuItem -> {menuItem.setSelected(true); menuItem.fire();});
    });
    donateMenu.setGraphic(new Label()); // This enables the CSS style for the menu.
    emailSizeComboBox.setItems(FXCollections.observableList(getEmailSizeOptions()));
    int emailSize = controller.getConfig().getEmailSize();
    int emailSizeIndex = IntStream.range(0, emailSizeComboBox.getItems().size())
        .filter(i -> emailSizeComboBox.getItems().get(i).value().equals(emailSize))
        .findFirst().orElse(1);
    emailSizeComboBox.getSelectionModel().select(emailSizeIndex);
    searchQueryTextField.setText(controller.getConfig().getSearchQuery());
    searchProgressBarWithText.progressProperty().setValue(0);
    searchProgressBarWithText.textProperty().setValue("(Searching not started yet.)");
    toggleAllEmailsCheckBox.setTooltip(new Tooltip(SELECT_ALL_CAPTION));
    toggleAllEmailsCheckBox.selectedProperty()
        .addListener((checkbox, previous, current) -> onToggleAllEmailsCheckBoxChange());
    selectedTableColumn.setComparator((cb1, cb2) -> Boolean.compare(cb1.isSelected(), cb2.isSelected()));
    targetDirectoryTextField.setText(controller.getConfig().getTargetDirectory());
    processingProgressBarWithText.progressProperty().setValue(0);
    processingProgressBarWithText.textProperty().setValue("(Processing of emails not started yet.)");
    labelsListViewLabel.setText("""
        Your Gmail labels (optional):
        - Each result will have at least one selected label.
        - Select no labels to ignore this filter.
        - %s-click on a label to unselect it.""".formatted(SystemUtils.IS_OS_MAC ? "⌘" : "Ctrl"));
    labelsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    List<GmailLabel> labels = controller.getIdToLabel().entrySet().stream()
        .map(e -> new GmailLabel(e.getKey(), e.getValue())).sorted(Comparator.comparing(GmailLabel::name))
        .collect(Collectors.toList());
    labelsListView.setItems(FXCollections.observableList(labels));
    selectSavedLabels(labels);
    saveLabelsOnChange();
    resultsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    enableScheduleCheckBox.selectedProperty()
        .addListener((checkBox, previous, current) -> onEnableScheduleCheckBoxChange());
    schedulePeriodComboBox.setItems(FXCollections.observableList(Arrays.asList(
        new SchedulePeriod("minute", 60),
        new SchedulePeriod("5 minutes", 5 * 60),
        new SchedulePeriod("10 minutes", 10 * 60),
        new SchedulePeriod("15 minutes", 15 * 60),
        new SchedulePeriod("30 minutes", 30 * 60),
        new SchedulePeriod("1 hour", 3600),
        new SchedulePeriod("3 hours", 3 * 3600),
        new SchedulePeriod("6 hours", 6 * 3600),
        new SchedulePeriod("12 hours", 12 * 3600),
        new SchedulePeriod("24 hours", 24 * 3600)
    )));
    schedulePeriodComboBox.getSelectionModel().select(5);
  }

  private void addMenuForHidingColumns() {
    resultsTable.getColumns().forEach(column -> {
      CheckMenuItem menuItem = new CheckMenuItem(column.getText());
      menuItem.setSelected(true);
      menuItem.setOnAction(event -> column.setVisible(menuItem.isSelected()));
      viewColumnMenu.getItems().add(menuItem);
    });
  }

  private void addMenuForDateFormats() {
    String pattern = controller.getConfig().getDateFormat();
    for (DateFormat dateFormat : DateFormat.values()) {
      CheckMenuItem menuItem = new CheckMenuItem(dateFormat.getPattern());
      if (dateFormat.getPattern().equals(pattern)) {
        menuItem.setSelected(true);
      }
      menuItem.setOnAction(this::onDateFormatMenuItemPressed);
      dateFormatMenu.getItems().add(menuItem);
    }
  }

  private void onDateFormatMenuItemPressed(ActionEvent event) {
    dateFormatMenu.getItems().stream().map(CheckMenuItem.class::cast).forEach(e -> e.setSelected(false));
    CheckMenuItem checkMenuItem = (CheckMenuItem) event.getSource();
    checkMenuItem.setSelected(true);
    String pattern = checkMenuItem.getText();
    controller.getConfig().saveDateFormat(pattern);
    resultsTable.refresh();
  }

  private void selectSavedLabels(List<GmailLabel> labels) {
    Map<String, GmailLabel> idToIdLabel = labels.stream().collect(Collectors.toMap(GmailLabel::id, Function.identity()));
    controller.getConfig().getLabelIds().stream().map(idToIdLabel::get).filter(Objects::nonNull).
        forEach(labelsListView.getSelectionModel()::select);
  }

  private void saveLabelsOnChange() {
    labelsListView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<GmailLabel>) change -> {
      List<String> labelIds = labelsListView.getSelectionModel().getSelectedItems()
          .stream().map(GmailLabel::id).collect(Collectors.toList());
      controller.getConfig().saveLabelIds(labelIds);
    });
  }

  private Vector<ComboItem<Integer>> getEmailSizeOptions() {
    Vector<ComboItem<Integer>> options = new Vector<>();
    for (int minEmailSizeInMb : new int[] {0, 1, 2, 3, 5, 10, 25, 50, 100}) {
      String caption = minEmailSizeInMb == 0 ? "all sizes" : String.format("more than %d MB", minEmailSizeInMb);
      options.add(new ComboItem<>(caption, minEmailSizeInMb));
    }
    return options;
  }

  @FXML
  private void onSignOutButtonPressed() throws IOException {
    controller.signOut();
    Scenes.setScene(Scenes.SIGN_IN);
  }

  @FXML
  private void onSignInAutomaticallyCheckMenuItemAction() {
    controller.getConfig().saveSignInAutomatically(signInAutomaticallyCheckMenuItem.isSelected());
  }

  @FXML
  private void onProcessEmbeddedCheckMenuItemPressed() {
    controller.getConfig().saveProcessEmbedded(processEmbeddedCheckMenuItem.isSelected());
  }

  @FXML
  private void onAboutButtonPressed() {
    controller.openUnattachHomepage();
  }

  @FXML
  private void onFeedbackMenuItemPressed() {
    try {
      Stage dialog = Scenes.createNewStage(Constants.PRODUCT_NAME +" : feedback");
      dialog.initOwner(root.getScene().getWindow());
      dialog.initModality(Modality.APPLICATION_MODAL);
      dialog.setScene(Scenes.loadScene("/feedback.view.fxml"));
      Scenes.showAndPreventMakingSmaller(dialog);
    } catch (Throwable t) {
      reportError("Unable to open feedback view.", t);
    }
  }

  @FXML
  private void onSearchButtonPressed() {
    onSearchButtonPressed(null);
  }

  private void onSearchButtonPressed(Runnable successCallback) {
    disableControls();
    resultsSubView.setText("Results");
    stopSearchButton.setDisable(false);
    stopSearchButtonPressed = false;
    resultsTable.setItems(FXCollections.emptyObservableList());
    AtomicInteger currentBatch = new AtomicInteger();
    AtomicInteger numberOfBatches = new AtomicInteger();

    Task<Void> task = new Task<>() {
      @Override
      protected Void call() throws Exception {
        updateProgress(0, 1);
        updateMessage("Getting info about emails...");
        String query = getQuery();
        GetEmailMetadataTask longTask = controller.getSearchTask(query);
        currentBatch.set(0);
        numberOfBatches.set(longTask.getNumberOfSteps());
        updateProgress(currentBatch.get(), numberOfBatches.get());
        updateMessage(String.format("Getting info about emails (%s)...", getStatusString()));
        while (!stopSearchButtonPressed && longTask.hasMoreSteps()) {
          GetEmailMetadataTask.Result result = longTask.takeStep();
          currentBatch.set(result.currentBatchNumber());
          updateProgress(currentBatch.get(), numberOfBatches.get());
          updateMessage(String.format("Getting info about emails (%s)...", getStatusString()));
        }
        return null;
      }

      private String getStatusString() {
        if (numberOfBatches.get() == 0) {
          return "no emails matched the query";
        } else {
          return String.format("completed %d of %d batches, %d%%",
              currentBatch.get(), numberOfBatches.get(), 100 * currentBatch.get() / numberOfBatches.get());
        }
      }

      @Override
      protected void succeeded() {
        boolean successful = false;
        try {
          String message = "Finished getting info about emails (%s).".formatted(getStatusString());
          logger.info(message);
          updateMessage(message);
          List<Email> emails = controller.getSearchResults();
          ObservableList<Email> observableEmails = FXCollections.observableList(emails, email -> new Observable[]{email});
          resultsTable.setItems(observableEmails);
          updateResultsCaption();
          observableEmails.addListener((ListChangeListener<? super Email>) change -> updateResultsCaption());
          successful = true;
        } catch (Throwable t) {
          String message = "Failed to get email info.";
          updateMessage(message);
          reportError(message, t);
        } finally {
          resetControls();
        }
        if (successful && successCallback != null) {
          successCallback.run();
        }
      }

      @Override
      protected void failed() {
        String message = "Failed to get email info.";
        updateMessage(message);
        reportError(message, getException());
        resetControls();
      }
    };

    searchProgressBarWithText.progressProperty().bind(task.progressProperty());
    searchProgressBarWithText.textProperty().bind(task.messageProperty());

    new Thread(task).start();
  }

  private void updateResultsCaption() {
    Platform.runLater(() -> {
      int selected = 0, total = 0, selectedSizeInMegaBytes = 0, totalSizeInMegaBytes = 0;
      for (Email email : resultsTable.getItems()) {
        if (email.isSelected()) {
          ++selected;
          selectedSizeInMegaBytes += email.getSizeInMegaBytes();
        }
        ++total;
        totalSizeInMegaBytes += email.getSizeInMegaBytes();
      }
      resultsSubView.setText(String.format("Results: selected %d/%d (%dMB/%dMB)",
              selected, total, selectedSizeInMegaBytes, totalSizeInMegaBytes));
    });
  }

  private void reportError(String message, Throwable t) {
    logger.error(message, t);
    String stackTraceText = ExceptionUtils.getStackTrace(t);
    controller.sendToServer("stack trace", stackTraceText, null);
  }

  @FXML
  private void onStopSearchButtonPressed() {
    stopSearchButtonPressed = true;
  }

  @FXML
  private void onToggleAllEmailsCheckBoxChange() {
    EmailStatus targetStatus = toggleAllEmailsCheckBox.isSelected() ? EmailStatus.TO_PROCESS : EmailStatus.NOT_SELECTED;
    resultsTable.getItems().forEach(email -> {
      if (email.getStatus() == EmailStatus.NOT_SELECTED || email.getStatus() == EmailStatus.TO_PROCESS) {
        email.setStatus(targetStatus);
      }
    });
    resultsTable.refresh();
    Tooltip tooltip = new Tooltip(toggleAllEmailsCheckBox.isSelected() ? DESELECT_ALL_CAPTION : SELECT_ALL_CAPTION);
    toggleAllEmailsCheckBox.setTooltip(tooltip);
  }

  private String getQuery() {
    StringBuilder query = new StringBuilder();
    if (basicSearchTab.isSelected()) {
      int minEmailSizeInMb = emailSizeComboBox.getSelectionModel().getSelectedItem().value();
      query.append(String.format("has:attachment size:%dm", minEmailSizeInMb));
      ObservableList<GmailLabel> labels = labelsListView.getSelectionModel().getSelectedItems();
      if (!labels.isEmpty()) {
        query.append(" {");
        query.append(labels.stream().map(label -> String.format("label:\"%s\"", label.name()))
                .collect(Collectors.joining(" ")));
        query.append("}");
      }
    } else {
      query = new StringBuilder(searchQueryTextField.getText());
      controller.getConfig().saveSearchQuery(searchQueryTextField.getText());
    }
    return query.toString();
  }

  @FXML
  private void onBrowseButtonPressed() {
    DirectoryChooser directoryChooser = new DirectoryChooser();
    directoryChooser.setTitle("Set directory for storing attachments");
    File initialDirectory = new File(targetDirectoryTextField.getText());
    if (initialDirectory.exists()) {
      directoryChooser.setInitialDirectory(initialDirectory);
    }
    File newTargetDirectory = directoryChooser.showDialog(targetDirectoryTextField.getScene().getWindow());
    if (newTargetDirectory != null) {
      targetDirectoryTextField.setText(newTargetDirectory.getAbsolutePath());
      controller.getConfig().saveTargetDirectory(newTargetDirectory.getAbsolutePath());
    }
  }

  @FXML
  private void onOpenButtonPressed() {
    File targetDirectory = getTargetDirectory();
    if (targetDirectory.mkdirs()) {
      logger.info("Created directory \"" + targetDirectory.getAbsolutePath() + "\".");
    }
    controller.openFile(targetDirectory);
  }

  private File getTargetDirectory() {
    return new File(targetDirectoryTextField.getText());
  }

  @FXML
  private void onDownloadButtonPressed() {
    String downloadedLabelId = controller.getOrCreateDownloadedLabelId();
    String removedLabelId = controller.getOrCreateRemovedLabelId();
    ProcessOption processOption = new ProcessOption(DOWNLOAD, processEmbeddedCheckMenuItem.isSelected(),
        backupCheckBox.isSelected(), false, downloadedLabelId, removedLabelId);
    processEmails(processOption);
  }

  @FXML
  private void onDownloadAndRemoveButtonPressed() {
    boolean permanentlyRemoveOriginal = permanentlyRemoveOriginalMenuItem.isSelected();
    String downloadedLabelId = controller.getOrCreateDownloadedLabelId();
    String removedLabelId = controller.getOrCreateRemovedLabelId();
    ProcessOption processOption = new ProcessOption(DOWNLOAD_AND_REMOVE, processEmbeddedCheckMenuItem.isSelected(),
        backupCheckBox.isSelected(), permanentlyRemoveOriginal, downloadedLabelId, removedLabelId);
    processEmails(processOption);
  }

  @FXML
  private void onRemoveButtonPressed() {
    boolean permanentlyRemoveOriginal = permanentlyRemoveOriginalMenuItem.isSelected();
    String downloadedLabelId = controller.getOrCreateDownloadedLabelId();
    String removedLabelId = controller.getOrCreateRemovedLabelId();
    ProcessOption processOption = new ProcessOption(REMOVE, processEmbeddedCheckMenuItem.isSelected(),
        backupCheckBox.isSelected(), permanentlyRemoveOriginal, downloadedLabelId, removedLabelId);
    processEmails(processOption);
  }

  private void processEmails(ProcessOption processOption) {
    List<Email> emailsToProcess = getEmailsToProcess();
    if (emailsToProcess.isEmpty() && !enableScheduleCheckBox.isSelected()) {
      showNoEmailsAlert();
      return;
    }
    logger.info("Processing %d emails with %s and filename schema '%s'...", emailsToProcess.size(), processOption,
        controller.getConfig().getFilenameSchema());
    disableControls();
    stopProcessingButton.setDisable(false);
    stopProcessingButtonPressed = false;
    File targetDirectory = getTargetDirectory();
    bytesProcessed = 0;
    allBytesToProcess = emailsToProcess.stream().mapToLong(email -> (long) email.getSizeInBytes()).sum();
    processingProgressBarWithText.progressProperty().setValue(0);
    String filenameSchema = controller.getConfig().getFilenameSchema();
    ProcessSettings processSettings = new ProcessSettings(processOption, targetDirectory, filenameSchema,
        addMetadataCheckMenuItem.isSelected());
    processEmail(emailsToProcess, 0, 0, processSettings);
  }

  private void showNoEmailsAlert() {
    Alert alert = new Alert(Alert.AlertType.WARNING);
    alert.setTitle("No emails selected");
    alert.setHeaderText(null);
    alert.setContentText("Please select some or all emails in the search results. You can de/select an individual " +
        "email by clicking on the checkbox in its selected row. Alternatively, you can de/select all emails by " +
        "clicking on the checkbox in the table header.");
    alert.showAndWait();
  }

  private void processEmail(List<Email> emailsToProcess, int nextEmailIndex, int failed, ProcessSettings processSettings) {
    String processingStatusString = getProcessingStatusString(emailsToProcess, nextEmailIndex, failed);
    if (stopProcessingButtonPressed || nextEmailIndex >= emailsToProcess.size()) {
      String message = "Processing stopped (%s).".formatted(processingStatusString);
      logger.info(message);
      processingProgressBarWithText.textProperty().setValue(message);
      resetControls();
      if (enableScheduleCheckBox.isSelected()) {
        scheduleNextRun(processSettings.processOption().action());
      }
      return;
    }
    Email email = emailsToProcess.get(nextEmailIndex);
    logger.info("Processing email with subject '%s'...", email.getSubject());
    processingProgressBarWithText.textProperty().setValue(
        "Processing selected emails (%s)...".formatted(processingStatusString));

    Task<ProcessEmailResult> task = new Task<>() {
      @Override
      protected ProcessEmailResult call() throws Exception {
        LongTask<ProcessEmailResult> longTask = controller.getProcessTask(email, processSettings);
        // This is 'if' and not 'while', because longTask always has a single step.
        if (!stopProcessingButtonPressed && longTask.hasMoreSteps()) {
          return longTask.takeStep();
        }
        return null;
      }

      @Override
      protected void succeeded() {
        ProcessEmailResult processEmailResult = getValue();
        if (processEmailResult != null) {
          if (processEmailResult.newUniqueId() != null) {
            email.setUniqueId(processEmailResult.newUniqueId());
          }
          bytesProcessed += email.getSizeInBytes();
          processingProgressBarWithText.progressProperty().setValue(1.0 * bytesProcessed / allBytesToProcess);
          resultsTable.refresh();
        }
        processEmail(emailsToProcess, nextEmailIndex + 1, failed, processSettings);
      }

      @Override
      protected void failed() {
        email.setStatus(EmailStatus.FAILED);
        email.setProcessLog(getException().getMessage());
        resultsTable.refresh();
        reportError("Failed to process selected emails.", getException());
        processEmail(emailsToProcess, nextEmailIndex + 1, failed + 1, processSettings);
      }
    };

    new Thread(task).start();
  }

  private String getProcessingStatusString(List<Email> emailsToProcess, int nextEmailIndex, int failed) {
    return String.format("processed %d of %d, %dMB / %dMB, %d%% by size, %d failed",
        nextEmailIndex, emailsToProcess.size(), toMegaBytes(bytesProcessed), toMegaBytes(allBytesToProcess),
        allBytesToProcess == 0 ? 0 : 100 * bytesProcessed / allBytesToProcess, failed);
  }

  private static int toMegaBytes(long bytes) {
    return (int) (bytes / Constants.BYTES_IN_MEGABYTE);
  }

  @FXML
  private void onStopProcessingButtonPressed() {
    stopProcessingButton.setDisable(true);
    stopProcessingButtonPressed = true;
  }

  private List<Email> getEmailsToProcess() {
    return resultsTable.getItems().stream()
        .filter(email -> email.getStatus() == EmailStatus.TO_PROCESS).collect(Collectors.toList());
  }

  private void disableControls() {
    signOutMenuItem.setDisable(true);
    searchButton.setDisable(true);
    stopSearchButton.setDisable(true);
    resultsTable.setEditable(false);
    setResultsTableCheckboxesEnabled(false);
    toggleAllEmailsCheckBox.setDisable(true);
    targetDirectoryTextField.setDisable(true);
    browseButton.setDisable(true);
    backupCheckBox.setDisable(true);
    downloadButton.setDisable(true);
    downloadAndRemoveButton.setDisable(true);
    removeButton.setDisable(true);
    stopProcessingButton.setDisable(true);
  }

  private void resetControls() {
    signOutMenuItem.setDisable(false);
    searchButton.setDisable(false);
    stopSearchButton.setDisable(true);
    resultsTable.setEditable(true);
    setResultsTableCheckboxesEnabled(true);
    toggleAllEmailsCheckBox.setDisable(false);
    toggleAllEmailsCheckBox.setSelected(false);
    targetDirectoryTextField.setDisable(false);
    browseButton.setDisable(false);
    backupCheckBox.setDisable(false);
    downloadButton.setDisable(false);
    downloadAndRemoveButton.setDisable(false);
    removeButton.setDisable(false);
    stopProcessingButton.setDisable(true);
  }

  private void setResultsTableCheckboxesEnabled(boolean enabled) {
    for (int row = 0; row < resultsTable.getItems().size(); ++row) {
      selectedTableColumn.getCellObservableValue(row).getValue().setDisable(!enabled);
    }
    resultsTable.refresh();
  }

  @FXML
  private void onDonateMenuItemPressed(ActionEvent event) {
    Object source = event.getSource();
    String currency = getSelectedCurrency();
    if (source == donateTwo) {
      controller.donate("Espresso", 2, currency);
    } else if (source == donateFive) {
      controller.donate("Cappuccino", 5, currency);
    } else if (source == donateTen) {
      controller.donate("Caramel Machiato", 10, currency);
    } else if (source == donateTwentyFive) {
      controller.donate("Bag of Coffee", 25, currency);
    } else if (source == donateFifty) {
      controller.donate("Coffee Machine", 50, currency);
    } else if (source == donateCustom) {
      controller.donate("A Truck of Coffee", 0, currency);
    }
  }

  @FXML
  private void onOpenLogMenuItemPressed() {
    try {
      String home = System.getProperty("user.home");
      File homeFile = new File(home);
      File[] logFiles = homeFile.listFiles(file -> {
        String name = file.getName();
        return name.startsWith(".unattach") && name.endsWith(".log");
      });
      long latestTimestamp = 0;
      File latest = null;
      if (logFiles == null) {
        return;
      }
      for (File logFile : logFiles) {
        if (logFile.lastModified() > latestTimestamp) {
          latestTimestamp = logFile.lastModified();
          latest = logFile;
        }
      }
      controller.openFile(latest);
    } catch (Throwable t) {
      reportError("Couldn't open the log file.", t);
    }
  }

  @FXML
  private void onRemoveOriginalMenuItemPressed() {
    permanentlyRemoveOriginalMenuItem.setSelected(true);
    trashOriginalMenuItem.setSelected(false);
    controller.getConfig().saveRemoveOriginal(true);
  }

  @FXML
  private void onTrashOriginalMenuItemPressed() {
    permanentlyRemoveOriginalMenuItem.setSelected(false);
    trashOriginalMenuItem.setSelected(true);
    controller.getConfig().saveRemoveOriginal(false);
  }

  @FXML
  private void onQueryButtonPressed() {
    controller.openQueryLanguagePage();
  }

  @FXML
  private void onFilenameSchemaMenuItemPressed() {
    try {
      Stage dialog = Scenes.createNewStage(Constants.PRODUCT_NAME + " : file name scheme");
      dialog.initOwner(root.getScene().getWindow());
      dialog.initModality(Modality.APPLICATION_MODAL);
      Scene scene = Scenes.loadScene("/filename-schema.view.fxml");
      FilenameSchemaController filenameSchemaController = (FilenameSchemaController) scene.getUserData();
      filenameSchemaController.setSchema(controller.getConfig().getFilenameSchema());
      dialog.setScene(scene);
      Scenes.showAndPreventMakingSmaller(dialog);
    } catch (IOException e) {
      logger.error("Failed to open the file name scheme dialog.", e);
    }
  }

  @FXML
  private void onTutorialVideoButtonPressed() {
    controller.openWebPage(Constants.TUTORIAL_VIDEO_URL);
  }

  @FXML
  private void onEmailSizeComboBoxChanged() {
    controller.getConfig().saveEmailSize(emailSizeComboBox.getValue().value());
  }

  @FXML
  private void onUnattachLabelMenuItemPressed() {
    try {
      Stage dialog = Scenes.createNewStage(Constants.PRODUCT_NAME + " : Unattach labels");
      dialog.initOwner(root.getScene().getWindow());
      dialog.initModality(Modality.APPLICATION_MODAL);
      Scene scene = Scenes.loadScene("/unattach-labels.view.fxml");
      dialog.setScene(scene);
      Scenes.showAndPreventMakingSmaller(dialog);
    } catch (IOException e) {
      logger.error("Failed to open the Unattach labels dialog.", e);
    }
  }

  @FXML
  private void onDonationCurrencySelected(ActionEvent actionEvent) {
    donationCurrencyMenu.getItems().stream().map(CheckMenuItem.class::cast).forEach(e -> e.setSelected(false));
    ((CheckMenuItem) actionEvent.getSource()).setSelected(true);
    String currency = getSelectedCurrency();
    donateMenu.getItems().forEach(e -> {
      String text = e.getText();
      int start = text.indexOf('(');
      String details = text.substring(start + 1, text.length() - 1);
      String[] parts = details.split(" ");
      if (parts.length == 2) {
        String prefix = text.substring(0, start);
        e.setText(prefix + "(" + parts[0] + " " + currency + ")");
      }
    });
  }

  private String getSelectedCurrency() {
    Optional<CheckMenuItem> selectedCurrencyMenu = donationCurrencyMenu.getItems().stream()
            .map(CheckMenuItem.class::cast).filter(CheckMenuItem::isSelected).findFirst();
    return selectedCurrencyMenu.isEmpty() ? null : selectedCurrencyMenu.get().getText();
  }

  private void onEnableScheduleCheckBoxChange() {
    boolean enabled = enableScheduleCheckBox.isSelected();
    schedulePeriodPrefixLabel.setDisable(!enabled);
    schedulePeriodComboBox.setDisable(!enabled);
  }

  private void scheduleNextRun(Action action) {
    stopAnyRunningSchedule();
    stopScheduleButton.setDisable(false);
    SchedulePeriod schedulePeriod = schedulePeriodComboBox.getSelectionModel().getSelectedItem();
    LocalDateTime nextRunTime = LocalDateTime.now().plusSeconds(schedulePeriod.seconds());
    timeline = new Timeline(new KeyFrame(Duration.ZERO, event -> {
      LocalDateTime now = LocalDateTime.now();
      if (now.isAfter(nextRunTime)) {
        timeline.stop();
        timeline = null;
        scheduleTimeLabel.setText("");
        onSchedule(action);
      } else {
        long durationMillis = java.time.Duration.between(now, nextRunTime).toMillis();
        String duration = DurationFormatUtils.formatDurationWords(durationMillis, true, false);
        scheduleTimeLabel.setText("Next '" + action + "' in " + duration + ".");
      }
    }), new KeyFrame(Duration.seconds(1)));
    timeline.setCycleCount(Timeline.INDEFINITE);
    timeline.play();
  }

  @FXML
  private void onSchedule(Action action) {
    onSearchButtonPressed(() -> {
      toggleAllEmailsCheckBox.setSelected(true);
      switch (action) {
        case DOWNLOAD -> onDownloadButtonPressed();
        case REMOVE -> onRemoveButtonPressed();
        case DOWNLOAD_AND_REMOVE -> onDownloadAndRemoveButtonPressed();
      }
    });
  }

  @FXML
  private void onStopScheduleButtonPressed() {
    stopAnyRunningSchedule();
    enableScheduleCheckBox.setSelected(false);
  }

  private void stopAnyRunningSchedule() {
    if (timeline != null) {
      timeline.stop();
      timeline = null;
    }
    scheduleTimeLabel.setText("");
    stopScheduleButton.setDisable(true);
  }
}
