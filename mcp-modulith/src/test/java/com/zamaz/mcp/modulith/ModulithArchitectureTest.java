package com.zamaz.mcp.modulith;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Tests to verify the modulith architecture.
 */
class ModulithArchitectureTest {

    private final ApplicationModules modules = ApplicationModules.of(McpModulithApplication.class);

    @Test
    void verifyModularStructure() {
        // Verifies that the modular structure is valid
        modules.verify();
    }

    @Test
    void createModuleDocumentation() {
        // Creates documentation for the modules
        new Documenter(modules)
            .writeDocumentation()
            .writeIndividualModulesAsPlantUml();
    }

    @Test
    void printModuleStructure() {
        // Print module information
        modules.forEach(module -> {
            System.out.println("Module: " + module.getName());
            System.out.println("  Display Name: " + module.getDisplayName());
            System.out.println("  Base Package: " + module.getBasePackage());
            System.out.println("  Dependencies: " + module.getDependencies(modules));
            System.out.println();
        });
    }
}