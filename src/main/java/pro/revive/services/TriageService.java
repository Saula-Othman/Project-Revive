package pro.revive.services;

import pro.revive.entities.Triage;
import pro.revive.interfaces.IService;
import pro.revive.utils.AdmissionItem;
import pro.revive.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TriageService implements IService<Triage> {

    // Each call borrows a fresh connection from the pool — always use in try-with-resources.
    private Connection getCnx() {
        return MyConnection.getInstance().getCnx();
    }

    // ══════════════════════════════════════════
    // addEntity — INSERT using Statement (required by teacher)
    // ══════════════════════════════════════════
    @Override
    public void addEntity(Triage triage) {
        GravityCalculator.calculateScore(triage);

        String requete = "INSERT INTO triage (id_admission, id_personnel, constantes_ta_sys, constantes_ta_dia, " +
                "constantes_pouls, constantes_temperature, spo2, glycemie, score_douleur, gcs_score, " +
                "frequence_respiratoire, symptomes, score_calcule, niveau_auto, niveau_final, " +
                "analyse_auto, patient_state, date_heure_triage) VALUES (" +
                triage.getIdAdmission() + ", " +
                triage.getIdPersonnel() + ", " +
                triage.getConstancesTaSys() + ", " +
                triage.getConstancesTaDia() + ", " +
                triage.getConstancesPouls() + ", " +
                triage.getConstancesTemperature() + ", " +
                triage.getSpo2() + ", " +
                triage.getGlycemie() + ", " +
                triage.getScoreDouleur() + ", " +
                triage.getGcsScore() + ", " +
                triage.getFrequenceRespiratoire() + ", " +
                "'" + esc(triage.getSymptomes()) + "', " +
                triage.getScoreCalcule() + ", " +
                triage.getNiveauAuto() + ", " +
                triage.getNiveauFinal() + ", " +
                "'" + esc(triage.getAnalyseAuto()) + "', " +
                "'Triaged', NOW())";
        try (Connection c = getCnx(); Statement st = c.createStatement()) {
            st.executeUpdate(requete);
            System.out.println("Triage ajoute! Niveau: " + triage.getNiveauFinal() + " - " + GravityCalculator.levelLabel(triage.getNiveauFinal()));
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Echec ajout triage: " + e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════
    // addEntity2 — INSERT using PreparedStatement (required by teacher)
    // ══════════════════════════════════════════
    public void addEntity2(Triage triage) {
        GravityCalculator.calculateScore(triage);

        String requete = "INSERT INTO triage (id_admission, id_personnel, constantes_ta_sys, constantes_ta_dia, " +
                "constantes_pouls, constantes_temperature, spo2, glycemie, score_douleur, gcs_score, " +
                "frequence_respiratoire, symptomes, score_calcule, niveau_auto, niveau_final, " +
                "analyse_auto, patient_state, date_heure_triage) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try (Connection c = getCnx();
             PreparedStatement pst = c.prepareStatement(requete, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, triage.getIdAdmission());
            pst.setInt(2, triage.getIdPersonnel());
            pst.setFloat(3, triage.getConstancesTaSys());
            pst.setFloat(4, triage.getConstancesTaDia());
            pst.setInt(5, triage.getConstancesPouls());
            pst.setFloat(6, triage.getConstancesTemperature());
            pst.setInt(7, triage.getSpo2());
            pst.setFloat(8, triage.getGlycemie());
            pst.setInt(9, triage.getScoreDouleur());
            pst.setInt(10, triage.getGcsScore());
            pst.setInt(11, triage.getFrequenceRespiratoire());
            pst.setString(12, triage.getSymptomes());
            pst.setInt(13, triage.getScoreCalcule());
            pst.setInt(14, triage.getNiveauAuto());
            pst.setInt(15, triage.getNiveauFinal());
            pst.setString(16, triage.getAnalyseAuto());
            pst.setString(17, "Triaged");
            pst.executeUpdate();

            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    int newId = rs.getInt(1);
                    triage.setIdTriage(newId);
                    System.out.println("Triage ajoute avec ID: " + newId);
                    System.out.println("Score: " + triage.getScoreCalcule() + " -> Niveau: " + triage.getNiveauFinal() + " - " + GravityCalculator.levelLabel(triage.getNiveauFinal()));
                    System.out.println("Analyse: " + triage.getAnalyseAuto());

                    RoomAssignmentService roomService = new RoomAssignmentService();
                    roomService.assignBestRoom(triage);

                    if (triage.getNiveauFinal() <= 2) {
                        System.out.println("ALERTE: Patient critique niveau " + triage.getNiveauFinal() + " - Medecin notifie!");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Echec ajout triage: " + e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════
    // deleteEntity — DELETE using PreparedStatement
    // ══════════════════════════════════════════
    @Override
    public void deleteEntity(Triage triage) {
        String requete = "DELETE FROM triage WHERE id_triage = ?";
        try (Connection c = getCnx(); PreparedStatement pst = c.prepareStatement(requete)) {
            pst.setInt(1, triage.getIdTriage());
            pst.executeUpdate();
            System.out.println("Triage supprime!");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Echec suppression triage: " + e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════
    // updateEntity — UPDATE using PreparedStatement
    // BUG-1 fix: persists score_calcule, niveau_auto, analyse_auto.
    // Does NOT touch override fields — use applyOverride() for that.
    // ══════════════════════════════════════════
    @Override
    public void updateEntity(int id, Triage triage) {
        String requete = "UPDATE triage SET constantes_ta_sys=?, constantes_ta_dia=?, constantes_pouls=?, " +
                "constantes_temperature=?, spo2=?, glycemie=?, score_douleur=?, gcs_score=?, " +
                "frequence_respiratoire=?, symptomes=?, " +
                "score_calcule=?, niveau_auto=?, niveau_final=?, analyse_auto=?, " +
                "patient_state=? " +
                "WHERE id_triage=?";
        try (Connection c = getCnx(); PreparedStatement pst = c.prepareStatement(requete)) {
            pst.setFloat(1, triage.getConstancesTaSys());
            pst.setFloat(2, triage.getConstancesTaDia());
            pst.setInt(3, triage.getConstancesPouls());
            pst.setFloat(4, triage.getConstancesTemperature());
            pst.setInt(5, triage.getSpo2());
            pst.setFloat(6, triage.getGlycemie());
            pst.setInt(7, triage.getScoreDouleur());
            pst.setInt(8, triage.getGcsScore());
            pst.setInt(9, triage.getFrequenceRespiratoire());
            pst.setString(10, triage.getSymptomes());
            pst.setInt(11, triage.getScoreCalcule());
            pst.setInt(12, triage.getNiveauAuto());
            pst.setInt(13, triage.getNiveauFinal());
            pst.setString(14, triage.getAnalyseAuto());
            pst.setString(15, triage.getPatientState());
            pst.setInt(16, id);
            pst.executeUpdate();
            System.out.println("Triage mis a jour!");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Echec mise a jour triage: " + e.getMessage(), e);
        }
    }

    // BUG-2 fix: explicit override — stamps date_override = NOW() only when called.
    public void applyOverride(int idTriage, int newNiveauFinal, String overrideNote, int idPersonnelOverride) {
        String requete = "UPDATE triage SET niveau_final=?, override_note=?, date_override=NOW(), id_personnel_override=? WHERE id_triage=?";
        try (Connection c = getCnx(); PreparedStatement pst = c.prepareStatement(requete)) {
            pst.setInt(1, newNiveauFinal);
            pst.setString(2, overrideNote);
            pst.setInt(3, idPersonnelOverride);
            pst.setInt(4, idTriage);
            pst.executeUpdate();
            System.out.println("Override applique: niveau " + newNiveauFinal);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Echec application override: " + e.getMessage(), e);
        }
    }

    // updateEntity2 — UPDATE patient state using PreparedStatement
    public void updateEntity2(int id, String newState) {
        String requete = "UPDATE triage SET patient_state=? WHERE id_triage=?";
        try (Connection c = getCnx(); PreparedStatement pst = c.prepareStatement(requete)) {
            pst.setString(1, newState);
            pst.setInt(2, id);
            pst.executeUpdate();
            System.out.println("Etat patient mis a jour: " + newState);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Echec mise a jour etat: " + e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════
    // getData — SELECT all active using Statement
    // ══════════════════════════════════════════
    @Override
    public List<Triage> getData() {
        List<Triage> list = new ArrayList<>();
        String requete = "SELECT t.*, p.nom, p.prenom, s.nom_salle " +
                "FROM triage t " +
                "JOIN admissions a ON t.id_admission = a.id_admission " +
                "JOIN patients p ON a.id_patient = p.id_patient " +
                "LEFT JOIN salles s ON t.id_salle = s.id_salle " +
                "WHERE t.patient_state NOT IN ('Discharged','Cancelled','LeftWithoutSeen') " +
                "ORDER BY t.niveau_final ASC, t.date_heure_triage ASC";
        try (Connection c = getCnx();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(requete)) {
            while (rs.next()) {
                list.add(mapTriage(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // getData2 — SELECT by patient name using PreparedStatement
    public List<Triage> getData2(String keyword) {
        List<Triage> list = new ArrayList<>();
        String requete = "SELECT t.*, p.nom, p.prenom, s.nom_salle " +
                "FROM triage t " +
                "JOIN admissions a ON t.id_admission = a.id_admission " +
                "JOIN patients p ON a.id_patient = p.id_patient " +
                "LEFT JOIN salles s ON t.id_salle = s.id_salle " +
                "WHERE p.nom LIKE ? OR p.prenom LIKE ?";
        try (Connection c = getCnx(); PreparedStatement pst = c.prepareStatement(requete)) {
            pst.setString(1, "%" + keyword + "%");
            pst.setString(2, "%" + keyword + "%");
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(mapTriage(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // getData3 — SELECT waiting patients using Statement
    public List<Triage> getData3() {
        List<Triage> list = new ArrayList<>();
        String requete = "SELECT t.*, p.nom, p.prenom, s.nom_salle " +
                "FROM triage t " +
                "JOIN admissions a ON t.id_admission = a.id_admission " +
                "JOIN patients p ON a.id_patient = p.id_patient " +
                "LEFT JOIN salles s ON t.id_salle = s.id_salle " +
                "WHERE t.patient_state = 'WaitingRoom' " +
                "ORDER BY t.niveau_final ASC, t.date_heure_triage ASC";
        try (Connection c = getCnx();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(requete)) {
            while (rs.next()) {
                list.add(mapTriage(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // getData4 — SELECT by id using PreparedStatement
    public Triage getData4(int id) {
        String requete = "SELECT t.*, p.nom, p.prenom, s.nom_salle " +
                "FROM triage t " +
                "JOIN admissions a ON t.id_admission = a.id_admission " +
                "JOIN patients p ON a.id_patient = p.id_patient " +
                "LEFT JOIN salles s ON t.id_salle = s.id_salle " +
                "WHERE t.id_triage = ?";
        try (Connection c = getCnx(); PreparedStatement pst = c.prepareStatement(requete)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return mapTriage(rs);
            }
        } catch (SQLException e) {
            System.out.println("getData4 (join): " + e.getMessage());
        }
        // Fallback: load triage alone if admission/patient join fails
        String fallback = "SELECT t.*, NULL as nom, NULL as prenom, s.nom_salle " +
                "FROM triage t " +
                "LEFT JOIN salles s ON t.id_salle = s.id_salle " +
                "WHERE t.id_triage = ?";
        try (Connection c = getCnx(); PreparedStatement pst = c.prepareStatement(fallback)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return mapTriage(rs);
            }
        } catch (SQLException e) {
            System.out.println("getData4 (fallback): " + e.getMessage());
        }
        return null;
    }

    /** Alias for getData4 — used by controllers */
    public Triage getById(int id) {
        return getData4(id);
    }

    // discharge patient — frees room and sets date_liberation
    public void discharge(int idTriage) {
        String requete = "UPDATE triage SET patient_state='Discharged', date_liberation=NOW(), id_salle=NULL WHERE id_triage=?";
        try (Connection c = getCnx(); PreparedStatement pst = c.prepareStatement(requete)) {
            pst.setInt(1, idTriage);
            pst.executeUpdate();
            System.out.println("Patient decharge!");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Echec decharge: " + e.getMessage(), e);
        }
    }

    // update room assignment
    public void updateRoom(int idTriage, int idSalle, String state) {
        String requete = "UPDATE triage SET id_salle=?, patient_state=? WHERE id_triage=?";
        try (Connection c = getCnx(); PreparedStatement pst = c.prepareStatement(requete)) {
            if (idSalle == 0) pst.setNull(1, Types.INTEGER);
            else pst.setInt(1, idSalle);
            pst.setString(2, state);
            pst.setInt(3, idTriage);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Echec mise a jour salle: " + e.getMessage(), e);
        }
    }

    /** Fetch active admissions for ComboBox in Add screen */
    public List<AdmissionItem> getActiveAdmissions() {
        List<AdmissionItem> items = new ArrayList<>();
        String requete = "SELECT a.id_admission, p.nom, p.prenom " +
                "FROM admissions a " +
                "JOIN patients p ON a.id_patient = p.id_patient " +
                "ORDER BY a.id_admission DESC";
        try (Connection c = getCnx();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(requete)) {
            while (rs.next()) {
                items.add(new AdmissionItem(rs.getInt("id_admission"), rs.getString("nom"), rs.getString("prenom")));
            }
        } catch (SQLException e) {
            System.out.println("getActiveAdmissions: " + e.getMessage());
        }
        return items;
    }

    private Triage mapTriage(ResultSet rs) throws SQLException {
        Triage t = new Triage();
        t.setIdTriage(rs.getInt("id_triage"));
        t.setIdAdmission(rs.getInt("id_admission"));
        t.setIdPersonnel(rs.getInt("id_personnel"));

        // BUG-9 fix: handle SQL NULL for nullable int columns
        int idSalle = rs.getInt("id_salle");
        t.setIdSalle(rs.wasNull() ? 0 : idSalle);

        t.setConstancesTaSys(rs.getFloat("constantes_ta_sys"));
        t.setConstancesTaDia(rs.getFloat("constantes_ta_dia"));
        t.setConstancesPouls(rs.getInt("constantes_pouls"));
        t.setConstancesTemperature(rs.getFloat("constantes_temperature"));
        t.setSpo2(rs.getInt("spo2"));
        t.setGlycemie(rs.getFloat("glycemie"));
        t.setScoreDouleur(rs.getInt("score_douleur"));
        t.setGcsScore(rs.getInt("gcs_score"));
        t.setFrequenceRespiratoire(rs.getInt("frequence_respiratoire"));
        t.setSymptomes(rs.getString("symptomes"));
        t.setScoreCalcule(rs.getInt("score_calcule"));
        t.setNiveauAuto(rs.getInt("niveau_auto"));
        t.setNiveauFinal(rs.getInt("niveau_final"));
        t.setAnalyseAuto(rs.getString("analyse_auto"));
        t.setOverrideNote(rs.getString("override_note"));
        t.setPatientState(rs.getString("patient_state"));
        Timestamp ts = rs.getTimestamp("date_heure_triage");
        if (ts != null) t.setDateHeureTriage(ts.toLocalDateTime());

        // BUG-8 fix: read date_liberation
        Timestamp tsLib = rs.getTimestamp("date_liberation");
        if (tsLib != null) t.setDateLiberation(tsLib.toLocalDateTime());

        t.setNomPatient(rs.getString("nom") != null ? rs.getString("nom") : "—");
        t.setPrenomPatient(rs.getString("prenom") != null ? rs.getString("prenom") : "");
        t.setNomSalle(rs.getString("nom_salle"));

        t.setSyndromeCategory(rs.getString("syndrome_category"));
        t.setDureeSymptomes(rs.getString("duree_symptomes"));
        t.setContactCasSimilaires(rs.getString("contact_cas_similaires"));
        t.setVoyageRecent(rs.getInt("voyage_recent") == 1);
        t.setVoyageDestination(rs.getString("voyage_destination"));
        t.setContagionFlag(rs.getString("contagion_flag") != null ? rs.getString("contagion_flag") : "aucun");
        t.setSuspectedDisease(rs.getString("suspected_disease"));

        Timestamp tsOverride = rs.getTimestamp("date_override");
        if (tsOverride != null) t.setDateOverride(tsOverride.toLocalDateTime());

        // BUG-9 fix: handle SQL NULL for id_personnel_override
        int idPersOver = rs.getInt("id_personnel_override");
        t.setIdPersonnelOverride(rs.wasNull() ? 0 : idPersOver);
        return t;
    }

    // ══════════════════════════════════════════
    // updateSurveillance
    // ══════════════════════════════════════════
    public void updateSurveillance(int idTriage, String syndromeCategory, String dureeSymptomes,
                                   String contactCasSimilaires, boolean voyageRecent,
                                   String voyageDestination, String contagionFlag,
                                   String suspectedDisease) {
        String requete = "UPDATE triage SET syndrome_category=?, duree_symptomes=?, " +
                "contact_cas_similaires=?, voyage_recent=?, voyage_destination=?, " +
                "contagion_flag=?, suspected_disease=? WHERE id_triage=?";
        try (Connection c = getCnx(); PreparedStatement pst = c.prepareStatement(requete)) {
            pst.setString(1, syndromeCategory);
            pst.setString(2, dureeSymptomes);
            pst.setString(3, contactCasSimilaires);
            pst.setInt(4, voyageRecent ? 1 : 0);
            pst.setString(5, voyageDestination);
            pst.setString(6, contagionFlag != null ? contagionFlag : "aucun");
            pst.setString(7, suspectedDisease);
            pst.setInt(8, idTriage);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Echec mise a jour surveillance: " + e.getMessage(), e);
        }
    }

    public void updateContagionFlag(int idTriage, String contagionFlag, String suspectedDisease) {
        String requete = "UPDATE triage SET contagion_flag=?, suspected_disease=? WHERE id_triage=?";
        try (Connection c = getCnx(); PreparedStatement pst = c.prepareStatement(requete)) {
            pst.setString(1, contagionFlag != null ? contagionFlag : "aucun");
            pst.setString(2, suspectedDisease);
            pst.setInt(3, idTriage);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // BUG-7 fix: also escape backslashes
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "''");
    }
}
