package pro.revive.interfaces;

import pro.revive.entities.EntitiesLabo.Resultats;
import java.util.List;

public interface IResultatCrud {
    void ajouterResultat(Resultats resultat);
    void modifierResultat(Resultats resultat);
    void supprimerResultat(int idResultat);
    Resultats getResultatById(int idResultat);
    List<Resultats> getAllResultats();
    List<Resultats> getResultatsByDemande(int idDemande);
}