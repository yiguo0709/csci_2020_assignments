/*
 * Yi Guo 100518792
 * Andrew Selvarajah 100510671
 *
 *
 */
package sample;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Path;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.*;
import java.util.*;
import java.lang.Math.*;

public class Main extends Application {
    private BorderPane layout;
    private TableView<TestFile> table;
    private float accuracy, precision;

    @Override

    public void start(Stage primaryStage) throws Exception {
        // Constructing the UI
        primaryStage.setTitle("Spam Detector");

        TableColumn<TestFile, String> fileColumn = null;
        fileColumn = new TableColumn<>("File");
        fileColumn.setMinWidth(300);
        fileColumn.setCellValueFactory(new PropertyValueFactory<>("filename"));

        TableColumn<TestFile, String> actualColumn = null;
        actualColumn = new TableColumn<>("Actual Class");
        actualColumn.setMinWidth(200);
        actualColumn.setCellValueFactory(new PropertyValueFactory<>("actualClass"));

        TableColumn<TestFile, Double> probColumn = null;
        probColumn = new TableColumn<>("Spam Probability");
        probColumn.setMinWidth(300);
        probColumn.setCellValueFactory(new PropertyValueFactory<>("spamProbability"));

        table = new TableView<>();
        table.getColumns().addAll(fileColumn, actualColumn, probColumn);

        GridPane infoArea = new GridPane();
        infoArea.setPadding(new Insets(10,10,10,10));
        infoArea.setVgap(10);
        infoArea.setHgap(10);

        Label accuracyLabel = new Label("Accuracy: ");
        infoArea.add(accuracyLabel, 0, 0);
        TextField accuracyField = new TextField();
        accuracyField.setDisable(true);
        infoArea.add(accuracyField, 1, 0);

        Label precisionLabel = new Label("Precision: ");
        infoArea.add(precisionLabel, 0, 1);
        TextField precisionField = new TextField();
        precisionField.setDisable(true);
        infoArea.add(precisionField, 1, 1);

        Label outputLabel = new Label("Predicting spam");
        infoArea.add(outputLabel,2,0);

        layout = new BorderPane();
        layout.setTop(table);
        layout.setBottom(infoArea);
        Scene scene = new Scene(layout, 800,500);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Constructing the directory chooser
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File("."));
        File mainDirectory = directoryChooser.showDialog(primaryStage);
        System.out.println(mainDirectory.getAbsolutePath());

        // Check if the file exists
        if (!(new File(mainDirectory.getAbsolutePath()+"/train").exists())) {
            System.out.println(mainDirectory.getAbsolutePath()+"/train does not exist.");
            outputLabel.setText(mainDirectory.getAbsolutePath()+"/train does not exist.");
        }

        else if (!(new File(mainDirectory.getAbsolutePath()+"/train/ham").exists() && new File(mainDirectory.getAbsolutePath()+"/train/spam").exists())) {
            System.out.println(mainDirectory.getAbsolutePath()+"/train/spam or "+mainDirectory.getAbsolutePath()+"/train/ham does not exist.");
            outputLabel.setText(mainDirectory.getAbsolutePath()+"/train/spam or "+mainDirectory.getAbsolutePath()+"/train/ham does not exist.");
        }

        else if (!(new File(mainDirectory.getAbsolutePath()+"/test").exists())) {
            System.out.println(mainDirectory.getAbsolutePath()+"/test does not exist.");
            outputLabel.setText(mainDirectory.getAbsolutePath()+"/test does not exist.");
        }

