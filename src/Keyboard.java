import javafx.scene.input.KeyCode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Keyboard {
    private boolean[] keys;
    private Map<String, Integer> keyMap = new HashMap<>();

    public Keyboard() {
        keys = new boolean[16];
        Arrays.fill(keys, false);
        keyMap.put("1", 1);
        keyMap.put("2", 2);
        keyMap.put("3", 3);
        keyMap.put("4", 12);
        keyMap.put("q", 4);
        keyMap.put("w", 5);
        keyMap.put("e", 6);
        keyMap.put("r", 13);
        keyMap.put("a", 7);
        keyMap.put("s", 8);
        keyMap.put("d", 9);
        keyMap.put("f", 14);
        keyMap.put("z", 10);
        keyMap.put("x", 0);
        keyMap.put("c", 11);
        keyMap.put("v", 15);
    }

    public void pressKey(KeyCode k) {
        if(keyMap.containsKey(k.getName().toLowerCase())){
            keys[keyMap.get(k.getName().toLowerCase())] = true;
        }
    }

    public void releaseKey(KeyCode k) {
        if(keyMap.containsKey(k.getName().toLowerCase())){
            keys[keyMap.get(k.getName().toLowerCase())] = false;
        }
    }

    public boolean isPressed(int i) {
        return keys[i];
    }
}
