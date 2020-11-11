import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;


public class Main extends Application {
    private static Stage stage;

    private CPU cpu;
    private Display display;
    private Keyboard keyboard;

    private double cpuSpeed = 500;
    private Timeline gameLoop;

    private boolean debug = false;


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;

        initEmulator();

        BorderPane root = new BorderPane();
        Scene scene = new Scene(root, 800, 450);

        //MenuBar
        Menu fileMenu = new Menu("File");
        MenuItem loadRomItem = new MenuItem("Load rom");
        MenuItem resetItem = new MenuItem("Reset");
        MenuItem stopItem = new MenuItem("Stop");
        fileMenu.getItems().addAll(loadRomItem, resetItem, stopItem);

        Menu optionMenu = new Menu("Options");
        MenuItem cpuSpeedItem = new MenuItem("Change CPU speed");
        optionMenu.getItems().addAll(cpuSpeedItem);

        MenuBar menuBar = new MenuBar(fileMenu, optionMenu);


        //Emulator
        display = new Display(800, 400);
        keyboard = new Keyboard();
        cpu = new CPU(display, keyboard);

        //Keyboard handler
        scene.setOnKeyPressed(event -> {
            System.out.println("Pressed: " + event.getText());
            keyboard.pressKey(event.getCode());
        });
        scene.setOnKeyReleased(event -> keyboard.releaseKey(event.getCode()));

        //View
        root.setTop(menuBar);
        root.setCenter(display);
        primaryStage.setScene(scene);
        primaryStage.setTitle("CHIP-8");
        primaryStage.setResizable(false);
        primaryStage.show();

        //Bind MenuItems
        loadRomItem.setOnAction(event -> loadRom());
        resetItem.setOnAction(event -> reset());
        stopItem.setOnAction(event -> stopEmulation());
        cpuSpeedItem.setOnAction(event -> changeCpuSpeed());
    }
    //TODO relpace with threads
    private void initEmulator() {
        gameLoop = new Timeline();
        KeyFrame cpuKeyFrame = new KeyFrame(Duration.seconds(1 / cpuSpeed), event -> {
            try {
                cpu.cycle();

                if (debug) {
                    cpu.debug();
                }

                if (cpu.isDrawFlag()) {
                    display.render();
                    cpu.setDrawFlag(false);
                }

            } catch (RuntimeException e) {
                gameLoop.stop();
            }
        });

        KeyFrame timerKeyFrame = new KeyFrame(Duration.millis(16), event -> cpu.updateTimers());


        gameLoop.setCycleCount(Timeline.INDEFINITE);
        gameLoop.getKeyFrames().addAll(cpuKeyFrame, timerKeyFrame);
    }


    private void loadRom() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        File rom = fileChooser.showOpenDialog(stage);

        if (rom == null) {
            return;
        }

        stopEmulation();

        initEmulator();

        cpu.loadRom(rom.getPath());

        gameLoop.play();
    }

    private void reset() {
        if (!cpu.isRunning())
            return;

        initEmulator();

        gameLoop.stop();
        cpu.softReset();
        gameLoop.play();
    }

    private void stopEmulation() {
        gameLoop.stop();
        cpu.hardReset();
        display.render();
    }

    private void changeCpuSpeed() {

    }

}
