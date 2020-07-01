package interpreter;

import cpuscheduler.PCB;
import cpuscheduler.State;
import memory.virtual.VirtualMemory;
import java.io.File;
import java.util.*;
public class InterpreterStart{

    public static void main(String[] args) {
        //add(t);
        Scanner readInput = new Scanner(System.in);
        String input = "";
        Boolean step = false;
        PCB First = new PCB(2, 3, State.NEW, "Pierwszy");
        VirtualMemory Mem = new VirtualMemory(1024, 1024);
        Interpreter asm = new Interpreter(new File("asm.txt"), First, Mem);
        System.out.println("Press enter to see next instructions");
        System.out.println("r - Show registers");
        System.out.println("m - Show memory");
        System.out.println("e - Exit");

        while (First.state != State.TERMINATED && !input.equals("e")) {
            input = readInput.nextLine();

            if(input.equals("")) {
                asm.changeStepMode();
                asm.runInterpreter();
            }
            else if(input.equals("r"))
            {
                asm.showRegisters();
            }
            else if(input.equals("m"))
            {
                System.out.print("Memory" );
                asm.showMemory();
            }
            else if(input.equals("e"))
            {
                System.out.println("Program has ended");
            }


        }


       // asm.runInterpreter();
    }
}
