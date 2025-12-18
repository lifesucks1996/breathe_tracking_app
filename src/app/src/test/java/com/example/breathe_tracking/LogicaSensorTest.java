package com.example.breathe_tracking;

import org.junit.Test;
import static org.junit.Assert.*;

public class LogicaSensorTest {

    @Test
    public void verificarAlertasCo2() {
        System.out.println(" __________________________________TEST______________________________________________");
        System.out.println("[TEST 1] Verificando Lógica de CO2...");

        // Caso 1
        int valorPeligroso = 1300;
        System.out.println("    - Preguntando al código: '¿Es " + valorPeligroso + " ppm peligroso?'");
        boolean esPeligroso = SensorTrackingService.esCo2Peligroso(valorPeligroso);
        System.out.println("    - El código responde: " + (esPeligroso ? "SÍ" : "NO"));
        assertTrue("Error: 1300 debería ser peligroso", esPeligroso);

        // Caso 2
        int valorSeguro = 800;
        System.out.println("    - Preguntando al código: '¿Es " + valorSeguro + " ppm peligroso?'");
        boolean esSeguro = SensorTrackingService.esCo2Peligroso(valorSeguro);
        System.out.println("    - El código responde: " + (esSeguro ? "SÍ" : "NO"));
        assertFalse("Error: 800 debería ser seguro", esSeguro);

        System.out.println(" Test de CO2 superado.\n");
    }

    @Test
    public void verificarAlertaBateria() {


        System.out.println(" [TEST 2] Verificando Lógica de Batería...");

        System.out.println("    - Probando batería al 15% (Límite crítico)...");
        boolean critico = SensorTrackingService.esBateriaCritica(15);
        System.out.println("    - Resultado: " + critico);
        assertTrue(critico);

        System.out.println("    - Probando batería al 20% (Nivel normal)...");
        boolean normal = SensorTrackingService.esBateriaCritica(20);
        System.out.println("    - Resultado: " + normal);
        assertFalse(normal);

        System.out.println("Test de Batería superado.\n");
        System.out.println(" _______________________________FIN TEST______________________________________________");
    }

    @Test
    public void verificarTemperatura() {
        System.out.println("[TEST 3] Verificando Temperatura...");
        System.out.println("    - Probando 36ºC (Calor)...");
        assertTrue(SensorTrackingService.esTemperaturaPeligrosa(36.0f));
        System.out.println("Test de Temperatura superado.");
    }
}