package pro.revive.tests;

import pro.revive.entities.Salle;
import pro.revive.entities.Triage;
import pro.revive.services.AlertService;
import pro.revive.services.GravityCalculator;
import pro.revive.services.SalleService;
import pro.revive.services.TriageService;

import java.util.List;

public class MainClass {

    public static void main(String[] args) {

        System.out.println("===========================================");
        System.out.println("   REVIVE - Module 2 - Triage & Salles");
        System.out.println("===========================================");

        TriageService triageService = new TriageService();
        SalleService salleService = new SalleService();
        AlertService alertService = new AlertService();

        // ─── TEST 1: Show all salles ───
        System.out.println("\n--- Toutes les Salles ---");
        List<Salle> salles = salleService.getData();
        for (Salle s : salles) {
            System.out.println(s);
        }

        // ─── TEST 2: Add new triage (addEntity2 - full auto chain) ───
        System.out.println("\n--- Nouveau Triage (addEntity2) ---");
        Triage t = new Triage(
                1,    // id_admission
                2,    // id_personnel
                85,   // constancesTaSys
                60,   // constancesTaDia
                140,  // constancesPouls
                39.5f,// constancesTemperature
                88,   // spo2
                0.4f, // glycemie
                8,    // scoreDouleur
                11,   // gcsScore
                26,   // frequenceRespiratoire
                "Douleur thoracique, essoufflement"  // symptomes
        );
        triageService.addEntity2(t);

        // ─── TEST 3: Show all active triages ───
        System.out.println("\n--- Triages Actifs ---");
        List<Triage> triages = triageService.getData();
        for (Triage triage : triages) {
            System.out.println(triage);
        }

        // ─── TEST 4: Search by patient name (getData2) ───
        System.out.println("\n--- Recherche Patient 'Mohamed' ---");
        List<Triage> found = triageService.getData2("Mohamed");
        for (Triage triage : found) {
            System.out.println(triage);
        }

        // ─── TEST 5: Waiting patients (getData3) ───
        System.out.println("\n--- Patients en Attente ---");
        List<Triage> waiting = triageService.getData3();
        for (Triage triage : waiting) {
            System.out.println(triage);
        }

        // ─── TEST 6: Check alerts ───
        System.out.println("\n--- Verification Alertes ---");
        alertService.checkCriticalWaiting();
        alertService.checkRoomOverflow();

        // ─── TEST 7: Test gravity calculator ───
        System.out.println("\n--- Test Calcul Gravite ---");
        Triage testCalc = new Triage();
        testCalc.setConstancesPouls(45);
        testCalc.setConstancesTaSys(75);
        testCalc.setConstancesTemperature(41.0f);
        testCalc.setSpo2(83);
        testCalc.setGlycemie(0.3f);
        testCalc.setScoreDouleur(9);
        testCalc.setGcsScore(8);
        testCalc.setFrequenceRespiratoire(32);
        testCalc.setSymptomes("Test critique");
        GravityCalculator.calculateScore(testCalc);
        System.out.println("Score: " + testCalc.getScoreCalcule());
        System.out.println("Niveau: " + testCalc.getNiveauFinal() + " - " + GravityCalculator.levelLabel(testCalc.getNiveauFinal()));
        System.out.println("Analyse: " + testCalc.getAnalyseAuto());

        System.out.println("\n===========================================");
        System.out.println("   Tests termines avec succes!");
        System.out.println("===========================================");
    }
}
