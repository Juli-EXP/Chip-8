import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Display extends Canvas {
    private static final int WIDTH = 64;
    private static final int HEIGHT = 32;
    private static final int scale = 12;

    private final int[][] graphic = new int[WIDTH][HEIGHT];

    private final GraphicsContext gc;

    public Display(double width, double height) {
        super(width, height);
        setFocusTraversable(true);

        gc = this.getGraphicsContext2D();
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, width, height);
        clear();
    }

    public void clear() {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                graphic[x][y] = 0;
            }
        }
    }

    public void render() {
        for (int x = 0; x < graphic.length; x++) {
            for (int y = 0; y < graphic[y].length; y++) {
                if (graphic[x][y] == 1) {
                    gc.setFill(Color.WHITE);
                } else {
                    gc.setFill(Color.BLACK);
                }

                gc.fillRect(x * scale, y * scale, scale, scale);
            }
        }
    }

    public int getPixel(int x, int y) {
        return graphic[x][y];
    }

    public void setPixel(int x, int y) {
        graphic[x][y] ^= 1;
    }
}
