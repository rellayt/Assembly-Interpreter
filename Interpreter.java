package interpreter;

import cpuscheduler.*;
import jdk.jshell.spi.ExecutionControl;
import jdk.swing.interop.SwingInterOpUtils;
import memory.virtual.VirtualMemory;
import shell.Process;

import java.io.*;
import java.util.HashMap;
import java.util.*;
import java.lang.String;

/**
 * <h1>KnotOS Interpreter</h1>
 *
 * @author Zbigniew Jaryst
 * @version 1.0
 * @since 12.2019
 * This code is a project for Operating Systems 2019 subject.
 */

public class Interpreter {
    private HashMap<Integer, String> instructionMap = new HashMap<Integer, String>();
    private Vector<String> lines = new Vector<String>();
    private Vector<Byte> data = new Vector<Byte>();
    private VirtualMemory memory;
    byte singleByte;
    private PCB process;
    File file;
    byte[] arrayByte;
    int instructionNumber = 0;
    boolean stepMode = true;
    byte[] InterepterMemory = new byte[128];
    //liczba rozkazow, ilosc bajtow
    private HashMap<Integer, Integer> instructionHash = new HashMap<Integer, Integer>();

    public Interpreter() {

    }

    public Interpreter(File file, PCB process, VirtualMemory memory) {
        this.process = process;
        this.memory = memory;
        //Added mnemonics with machine codes
        instructionMap.put(1, "ADD"); //4 Bytes - R - L
        instructionMap.put(2, "SUB"); //4 Bytes - R - L
        instructionMap.put(3, "MUL"); //4 Bytes - R - L
        instructionMap.put(4, "INC"); //2 Bytes - Normnal, 3 - Logical
        instructionMap.put(5, "DEC"); //2 Bytes - Normnal, 3 - Logical
        instructionMap.put(6, "MOV"); //4 Bytes - R + 20
        instructionMap.put(7, "MVI"); //4 Bytes
        instructionMap.put(8, "RES"); //1 Byte
        instructionMap.put(9, "JMP"); //3 Bytes
        instructionMap.put(10, "JAXZ"); //3 Bytes
        instructionMap.put(11, "JIZ"); //3 Bytes
        instructionMap.put(12, "JINZ"); //3 Bytes
        instructionMap.put(13, "CP"); //3 Bytes
        instructionMap.put(14, "DP"); //2 Bytes
        instructionMap.put(19, "HLT"); //1 Byte
        instructionMap.put(20, ""); //2 Bytes, values
        //Added registers with machine codes
        instructionMap.put(15, "AX"); //1 Byte
        instructionMap.put(16, "BX"); //1 Byte
        instructionMap.put(17, "CX"); //1 Byte
        instructionMap.put(18, "DX"); //1 Byte
        this.file = file;
        Vector<Byte> Bytes = new Vector<Byte>();
        Bytes = getBytesFromFile(file);
        arrayByte = new byte[Bytes.size()];
        for (int i = 0; i < Bytes.size(); i++) {
            arrayByte[i] = Bytes.get(i);
           // InterepterMemory[i] = Bytes.get(i);
        }
        this.memory.load(process.PID, arrayByte.length, 0, arrayByte);
    }

    void updateMemory (int length)
    {
        this.file = file;
        Vector<Byte> Bytes = new Vector<Byte>();
        Bytes = getBytesFromFile(file);
        for(int i=process.programCounter; i < process.programCounter + length; i++)
            InterepterMemory[i] = Bytes.get(i);
    }
    public PCB getPcb() {
        return process;
    }

    public void showMemory() {
        System.out.println(Arrays.toString(InterepterMemory));
    }

    void writeMemory(int offset, byte value) {
        InterepterMemory[offset] = value;
    }

    byte readMemory(int offset) {
        byte a = InterepterMemory[offset];
        return a;
    }

    public void showRegisters()
    {
        System.out.println(process.registers.toString());
    }

    /**
     * The method is responsible for calling the rest of the methods needed.
     */
    public void runInterpreter() {
        int limit = memory.getLimit(process.PID, true) - 1;
        while (process.programCounter <= limit) {

           // if (Process.getStepMode()) System.out.println(process.registers.toString());
            showLine(process.programCounter);


            instructionExecute();
            //System.out.println(process.registers.toString());
            if(stepMode)
                break;
            /*if (Process.getStepMode()) {
                break;
            }
            if (process.state == State.TERMINATED) {
                break;
            }*/
        }
        /*if (!Process.getStepMode())
            System.out.println(process.registers.toString());*/

    }

    byte toByte(String instruction) {
        byte variable = 0;
        for (HashMap.Entry<Integer, String> entry : instructionMap.entrySet()) {
            if (entry.getValue().equals(instruction)) {
                variable = entry.getKey().byteValue();
            }
        }
        return variable;
    }

    public void changeStepMode() {
        stepMode = false;
    }

    /**
     * Method shows current process with specific address
     *
     * @param offset logical address
     */
    void showLine(int offset) {
        System.out.print(process.programCounter + ": " + byteInstructionToMnemonic(process, offset));
    }

