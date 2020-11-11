import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;

public class CPU {
    private static final int PC_START = 0x200;  //512
    private static final int PC_END = 0xEFF;
    private static final int STACK_SIZE = 16;
    private static final int REGISTER_AMOUNT = 16;

    private final Memory memory;
    private final Display display;
    private final Keyboard keyboard;

    private int pc;     //Program counter
    private final int[] stack;
    private int sp;     //Stack pointer
    private int iReg;   //i register
    private final int[] vReg;    //Registers
    private int delayTimer;
    private int soundTimer;
    private int opcode; //current opcode esay

    private int x;
    private int y;
    private int n;
    private int nn;
    private int nnn;

    private boolean running;
    private boolean drawFlag;


    public CPU(Display display, Keyboard keyboard) {
        memory = new Memory();
        this.display = display;
        this.keyboard = keyboard;

        pc = PC_START;
        stack = new int[STACK_SIZE];
        sp = 0;
        iReg = 0;
        vReg = new int[REGISTER_AMOUNT];
        delayTimer = 0;
        soundTimer = 0;
        opcode = 0;

        running = false;
        drawFlag = false;

        initFont();
    }

    public void initFont() {
        int[] fontSet = new int[]{
                0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
                0x20, 0x60, 0x20, 0x20, 0x70, // 1
                0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
                0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
                0x90, 0x90, 0xF0, 0x10, 0x10, // 4
                0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
                0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
                0xF0, 0x10, 0x20, 0x40, 0x40, // 7
                0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
                0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
                0xF0, 0x90, 0xF0, 0x90, 0x90, // A
                0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
                0xF0, 0x80, 0x80, 0x80, 0xF0, // C
                0xE0, 0x90, 0x90, 0x90, 0xE0, // D
                0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
                0xF0, 0x80, 0xF0, 0x80, 0x80, // F
        };

        memory.set(fontSet, 0);
    }

    public void softReset() {
        display.clear();
        drawFlag = true;

        pc = PC_START;
        Arrays.fill(stack, 0);
        sp = 0;
        iReg = 0;
        Arrays.fill(vReg, 0);
        delayTimer = 0;
        soundTimer = 0;
        opcode = 0;


    }

    public void hardReset() {
        memory.clear(PC_START);
        softReset();
        running = false;
    }

    public void loadRom(String path) {
        memory.clear(PC_START, PC_END);

        try {
            byte[] data = Files.readAllBytes(Paths.get(path));
            int[] buffer = new int[data.length];

            for (int i = 0; i < data.length; ++i) {
                buffer[i] = data[i];
            }

            memory.set(buffer, PC_START);
        } catch (IOException e) {
            e.printStackTrace();
        }

        running = true;
        drawFlag = true;
    }