        else if (!(new File(mainDirectory.getAbsolutePath()+"/test/ham").exists() &&
                new File(mainDirectory.getAbsolutePath()+"/test/spam").exists())) {
            System.out.println(mainDirectory.getAbsolutePath()+"/test/spam or "+mainDirectory.getAbsolutePath()+"/test/ham does not exist.");
            outputLabel.setText(mainDirectory.getAbsolutePath()+"/test/spam or "+mainDirectory.getAbsolutePath()+"/test/ham does not exist.");
        }
        //Starting predicting
        else {
            table.setItems(predictSpam(mainDirectory));
            accuracyField.setText(Float.toString(accuracy));
            precisionField.setText(Float.toString(precision));
        }
    }

    public ObservableList<TestFile> predictSpam(File mainDirectory) throws IOException {
        ObservableList<TestFile> files = FXCollections.observableArrayList();

        try {
            //Creating training files
            File hamFile = new File(mainDirectory.getAbsolutePath()+"/train/ham");
            int numHamFiles = hamFile.listFiles().length;
            File spamFile = new File(mainDirectory.getAbsolutePath()+"/train/spam");
            int numSpamFiles = spamFile.listFiles().length;

            //Freq maps constructed
            Map<String, Integer> trainHamFreq = readFiles(hamFile);
            Map<String, Integer> trainSpamFreq = readFiles(spamFile);

            // Pr(S|W1) = (Pr(W1|S)) / (Pr(W1|S) + Pr(W1|H)
            Map<String, Double> prsw1 = new TreeMap<>();
            for (String s : trainSpamFreq.keySet()) {
                if (trainHamFreq.get(s)!=null) {
                    prsw1.put(s, ((double)trainSpamFreq.get(s)/(double)numSpamFiles)/(((double)trainSpamFreq.get(s)/(double)numSpamFiles)+((double)trainHamFreq.get(s)/(double)numHamFiles)));
                }
            }

            //Creating test files
            File testHam = new File(mainDirectory.getAbsolutePath()+"/test/ham");
            File testSpam = new File(mainDirectory.getAbsolutePath()+"/test/spam");

            int numFiles = 0;
            int numTruePositives = 0;
            int numTrueNegatives = 0;
            int numFalsePositives = 0;

            for (File f : testHam.listFiles()) {
                double eta=0;
                WordCounter wc = new WordCounter();
                wc.processFile(f);

                // Eta = sum(ln(1-Pr(S|W1) - ln(Pr(S|W1))
                for (String s : wc.wordCounts.keySet()) {
                    if (prsw1.containsKey(s)) {
                        eta+=(Math.log(1-prsw1.get(s))-Math.log(prsw1.get(s)));
                    }
                }

                // Pr(S|F) = 1 / (1+exp(eta))
                files.add(new TestFile(f.getName(),1/(1+Math.exp(eta)), "Ham"));

                numFiles+=1;
                if (1/(1+Math.exp(eta))<0.5) {
                    numTrueNegatives+=1;
                }
                else {
                    numFalsePositives+=1;
                }
            }

            for (File f : testSpam.listFiles()) {
                double eta=0;
                WordCounter wc = new WordCounter();
                wc.processFile(f);

                for (String s : wc.wordCounts.keySet()) {
                    if (prsw1.containsKey(s)) {
                        eta+=(Math.log(1-prsw1.get(s))-Math.log(prsw1.get(s)));
                    }
                }
                files.add(new TestFile(f.getName(),1/(1+Math.exp(eta)), "Spam"));

                numFiles+=1;
                if (1/(1+Math.exp(eta))>=0.5) {
                    numTruePositives+=1;
                }
                else {
                    numFalsePositives+=1;
                }
            }

            // accuracy = numCorrectGuesses / numGuesses = (numTruePositives + numTrueNegatives) / numFiles
            // precision = numTruePositives / (numFalsePositives + numTruePositives)
            accuracy = (float)(numTruePositives+numTrueNegatives)/(float)(numFiles);
            precision = (float)(numTruePositives)/(float)(numFalsePositives+numTruePositives);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return files;
    }

    public Map<String, Integer> readFiles(File directory) throws IOException {
        Map<String, Integer> words = new TreeMap<>();
        File[] files = directory.listFiles();

        for (int i=0; i<files.length; i++) {
            WordCounter wordCounter = new WordCounter();

            try {
                wordCounter.processFile(files[i]);
            } catch (IOException e) {
                e.printStackTrace();
            }

            for (String w : wordCounter.wordCounts.keySet()) {
                if (words.containsKey(w)) {
                    words.put(w, words.get(w)+1);
                }
                else {
                    words.put(w, 1);
                }
            }
        }
        return words;
    }

    public static void main(String[] args) {
        launch(args);
    }
}