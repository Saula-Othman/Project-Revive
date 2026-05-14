-- ═══════════════════════════════════════════════════════════════════════════
-- REVIVE — Migration Module 1 : Admission & Accueil
-- Exécuter UNE SEULE FOIS dans MySQL Workbench / phpMyAdmin
-- Prérequis : le script SETUP_COMPLET.sql du Module 3 doit avoir déjà été joué
-- ═══════════════════════════════════════════════════════════════════════════

USE revive;

-- ═══════════════════════════════════════════════════════════════════════════
-- ÉTAPE 1 : Enrichissement de la table `patients`
-- Colonnes de base existantes : id_patient, nom, prenom, date_naissance,
--   num_secu, groupe_sanguin, telephone_urgence, email
-- On ajoute les colonnes supplémentaires nécessaires au module Admission.
-- ═══════════════════════════════════════════════════════════════════════════

ALTER TABLE patients
    ADD COLUMN IF NOT EXISTS sexe                VARCHAR(10)  DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS email               VARCHAR(150) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS adresse             TEXT         DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS allergies           TEXT         DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS antecedents         TEXT         DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS nationalite         VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS num_cin             VARCHAR(30)  DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS contact_urgence_nom VARCHAR(150) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS contact_urgence_tel VARCHAR(30)  DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS actif               TINYINT(1)   NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS date_creation       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Index utile pour la recherche par CIN / numéro sécu
CREATE INDEX IF NOT EXISTS idx_patients_cin   ON patients(num_cin);
CREATE INDEX IF NOT EXISTS idx_patients_actif ON patients(actif);

-- ═══════════════════════════════════════════════════════════════════════════
-- ÉTAPE 2 : Enrichissement de la table `admissions`
-- Colonnes de base existantes : id_admission, id_patient, mode_arrivee,
--   motif_consultation, statut (+ constantes vitales ajoutées par Module 3)
-- On ajoute date_heure_arrivee si absente, puis les colonnes Admission.
-- ═══════════════════════════════════════════════════════════════════════════

ALTER TABLE admissions
    ADD COLUMN IF NOT EXISTS date_heure_arrivee DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS priorite_initiale  VARCHAR(30)  DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS agent_accueil_id   INT          DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS notes              TEXT         DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS ambulance_id       INT          DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS patient_inconnu    TINYINT(1)   NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS actif              TINYINT(1)   NOT NULL DEFAULT 1;

-- FK optionnelle vers ambulance_suivi (ajoutée après création de la table)
-- ALTER TABLE admissions ADD CONSTRAINT fk_admission_ambulance
--     FOREIGN KEY (ambulance_id) REFERENCES ambulance_suivi(id) ON DELETE SET NULL;

