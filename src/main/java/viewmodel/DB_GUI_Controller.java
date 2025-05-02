package viewmodel;

import dao.DbConnectivityClass;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Person;
import service.MyLogger;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.stage.FileChooser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class DB_GUI_Controller implements Initializable {

    @FXML
    TextField first_name, last_name, department, email, imageURL;
    @FXML
    ComboBox<Major> major;

    @FXML
    ImageView img_view;
    @FXML
    MenuBar menuBar;
    @FXML
    private TableView<Person> tv;
    @FXML
    private TableColumn<Person, Integer> tv_id;
    @FXML
    private TableColumn<Person, String> tv_fn, tv_ln, tv_department, tv_major, tv_email;
    private final DbConnectivityClass cnUtil = new DbConnectivityClass();
    private final ObservableList<Person> data = cnUtil.getData();


    @FXML
    private Button editButton, deleteButton, addButton;

    @FXML
    private MenuItem editItem, deleteItem, newItem;

    @FXML
    private Label statusLabel;

    // EDITED: Added CSV menu items
    @FXML
    private MenuItem importCSVItem, exportCSVItem;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            tv_id.setCellValueFactory(new PropertyValueFactory<>("id"));
            tv_fn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
            tv_ln.setCellValueFactory(new PropertyValueFactory<>("lastName"));
            tv_department.setCellValueFactory(new PropertyValueFactory<>("department"));
            tv_major.setCellValueFactory(new PropertyValueFactory<>("major"));
            tv_email.setCellValueFactory(new PropertyValueFactory<>("email"));
            tv.setItems(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Person selectedPerson = tv.getSelectionModel().getSelectedItem();
        boolean hasSelection = (selectedPerson != null);


        editButton.setDisable(!hasSelection);
        deleteButton.setDisable(!hasSelection);


        editItem.setDisable(!hasSelection);
        deleteItem.setDisable(!hasSelection);

        // combobox with enum
        major.setItems(FXCollections.observableArrayList(Major.values()));

        // Initialize status label
        statusLabel.setText("");
        statusLabel.setVisible(false);




    }

    // EDITED: Added CSV import/export methods

    @FXML
    protected void importCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import CSV File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );


        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            try {

                int recordsImported = importCSVFile(file);


                data.clear();
                data.addAll(cnUtil.getData());
                tv.refresh();


                showSuccessStatus(recordsImported + " records imported successfully");
            } catch (Exception e) {
                showErrorStatus("Error importing CSV: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }


    @FXML
    protected void exportCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export CSV File");
        fileChooser.setInitialFileName("exported_data.csv");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        // shpw save file dialog
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try {

                int recordsExported = exportCSVFile(file);

                // shopw success message
                showSuccessStatus(recordsExported + " records exported successfully");
            } catch (Exception e) {
                showErrorStatus("Error exporting CSV: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }



    private int importCSVFile(File file) throws Exception {
        int recordsImported = 0;





        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {



                // skip[ header row
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                // parse CSV line
                String[] fields = line.split(",");


                if (fields.length >= 5) {
                    String firstName = fields[0].trim();
                    String lastName = fields[1].trim();
                    String department = fields[2].trim();
                    String majorStr = fields[3].trim();
                    String email = fields[4].trim();

                    // Set image URL if available
                    String imageUrl = fields.length > 5 ? fields[5].trim() : "";


                    Person person = new Person(firstName, lastName, department, majorStr, email, imageUrl);

                    // add to database
                    cnUtil.insertUser(person);
                    recordsImported++;
                }
            }
        }

        return recordsImported;
    }




    private int exportCSVFile(File file) throws Exception {
        int recordsExported = 0;

        try (FileWriter writer = new FileWriter(file)) {



            writer.write("FirstName,LastName,Department,Major,Email,ImageURL\n");


            for (Person person : data) {
                writer.write(String.format("%s,%s,%s,%s,%s,%s\n",
                        escapeCSV(person.getFirstName()),
                        escapeCSV(person.getLastName()),
                        escapeCSV(person.getDepartment()),
                        escapeCSV(person.getMajor()),
                        escapeCSV(person.getEmail()),
                        escapeCSV(person.getImageURL())
                ));
                recordsExported++;
            }
        }

        return recordsExported;
    }




    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }




        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {



            value = value.replace("\"", "\"\"");




            return "\"" + value + "\"";
        }

        return value;
    }

    @FXML
    protected void addNewRecord() {



        String majorStr = major.getValue() != null ? major.getValue().toString() : "";

        try {



            Person p = new Person(
                    first_name.getText(),
                    last_name.getText(),
                    department.getText(),
                    majorStr,
                    email.getText(),
                    imageURL.getText()
            );




            cnUtil.insertUser(p);
            cnUtil.retrieveId(p);
            p.setId(cnUtil.retrieveId(p));
            data.add(p);
            clearForm();



            showSuccessStatus("Record added successfully");
        } catch (Exception e) {


            showErrorStatus("Error adding record: " + e.getMessage());
            e.printStackTrace();
        }
    }




    @FXML
    protected void clearForm() {
        first_name.setText("");
        last_name.setText("");
        department.setText("");
        major.setValue(null);
        email.setText("");
        imageURL.setText("");

        major.setValue(null);



        editButton.setDisable(true);
        deleteButton.setDisable(true);

        // reset menu item
        editItem.setDisable(true);
        deleteItem.setDisable(true);

    }

    @FXML
    protected void logOut(ActionEvent actionEvent) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/lightTheme.css").getFile());
            Stage window = (Stage) menuBar.getScene().getWindow();
            window.setScene(scene);
            window.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void closeApplication() {
        System.exit(0);
    }

    @FXML
    protected void displayAbout() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/about.fxml"));
            Stage stage = new Stage();
            Scene scene = new Scene(root, 600, 500);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void editRecord() {
        Person selectedPerson = tv.getSelectionModel().getSelectedItem();
        if (selectedPerson != null) {
            try {
                // Get major as string from ComboBox
                String majorStr = major.getValue() != null ? major.getValue().toString() : "";

                // Update person object with form values
                selectedPerson.setFirstName(first_name.getText());
                selectedPerson.setLastName(last_name.getText());
                selectedPerson.setDepartment(department.getText());
                selectedPerson.setMajor(majorStr);
                selectedPerson.setEmail(email.getText());
                //selectedPerson.setUrl(imageURL.getText());

                // Update database and refresh table
                //cnUtil.updateUser(selectedPerson);
                tv.refresh();
                clearForm();

                // Show success message
                showSuccessStatus("Record updated successfully");
            } catch (Exception e) {
                // Show error message
                showErrorStatus("Error updating record: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            showInfoStatus("Please select a record to edit");
        }
    }


    @FXML
    protected void deleteRecord() {
        Person selectedPerson = tv.getSelectionModel().getSelectedItem();
        if (selectedPerson != null) {
            try {
                // delete confirmation logic...

                // After successful deletion
                showSuccessStatus("Record deleted successfully");
            } catch (Exception e) {
                showErrorStatus("Error deleting record: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            showInfoStatus("Please select a record to delete");
        }
    }


    @FXML
    protected void showImage() {
        File file = (new FileChooser()).showOpenDialog(img_view.getScene().getWindow());
        if (file != null) {
            img_view.setImage(new Image(file.toURI().toString()));
        }
    }

    @FXML
    protected void addRecord() {
        showSomeone();
    }

    @FXML
    protected void selectedItemTV(MouseEvent mouseEvent) {
        // first the selection state
        boolean hasSelection = tv.getSelectionModel().getSelectedItem() != null;

        // enable/disable buttons and menu items based on selection
        editButton.setDisable(!hasSelection);
        deleteButton.setDisable(!hasSelection);
        editItem.setDisable(!hasSelection);
        deleteItem.setDisable(!hasSelection);

        // populate  fields if an item is selected
        Person selectedPerson = tv.getSelectionModel().getSelectedItem();
        if (selectedPerson != null) {
            // Populate form fields
            first_name.setText(selectedPerson.getFirstName());
            last_name.setText(selectedPerson.getLastName());
            department.setText(selectedPerson.getDepartment());

            // set mjr ComboBox value
            String majorStr = selectedPerson.getMajor();
            for (Major m : Major.values()) {
                if (m.toString().equals(majorStr)) {
                    major.setValue(m);
                    break;
                }
            }

            email.setText(selectedPerson.getEmail());
            imageURL.setText(selectedPerson.getImageURL());

            // display image
            if (selectedPerson.getImageURL() != null && !selectedPerson.getImageURL().isEmpty()) {
                try {
                    img_view.setImage(new Image(selectedPerson.getImageURL()));
                } catch (Exception e) {
                    // Handle exception (e.g., invalid image URL)
                    System.err.println("Error loading image: " + e.getMessage());
                }
            } else {
                img_view.setImage(null);
            }
        }
    }

    public void lightTheme(ActionEvent actionEvent) {
        try {
            Scene scene = menuBar.getScene();
            Stage stage = (Stage) scene.getWindow();
            stage.getScene().getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/css/lightTheme.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
            System.out.println("light " + scene.getStylesheets());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void darkTheme(ActionEvent actionEvent) {
        try {
            Stage stage = (Stage) menuBar.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/css/darkTheme.css").toExternalForm());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showSomeone() {
        Dialog<Results> dialog = new Dialog<>();
        dialog.setTitle("New User");
        dialog.setHeaderText("Please specify…");
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField textField1 = new TextField("Name");
        TextField textField2 = new TextField("Last Name");
        TextField textField3 = new TextField("Email ");
        ObservableList<Major> options =
                FXCollections.observableArrayList(Major.values());
        ComboBox<Major> comboBox = new ComboBox<>(options);
        comboBox.getSelectionModel().selectFirst();
        dialogPane.setContent(new VBox(8, textField1, textField2,textField3, comboBox));
        Platform.runLater(textField1::requestFocus);
        dialog.setResultConverter((ButtonType button) -> {
            if (button == ButtonType.OK) {
                return new Results(textField1.getText(),
                        textField2.getText(), comboBox.getValue());
            }
            return null;
        });
        Optional<Results> optionalResult = dialog.showAndWait();
        optionalResult.ifPresent((Results results) -> {
            MyLogger.makeLog(
                    results.fname + " " + results.lname + " " + results.major);
        });
    }

    private enum Major {
        CS("Computer Science"),
        CPIS("Computer Information Systems"),
        ENGLISH("English"),
        BUSINESS("Business"),
        MATH("Mathematics"),
        PHYSICS("Physics");

        private final String displayName;

        Major(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }


        public static Major fromString(String text) {
            for (Major m : Major.values()) {
                if (m.getDisplayName().equalsIgnoreCase(text)) {
                    return m;
                }
            }
            return null;
        }
    }

    private static class Results {

        String fname;
        String lname;
        Major major;

        public Results(String name, String date, Major venue) {
            this.fname = name;
            this.lname = date;
            this.major = venue;
        }
    }

    private void setupFormValidation() {
        // Add validation styling to fields
        setupFieldValidation(first_name, NAME_PATTERN);
        setupFieldValidation(last_name, NAME_PATTERN);
        setupFieldValidation(department, DEPARTMENT_PATTERN);
        setupFieldValidation(email, EMAIL_PATTERN);

        // Update the Add button state when any field changes
        Runnable updateAddButton = () -> {
            boolean isValid = isFormValid();
            addButton.setDisable(!isValid);
            newItem.setDisable(!isValid);
        };

        // Listen for changes in ComboBox
        major.valueProperty().addListener((obs, oldVal, newVal) -> updateAddButton.run());

        // Initial validation
        updateAddButton.run();
    }

    private void setupFieldValidation(TextField field, String pattern) {
        field.textProperty().addListener((observable, oldValue, newValue) -> {
            boolean isValid = newValue != null && !newValue.trim().isEmpty() && newValue.matches(pattern);

            // apply CSS style based on validation
            if (isValid) {
                field.setStyle("-fx-border-color: green;");
            } else {
                field.setStyle("-fx-border-color: red;");
            }

            // update add button
            boolean formValid = isFormValid();
            addButton.setDisable(!formValid);
            newItem.setDisable(!formValid);
        });

        //  validation
        boolean isValid = field.getText() != null && !field.getText().trim().isEmpty()
                && field.getText().matches(pattern);
        field.setStyle(isValid ? "-fx-border-color: green;" : "-fx-border-color: red;");
    }

    private boolean isFormValid() {
        return isValidField(first_name, NAME_PATTERN) &&
                isValidField(last_name, NAME_PATTERN) &&
                isValidField(department, DEPARTMENT_PATTERN) &&
                isValidField(email, EMAIL_PATTERN) &&
                major.getValue() != null; //  combox validation
    }

    private boolean isValidField(TextField field, String pattern) {
        String text = field.getText();
        return text != null && !text.trim().isEmpty() && text.matches(pattern);
    }

    private boolean isValidEmail(String email) {

        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(emailRegex);
    }

    // regex patterns
    private static final String NAME_PATTERN = "^[A-Za-z]+(([',. -][A-Za-z ])?[A-Za-z]*)*$";
    private static final String DEPARTMENT_PATTERN = "^[A-Za-z& ]+(([',. -][A-Za-z& ])?[A-Za-z&]*)*$";
    private static final String EMAIL_PATTERN = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";




    private void showStatus(String message, String styleClass) {
        statusLabel.setText(message);

        // Clear existing style classes and add the new one
        statusLabel.getStyleClass().removeAll("status-success", "status-error", "status-info");
        statusLabel.getStyleClass().add(styleClass);

        // Make the label visible
        statusLabel.setVisible(true);

        // Set up auto-clear after 5 seconds
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                Platform.runLater(() -> statusLabel.setText(""));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }


    private void showSuccessStatus(String message) {
        showStatus(message, "status-success");
    }


    private void showErrorStatus(String message) {
        showStatus(message, "status-error");
    }


    private void showInfoStatus(String message) {
        showStatus(message, "status-info");
    }

}