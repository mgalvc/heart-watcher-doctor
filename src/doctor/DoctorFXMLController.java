/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package doctor;

import com.jfoenix.controls.JFXListView;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 *
 * @author matheus
 */
public class DoctorFXMLController implements Initializable {
    
    @FXML
    private VBox vboxSpecifics;
    
    @FXML
    private Label patientName;

    @FXML
    private TableView<Register> table;
    
    @FXML
    private TableColumn<Register, String> pressureCol;

    @FXML
    private TableColumn<Register, String> heartRateCol;
    
    @FXML
    private TableColumn<Register, String> movementCol;
    
    @FXML
    private JFXListView<VBox> list;
    
    @FXML
    private Label label;
    
    private boolean connected;
    private int selectedIdAux;
    private int selectedId;
    
    private ArrayList<HashMap<String, Object>> patients;
    
    @FXML
    void selectSpecificClient(MouseEvent event) throws IOException, ClassNotFoundException {
        //pega o ID selecionado
        selectedId = list.getSelectionModel().getSelectedIndices().get(0);
        
        vboxSpecifics.setVisible(true);
        
        if(selectedId != selectedIdAux) {
            table.getItems().clear();
            selectedIdAux = selectedId;
        }
    }
    
    /**
     * registers the doctor to the server
     * @throws ClassNotFoundException
     * @throws IOException 
     */
    private void registerToServer() throws ClassNotFoundException, IOException {
        HashMap<String, Object> message = new HashMap<>();
        message.put("source", "doctor");
        message.put("action", "register");
        
        System.out.println("registering to server...");
        
        //envia mensagem para o server
        HashMap<String, Object> response = Client.send(message);
        System.out.println(response.get("message"));
        
        connected = true;
    }
    
    /**
     * requests to server the general data of all patients
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    private void requestGeneralData() throws IOException, ClassNotFoundException {
        HashMap<String, Object> message = new HashMap<>();
        message.put("source", "doctor");
        message.put("action", "get_general");
        
        patients = (ArrayList<HashMap<String, Object>>) Client.send(message).get("payload");
        
        System.out.println(patients.toString());
        
        //special thread to change javaFX components
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Platform.runLater(() -> {
                    list.getItems().clear();

                    for (HashMap<String, Object> patient : patients) {
                        VBox box = new VBox(5);
                        Label name = new Label((String) patient.get("name"));
                        Label id = new Label(String.valueOf(patient.get("id")));
                        box.getChildren().addAll(name, id);
                        
                        if((boolean) patient.get("in_risk")) {
                            box.setStyle("-fx-background-color: #E57373");
                        } else {
                            box.setStyle("-fx-background-color: #B2FF59");
                        }

                        list.getItems().add(box);
                    }
                });
                return null;
            }
            
        };
        new Thread(task).start();
    }
    
    private void requestSpecificData() throws IOException, ClassNotFoundException {
        if(selectedId == -1) {
            return;
        }
        
        HashMap<String, Object> message = new HashMap<>();
        message.put("source", "doctor");
        message.put("action", "get_specifics");
        message.put("id", selectedId);
        
        HashMap<String, Object> response = (HashMap<String, Object>) Client.send(message).get("payload");
        System.out.println(response.toString());
        
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Platform.runLater(() -> {
                    patientName.setText((String) response.get("name"));
                    patientName.setVisible(true);
                });
                return null;
            }
        };
        new Thread(task).start();
        
        String pressure = String.join("/", (String[]) response.get("pressure"));

        table.getItems().add(new Register(response.get("heart_rate").toString(), pressure, response.get("movement").toString()));
        
        String movement = (boolean) response.get("movement") ? "running" : "resting";

        if((boolean) response.get("in_risk")) {
            FileWriter fw = new FileWriter(response.get("name").toString() + ".txt", true);
            String toWrite = String.format("%s bpm, %s, %s, %s\n",
                    response.get("heart_rate").toString(),
                    pressure, movement, (String) response.get("time"));
            fw.write(toWrite);
            fw.close();
        }
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        vboxSpecifics.setVisible(false);
        patientName.setVisible(false);
        
        heartRateCol.setCellValueFactory(new PropertyValueFactory<>("heartRate"));
        pressureCol.setCellValueFactory(new PropertyValueFactory<>("pressure"));
        movementCol.setCellValueFactory(new PropertyValueFactory<>("movement"));
        
        table.setItems(FXCollections.observableArrayList());
        
        new Thread(() -> {
            try {
                registerToServer();
                selectedId = -1;
                
                System.out.println("initialized");
                
                while(connected) {
                    requestGeneralData();
                    requestSpecificData();
                    Thread.sleep(5000);
                }
            } catch (ClassNotFoundException | IOException | InterruptedException ex) {
                Logger.getLogger(DoctorFXMLController.class.getName()).log(Level.SEVERE, null, ex);
            }            
        }).start();
    }
    
    public static class Register {
        private final SimpleStringProperty heartRate;
        private final SimpleStringProperty pressure;
        private final SimpleStringProperty movement;
        
        public Register(String heartRate, String pressure, String movement) {
            this.heartRate = new SimpleStringProperty(heartRate);
            this.pressure = new SimpleStringProperty(pressure);
            this.movement = new SimpleStringProperty(movement);
        }
        
        public String getHeartRate() {
            return this.heartRate.toString();
        }
        
        public String getPressure() {
            return this.pressure.toString();
        }
        
        public String getMovement() {
            return this.movement.toString();
        }
        
        public SimpleStringProperty heartRateProperty() {
            return this.heartRate;
        }
        
        public SimpleStringProperty pressureProperty() {
            return this.pressure;
        }
        
        public SimpleStringProperty movementProperty() {
            return this.movement;
        }
                
    }
    
}
