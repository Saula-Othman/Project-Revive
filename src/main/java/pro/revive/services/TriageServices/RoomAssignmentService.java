package pro.revive.services;

import pro.revive.entities.Salle;
import pro.revive.entities.Triage;

import java.util.List;

public class RoomAssignmentService {

    private SalleService salleService = new SalleService();
    private TriageService triageService = new TriageService();

    public void assignBestRoom(Triage triage) {
        List<Salle> available = salleService.getData2(triage.getNiveauFinal());

        if (!available.isEmpty()) {
            Salle best = available.get(0);
            triageService.updateRoom(triage.getIdTriage(), best.getIdSalle(), "InRoom");
            salleService.updateEntity2(best.getIdSalle(), 1);
            triage.setIdSalle(best.getIdSalle());
            triage.setNomSalle(best.getNomSalle());
            triage.setPatientState("InRoom");
            System.out.println("Salle assignee: " + best.getNomSalle());
        } else {
            triageService.updateRoom(triage.getIdTriage(), 0, "WaitingRoom");
            triage.setPatientState("WaitingRoom");
            System.out.println("Toutes les salles sont pleines. Patient en liste d'attente.");

            // BUG-10 fix: increment patients_en_attente on the FIRST room targeting this gravity level
            // (per-room counter must balance with decrementWaitlist, which only decrements the room
            // being freed). Choosing the highest-priority room is consistent with assignBestRoom.
            List<Salle> targetRooms = salleService.getData(); // ordered by priorite ASC
            for (Salle s : targetRooms) {
                if (s.getNiveauGraviteCible() == triage.getNiveauFinal()) {
                    salleService.incrementWaitlist(s.getIdSalle());
                    break;
                }
            }

            if (triage.getNiveauFinal() == 1) {
                System.out.println("ALERTE CRITIQUE: Patient niveau 1 en attente!");
            }
        }
    }

    public void freeRoom(int idTriage) {
        Triage t = triageService.getData4(idTriage);
        if (t == null) return;

        int idSalle      = t.getIdSalle();
        int gravityLevel = t.getNiveauFinal();

        try {
            pro.revive.utils.MyConnection.getInstance().runInTransaction(conn -> {
                triageService.discharge(idTriage);

                if (idSalle > 0) {
                    salleService.updateEntity2(idSalle, -1);

                    List<Triage> waiting = triageService.getData3();
                    for (Triage wt : waiting) {
                        if (wt.getNiveauFinal() == gravityLevel) {
                            triageService.updateRoom(wt.getIdTriage(), idSalle, "InRoom");
                            salleService.updateEntity2(idSalle, 1);

                            // Decrement waitlist on the highest-priority room for this level
                            List<Salle> targetRooms = salleService.getData();
                            for (Salle s : targetRooms) {
                                if (s.getNiveauGraviteCible() == gravityLevel) {
                                    salleService.decrementWaitlist(s.getIdSalle());
                                    break;
                                }
                            }
                            System.out.println("Prochain patient assigne: " + wt.getNomPatient());
                            break;
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Echec liberation salle: " + e.getMessage(), e);
        }
    }

    public void transferRoom(int idTriage, int newIdSalle) {
        Triage t = triageService.getData4(idTriage);
        if (t == null) return;

        int oldIdSalle = t.getIdSalle();
        if (oldIdSalle > 0) {
            salleService.updateEntity2(oldIdSalle, -1);
        }

        // BUG-11 fix: after a transfer, the patient is occupying the new room — state should be "InRoom",
        // not "Transferred" (which sounds final). The audit trail of the transfer is captured by
        // id_salle changing; "Transferred" was misleading and broke timeline rendering.
        triageService.updateRoom(idTriage, newIdSalle, "InRoom");
        salleService.updateEntity2(newIdSalle, 1);
        System.out.println("Patient transfere vers salle ID: " + newIdSalle);
    }
}