-- ═══════════════════════════════════════════════════════════════════════════
-- ÉTAPE 3 : Table `ambulance_suivi`
-- Nouveau — suivi GPS des véhicules en route vers les urgences.
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS ambulance_suivi (
    id                      INT AUTO_INCREMENT PRIMARY KEY,
    matricule               VARCHAR(50)    NOT NULL,
    latitude                DECIMAL(10, 7) DEFAULT NULL,
    longitude               DECIMAL(10, 7) DEFAULT NULL,
    eta_minutes             INT            DEFAULT NULL    COMMENT 'Temps estimé d''arrivée en minutes',
    niveau_urgence          VARCHAR(30)    DEFAULT 'Normal',
    patient_info_provisoire TEXT           DEFAULT NULL    COMMENT 'Infos patient avant création du dossier',
    statut                  VARCHAR(50)    NOT NULL DEFAULT 'En route',
    date_mise_a_jour        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    date_depart             DATETIME       DEFAULT NULL,
    date_arrivee_prevue     DATETIME       DEFAULT NULL,
    personnel_id            INT            DEFAULT NULL,
    admission_id            INT            DEFAULT NULL,
    INDEX idx_ambulance_statut      (statut),
    INDEX idx_ambulance_admission   (admission_id),
    INDEX idx_ambulance_personnel   (personnel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ═══════════════════════════════════════════════════════════════════════════
-- ÉTAPE 4 : Table `notifications`
-- Messagerie inter-modules (Admission → Triage, etc.).
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS notifications (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    source_module VARCHAR(100)  NOT NULL,
    cible_module  VARCHAR(100)  NOT NULL,
    type_notif    VARCHAR(50)   NOT NULL,
    titre         VARCHAR(255)  NOT NULL,
    message       TEXT          DEFAULT NULL,
    patient_id    INT           DEFAULT NULL,
    admission_id  INT           DEFAULT NULL,
    ambulance_id  INT           DEFAULT NULL,
    lu            TINYINT(1)    NOT NULL DEFAULT 0,
    date_creation TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notif_cible        (cible_module),
    INDEX idx_notif_patient      (patient_id),
    INDEX idx_notif_admission    (admission_id),
    INDEX idx_notif_lu           (lu)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ═══════════════════════════════════════════════════════════════════════════
-- ÉTAPE 5 : Table `historique_patient`
-- Dossier médical inter-établissements — agrège les documents de tous modules.
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS historique_patient (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    patient_id       INT           NOT NULL,
    admission_id     INT           DEFAULT NULL,
    type_document    VARCHAR(80)   NOT NULL    COMMENT 'Consultation, Ordonnance, Resultat Examen, Hospitalisation, ...',
    titre            VARCHAR(255)  NOT NULL,
    contenu          TEXT          DEFAULT NULL,
    medecin_nom      VARCHAR(150)  DEFAULT NULL,
    etablissement    VARCHAR(200)  DEFAULT NULL,
    source           VARCHAR(80)   NOT NULL DEFAULT 'LOCAL'
                         COMMENT 'LOCAL | MDIA_API | MODULE_1_ADMISSION | MODULE_3 | MODULE_4',
    date_consultation DATE         DEFAULT NULL,
    date_import      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id)   REFERENCES patients(id_patient) ON DELETE CASCADE,
    INDEX idx_hist_patient     (patient_id),
    INDEX idx_hist_source      (source),
    INDEX idx_hist_date        (date_consultation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ═══════════════════════════════════════════════════════════════════════════
-- ÉTAPE 6 : Vue `v_historique_patient_complet`
-- Vue unifiée du dossier patient : historique_patient + consultations
-- + ordonnances + résultats d'examens.
-- Utilisée par HistoriqueDAO.findAllByPatient().
-- ═══════════════════════════════════════════════════════════════════════════

CREATE OR REPLACE VIEW v_historique_patient_complet AS

    -- 1. Entrées directes (tous modules, tous types de documents)
    SELECT
        h.id,
        h.patient_id,
        h.admission_id,
        h.type_document,
        h.titre,
        h.contenu,
        h.medecin_nom,
        h.etablissement,
        h.source,
        h.date_consultation,
        h.date_import
    FROM historique_patient h

    UNION ALL

    -- 2. Consultations du module Médecin (Module 3)
    SELECT
        c.id_consultation                        AS id,
        a.id_patient                             AS patient_id,
        c.id_admission                           AS admission_id,
        'Consultation'                           AS type_document,
        'Consultation aux urgences'              AS titre,
        CONCAT_WS('\n',
            IF(c.diagnostic  IS NOT NULL, CONCAT('Diagnostic: ',  c.diagnostic),  NULL),
            IF(c.orientation IS NOT NULL, CONCAT('Orientation: ', c.orientation), NULL)
        )                                        AS contenu,
        NULL                                     AS medecin_nom,
        'Service des Urgences'                   AS etablissement,
        'MODULE_3'                               AS source,
        DATE(c.date_heure_debut)                 AS date_consultation,
        c.date_heure_debut                       AS date_import
    FROM consultations c
    JOIN admissions a ON c.id_admission = a.id_admission

    UNION ALL

    -- 3. Ordonnances du module Médecin (Module 3)
    SELECT
        o.id_ordonnance                          AS id,
        a.id_patient                             AS patient_id,
        c.id_admission                           AS admission_id,
        'Ordonnance'                             AS type_document,
        'Ordonnance médicale'                    AS titre,
        CONCAT_WS('\n',
            IF(o.medicament IS NOT NULL, CONCAT('Médicament: ', o.medicament), NULL),
            IF(o.posologie  IS NOT NULL, CONCAT('Posologie: ',  o.posologie),  NULL),
            IF(o.duree_jours > 0,        CONCAT('Durée: ',      o.duree_jours, ' jour(s)'), NULL)
        )                                        AS contenu,
        NULL                                     AS medecin_nom,
        'Service des Urgences'                   AS etablissement,
        'MODULE_3'                               AS source,
        DATE(o.date_ordonnance)                  AS date_consultation,
        o.date_ordonnance                        AS date_import
    FROM ordonnances o
    JOIN consultations c ON o.id_consultation = c.id_consultation
    JOIN admissions a    ON c.id_admission    = a.id_admission

    UNION ALL

    -- 4. Résultats d'examens du module Biologiste (Module 4)
    SELECT
        r.id_resultat                            AS id,
        a.id_patient                             AS patient_id,
        c.id_admission                           AS admission_id,
        'Resultat Examen'                        AS type_document,
        CONCAT('Résultat: ', COALESCE(ed.type_examen, 'Examen')) AS titre,
        CONCAT_WS('\n',
            r.compte_rendu_texte,
            IF(r.recommandation IS NOT NULL, CONCAT('Recommandation: ', r.recommandation), NULL)
        )                                        AS contenu,
        NULL                                     AS medecin_nom,
        'Service Biologie/Radiologie'            AS etablissement,
        'MODULE_4'                               AS source,
        DATE(r.date_resultat)                    AS date_consultation,
        r.date_resultat                          AS date_import
    FROM resultats r
    JOIN examens_demandes ed ON r.id_demande      = ed.id_demande
    JOIN consultations c     ON ed.id_consultation = c.id_consultation
    JOIN admissions a        ON c.id_admission     = a.id_admission;

-- ═══════════════════════════════════════════════════════════════════════════
-- ÉTAPE 7 : Personnels de type "Agent Accueil" pour les tests
-- (insère seulement si aucun Agent Accueil n'existe)
-- ═══════════════════════════════════════════════════════════════════════════

INSERT INTO personnel (nom, prenom, role, identifiant, mot_de_passe, statut, premier_connexion)
SELECT * FROM (VALUES
    ROW('Missaoui', 'Sara',   'Agent Accueil', 's.missaoui', 'pass123', 'ACTIF', FALSE),
    ROW('Jomni',    'Khalil', 'Agent Accueil', 'k.jomni',   'pass123', 'ACTIF', FALSE)
) AS tmp(nom, prenom, role, identifiant, mot_de_passe, statut, premier_connexion)
WHERE NOT EXISTS (SELECT 1 FROM personnel WHERE role = 'Agent Accueil');

-- ═══════════════════════════════════════════════════════════════════════════
-- ÉTAPE 8 : Colonne `snapshot` dans `audit_log`
-- AuditService.log() écrit un JSON de l'agent dans cette colonne pour
-- permettre la restauration. La table existe déjà mais sans cette colonne.
-- ═══════════════════════════════════════════════════════════════════════════

ALTER TABLE audit_log
    ADD COLUMN IF NOT EXISTS snapshot MEDIUMTEXT DEFAULT NULL
        COMMENT 'JSON snapshot de l''agent au moment de l''action (pour restauration)';

-- ═══════════════════════════════════════════════════════════════════════════
-- VÉRIFICATION
-- ═══════════════════════════════════════════════════════════════════════════

SELECT 'patients colonnes ajoutées'      AS check_point, COUNT(*) AS rows FROM patients;
SELECT 'admissions colonnes ajoutées'    AS check_point, COUNT(*) AS rows FROM admissions;
SELECT 'ambulance_suivi créée'           AS check_point, COUNT(*) AS rows FROM ambulance_suivi;
SELECT 'notifications créée'            AS check_point, COUNT(*) AS rows FROM notifications;
SELECT 'historique_patient créée'       AS check_point, COUNT(*) AS rows FROM historique_patient;
SELECT 'Agents Accueil dans personnel'  AS check_point, COUNT(*) AS rows FROM personnel WHERE role = 'Agent Accueil';
SELECT 'audit_log snapshot ajoutée'     AS check_point, COUNT(*) AS rows FROM audit_log;