    /**
     * Method checks if given instruction is single integer
     *
     * @param word
     * @return
     */
    boolean isInteger(String word) {
        try {
            int number = Integer.parseInt(word);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Method translates received instruction to vector bytes
     *
     * @param file
     * @return
     */
    public Vector<Byte> getBytesFromFile(File file) {
        lines = fileToLines(file);
        char space = ' ';
        String instruction = "";
        String firstParameter = "";
        String second_parameter = "";
        boolean Bracket = false;

        Vector<Byte> byteInstruction = new Vector<Byte>();
        int i = 0;

        for (String singleLine : lines) {
            if (isInteger(singleLine)) {
                data.add((byte) 20);
                data.add((byte) Integer.parseInt(singleLine));
            } else {
                if (singleLine.equals("RES") || singleLine.equals("HLT")) {
                    singleByte = toByte(singleLine);
                    data.add(singleByte);
                } else if (!singleLine.equals("HLT") || !singleLine.equals("RES")) {
                    //Receiving a single command
                    while (singleLine.charAt(i) != space) {
                        instruction += singleLine.charAt(i);
                        i++;
                    }
                    if (instruction.equals("ADD") || instruction.equals("SUB") || instruction.equals("MUL") || instruction.equals("MOV")) {
                        singleByte = toByte(instruction);
                        byteInstruction.add(singleByte);
                        i = 4;
                        while (singleLine.charAt(i) != space) {
                            firstParameter += singleLine.charAt(i);
                            i++;
                        }
                        if (firstParameter.charAt(0) == '[')
                            Bracket = true;
                        else
                        {
                            singleByte = toByte(firstParameter);
                            byteInstruction.add(singleByte);
                            i = 7;
                        }

                        if (Bracket) {
                            i = instruction.length() + 1 + firstParameter.length() + 1;
                            while (i < singleLine.length()) {
                                second_parameter += singleLine.charAt(i);
                                i++;
                            }
                        } else {
                            while (i < singleLine.length()) {
                                second_parameter += singleLine.charAt(i);
                                i++;
                            }
                        }


                        if (firstParameter.charAt(0) == '[') {
                            byte value = byteInstruction.get(0);
                            value += (byte) 60;
                            byteInstruction.set(0, value);
                            String number = "";
                            for (int j = 1; j < firstParameter.length() - 1; j++) {
                                number += firstParameter.charAt(j);
                            }
                            if (number.length() == 1 || number.length() == 2) {
                                byteInstruction.add((byte) 0);
                                byteInstruction.add((byte) Integer.parseInt(number));
                            }
                            if (number.length() == 3) {
                                byteInstruction.add((byte) Integer.parseInt(number.substring(0, 1)));
                                byteInstruction.add((byte) Integer.parseInt(number.substring(1, 3)));
                            }
                        }


                        if (second_parameter.charAt(0) == '[' && byteInstruction.get(0) != (byte) 60) {
                            String number = "";
                            String number2 = "";
                            byte value = byteInstruction.get(0);
                            value += (byte) 40;
                            byteInstruction.set(0, value);
                            for (int j = 1; j < second_parameter.length() - 1; j++) {
                                number += second_parameter.charAt(j);
                            }
                            if (number.length() == 1 || number.length() == 2) {
                                byteInstruction.add((byte) 0);
                                byteInstruction.add((byte) Integer.parseInt(number));
                            }
                            if (number.length() == 3) {
                                number2 = "0" + number.charAt(0);
                                byteInstruction.add((byte) Integer.parseInt(number2));
                                byteInstruction.add((byte) Integer.parseInt(number.substring(1, 3)));
                            }
                            if (number.length() == 4) {
                                throw new IllegalArgumentException("No specified logical address!");
                            }
                        }
                        if (second_parameter.equals("AX") || second_parameter.equals("BX") || second_parameter.equals("CX") || second_parameter.equals("DX")) {
                            byte value = byteInstruction.get(0);
                            if (value < (byte) 60) {
                                value += (byte) 20;
                                byteInstruction.set(0, value);
                            }
                            singleByte = toByte(second_parameter);
                            byteInstruction.add(singleByte);
                            if(!Bracket)
                                byteInstruction.add((byte) 0);
                        } else { //CHECKS IF SECOND PARAMETR IS NUMBER
                            if (second_parameter.length() == 1 || second_parameter.length() == 2) {
                                byteInstruction.add((byte) 0);
                                byteInstruction.add((byte) Integer.parseInt(second_parameter));
                            }
                            if (second_parameter.length() == 3) {
                                byteInstruction.add((byte) Integer.parseInt(second_parameter.substring(0, 1)));
                                byteInstruction.add((byte) Integer.parseInt(second_parameter.substring(1, 3)));
                            }
                            if (second_parameter.length() == 4) {
                                byteInstruction.add((byte) Integer.parseInt(second_parameter.substring(0, 2)));
                                byteInstruction.add((byte) Integer.parseInt(second_parameter.substring(2, 4)));
                            }
                        }
                        Bracket = false;
                        firstParameter = "";
                        second_parameter = "";
                        for (byte a : byteInstruction)
                            data.add(a);
                        byteInstruction.clear();
                    } else if (instruction.equals("MVI")) {
                        singleByte = toByte(instruction);
                        byteInstruction.add(singleByte);
                        i = 4;
                        while (singleLine.charAt(i) != space) {
                            firstParameter += singleLine.charAt(i);
                            i++;
                        }
                        i += 1;
                        while (i < singleLine.length()) {
                            second_parameter += singleLine.charAt(i);
                            i++;
                        }
                        if (firstParameter.charAt(0) == '[') {
                            byte value = byteInstruction.get(0);
                            value += (byte) 40;
                            byteInstruction.set(0, value);
                            String number = "";
                            for (int j = 1; j < firstParameter.length() - 1; j++) {
                                number += firstParameter.charAt(j);
                            }
                            if (number.length() == 1 || number.length() == 2) {
                                byteInstruction.add((byte) Integer.parseInt(number));
                            }
                            if (number.length() == 3) {
                                byteInstruction.add((byte) Integer.parseInt(number.substring(0, 1)));
                                byteInstruction.add((byte) Integer.parseInt(number.substring(1, 3)));
                            }
                            if (second_parameter.length() == 1 || second_parameter.length() == 2) {
                                byteInstruction.add((byte) 0);
                                byteInstruction.add((byte) Integer.parseInt(second_parameter));
                            }
                            if (second_parameter.length() == 3) {
                                byteInstruction.add((byte) Integer.parseInt(second_parameter.substring(0, 1)));
                                byteInstruction.add((byte) Integer.parseInt(second_parameter.substring(1, 3)));
                            }
                            if (second_parameter.length() == 4) {
                                byteInstruction.add((byte) Integer.parseInt(second_parameter.substring(0, 2)));
                                byteInstruction.add((byte) Integer.parseInt(second_parameter.substring(2, 4)));
                            }
                        } else if (firstParameter.charAt(0) == 'A' || firstParameter.charAt(0) == 'B' || firstParameter.charAt(0) == 'C' || firstParameter.charAt(0) == 'D') {
                            singleByte = toByte(firstParameter);
                            byteInstruction.add(singleByte);
                            if (second_parameter.length() == 1 || second_parameter.length() == 2) {
                                byteInstruction.add((byte) 0);
                                byteInstruction.add((byte) Integer.parseInt(second_parameter));
                            }
                            if (second_parameter.length() == 3) {
                                byteInstruction.add((byte) Integer.parseInt(second_parameter.substring(0, 1)));
                                byteInstruction.add((byte) Integer.parseInt(second_parameter.substring(1, 3)));
                            }
                            if (second_parameter.length() == 4) {
                                byteInstruction.add((byte) Integer.parseInt(second_parameter.substring(0, 2)));
                                byteInstruction.add((byte) Integer.parseInt(second_parameter.substring(2, 4)));
                            }
                        }
                        firstParameter = "";
                        second_parameter = "";
                        for (byte a : byteInstruction)
                            data.add(a);
                        byteInstruction.clear();
                    } else if (instruction.equals("INC") || instruction.equals("DEC")) {
                        singleByte = toByte(instruction);
                        byteInstruction.add(singleByte);

                        i = 4;
                        while (i < singleLine.length()) {
                            firstParameter += singleLine.charAt(i);
                            i++;
                        }
                        if (firstParameter.charAt(0) == '[') {
                            String number = "";
                            String number2 = "";
                            byte value = byteInstruction.get(0);
                            value += (byte) 40;
                            byteInstruction.set(0, value);
                            for (int j = 1; j < firstParameter.length() - 1; j++) {
                                number += firstParameter.charAt(j);
                            }
                            if (number.length() == 1 || number.length() == 2) {
                                byteInstruction.add((byte) 0);
                                byteInstruction.add((byte) Integer.parseInt(number));
                            }
                            if (number.length() == 3) {
                                number2 = "0" + number.charAt(0);
                                byteInstruction.add((byte) Integer.parseInt(number2));
                                byteInstruction.add((byte) Integer.parseInt(number.substring(1, 3)));
                            }
                            if (number.length() == 4) {
                                throw new IllegalArgumentException("Nie ma określonego adresu logicznego!");
                            }
                        }
                        if (firstParameter.equals("AX") || firstParameter.equals("BX") || firstParameter.equals("CX") || firstParameter.equals("DX")) {
                            singleByte = toByte(firstParameter);
                            byteInstruction.add(singleByte);
                        }
                        firstParameter = "";
                    } else if (instruction.equals("JMP") || instruction.equals("JAXZ") || instruction.equals("JIZ") || instruction.equals("JINZ")) {
                        singleByte = toByte(instruction);
                        byteInstruction.add(singleByte);

                        i = 4;
                        if (instruction.equals("JAXZ") || instruction.equals("JINZ"))
                            i = 5;
                        while (i < singleLine.length()) {
                            firstParameter += singleLine.charAt(i);
                            i++;
                        }
                        if (firstParameter.charAt(0) == '[') {
                            String number = "";
                            String number2 = "";
                            for (int j = 1; j < firstParameter.length() - 1; j++) {
                                number += firstParameter.charAt(j);
                            }
                            if (number.length() == 1 || number.length() == 2) {
                                byteInstruction.add((byte) 0);
                                byteInstruction.add((byte) Integer.parseInt(number));
                            }
                            if (number.length() == 3) {
                                number2 = "0" + number.charAt(0);
                                byteInstruction.add((byte) Integer.parseInt(number2));
                                byteInstruction.add((byte) Integer.parseInt(number.substring(1, 3)));
                            }
                            if (number.length() == 4) {
                                throw new IllegalArgumentException("Nie ma określonego adresu logicznego!");
                            }
                        }
                        firstParameter = "";
                    } else if (instruction.equals("CP")) {
                        singleByte = toByte(instruction);
                        byteInstruction.add(singleByte);

                        //   data.add(singleByte);
                        i = 3;
                        while (singleLine.charAt(i) != space) {
                            firstParameter += singleLine.charAt(i);
                            i++;
                        }
                        byteInstruction.add((byte) Integer.parseInt(firstParameter));

                        i = 5;
                        while (i < singleLine.length()) {
                            second_parameter += singleLine.charAt(i);
                            i++;
                        }
                        byteInstruction.add((byte) Integer.parseInt(second_parameter));

                        firstParameter = "";
                        second_parameter = "";
                    } else if (instruction.equals("DP")) {
                        singleByte = toByte(instruction);
                        byteInstruction.add(singleByte);
                        //data.add(singleByte);

                        i = 3;
                        while (i < singleLine.length()) {
                            firstParameter += singleLine.charAt(i);
                            i++;
                        }
                        byteInstruction.add((byte) Integer.parseInt(firstParameter));

                        firstParameter = "";
                    } else {
                        if (Process.getStepMode()) System.out.println("NUMBER: " + instruction);
                    }
                    for (byte a : byteInstruction)
                        data.add(a);
                    byteInstruction.clear();

                }
                instruction = "";
                i = 0;
            }
        }
        /*for (byte a : data)
            System.out.println(a);*/
        return data;
    }

    Vector<Byte> loadBytesToByteInstruction(int PID, int offset) {
        Vector<Byte> oneInstruction = new Vector<Byte>();

        byte oneByte = memory.read(process.PID, offset);
        //Read 4 bytes
        if (oneByte == 1 || oneByte == 2 || oneByte == 3 || oneByte == 26 || oneByte == 7 || oneByte == 21 || oneByte == 22 || oneByte == 23 || oneByte == 41 || oneByte == 42 || oneByte == 43 || oneByte == 47 || oneByte == 66) {
            oneInstruction.add(oneByte);
            for (int i = offset + 1; i < offset + 4; i++) {
                oneByte = memory.read(process.PID, i);
                oneInstruction.add(oneByte);
            }
        }//Read 3 bytes
        else if (oneByte == 44 || oneByte == 45 || oneByte == 9 || oneByte == 10 || oneByte == 11 || oneByte == 12 || oneByte == 13) {
            oneInstruction.add(oneByte);
            for (int i = offset + 1; i < offset + 3; i++) {
                oneByte = memory.read(process.PID, i);
                oneInstruction.add(oneByte);
            }
        }//Read 2 bytes
        else if (oneByte == 4 || oneByte == 5 || oneByte == 14 || oneByte == 20) {
            oneInstruction.add(oneByte);
            for (int i = offset + 1; i < offset + 2; i++) {
                oneByte = memory.read(process.PID, i);
                oneInstruction.add(oneByte);
            }
        }//Read 1 byte
        else if (oneByte == 8 || oneByte == 19) {
            oneInstruction.add(oneByte);
        }
        return oneInstruction;
    }

    public String byteInstructionToMnemonic(PCB process, int offset) {
        Vector<Byte> oneInstruction = new Vector<Byte>();
        oneInstruction = loadBytesToByteInstruction(process.PID, offset);
        String trueInstruction = "";
        if (oneInstruction.get(0) <= 20) {
            if (oneInstruction.get(0) == 1) {
                trueInstruction += "ADD ";
                if (oneInstruction.get(1) == 15) {
                    trueInstruction += "AX ";
                } else if (oneInstruction.get(1) == 16) {
                    trueInstruction += "BX ";
                } else if (oneInstruction.get(1) == 17) {
                    trueInstruction += "CX ";
                } else if (oneInstruction.get(1) == 18) {
                    trueInstruction += "DX ";
                }
                if (oneInstruction.get(2) != 0 && oneInstruction.get(3) != 0) {
                    trueInstruction += Byte.toString(oneInstruction.get(2)) + Byte.toString(oneInstruction.get(3));
                } else if (oneInstruction.get(2) != 0 && oneInstruction.get(3) == 0) {
                    trueInstruction += Byte.toString(oneInstruction.get(2)) + "00";
                } else if (oneInstruction.get(2) == 0 && oneInstruction.get(3) != 0) {
                    trueInstruction += Byte.toString(oneInstruction.get(3));
                } else if (oneInstruction.get(2) == 0 && oneInstruction.get(3) == 0) {
                    trueInstruction += Byte.toString(oneInstruction.get(3));
                }
            } else if (oneInstruction.get(0) == 2) {
                trueInstruction += "SUB ";
                if (oneInstruction.get(1) == 15) {
                    trueInstruction += "AX ";
                } else if (oneInstruction.get(1) == 16) {
                    trueInstruction += "BX ";
                } else if (oneInstruction.get(1) == 17) {
                    trueInstruction += "CX ";
                } else if (oneInstruction.get(1) == 18) {
                    trueInstruction += "DX ";
                }
                if (oneInstruction.get(2) != 0 && oneInstruction.get(3) != 0) {
                    trueInstruction += Byte.toString(oneInstruction.get(2)) + Byte.toString(oneInstruction.get(3));
                } else if (oneInstruction.get(2) != 0 && oneInstruction.get(3) == 0) {
                    trueInstruction += Byte.toString(oneInstruction.get(2)) + "00";
                } else if (oneInstruction.get(2) == 0 && oneInstruction.get(3) != 0) {
                    trueInstruction += Byte.toString(oneInstruction.get(3));
                } else if (oneInstruction.get(2) == 0 && oneInstruction.get(3) == 0) {
                    trueInstruction += Byte.toString(oneInstruction.get(3));
                }
            } else if (oneInstruction.get(0) == 3) {
                trueInstruction += "MUL ";
                if (oneInstruction.get(1) == 15) {
                    trueInstruction += "AX ";
                } else if (oneInstruction.get(1) == 16) {
                    trueInstruction += "BX ";
                } else if (oneInstruction.get(1) == 17) {
                    trueInstruction += "CX ";
                } else if (oneInstruction.get(1) == 18) {
                    trueInstruction += "DX ";
                }
                if (oneInstruction.get(2) != 0 && oneInstruction.get(3) != 0) {
                    trueInstruction += Byte.toString(oneInstruction.get(2)) + Byte.toString(oneInstruction.get(3));
                } else if (oneInstruction.get(2) != 0 && oneInstruction.get(3) == 0) {
                    trueInstruction += Byte.toString(oneInstruction.get(2)) + "00";
                } else if (oneInstruction.get(2) == 0 && oneInstruction.get(3) != 0) {
                    trueInstruction += Byte.toString(oneInstruction.get(3));
                } else if (oneInstruction.get(2) == 0 && oneInstruction.get(3) == 0) {
                    trueInstruction += Byte.toString(oneInstruction.get(3));
                }
            } else if (oneInstruction.get(0) == 4) {
                trueInstruction += "INC ";
                if (oneInstruction.get(1) == 15) {
                    trueInstruction += "AX";
                } else if (oneInstruction.get(1) == 16) {
                    trueInstruction += "BX";
                } else if (oneInstruction.get(1) == 17) {
                    trueInstruction += "CX";
                } else if (oneInstruction.get(1) == 18) {
                    trueInstruction += "DX";
                }
            } else if (oneInstruction.get(0) == 5) {
                trueInstruction += "DEC ";
                if (oneInstruction.get(1) == 15) {
                    trueInstruction += "AX";
                } else if (oneInstruction.get(1) == 16) {
                    trueInstruction += "BX";
                } else if (oneInstruction.get(1) == 17) {
                    trueInstruction += "CX";
                } else if (oneInstruction.get(1) == 18) {
                    trueInstruction += "DX";
                }
            } else if (oneInstruction.get(0) == 7) {
                trueInstruction += "MVI ";
                if (oneInstruction.get(1) == 15) {
                    trueInstruction += "AX ";
                } else if (oneInstruction.get(1) == 16) {
                    trueInstruction += "BX ";
                } else if (oneInstruction.get(1) == 17) {
                    trueInstruction += "CX ";
                } else if (oneInstruction.get(1) == 18) {
                    trueInstruction += "DX ";
                }
                if (oneInstruction.get(2) != 0 && oneInstruction.get(3) != 0) {
                    trueInstruction += Byte.toString(oneInstruction.get(2)) + Byte.toString(oneInstruction.get(3));
                } else if (oneInstruction.get(2) != 0 && oneInstruction.get(3) == 0) {
                    trueInstruction += oneInstruction.get(2) + "00";
                } else if (oneInstruction.get(2) == 0 && oneInstruction.get(3) != 0) {
                    trueInstruction += Byte.toString(oneInstruction.get(3));
                }
            } else if (oneInstruction.get(0) == 8)
                trueInstruction += "RES";
            else if (oneInstruction.get(0) == 9) {
                trueInstruction += "JMP ";
                if (oneInstruction.get(1) == 0 && oneInstruction.get(2) != 0)
                    trueInstruction += "[" + oneInstruction.get(2) + "]";
                else if (oneInstruction.get(1) != 0 && oneInstruction.get(2) != 0)
                    trueInstruction += "[" + oneInstruction.get(1) + oneInstruction.get(2) + "]";
                else if (oneInstruction.get(1) != 0 && oneInstruction.get(2) == 0)
                    trueInstruction += "[" + oneInstruction.get(1) + "00]";
                else if (oneInstruction.get(1) == 0 && oneInstruction.get(2) == 0)
                    trueInstruction += "[0]";
            } else if (oneInstruction.get(0) == 10) {
                trueInstruction += "JAXZ ";
                if (oneInstruction.get(1) == 0 && oneInstruction.get(2) != 0)
                    trueInstruction += "[" + oneInstruction.get(2) + "]";
                else if (oneInstruction.get(1) != 0 && oneInstruction.get(2) != 0)
                    trueInstruction += "[" + oneInstruction.get(1) + oneInstruction.get(2) + "]";
                else if (oneInstruction.get(1) != 0 && oneInstruction.get(2) == 0)
                    trueInstruction += "[" + oneInstruction.get(1) + "00]";
                else if (oneInstruction.get(1) == 0 && oneInstruction.get(2) == 0)
                    trueInstruction += "[0]";
                else if (oneInstruction.get(1) == 0 && oneInstruction.get(2) == 0)
                    trueInstruction += "[0]";
            } else if (oneInstruction.get(0) == 11) {
                trueInstruction += "JIZ ";
                if (oneInstruction.get(1) == 0 && oneInstruction.get(2) != 0)
                    trueInstruction += "[" + oneInstruction.get(2) + "]";
                else if (oneInstruction.get(1) != 0 && oneInstruction.get(2) != 0)
                    trueInstruction += "[" + oneInstruction.get(1) + oneInstruction.get(2) + "]";
                else if (oneInstruction.get(1) != 0 && oneInstruction.get(2) == 0)
                    trueInstruction += "[" + oneInstruction.get(1) + "00]";
                else if (oneInstruction.get(1) == 0 && oneInstruction.get(2) == 0)
                    trueInstruction += "[0]";
            } else if (oneInstruction.get(0) == 12) {
                trueInstruction += "JINZ ";
                if (oneInstruction.get(1) == 0 && oneInstruction.get(2) != 0)
                    trueInstruction += "[" + oneInstruction.get(2) + "]";
                else if (oneInstruction.get(1) != 0 && oneInstruction.get(2) != 0)
                    trueInstruction += "[" + oneInstruction.get(1) + oneInstruction.get(2) + "]";
                else if (oneInstruction.get(1) != 0 && oneInstruction.get(2) == 0)
                    trueInstruction += "[" + oneInstruction.get(1) + "00]";
                else if (oneInstruction.get(1) == 0 && oneInstruction.get(2) == 0)
                    trueInstruction += "[0]";
            } else if (oneInstruction.get(0) == 13) {
                trueInstruction += "CP " + oneInstruction.get(1) + " " + oneInstruction.get(2);
            } else if (oneInstruction.get(0) == 14) {
                trueInstruction += "DP " + oneInstruction.get(1);
            } else if (oneInstruction.get(0) == 19) {
                trueInstruction += "HLT";
            } else if (oneInstruction.get(0) == 20) {
                trueInstruction += oneInstruction.get(1);
            }

        } else if (oneInstruction.get(0) > 20) {
            if (oneInstruction.get(0) == 26) {
                trueInstruction += "MOV ";
                if (oneInstruction.get(1) == 15)
                    trueInstruction += "AX ";
                else if (oneInstruction.get(1) == 16)
                    trueInstruction += "BX ";
                else if (oneInstruction.get(1) == 17)
                    trueInstruction += "CX ";
                else if (oneInstruction.get(1) == 18)
                    trueInstruction += "DX ";

                if (oneInstruction.get(2) == 15)
                    trueInstruction += "AX";
                else if (oneInstruction.get(2) == 16)
                    trueInstruction += "BX";
                else if (oneInstruction.get(2) == 17)
                    trueInstruction += "CX";
                else if (oneInstruction.get(2) == 18)
                    trueInstruction += "DX";
            } else if (oneInstruction.get(0) == 21) {
                trueInstruction += "ADD ";
                if (oneInstruction.get(1) == 15)
                    trueInstruction += "AX ";
                else if (oneInstruction.get(1) == 16)
                    trueInstruction += "BX ";
                else if (oneInstruction.get(1) == 17)
                    trueInstruction += "CX ";
                else if (oneInstruction.get(1) == 18)
                    trueInstruction += "DX ";

                if (oneInstruction.get(2) == 15)
                    trueInstruction += "AX";
                else if (oneInstruction.get(2) == 16)
                    trueInstruction += "BX";
                else if (oneInstruction.get(2) == 17)
                    trueInstruction += "CX";
                else if (oneInstruction.get(2) == 18)
                    trueInstruction += "DX";
            } else if (oneInstruction.get(0) == 22) {
                trueInstruction += "SUB ";
                if (oneInstruction.get(1) == 15)
                    trueInstruction += "AX ";
                else if (oneInstruction.get(1) == 16)
                    trueInstruction += "BX ";
                else if (oneInstruction.get(1) == 17)
                    trueInstruction += "CX ";
                else if (oneInstruction.get(1) == 18)
                    trueInstruction += "DX ";

                if (oneInstruction.get(2) == 15)
                    trueInstruction += "AX";
                else if (oneInstruction.get(2) == 16)
                    trueInstruction += "BX";
                else if (oneInstruction.get(2) == 17)
                    trueInstruction += "CX";
                else if (oneInstruction.get(2) == 18)
                    trueInstruction += "DX";
            } else if (oneInstruction.get(0) == 23) {
                trueInstruction += "MUL ";
                if (oneInstruction.get(1) == 15)
                    trueInstruction += "AX ";
                else if (oneInstruction.get(1) == 16)
                    trueInstruction += "BX ";
                else if (oneInstruction.get(1) == 17)
                    trueInstruction += "CX ";
                else if (oneInstruction.get(1) == 18)
                    trueInstruction += "DX ";

                if (oneInstruction.get(2) == 15)
                    trueInstruction += "AX";
                else if (oneInstruction.get(2) == 16)
                    trueInstruction += "BX";
                else if (oneInstruction.get(2) == 17)
                    trueInstruction += "CX";
                else if (oneInstruction.get(2) == 18)
                    trueInstruction += "DX";
            } else if (oneInstruction.get(0) == 41) {
                trueInstruction += "ADD ";
                if (oneInstruction.get(1) == 15)
                    trueInstruction += "AX ";
                else if (oneInstruction.get(1) == 16)
                    trueInstruction += "BX ";
                else if (oneInstruction.get(1) == 17)
                    trueInstruction += "CX ";
                else if (oneInstruction.get(1) == 18)
                    trueInstruction += "DX ";

                if (oneInstruction.get(2) == 0 && oneInstruction.get(3) != 0)
                    trueInstruction += "[" + oneInstruction.get(3) + "]";
                else if (oneInstruction.get(2) != 0 && oneInstruction.get(3) != 0)
                    trueInstruction += "[" + oneInstruction.get(2) + oneInstruction.get(3) + "]";
                else if (oneInstruction.get(2) != 0 && oneInstruction.get(3) == 0)
                    trueInstruction += "[" + oneInstruction.get(2) + "00]";
                else if (oneInstruction.get(1) == 0 && oneInstruction.get(2) == 0)
                    trueInstruction += "[0]";
            } else if (oneInstruction.get(0) == 42) {
                trueInstruction += "SUB ";
                if (oneInstruction.get(1) == 15)
                    trueInstruction += "AX ";
                else if (oneInstruction.get(1) == 16)
                    trueInstruction += "BX ";
                else if (oneInstruction.get(1) == 17)
                    trueInstruction += "CX ";
                else if (oneInstruction.get(1) == 18)
                    trueInstruction += "DX ";

                if (oneInstruction.get(2) == 0 && oneInstruction.get(3) != 0)
                    trueInstruction += "[" + oneInstruction.get(3) + "]";
                else if (oneInstruction.get(2) != 0 && oneInstruction.get(3) != 0)
                    trueInstruction += "[" + oneInstruction.get(2) + oneInstruction.get(3) + "]";
                else if (oneInstruction.get(2) != 0 && oneInstruction.get(3) == 0)
                    trueInstruction += "[" + oneInstruction.get(2) + "00]";
                else if (oneInstruction.get(1) == 0 && oneInstruction.get(2) == 0)
                    trueInstruction += "[0]";
            } else if (oneInstruction.get(0) == 43) {
                trueInstruction += "MUL ";
                if (oneInstruction.get(1) == 15)
                    trueInstruction += "AX ";
                else if (oneInstruction.get(1) == 16)
                    trueInstruction += "BX ";
                else if (oneInstruction.get(1) == 17)
                    trueInstruction += "CX ";
                else if (oneInstruction.get(1) == 18)
                    trueInstruction += "DX ";

                if (oneInstruction.get(2) == 0 && oneInstruction.get(3) != 0)
                    trueInstruction += "[" + oneInstruction.get(3) + "]";
                else if (oneInstruction.get(2) != 0 && oneInstruction.get(3) != 0)
                    trueInstruction += "[" + oneInstruction.get(2) + oneInstruction.get(3) + "]";
                else if (oneInstruction.get(2) != 0 && oneInstruction.get(3) == 0)
                    trueInstruction += "[" + oneInstruction.get(2) + "00]";
                else if (oneInstruction.get(1) == 0 && oneInstruction.get(2) == 0)
                    trueInstruction += "[0]";
            } else if (oneInstruction.get(0) == 44) {
                trueInstruction += "INC ";
                {
                    if (oneInstruction.get(1) == 0 && oneInstruction.get(2) != 0)
                        trueInstruction += "[" + oneInstruction.get(2) + "]";
                    else if (oneInstruction.get(1) != 0 && oneInstruction.get(2) != 0)
                        trueInstruction += "[" + oneInstruction.get(1) + oneInstruction.get(2) + "]";
                    else if (oneInstruction.get(1) != 0 && oneInstruction.get(2) == 0)
                        trueInstruction += "[" + oneInstruction.get(1) + "00]";
                    else if (oneInstruction.get(1) == 0 && oneInstruction.get(2) == 0)
                        trueInstruction += "[0]";
                }
            } else if (oneInstruction.get(0) == 45) {
                trueInstruction += "DEC ";
                {
                    if (oneInstruction.get(1) == 0 && oneInstruction.get(2) != 0)
                        trueInstruction += "[" + oneInstruction.get(2) + "]";
                    else if (oneInstruction.get(1) != 0 && oneInstruction.get(2) != 0)
                        trueInstruction += "[" + oneInstruction.get(1) + oneInstruction.get(2) + "]";
                    else if (oneInstruction.get(1) != 0 && oneInstruction.get(2) == 0)
                        trueInstruction += "[" + oneInstruction.get(1) + "00]";
                    else if (oneInstruction.get(1) == 0 && oneInstruction.get(2) == 0)
                        trueInstruction += "[0]";
                }
            } else if (oneInstruction.get(0) == 47) {
                trueInstruction += "MVI ";
                {
                    if (oneInstruction.get(1) != 0)
                        trueInstruction += "[" + oneInstruction.get(1) + "]";

                    if (oneInstruction.get(2) == 0 && oneInstruction.get(3) != 0)
                        trueInstruction += " " + oneInstruction.get(3);

                    if (oneInstruction.get(2) != 0 && oneInstruction.get(3) == 0)
                        trueInstruction += " " + oneInstruction.get(2) + "00";
                    else if (oneInstruction.get(2) != 0 && oneInstruction.get(3) != 0) {
                        if (oneInstruction.get(3) <= 9)
                            trueInstruction += " " + oneInstruction.get(2) + "0" + oneInstruction.get(3);
                        else
                            trueInstruction += " " + oneInstruction.get(2) + oneInstruction.get(3);
                    } else if (oneInstruction.get(2) == 0 && oneInstruction.get(3) == 0)
                        trueInstruction += " 0";
                }
            }
            else if (oneInstruction.get(0) == 66)
            {
                trueInstruction += "MOV ";
                    if (oneInstruction.get(1) == 0 && oneInstruction.get(2) != 0)
                    trueInstruction += "[" + oneInstruction.get(2) + "]";

                if (oneInstruction.get(1) != 0 && oneInstruction.get(2) == 0)
                    trueInstruction += "[" + oneInstruction.get(1) + "00]";

                if (oneInstruction.get(1) != 0 && oneInstruction.get(2) != 0)
                {
                    trueInstruction += "[" + oneInstruction.get(1);
                    if(oneInstruction.get(2) <= 9)
                        trueInstruction += "0" + oneInstruction.get(2) +"]";
                    else
                        trueInstruction += oneInstruction.get(2) + "]";
                }
                if (oneInstruction.get(1) == 0 && oneInstruction.get(2) == 0)
                    trueInstruction += "[0]";

                if(oneInstruction.get(3) == 15)
                    trueInstruction += " AX";
                else if(oneInstruction.get(3) == 16)
                    trueInstruction += " BX";
                else if(oneInstruction.get(3) == 17)
                    trueInstruction += " CX";
                else if(oneInstruction.get(3) == 18)
                    trueInstruction += " DX";

            }
        }
        return trueInstruction;
    }

    private Vector<String> fileToLines(File file) {
        Vector<String> lines = new Vector<String>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String text = null;

            while ((text = reader.readLine()) != null) {
                lines.add(text);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
            }
        }
        return lines;
    }


    void instructionExecute() {
        String instruction = "";
        Registers regs = process.registers;
        int size = 0;
        char space = ' ';
        String word = "";
        String firstParameter = "";
        String secondParameter = "";
        String v = "";
        Boolean Bracket = false;
        instruction = byteInstructionToMnemonic(process, process.programCounter);
        instruction = instruction.toUpperCase();
        if (!isInteger(instruction)) {
            if (instruction.charAt(0) == 'R' || instruction.charAt(0) == 'H') {
                if (instruction.equals("RES")) {
                    if (Process.getStepMode())
                    instructionNumber++;
                    process.programCounter += 1;
                    instructionHash.put(instructionNumber, 1);
                    regs.ax = 0;
                    regs.bx = 0;
                    regs.cx = 0;
                    regs.dx = 0;
                } else if (instruction.equals("HLT")) {
                    updateMemory(1);
                    process.programCounter++;
                    process.state = State.TERMINATED;
                }
            } else {
                while (instruction.charAt(size) != space) {
                    word += instruction.charAt(size);
                    size++;
                }
                //Parameter completion
                if (word.equals("MVI")) {
                    updateMemory(4);
                    int i = 4;
                    while (instruction.charAt(i) != space) {
                        firstParameter += instruction.charAt(i);
                        i++;
                    }
                    i += 1;
                    while (i < instruction.length()) {
                        secondParameter += instruction.charAt(i);
                        i++;
                    }

                    if (firstParameter.charAt(0) == '[') {
                        for (int j = 1; j < firstParameter.length() - 1; j++) {
                            v += firstParameter.charAt(j);
                        }
                    }
                } else if (word.equals("ADD") || word.equals("SUB") || word.equals("MUL") || word.equals("MOV")) {
                    int i = 4;
                    while (instruction.charAt(i) != space) {
                        firstParameter += instruction.charAt(i);
                        i++;
                    }
                    i = 7;
                    if(firstParameter.charAt(0) == '[')
                        Bracket=true;

                    if(Bracket)
                    {
                        i=word.length()+1+firstParameter.length()+1;
                        while(i<instruction.length())
                        {
                            secondParameter +=instruction.charAt(i);
                            i++;
                        }
                    }
                    else
                    {
                        while (i < instruction.length()) {
                            secondParameter += instruction.charAt(i);
                            i++;
                        }
                    }
                } else if (word.equals("INC") || word.equals("DEC") || word.equals("JMP") || word.equals("JIZ")) {
                    int i = 4;
                    while (i < instruction.length()) {
                        firstParameter += instruction.charAt(i);
                        i++;
                    }

                } else if (word.equals("JAXZ") || word.equals("JINZ")) {
                    int i = 5;
                    while (i < instruction.length()) {
                        firstParameter += instruction.charAt(i);
                        i++;
                    }

                } else if (word.equals("CP")) {
                    int i = 3;
                    while (instruction.charAt(i) != space) {
                        firstParameter += instruction.charAt(i);
                        i++;
                    }
                    i = 5;

                    while (i < instruction.length()) {
                        secondParameter += instruction.charAt(i);
                        i++;
                    }

                } else if (word.equals("DP")) {
                    int i = 3;
                    while (i < instruction.length()) {
                        firstParameter += instruction.charAt(i);
                        i++;
                    }
                }
            }

            //Executing instructions
            if (size == 2) {
                if (word.equals("CP")) {
                    instructionNumber++;
                    instructionHash.put(instructionNumber, 2);
                    System.out.println("Tu jest tworzony proces o ID: " + firstParameter + " i priorytecie:  " + secondParameter);
                    //create process
                }
                if (word.equals("DP")) {
                    instructionNumber++;
                    instructionHash.put(instructionNumber, 2);
                    System.out.println("Tu jest usuwany proces o ID: " + firstParameter + " i priorytecie:  " + secondParameter);

                    //delete process
                }
            } else if (size == 3) {
                if (word.equals("ADD")) {
                    instructionNumber++;
                    instructionHash.put(instructionNumber, 4);
                    updateMemory(4);
                    process.programCounter += 4;

                    //Checks logical address
                    if (secondParameter.charAt(0) == '[') {
                        String value = "";
                        int i = 1;
                        while (i < secondParameter.length() - 1) {
                            value += secondParameter.charAt(i);
                            i++;
                        }
                        int logicalAddress = Integer.parseInt(value);
                        if (firstParameter.equals("AX")) {
                            int val = (int) (readMemory(logicalAddress));
                            regs.ax += val;
                            //Integer.parseInt(byteInstructionToMnemonic(process, logicalAddress));
                        } else if (firstParameter.equals("BX")) {
                            int val = (int) (readMemory(logicalAddress));
                            regs.bx += val;
                        } else if (firstParameter.equals("CX")) {
                            int val = (int) (readMemory(logicalAddress));
                            regs.cx += val;
                        } else if (firstParameter.equals("DX")) {
                            int val = (int) (readMemory(logicalAddress));
                            regs.dx += val;
                        }
                        //Checks Registers
                    } else if (secondParameter.charAt(0) == 'A' || secondParameter.charAt(0) == 'B' || secondParameter.charAt(0) == 'C' || secondParameter.charAt(0) == 'D') {
                        if (firstParameter.equals("AX") && secondParameter.equals("BX"))
                            regs.ax += regs.bx;
                        else if (firstParameter.equals("AX") && secondParameter.equals("CX"))
                            regs.ax += regs.cx;
                        else if (firstParameter.equals("AX") && secondParameter.equals("DX"))
                            regs.ax += regs.dx;

                        if (firstParameter.equals("BX") && secondParameter.equals("AX"))
                            regs.bx += regs.ax;
                        else if (firstParameter.equals("BX") && secondParameter.equals("CX"))
                            regs.bx += regs.cx;
                        else if (firstParameter.equals("BX") && secondParameter.equals("DX"))
                            regs.bx += regs.dx;

                        if (firstParameter.equals("CX") && secondParameter.equals("AX"))
                            regs.cx += regs.ax;
                        else if (firstParameter.equals("CX") && secondParameter.equals("BX"))
                            regs.cx += regs.bx;
                        else if (firstParameter.equals("CX") && secondParameter.equals("DX"))
                            regs.cx += regs.dx;

                        if (firstParameter.equals("DX") && secondParameter.equals("AX"))
                            regs.dx += regs.ax;
                        else if (firstParameter.equals("DX") && secondParameter.equals("BX"))
                            regs.dx += regs.bx;
                        else if (firstParameter.equals("DX") && secondParameter.equals("CX"))
                            regs.dx += regs.cx;
                    }
                    //Checks values
                    else {
                        int value = Integer.parseInt(secondParameter);
                        if (firstParameter.equals("AX"))
                            regs.ax += value;
                        if (firstParameter.equals("BX"))
                            regs.bx += value;
                        if (firstParameter.equals("CX"))
                            regs.cx += value;
                        if (firstParameter.equals("DX"))
                            regs.dx += value;
                    }
                }
                if (word.equals("SUB")) {
                    instructionNumber++;
                    instructionHash.put(instructionNumber, 4);
                    updateMemory(4);
                    process.programCounter += 4;

                    if (secondParameter.charAt(0) == '[') {
                        String value = "";
                        int i = 1;
                        while (i < secondParameter.length() - 1) {
                            value += secondParameter.charAt(i);
                            i++;
                        }
                        int logicalAddress = Integer.parseInt(value);
                        if (firstParameter.equals("AX")) {
                            int val = (int) (readMemory(logicalAddress));
                            regs.ax -= val;
                        } else if (firstParameter.equals("BX")) {
                            int val = (int) (readMemory(logicalAddress));
                            regs.bx -= val;
                        } else if (firstParameter.equals("CX")) {
                            int val = (int) (readMemory(logicalAddress));
                            regs.cx -= val;
                        } else if (firstParameter.equals("DX")) {
                            int val = (int) (readMemory(logicalAddress));
                            regs.dx -= val;
                        }

                    } else if (secondParameter.charAt(0) == 'A' || secondParameter.charAt(0) == 'B' || secondParameter.charAt(0) == 'C' || secondParameter.charAt(0) == 'D') {
                        if (firstParameter.equals("AX") && secondParameter.equals("BX"))
                            regs.ax -= regs.bx;
                        else if (firstParameter.equals("AX") && secondParameter.equals("CX"))
                            regs.ax -= regs.cx;
                        else if (firstParameter.equals("AX") && secondParameter.equals("DX"))
                            regs.ax -= regs.dx;

                        if (firstParameter.equals("BX") && secondParameter.equals("AX"))
                            regs.bx -= regs.ax;
                        else if (firstParameter.equals("BX") && secondParameter.equals("CX"))
                            regs.bx -= regs.cx;
                        else if (firstParameter.equals("BX") && secondParameter.equals("DX"))
                            regs.bx -= regs.dx;

                        if (firstParameter.equals("CX") && secondParameter.equals("AX"))
                            regs.cx -= regs.ax;
                        else if (firstParameter.equals("CX") && secondParameter.equals("BX"))
                            regs.cx -= regs.bx;
                        else if (firstParameter.equals("CX") && secondParameter.equals("DX"))
                            regs.cx -= regs.dx;

                        if (firstParameter.equals("DX") && secondParameter.equals("AX"))
                            regs.dx -= regs.ax;
                        else if (firstParameter.equals("DX") && secondParameter.equals("BX"))
                            regs.dx -= regs.bx;
                        else if (firstParameter.equals("DX") && secondParameter.equals("CX"))
                            regs.dx -= regs.cx;
                    } else {
                        int value = Integer.parseInt(secondParameter);
                        if (firstParameter.equals("AX"))
                            regs.ax -= value;
                        if (firstParameter.equals("BX"))
                            regs.bx -= value;
                        if (firstParameter.equals("CX"))
                            regs.cx -= value;
                        if (firstParameter.equals("DX"))
                            regs.dx -= value;
                    }
                }
                if (word.equals("MUL")) {
                    instructionNumber++;
                    instructionHash.put(instructionNumber, 4);
                    updateMemory(4);
                    process.programCounter += 4;
                    if (secondParameter.charAt(0) == '[') {
                        String value = "";
                        int i = 1;
                        while (i < secondParameter.length() - 1) {
                            value += secondParameter.charAt(i);
                            i++;
                        }
                        int logicalAddress = Integer.parseInt(value);
                        if (firstParameter.equals("AX")) {
                            regs.ax *= Integer.parseInt(byteInstructionToMnemonic(process, logicalAddress));
                        } else if (firstParameter.equals("BX")) {
                            regs.bx *= Integer.parseInt(byteInstructionToMnemonic(process, logicalAddress));
                        } else if (firstParameter.equals("CX")) {
                            regs.cx *= Integer.parseInt(byteInstructionToMnemonic(process, logicalAddress));
                        } else if (firstParameter.equals("DX")) {
                            regs.dx *= Integer.parseInt(byteInstructionToMnemonic(process, logicalAddress));
                        }

                    } else if (secondParameter.charAt(0) == 'A' || secondParameter.charAt(0) == 'B' || secondParameter.charAt(0) == 'C' || secondParameter.charAt(0) == 'D') {
                        if (firstParameter.equals("AX") && secondParameter.equals("BX"))
                            regs.ax *= regs.bx;
                        else if (firstParameter.equals("AX") && secondParameter.equals("CX"))
                            regs.ax *= regs.cx;
                        else if (firstParameter.equals("AX") && secondParameter.equals("DX"))
                            regs.ax *= regs.dx;

                        if (firstParameter.equals("BX") && secondParameter.equals("AX"))
                            regs.bx *= regs.ax;
                        else if (firstParameter.equals("BX") && secondParameter.equals("CX"))
                            regs.bx *= regs.cx;
                        else if (firstParameter.equals("BX") && secondParameter.equals("DX"))
                            regs.bx *= regs.dx;

                        if (firstParameter.equals("CX") && secondParameter.equals("AX"))
                            regs.cx *= regs.ax;
                        else if (firstParameter.equals("CX") && secondParameter.equals("BX"))
                            regs.cx *= regs.bx;
                        else if (firstParameter.equals("CX") && secondParameter.equals("DX"))
                            regs.cx *= regs.dx;

                        if (firstParameter.equals("DX") && secondParameter.equals("AX"))
                            regs.dx *= regs.ax;
                        else if (firstParameter.equals("DX") && secondParameter.equals("BX"))
                            regs.dx *= regs.bx;
                        else if (firstParameter.equals("DX") && secondParameter.equals("CX"))
                            regs.dx *= regs.cx;
                    } else {
                        int value = Integer.parseInt(secondParameter);
                        if (firstParameter.equals("AX"))
                            regs.ax *= value;
                        if (firstParameter.equals("BX"))
                            regs.bx *= value;
                        if (firstParameter.equals("CX"))
                            regs.cx *= value;
                        if (firstParameter.equals("DX"))
                            regs.dx *= value;
                    }
                }
                if (word.equals("INC")) {
                    if (firstParameter.charAt(0) == '[') {
                        instructionNumber++;
                        instructionHash.put(instructionNumber, 3);
                        updateMemory(3);
                        process.programCounter += 3;
                        String value = "";
                        int i = 1;
                        int j = 0;
                        while (i < firstParameter.length() - 1) {
                            value += firstParameter.charAt(i);
                            i++;
                        }
                        int logicalAddress = Integer.parseInt(value);
                        byte singleByte = readMemory(logicalAddress);
                        int temp = (int) singleByte;
                        temp++;
                        singleByte = (byte) temp;
                        writeMemory(logicalAddress, singleByte);

                    } else if (firstParameter.equals("AX")) {
                        regs.ax++;
                        instructionNumber++;
                        instructionHash.put(instructionNumber, 2);
                        updateMemory(2);
                        process.programCounter += 2;
                    } else if (firstParameter.equals("BX")) {
                        regs.bx++;
                        instructionNumber++;
                        instructionHash.put(instructionNumber, 2);
                        updateMemory(2);
                        process.programCounter += 2;
                    } else if (firstParameter.equals("CX")) {
                        regs.bx++;
                        instructionNumber++;
                        instructionHash.put(instructionNumber, 2);
                        updateMemory(2);
                        process.programCounter += 2;
                    } else if (firstParameter.equals("DX")) {
                        regs.dx++;
                        instructionNumber++;
                        instructionHash.put(instructionNumber, 2);
                        updateMemory(2);
                        process.programCounter += 2;
                    }
                }
                if (word.equals("DEC")) {
                    if (firstParameter.charAt(0) == '[') {
                        instructionNumber++;
                        instructionHash.put(instructionNumber, 3);
                        updateMemory(3);
                        process.programCounter += 3;
                        String value = "";
                        int i = 1;
                        int j = 0;
                        while (i < firstParameter.length() - 1) {
                            value += firstParameter.charAt(i);
                            i++;
                        }
                        int logicalAddress = Integer.parseInt(value);
                        byte singleByte = readMemory(logicalAddress);
                        int temp = (int) singleByte;
                        temp--;
                        singleByte = (byte) temp;

                        writeMemory(logicalAddress, singleByte);

                    } else if (firstParameter.equals("AX")) {
                        regs.ax--;
                        instructionNumber++;
                        instructionHash.put(instructionNumber, 2);
                        updateMemory(2);
                        process.programCounter += 2;
                    } else if (firstParameter.equals("BX")) {
                        regs.bx--;
                        instructionNumber++;
                        instructionHash.put(instructionNumber, 2);
                        updateMemory(2);
                        process.programCounter += 2;
                    } else if (firstParameter.equals("CX")) {
                        regs.cx--;
                        instructionNumber++;
                        instructionHash.put(instructionNumber, 2);
                        updateMemory(2);
                        process.programCounter += 2;
                    } else if (firstParameter.equals("DX")) {
                        regs.dx--;
                        instructionNumber++;
                        instructionHash.put(instructionNumber, 2);
                        updateMemory(2);
                        process.programCounter += 2;
                    }
                }
                if (word.equals("MOV")) {
                    instructionNumber++;
                    instructionHash.put(instructionNumber, 4);
                    updateMemory(4);
                    process.programCounter += 4;
                    if(Bracket)
                    {
                        int logicalAddress;
                        String value ="";
                        for (int j = 1; j < firstParameter.length() - 1; j++) {
                            value += firstParameter.charAt(j);
                        }
                        logicalAddress = Integer.parseInt(value);
                        if(secondParameter.equals("AX"))
                             writeMemory(logicalAddress, (byte) regs.ax);
                        else if(secondParameter.equals("BX"))
                             writeMemory(logicalAddress, (byte) regs.bx);
                        else if(secondParameter.equals("CX"))
                             writeMemory(logicalAddress, (byte) regs.cx);
                        else if(secondParameter.equals("DX"))
                             writeMemory(logicalAddress, (byte) regs.dx);
                    }
                    else
                    {
                        if (secondParameter.charAt(0) == 'A' || secondParameter.charAt(0) == 'B' || secondParameter.charAt(0) == 'C' || secondParameter.charAt(0) == 'D') {
                            if (firstParameter.equals("AX") && secondParameter.equals("BX"))
                                regs.ax = regs.bx;
                            else if (firstParameter.equals("AX") && secondParameter.equals("CX"))
                                regs.ax = regs.cx;
                            else if (firstParameter.equals("AX") && secondParameter.equals("DX"))
                                regs.ax = regs.dx;

                            if (firstParameter.equals("BX") && secondParameter.equals("AX"))
                                regs.bx = regs.ax;
                            else if (firstParameter.equals("BX") && secondParameter.equals("CX"))
                                regs.bx = regs.cx;
                            else if (firstParameter.equals("BX") && secondParameter.equals("DX"))
                                regs.bx = regs.dx;

                            if (firstParameter.equals("CX") && secondParameter.equals("AX"))
                                regs.cx = regs.ax;
                            else if (firstParameter.equals("CX") && secondParameter.equals("BX"))
                                regs.cx = regs.bx;
                            else if (firstParameter.equals("CX") && secondParameter.equals("DX"))
                                regs.cx = regs.dx;

                            if (firstParameter.equals("DX") && secondParameter.equals("AX"))
                                regs.dx = regs.ax;
                            else if (firstParameter.equals("DX") && secondParameter.equals("BX"))
                                regs.dx = regs.bx;
                            else if (firstParameter.equals("DX") && secondParameter.equals("CX"))
                                regs.dx = regs.cx;
                        }
                    }
                }
                if (word.equals("MVI")) {
                    byte byteValue = 0;
                    instructionNumber++;
                    instructionHash.put(instructionNumber, 4);
                    updateMemory(4);
                    process.programCounter += 4;
                    int value = Integer.parseInt(secondParameter);
                    if (firstParameter.charAt(0) == 'A')
                        regs.ax = value;
                    else if (firstParameter.charAt(0) == 'B')
                        regs.bx = value;
                    else if (firstParameter.charAt(0) == 'C')
                        regs.cx = value;
                    else if (firstParameter.charAt(0) == 'D')
                        regs.dx = value;
                    else if (firstParameter.charAt(0) == '[') {
                        int logicalAddress = Integer.parseInt(v);
                        byteValue = (byte) value;
                        writeMemory(logicalAddress, byteValue);
                        //memory.write(process.PID, logicalAddress,byteValue);
                    }


                }
                if (word.equals("JMP")) {
                    instructionNumber++;
                    instructionHash.put(instructionNumber, 3);
                    updateMemory(3);
                    process.programCounter += 3;

                    if (firstParameter.charAt(0) == '[') {
                        String value = "";
                        int i = 1;
                        int j = 0;
                        while (i < firstParameter.length() - 1) {
                            value += firstParameter.charAt(i);
                            i++;
                        }
                        process.programCounter = Integer.parseInt(value);
                    }
                }
                if (word.equals("JIZ")) {
                    instructionNumber++;
                    instructionHash.put(instructionNumber, 3);
                    updateMemory(3);
                    process.programCounter += 3;
                    if (regs.ax == 0 && regs.bx == 0 && regs.cx == 0 && regs.dx == 0) {
                        if (firstParameter.charAt(0) == '[') {
                            String value = "";
                            int i = 1;
                            int j = 0;
                            while (i < firstParameter.length() - 1) {
                                value += firstParameter.charAt(i);
                                i++;
                            }
                            process.programCounter = Integer.parseInt(value);
                        }
                    } else
                        throw new IllegalArgumentException("Can't realize this condition");
                }
            } else if (size == 4) {
                if (word.equals("JAXZ")) {
                    instructionNumber++;
                    instructionHash.put(instructionNumber, 3);
                    updateMemory(3);
                    process.programCounter += 3;
                    if (regs.ax == 0) {
                        String value = "";
                        int i = 1;
                        while (i < firstParameter.length() - 1) {
                            value += firstParameter.charAt(i);
                            i++;
                        }
                        process.programCounter = Integer.parseInt(value);
                    }
                }
                if (word.equals("JINZ")) {
                    instructionNumber++;
                    instructionHash.put(instructionNumber, 3);
                    updateMemory(3);
                    process.programCounter += 3;

                    if (regs.ax != 0 || regs.bx != 0 || regs.cx != 0 || regs.dx != 0) {
                        String value = "";
                        int i = 1;
                        int j = 0;
                        while (i < firstParameter.length() - 1) {
                            value += firstParameter.charAt(i);
                            i++;
                        }
                        process.programCounter = Integer.parseInt(value);
                    }
                }
            }
        } else {
            updateMemory(2);
            process.programCounter += 2;
            instructionNumber++;
            instructionHash.put(instructionNumber, 2);
        }
        stepMode = true;
        process.saveRegisters(regs);
    }
}
