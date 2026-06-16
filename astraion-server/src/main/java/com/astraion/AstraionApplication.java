package com.astraion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ASTRAION - 星辰造物，AI管理一切
 */
@SpringBootApplication
public class AstraionApplication {

    public static void main(String[] args) {
        SpringApplication.run(AstraionApplication.class, args);
        System.out.println();
        System.out.println("  ╔═══════════════════════════════════════╗");
        System.out.println("  ║       A S T R A I O N               ║");
        System.out.println("  ║   星辰造物 · AI 管理一切             ║");
        System.out.println("  ║   ASTRAION forges the stars.        ║");
        System.out.println("  ╚═══════════════════════════════════════╝");
        System.out.println();
    }
}
