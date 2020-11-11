import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;


import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class Main extends Application {
    private static Stage stage;

    private CPU cpu;
    private Display display;
    private Keyboard keyboard;

    private final ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(2);
    private ScheduledFuture<?> cpuThread;
    private ScheduledFuture<?> displayThread;

    private boolean debug = false;


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;

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
        MenuItem debugTrueItem = new MenuItem("Enable debug");
        MenuItem debugFalseItem = new MenuItem("Disable debug");
        optionMenu.getItems().addAll(cpuSpeedItem, debugTrueItem, debugFalseItem);

        MenuBar menuBar = new MenuBar(fileMenu, optionMenu);

        //Emulator
        display = new Display(800, 400);
        keyboard = new Keyboard();
        cpu = new CPU(display, keyboard);

        //Keyboard handler
        scene.setOnKeyPressed(event -> keyboard.pressKey(event.getCode()));
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
        debugTrueItem.setOnAction(event -> debug = true);
        debugFalseItem.setOnAction(event -> debug = false);
    }

    private void loadRom() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        File rom = fileChooser.showOpenDialog(stage);

        if (rom == null) {
            return;
        }

        stopEmulation();

        cpu.loadRom(rom.getPath());

        startEmulation();
    }

    private void startEmulation() {
        cpuThread = threadPool.scheduleWithFixedDelay(() -> {
            cpu.cycle();

            if (debug) {
                cpu.debug();
            }
        }, 2, 2, TimeUnit.MILLISECONDS);

        displayThread = threadPool.scheduleWithFixedDelay(() -> {
            cpu.updateTimers();
            if (cpu.isDrawFlag()) {
                Platform.runLater(() -> {
                    display.render();
                    cpu.setDrawFlag(false);
                });
            }
        }, 17, 17, TimeUnit.MILLISECONDS);
    }

    private void reset() {
        if (!cpu.isRunning())
            return;

        if (cpuThread != null) {
            cpuThread.cancel(true);
            displayThread.cancel(true);
        }

        cpu.softReset();
        startEmulation();
    }

    private void stopEmulation() {
        if (!cpu.isRunning())
            return;

        if (cpuThread != null) {
            cpuThread.cancel(true);
            displayThread.cancel(true);
        }

        cpu.hardReset();
        display.render();
    }

    public void stopPool() {
        threadPool.shutdownNow();
    }

    private void changeCpuSpeed() {

    }

    public void stop() {
        stopEmulation();
        stopPool();
    }

}