    public void updateTimers() {
        if (delayTimer > 0) {
            --delayTimer;
        }

        if (soundTimer > 0) {
            if (soundTimer == 1) {
                Toolkit.getDefaultToolkit().beep();
                System.out.println("BEEP");
            }
            --soundTimer;
        }
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isDrawFlag() {
        return drawFlag;
    }

    public void setDrawFlag(boolean drawFlag) {
        this.drawFlag = drawFlag;
    }

    public void cycle() {
        //fetch
        opcode = memory.get(pc) << 8 | memory.get(pc + 1);
        pc += 2;

        //System.out.printf("0x%04X%n", opcode);

        decode();
        reduce();
    }

    public void debug() {
        System.out.println("-----------------");
        System.out.printf("0x%04X%n", opcode);
        System.out.println("PC: " + pc);
        System.out.println("SP: " + sp);
        System.out.println("iReg: " + iReg);
        System.out.println("vReg: " + Arrays.toString(vReg));
        System.out.println("Delay: " + delayTimer);
        System.out.println("Sound: " + soundTimer);

    }

    //reduces int to 8 bit
    private void reduce() {
        vReg[x] &= 0x00FF;
        vReg[y] &= 0x00FF;
    }

    public void decode() {
        x = (opcode & 0x0F00) >> 8;
        y = (opcode & 0x00F0) >> 4;
        n = opcode & 0x000F;
        nn = opcode & 0x00FF;
        nnn = opcode & 0x0FFF;

        switch (opcode & 0xF000) {
            case 0x000:
                switch (opcode & 0x00FF) {
                    case 0xE0:
                        //clear screen
                        clearScreen();
                        break;
                    case 0xEE:
                        //return from subroutine
                        returnSub();
                        break;
                    default:
                        opcodeNotDefined();
                        break;
                }
                break;
            case 0x1000:
                //jump to address
                jump();
                break;
            case 0x2000:
                //call subroutine
                callSub();
                break;
            case 0x3000:
                //skip if register equal value
                skipIfRegEqualVal();
                break;
            case 0x4000:
                //skip if register not equal value
                skipIfRegNotEqualVal();
                break;
            case 0x5000:
                //skip if register equal register
                skipIfRegEqualReg();
                break;
            case 0x6000:
                //set register to value
                setRegToVal();
                break;
            case 0x7000:
                //add value to register
                addValToReg();
                break;
            case 0x8000:
                switch (opcode & 0x000F) {
                    case 0x0000:
                        //set register to register
                        setRegToReg();
                        break;
                    case 0x0001:
                        //bitwise or
                        bitOr();
                        break;
                    case 0x0002:
                        //bitwise and
                        bitAnd();
                        break;
                    case 0x0003:
                        //bitwise xor
                        bitXor();
                        break;
                    case 0x0004:
                        //add register to register
                        addRegToReg();
                        break;
                    case 0x0005:
                        //subtract register from register | VX = VX - VY
                        subRegFromReg();
                        break;
                    case 0x0006:
                        //shift right
                        shiftRight();
                        break;
                    case 0x0007:
                        //subtract register from register reversed | VX = VY - VX
                        subRegFromRegReversed();
                        break;
                    case 0x000E:
                        //shift left
                        shiftLeft();
                        break;
                    default:
                        opcodeNotDefined();
                        break;
                }
                break;
            case 0x9000:
                //skip if register not equal register
                skipIfRegNotEqualReg();
                break;
            case 0xA000:
                //set iReg to value
                setiRegToVal();
                break;
            case 0xB000:
                //jump to address plus register zero
                jumpPlusRegZero();
                break;
            case 0xC000:
                //set registe to random
                setRegToRand();
                break;
            case 0xD000:
                //draw sprite
                drawSprite();
                break;
            case 0xE000:
                switch (opcode & 0x00FF) {
                    case 0x9E:
                        //skip if key pressed
                        skipIfKeyPressed();
                        break;
                    case 0xA1:
                        //skip if key not pressed
                        skipIfKeyNotPressed();
                        break;
                    default:
                        opcodeNotDefined();
                        break;
                }
                break;
            case 0xF000:
                switch (opcode & 0x00FF) {
                    case 0x07:
                        //set register to delay timer
                        setRegToDelay();
                        break;
                    case 0x0A:
                        //wait for keypress
                        waitForKey();
                        break;
                    case 0x15:
                        //set delay timer to register
                        setDelayToReg();
                        break;
                    case 0x18:
                        //set sound timer to register
                        setSoundToReg();
                        break;
                    case 0x1E:
                        //add register to i
                        addRegToiReg();
                        break;
                    case 0x29:
                        //load sprite to i
                        loadSpriteToiReg();
                        break;
                    case 0x30:
                        //load extended sprite to i | Not implemented
                        loadExSpriteToiReg();
                        break;
                    case 0x33:
                        //store bcd
                        storeBCD();
                        break;
                    case 0x55:
                        //store registers
                        storeReg();
                        break;
                    case 0x65:
                        //load registers
                        loadReg();
                        break;
                    default:
                        opcodeNotDefined();
                        break;
                }
                break;
            default:
                opcodeNotDefined();
                break;
        }
    }

    private void opcodeNotDefined() {
        System.err.println("Opcode: " + String.format("0x%04X", opcode) + " was not defined");
    }

    private void clearScreen() {
        display.clear();
        drawFlag = true;
    }

    private void returnSub() {
        pc = stack[--sp];
        drawFlag = true;
    }

    private void jump() {
        pc = nnn;
    }

    private void callSub() {
        stack[sp++] = pc;
        pc = nnn;
    }

    private void skipIfRegEqualVal() {
        if (vReg[x] == nn)
            pc += 2;
    }

    private void skipIfRegNotEqualVal() {
        if (vReg[x] != nn)
            pc += 2;
    }

    private void skipIfRegEqualReg() {
        if (vReg[x] == vReg[y])
            pc += 2;
    }

    private void setRegToVal() {
        vReg[x] = nn;
    }

    private void addValToReg() {
        vReg[x] += nn;
    }

    private void setRegToReg() {
        vReg[x] = vReg[y];
    }

    private void bitOr() {
        vReg[x] = vReg[x] | vReg[y];
    }

    private void bitAnd() {
        vReg[x] = vReg[x] & vReg[y];
    }

    private void bitXor() {
        vReg[x] = vReg[x] ^ vReg[y];
    }

    private void addRegToReg() {
        vReg[x] += vReg[y];

        //set VF to 1 if there is a carry
        if (vReg[x] + vReg[y] > 255) {
            vReg[0xF] = 1;
        } else {
            vReg[0xF] = 0;
        }
    }

    private void subRegFromReg() {
        //set VF to 0 if there is a borrow
        if (vReg[x] > vReg[y]) {
            vReg[0xF] = 1;
        } else {
            vReg[0xF] = 0;
        }

        vReg[x] -= vReg[y];
    }

    private void shiftRight() {
        //store lsb to VF
        vReg[0xF] = vReg[x] & 0x1;

        vReg[x] = vReg[x] >> 1;
    }

    private void subRegFromRegReversed() {
        //set VF to 0 if there is a borrow
        if (vReg[y] > vReg[x]) {
            vReg[0xF] = 1;
        } else {
            vReg[0xF] = 0;
        }

        vReg[x] = vReg[y] - vReg[x];
    }

    private void shiftLeft() {
        //store msb to VF
        vReg[0xF] = vReg[x] >> 7;

        vReg[x] = vReg[x] << 1;
    }

    private void skipIfRegNotEqualReg() {
        if (vReg[x] != vReg[y]) {
            pc += 2;
        }
    }

    private void setiRegToVal() {
        iReg = nnn;
    }

    private void jumpPlusRegZero() {
        pc = nnn + vReg[0x0];
    }

    private void setRegToRand() {
        vReg[x] = new Random().nextInt() % (0x100) & nn;
    }

    private void drawSprite() {
        vReg[0xF] = 0;

        for (int yLine = 0; yLine < n; ++yLine) {

            int pixel = memory.get(iReg + yLine);

            for (int xLine = 0; xLine < 8; ++xLine) {
                //a pixel gets drawn, if a bit is 1
                if ((pixel & (0x80 >> xLine)) != 0) {

                    //if the coordinate is greater than the screen
                    //int xCoord = vReg[x] + xLine % 64;
                    //int yCoord = vReg[y] + yLine % 32;
                    int xCoord = vReg[x] + xLine;
                    int yCoord = vReg[y] + yLine;

                    if (display.getPixel(xCoord, yCoord) == 1) {
                        vReg[0xF] = 1;
                    }

                    display.setPixel(xCoord, yCoord);
                }
            }
        }

        drawFlag = true;
    }

    private void skipIfKeyPressed() {
        if (keyboard.isPressed(x)) {
            pc += 2;
        }
    }

    private void skipIfKeyNotPressed() {
        if (!keyboard.isPressed(x)) {
            pc += 2;
        }
    }

    private void setRegToDelay() {
        vReg[x] = delayTimer;
    }

    private void waitForKey() {
        for (int i = 0; i <= 0xF; ++i) {
            if (keyboard.isPressed(i)) {
                System.out.println("Key: " + i);
                vReg[x] = i;
                keyboard.setKey(i, false);
                return;
            }
        }

        //try again if no key was pressed
        pc -= 2;
    }

    private void setDelayToReg() {
        delayTimer = vReg[x];
    }

    private void setSoundToReg() {
        soundTimer = vReg[x];
    }

    private void addRegToiReg() {
        iReg += vReg[x];
    }

    private void loadSpriteToiReg() {
        iReg = vReg[x] * 0x5;
        drawFlag = true;
    }

    private void loadExSpriteToiReg() {
        //empty
    }

    private void storeBCD() {
        memory.set(vReg[x] / 100, iReg);
        memory.set(vReg[x] % 100 / 10, iReg + 1);
        memory.set(vReg[x] % 10, iReg + 2);
    }

    private void storeReg() {
        for (int i = 0; i <= x; ++i) {
            memory.set(vReg[i], iReg + i);
        }
    }

    private void loadReg() {
        for (int i = 0; i <= x; ++i) {
            vReg[i] = memory.get(iReg + i);
        }
    }
}

