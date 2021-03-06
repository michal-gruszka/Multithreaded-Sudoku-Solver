package gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import loader.CsvLoader;
import model.Grid;
import model.SolutionListener;
import solver.Solver;
import solver.SolverThread;
import utils.InvalidSudokuException;

import java.io.File;

public class GUI extends Application implements SolutionListener {

    private static final int WINDOW_WIDTH = 350;
    private static final int WINDOW_HEIGHT = 550;
    private static final int MINIMUM_CLUES_REQUIRED = 17;
    private TextField[] cells = new TextField[81];
    private boolean isNonSolved = true;
    private Label messageDisplay;
    private Label threadsUsedLabel;

    @Override
    public void start(Stage primaryStage) {

        primaryStage.setTitle("Sudoku Solver");

        GridPane gpLayout = new GridPane();
        gpLayout.setPadding(new Insets(20, 20, 20, 20));
        gpLayout.setHgap(6);
        gpLayout.setVgap(8);

        MenuBar menuBar = createMenuBar(primaryStage);

        createInputCellsIn(gpLayout);

        messageDisplay = new Label("Enter sudoku manually or import from CSV file");
        Button solveButton = createSolveButton();
        Button clearButton = createClearButton();
        threadsUsedLabel = new Label("Threads used: -");

        VBox v = new VBox(10);
        v.getChildren().addAll(menuBar, gpLayout, solveButton, clearButton, messageDisplay, threadsUsedLabel);
        v.setAlignment(Pos.TOP_CENTER);
        primaryStage.setScene(new Scene(v, WINDOW_WIDTH, WINDOW_HEIGHT));
        primaryStage.show();
    }

    private MenuBar createMenuBar(Stage primaryStage) {

        MenuBar menuBar = new MenuBar();
        Menu menuFile = new Menu("File");
        MenuItem add = new MenuItem("Import from CSV");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Sudoku CSV file");

        add.setOnAction(t -> {
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                isNonSolved = true;
                int[] values = new CsvLoader().load(file.toString());
                fillWithValues(cells, values);
            }
        });

        menuFile.getItems().add(add);
        menuBar.getMenus().add(menuFile);

        return menuBar;
    }

    private void createInputCellsIn(GridPane gpLayout) {
        for (int i = 0; i < cells.length; i++) {
            TextField field = new TextField();
            cells[i] = field;
            field.setFont(Font.font("Verdana", FontWeight.BOLD, 14));
            gpLayout.getChildren().add(field);
            GridPane.setConstraints(field, i % 9, i / 9);
        }
    }

    private Button createSolveButton() {

        Button button = new Button("Solve!");

        button.setOnAction(event -> {
            try {
                if (this.isNonSolved) {
                    int[] sudokuArray = getSudokuIntArrayFrom(cells);
                    if (sudokuArray != null) {
                        Grid grid;
                        grid = new Grid(sudokuArray);
                        Solver solver = new Solver(grid);
                        SolverThread solverThread = new SolverThread(solver);
                        solverThread.registerListener(this);
                        solverThread.setThreadsUsed(0);
                        Thread thread = new Thread(solverThread);
                        thread.start();
                    }
                } else {
                    messageDisplay.setText("Sudoku is already solved");
                }
            }catch(InvalidSudokuException e){
                messageDisplay.setText("Invalid sudoku");
            }
        });

        return button;
    }

    private Button createClearButton() {

        Button button = new Button("Clear");

        button.setOnAction(event -> {
            threadsUsedLabel.setText("Threads used: -");
            messageDisplay.setText("Enter sudoku manually or import from CSV file");
            isNonSolved = true;
            for (TextField cell : cells)
                cell.setText("");
        });

        return button;
    }

    private void fillWithValues(TextField[] cells, int[] values) {
        for (int i = 0; i < cells.length; i++) {
            if(values[i] == 0) {
                cells[i].setText("");
            } else {
                cells[i].setText(String.valueOf(values[i]));
            }
        }
    }

    private int[] getSudokuIntArrayFrom(TextField[] cells) {
        int[] values = new int[cells.length];
        int clues = 0;

        for (int i = 0; i < values.length; i++) {
            int val;
            String str = cells[i].getCharacters().toString();
            if (str.equals("") || str.equals("0")) {
                val = 0;
            } else if (str.matches("^[1-9]$")) {
                val = Integer.valueOf(str);
                clues++;
            } else {
                throw new InvalidSudokuException();
            }

            values[i] = val;
        }

        if (clues < MINIMUM_CLUES_REQUIRED) throw new InvalidSudokuException();

        return values;
    }

    @Override
    public void solutionFound(int[] solution, int threadsUsed) {

        Platform.runLater(() -> {
            fillWithValues(cells, solution);
            setThreadsCounter(threadsUsed);
        });

        this.isNonSolved = false;
    }

    private void setThreadsCounter(int threadsUsed) {
        threadsUsedLabel.setText("Threads used: " + String.valueOf(threadsUsed));
    }
}
