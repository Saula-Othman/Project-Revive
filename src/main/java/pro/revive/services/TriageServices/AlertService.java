package pro.revive.services.TriageServices;

import pro.revive.entities.TriageEntities.Salle;
import pro.revive.entities.TriageEntities.Triage;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class AlertService {

    private final TriageService triageService = new TriageService();
    private final SalleService  salleService  = new SalleService();

    public void checkCriticalWaiting() {
        List<Triage> waiting = triageService.getData3();
        for (Triage t : waiting) {
            if (t.getDateHeureTriage() == null) continue;
            long minutesWaiting = Duration.between(t.getDateHeureTriage(), LocalDateTime.now()).toMinutes();
            if (t.getNiveauFinal() == 1 && minutesWaiting >= 5) {
                System.out.println("ALARME CRITIQUE: Patient " + t.getNomPatient() + " " + t.getPrenomPatient() +
                        " NIVEAU 1 en attente depuis " + minutesWaiting + " minutes! ACTION IMMEDIATE REQUISE!");
            }
            if (t.getNiveauFinal() == 2 && minutesWaiting >= 15) {
                System.out.println("AVERTISSEMENT: Patient " + t.getNomPatient() + " " + t.getPrenomPatient() +
                        " NIVEAU 2 en attente depuis " + minutesWaiting + " minutes!");
            }
        }
    }

    public void checkRoomOverflow() {
        // Fix: single DB call, reuse the list for both counts
        List<Salle> salles = salleService.getData();
        // BUG-5 fix: null-safe — flip operands
        long fullRooms  = salles.stream().filter(s -> "Pleine".equals(s.getStatut())).count();
        long totalRooms = salles.size();

        if (fullRooms == totalRooms && totalRooms > 0) {
            System.out.println("ALERTE DEBORDEMENT: TOUTES LES SALLES SONT PLEINES!");
        }
    }

}
