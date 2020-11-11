import java.util.Arrays;

public class Memory {
    private final int[] memory;
    private static final int MEMORY_SIZE = 0x1000;


    public Memory() {
        memory = new int[0x1000];  //4096
    }

    public int get(int pos) {
        return memory[pos] & 0xFF;
    }

    public int[] get(int start, int end) {
        int[] temp = new int[end - start + 1];

        System.arraycopy(memory, start, temp, 0, end - start + 1);

        return temp;
    }

    public void set(int value, int pos) {
        memory[pos] = value;
    }

    public void set(int[] values, int pos) {
        System.arraycopy(values, 0, memory, pos, values.length);
    }

    public void clear(int start, int end) {
        Arrays.fill(memory, start, end, 0);
    }

    public void clear(int start) {
        Arrays.fill(memory, start, MEMORY_SIZE - 1, 0);
    }

    public void clear() {
        Arrays.fill(memory, 0);
    }

    public void print() {
        System.out.println(Arrays.toString(memory));
    }
}
