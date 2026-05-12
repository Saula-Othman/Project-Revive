package pro.revive.interfaces;

import pro.revive.entities.EntitiesLabo.Examens_demandes;
import java.util.List;

public interface IExamenCrud {
    void ajouterExamen(Examens_demandes examen);
    void modifierExamen(Examens_demandes examen);
    void supprimerExamen(int idDemande);
    Examens_demandes getExamenById(int idDemande);
    List<Examens_demandes> getAllExamens();
    List<Examens_demandes> getExamensByConsultation(int idConsultation);
    void marquerRealise(int idDemande);
}