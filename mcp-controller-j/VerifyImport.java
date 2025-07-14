// This file verifies that StateMachineFactory can be imported from the correct package
// in Spring State Machine 3.2.1

import org.springframework.statemachine.config.StateMachineFactory;

public class VerifyImport {
    public static void main(String[] args) {
        System.out.println("If this compiles, the import path is correct!");
        System.out.println("StateMachineFactory should be imported from:");
        System.out.println("org.springframework.statemachine.config.StateMachineFactory");
    }
}