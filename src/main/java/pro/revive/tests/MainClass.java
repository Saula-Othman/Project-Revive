package pro.revive.tests;

import pro.revive.entities.EntitiesUser.Personne;
import pro.revive.services.ServicesUser.PersonneService;

import java.util.List;

public class MainClass {

    public static void main(String[] args) {

        System.out.println("===========================================");
        System.out.println("   REVIVE - Module 6 - Personnel");
        System.out.println("===========================================");

        PersonneService ps = new PersonneService();

        // ─── TEST 1: addEntity ───
        System.out.println("\n--- Test addEntity ---");
        Personne p1 = new Personne("Ben Ali", "Sami", "Infirmier Triage", "", "sami123");
        ps.addEntity(p1);

        // ─── TEST 2: getData (SELECT all) ───
        System.out.println("\n--- Test getData (Tout le Personnel) ---");
        List<Personne> all = ps.getData();
        for (Personne p : all) {
            System.out.println(p);
        }

        // ─── TEST 3: getData2 (Search) ───
        System.out.println("\n--- Test getData2 (Recherche 'Ben') ---");
        List<Personne> found = ps.getData2("Ben");
        for (Personne p : found) {
            System.out.println(p);
        }

        // ─── TEST 4: getData3 (By role) ───
        System.out.println("\n--- Test getData3 (Medecins Urgentistes) ---");
        List<Personne> medecins = ps.getData3("Medecin Urgentiste");
        for (Personne p : medecins) {
            System.out.println(p);
        }

        // ─── TEST 5: getData4 (Authentication) ───
        System.out.println("\n--- Test getData4 (Connexion admin) ---");
        Personne auth = ps.getData4("admin", "admin123");
        if (auth != null) {
            System.out.println("Connecte en tant que: " + auth.getNom() + " - Role: " + auth.getRole());
        }

        // ─── TEST 6: updateEntity ───
        System.out.println("\n--- Test updateEntity ---");
        Personne updated = new Personne();
        updated.setNom("Ben Ali");
        updated.setPrenom("Sami Updated");
        updated.setRole("Infirmier Triage");
        updated.setIdentifiant("TRBEN001");
        ps.updateEntity(2, updated);

        // ─── TEST 7: deleteEntity ───
        System.out.println("\n--- Test deleteEntity ---");
        Personne toDelete = new Personne();
        toDelete.setIdPersonnel(2);
        ps.deleteEntity(toDelete);

        // ─── TEST 8: getData after delete ───
        System.out.println("\n--- Personnel apres suppression ---");
        List<Personne> afterDelete = ps.getData();
        for (Personne p : afterDelete) {
            System.out.println(p);
        }

        System.out.println("\n===========================================");
        System.out.println("   Tests termines!");
        System.out.println("===========================================");
    }
}
