-- ═══════════════════════════════════════════════════════════════════════════
-- REVIVE — Script de configuration COMPLET — Module 3 (Médecin)
-- Exécuter UNE SEULE FOIS dans MySQL Workbench / phpMyAdmin
-- Ordre : migrations → données → triage → email
-- ═══════════════════════════════════════════════════════════════════════════

USE revive;

-- ═══════════════════════════════════════════════════════════════════════════
-- ÉTAPE 1 : MIGRATIONS — Ajouter les colonnes manquantes
-- (email_feature_migration.sql + migration_labo.sql)
-- ═══════════════════════════════════════════════════════════════════════════

-- Colonne email dans patients
ALTER TABLE patients
    ADD COLUMN IF NOT EXISTS email VARCHAR(255) DEFAULT NULL;

-- Colonnes ICD et labo dans consultations
ALTER TABLE consultations
    ADD COLUMN IF NOT EXISTS icd_code       VARCHAR(20)  DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS analyses       TEXT         DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS imageries      TEXT         DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS statut_demande VARCHAR(30)  DEFAULT 'Non envoyee';

-- Constantes vitales dans admissions
ALTER TABLE admissions
    ADD COLUMN IF NOT EXISTS constances_pouls          INT          DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS constances_temperature    DECIMAL(4,1) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS spo2                      INT          DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS glycemie                  DECIMAL(4,2) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS score_douleur             INT          DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS frequence_respiratoire    INT          DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS gcs_score                 INT          DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS motif_consultation        TEXT         DEFAULT NULL;

-- Index performance
CREATE INDEX IF NOT EXISTS idx_consultations_icd ON consultations(icd_code);
CREATE INDEX IF NOT EXISTS idx_patients_email    ON patients(email);

-- ═══════════════════════════════════════════════════════════════════════════
-- ÉTAPE 2 : DONNÉES DE BASE — Patients, Personnel, Admissions, Consultations
-- (data_test.sql) — Insère seulement si les tables sont vides
-- ═══════════════════════════════════════════════════════════════════════════

-- Patients (8 patients de test)
INSERT INTO patients (nom, prenom, date_naissance, num_secu, groupe_sanguin, telephone_urgence)
SELECT * FROM (VALUES
    ROW('Benali',   'Karim',   '1985-03-12', '1850312100001', 'A+',  '0612345678'),
    ROW('Trabelsi', 'Sonia',   '1992-07-24', '2920724200002', 'B+',  '0623456789'),
    ROW('Mansouri', 'Ahmed',   '1978-11-05', '1781105300003', 'O+',  '0634567890'),
    ROW('Chaabane', 'Fatma',   '2001-01-30', '2010130400004', 'AB+', '0645678901'),
    ROW('Jebali',   'Mohamed', '1965-09-18', '1650918500005', 'A-',  '0656789012'),
    ROW('Riahi',    'Leila',   '1990-04-07', '2900407600006', 'O-',  '0667890123'),
    ROW('Hamdi',    'Youssef', '1955-12-22', '1551222700007', 'B-',  '0678901234'),
    ROW('Bouzid',   'Amira',   '2005-06-14', '2050614800008', 'A+',  '0689012345')
) AS tmp(nom, prenom, date_naissance, num_secu, groupe_sanguin, telephone_urgence)
WHERE NOT EXISTS (SELECT 1 FROM patients WHERE nom = tmp.nom AND prenom = tmp.prenom);

-- Personnel médical (avec statut ACTIF et premier_connexion FALSE pour login direct)
INSERT INTO personnel (nom, prenom, role, specialite, identifiant, mot_de_passe, statut, premier_connexion)
SELECT * FROM (VALUES
    ROW('Gharbi',    'Nabil',  'Medecin Urgentiste', 'Cardiologie',      'n.gharbi',    'pass123', 'ACTIF', FALSE),
    ROW('Saidi',     'Rania',  'Medecin Urgentiste', 'Neurologie',       'r.saidi',     'pass123', 'ACTIF', FALSE),
    ROW('Khelifi',   'Tarek',  'Medecin Urgentiste', 'Traumatologie',    't.khelifi',   'pass123', 'ACTIF', FALSE),
    ROW('Bouaziz',   'Ines',   'Medecin Urgentiste', 'Medecine Interne', 'i.bouaziz',   'pass123', 'ACTIF', FALSE),
    ROW('Maaloul',   'Slim',   'Infirmier Triage',   NULL,               's.maaloul',   'pass123', 'ACTIF', FALSE),
    ROW('Ferchichi', 'Olfa',   'Infirmier Triage',   NULL,               'o.ferchichi', 'pass123', 'ACTIF', FALSE)
) AS tmp(nom, prenom, role, specialite, identifiant, mot_de_passe, statut, premier_connexion)
WHERE NOT EXISTS (SELECT 1 FROM personnel WHERE identifiant = tmp.identifiant);

-- Fix existing personnel that have NULL statut (login fix)
UPDATE personnel SET statut = 'ACTIF', premier_connexion = FALSE
WHERE statut IS NULL OR statut = '';

-- Admissions — liées aux patients par position (id_patient 1..8)
-- On insère seulement si aucune admission n'existe pour ces patients
INSERT INTO admissions (id_patient, date_heure_arrivee, mode_arrivee, motif_consultation, statut)
SELECT p.id_patient, NOW(), src.mode_arrivee, src.motif, src.statut
FROM (
    SELECT 1 AS rang, 'Ambulance'      AS mode_arrivee, 'Douleurs thoraciques intenses'         AS motif, 'Active'   AS statut UNION ALL
    SELECT 2,         'SMUR',                           'Traumatisme cranien suite a chute',              'Active'            UNION ALL
    SELECT 3,         'Ambulance',                      'Detresse respiratoire aigue',                    'Active'            UNION ALL
    SELECT 4,         'Propres moyens',                 'Fracture ouverte membre inferieur',              'Active'            UNION ALL
    SELECT 5,         'Ambulance',                      'Crise hypertensive severe',                      'Active'            UNION ALL
    SELECT 6,         'Propres moyens',                 'Intoxication medicamenteuse',                    'Active'            UNION ALL
    SELECT 7,         'Propres moyens',                 'Douleurs abdominales aigues',                    'Active'            UNION ALL
    SELECT 8,         'SMUR',                           'Convulsions febriles',                           'Active'
) src
JOIN (
    SELECT id_patient, ROW_NUMBER() OVER (ORDER BY id_patient) AS rang
    FROM patients
    ORDER BY id_patient
    LIMIT 8
) p ON p.rang = src.rang
WHERE NOT EXISTS (
    SELECT 1 FROM admissions a WHERE a.id_patient = p.id_patient AND a.motif_consultation = src.motif
);

-- Consultations — liées aux admissions réelles
-- On récupère les vrais id_admission et id_personnel depuis la DB
INSERT INTO consultations (id_admission, id_personnel_medecin, date_heure_debut, date_heure_fin, diagnostic, orientation)
SELECT
    a.id_admission,
    p.id_personnel,
    src.debut,
    src.fin,
    src.diagnostic,
    src.orientation
FROM (
    SELECT 1 AS rang_adm, 1 AS rang_med, DATE_SUB(NOW(), INTERVAL 3 HOUR)  AS debut, NULL                                AS fin, 'Syndrome coronarien aigu suspecte. ECG en cours. Troponines elevees a 2.4 ng/mL.'                    AS diagnostic, 'Hospitalisation' AS orientation UNION ALL
    SELECT 2, 2, DATE_SUB(NOW(), INTERVAL 5 HOUR),  DATE_SUB(NOW(), INTERVAL 4 HOUR),  'Contusion cerebrale legere. Scanner cerebral normal. Pas de lesion hemorragique.',                       'Sortie'          UNION ALL
    SELECT 3, 3, DATE_SUB(NOW(), INTERVAL 1 DAY),   DATE_SUB(NOW(), INTERVAL 23 HOUR), 'Pneumonie bilaterale avec hypoxemie (SpO2 88%). Mise sous oxygenotherapie 6L/min.',                      'Hospitalisation' UNION ALL
    SELECT 4, 1, DATE_SUB(NOW(), INTERVAL 2 DAY),   DATE_SUB(NOW(), INTERVAL 47 HOUR), 'Fracture ouverte tibia-perone gauche. Nettoyage et immobilisation provisoire.',                          'Transfert'       UNION ALL
    SELECT 5, 4, DATE_SUB(NOW(), INTERVAL 6 HOUR),  NULL,                              'HTA maligne. PA 210/130 mmHg. Traitement antihypertenseur IV en cours.',                                 'Hospitalisation'
) src
JOIN (
    SELECT id_admission, ROW_NUMBER() OVER (ORDER BY id_admission) AS rang
    FROM admissions ORDER BY id_admission LIMIT 5
) a ON a.rang = src.rang_adm
JOIN (
    SELECT id_personnel, ROW_NUMBER() OVER (ORDER BY id_personnel) AS rang
    FROM personnel WHERE role = 'Medecin Urgentiste' ORDER BY id_personnel LIMIT 4
) p ON p.rang = src.rang_med
WHERE NOT EXISTS (
    SELECT 1 FROM consultations c WHERE c.id_admission = a.id_admission AND c.diagnostic = src.diagnostic
);

-- Ordonnances — liées aux consultations réelles
INSERT INTO ordonnances (id_consultation, medicament, posologie, duree_jours)
SELECT c.id_consultation, src.medicament, src.posologie, src.duree
FROM (
    SELECT 1 AS rang_c, 'Aspirine 100mg'    AS medicament, '1 comprime par jour le matin'                        AS posologie, 30 AS duree UNION ALL
    SELECT 1,           'Clopidogrel 75mg',                 '1 comprime par jour',                                              30         UNION ALL
    SELECT 1,           'Heparine IV',                      'Perfusion continue selon protocole cardiologie',                    3          UNION ALL
    SELECT 2,           'Paracetamol 1g',                   '1 comprime toutes les 6h si douleur (max 4/jour)',                  5          UNION ALL
    SELECT 2,           'Ibuprofene 400mg',                 '1 comprime 3 fois par jour pendant les repas',                     3          UNION ALL
    SELECT 3,           'Amoxicilline 1g',                  '1 comprime 3 fois par jour',                                       10         UNION ALL
    SELECT 3,           'Prednisolone 40mg',                '1 comprime le matin a jeun',                                       7          UNION ALL
    SELECT 3,           'Salbutamol spray',                 '2 bouffees toutes les 4h en cas de dyspnee',                       14         UNION ALL
    SELECT 4,           'Amoxicilline-Acide clavulanique',  '1 comprime 3 fois par jour pendant les repas',                     7          UNION ALL
    SELECT 4,           'Tramadol 50mg',                    '1 gelule toutes les 6h si douleur intense',                        5          UNION ALL
    SELECT 5,           'Nicardipine IV',                   'Perfusion IV selon protocole urgences hypertensives',               2          UNION ALL
    SELECT 5,           'Amlodipine 10mg',                  '1 comprime par jour le soir',                                      30
) src
JOIN (
    SELECT id_consultation, ROW_NUMBER() OVER (ORDER BY id_consultation) AS rang
    FROM consultations ORDER BY id_consultation LIMIT 5
) c ON c.rang = src.rang_c
WHERE NOT EXISTS (
    SELECT 1 FROM ordonnances o WHERE o.id_consultation = c.id_consultation AND o.medicament = src.medicament
);

-- ═══════════════════════════════════════════════════════════════════════════
-- ÉTAPE 3 : TABLE TRIAGE — Création + données de test
-- (database_triage_setup.sql)
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS triage (
    id_triage                INT AUTO_INCREMENT PRIMARY KEY,
    id_admission             INT NOT NULL,
    constantes_ta_sys        DECIMAL(5,2) DEFAULT 0,
    constantes_ta_dia        DECIMAL(5,2) DEFAULT 0,
    constantes_pouls         DECIMAL(5,2) DEFAULT 0,
    constantes_temperature   DECIMAL(4,2) DEFAULT 0,
    spo2                     DECIMAL(5,2) DEFAULT 0,
    glycemie                 DECIMAL(5,2) DEFAULT 0,
    score_douleur            DECIMAL(3,1) DEFAULT 0,
    gcs_score                INT          DEFAULT 15,
    frequence_respiratoire   INT          DEFAULT 0,
    symptomes                TEXT,
    score_calcule            INT          DEFAULT 0,
    niveau_auto              VARCHAR(50),
    niveau_final             VARCHAR(50),
    analyse_auto             TEXT,
    patient_state            VARCHAR(50),
    -- Champs override (Module 2)
    override_note            TEXT,
    date_override            TIMESTAMP    NULL,
    id_personnel_override    INT          DEFAULT NULL,
    -- Champs surveillance épidémiologique
    syndrome_category        VARCHAR(100) DEFAULT NULL,
    duree_symptomes          VARCHAR(50)  DEFAULT NULL,
    contact_cas_similaires   VARCHAR(10)  DEFAULT NULL,
    voyage_recent            TINYINT(1)   DEFAULT 0,
    voyage_destination       VARCHAR(200) DEFAULT NULL,
    contagion_flag           VARCHAR(20)  DEFAULT 'aucun',
    suspected_disease        VARCHAR(200) DEFAULT NULL,
    -- Champs état patient
    id_salle                 INT          DEFAULT NULL,
    date_liberation          TIMESTAMP    NULL,
    date_heure_triage        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_admission) REFERENCES admissions(id_admission) ON DELETE CASCADE,
    INDEX idx_admission  (id_admission),
    INDEX idx_niveau     (niveau_final),
    INDEX idx_state      (patient_state),
    INDEX idx_date       (date_heure_triage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Triage data — liée aux vraies admissions
INSERT INTO triage (id_admission, constantes_ta_sys, constantes_ta_dia, constantes_pouls,
    constantes_temperature, spo2, glycemie, score_douleur, gcs_score,
    frequence_respiratoire, symptomes, score_calcule, niveau_auto, niveau_final, patient_state)
SELECT a.id_admission, src.ta_sys, src.ta_dia, src.pouls, src.temp, src.spo2,
       src.glyc, src.douleur, src.gcs, src.freq, src.symptomes, src.score, src.niveau, src.niveau, src.niveau
FROM (
    SELECT 1 AS rang, 180 AS ta_sys, 110 AS ta_dia, 120 AS pouls, 39.5 AS temp, 85 AS spo2, 2.5 AS glyc, 9 AS douleur, 10 AS gcs, 28 AS freq, 'Douleur thoracique intense, dyspnee severe, confusion'  AS symptomes, 95 AS score, 'CRITIQUE' AS niveau UNION ALL
    SELECT 2,         160,           95,            105,           38.8,          90,          3.2,          7,            13,          24,           'Fievre elevee, tachycardie, douleur abdominale aigue',                                                  75,           'URGENT'   UNION ALL
    SELECT 3,         135,           85,            88,            37.8,          95,          5.5,          5,            15,          18,           'Cephalees moderees, nausees, vertiges',                                                                 45,           'MODERE'   UNION ALL
    SELECT 4,         120,           80,            72,            36.8,          98,          5.0,          2,            15,          16,           'Consultation de suivi, etat general bon',                                                               15,           'STABLE'
) src
JOIN (
    SELECT id_admission, ROW_NUMBER() OVER (ORDER BY id_admission) AS rang
    FROM admissions ORDER BY id_admission LIMIT 4
) a ON a.rang = src.rang
WHERE NOT EXISTS (
    SELECT 1 FROM triage t WHERE t.id_admission = a.id_admission
);

-- ═══════════════════════════════════════════════════════════════════════════
-- ÉTAPE 4 : DONNÉES EMAIL — Emails patients + codes ICD
-- (email_test_data.sql)
-- ═══════════════════════════════════════════════════════════════════════════

-- Emails patients
UPDATE patients SET email = 'test.patient1@gmail.com' WHERE id_patient = (SELECT id_patient FROM (SELECT id_patient FROM patients ORDER BY id_patient LIMIT 1 OFFSET 0) t);
UPDATE patients SET email = 'test.patient2@gmail.com' WHERE id_patient = (SELECT id_patient FROM (SELECT id_patient FROM patients ORDER BY id_patient LIMIT 1 OFFSET 1) t);
UPDATE patients SET email = 'test.patient3@gmail.com' WHERE id_patient = (SELECT id_patient FROM (SELECT id_patient FROM patients ORDER BY id_patient LIMIT 1 OFFSET 2) t);

-- Codes ICD-10 sur les consultations existantes
UPDATE consultations SET icd_code = 'J18.9' WHERE diagnostic LIKE '%pneumonie%'     AND (icd_code IS NULL OR icd_code = '');
UPDATE consultations SET icd_code = 'I10'   WHERE diagnostic LIKE '%HTA%'           AND (icd_code IS NULL OR icd_code = '');
UPDATE consultations SET icd_code = 'S52'   WHERE diagnostic LIKE '%fracture%'      AND (icd_code IS NULL OR icd_code = '');
UPDATE consultations SET icd_code = 'S09'   WHERE diagnostic LIKE '%cranien%'       AND (icd_code IS NULL OR icd_code = '');
UPDATE consultations SET icd_code = 'I21'   WHERE diagnostic LIKE '%coronarien%'    AND (icd_code IS NULL OR icd_code = '');

-- ═══════════════════════════════════════════════════════════════════════════
-- VÉRIFICATION FINALE
-- ═══════════════════════════════════════════════════════════════════════════

SELECT '=== VERIFICATION ===' AS info;

SELECT 'patients'      AS table_name, COUNT(*) AS nb FROM patients
UNION ALL SELECT 'personnel',     COUNT(*) FROM personnel
UNION ALL SELECT 'admissions',    COUNT(*) FROM admissions
UNION ALL SELECT 'consultations', COUNT(*) FROM consultations
UNION ALL SELECT 'ordonnances',   COUNT(*) FROM ordonnances
UNION ALL SELECT 'triage',        COUNT(*) FROM triage;

-- ── DIAGNOSTIC CRITIQUE: voir pourquoi les noms ne s'affichent pas ──────
SELECT '=== DIAGNOSTIC JOIN ===' AS info;

-- Montre les id_admission dans consultations vs ce qui existe dans admissions
SELECT
    c.id_consultation,
    c.id_admission                          AS c_id_admission,
    a.id_admission                          AS a_id_admission_found,
    a.id_patient                            AS a_id_patient,
    p.id_patient                            AS p_id_patient_found,
    CONCAT(p.prenom,' ',p.nom)              AS nom_patient,
    CASE
        WHEN a.id_admission IS NULL THEN 'ADMISSION MANQUANTE'
        WHEN p.id_patient   IS NULL THEN 'PATIENT MANQUANT'
        ELSE 'OK'
    END AS statut_join
FROM consultations c
LEFT JOIN admissions a ON a.id_admission = c.id_admission
LEFT JOIN patients   p ON p.id_patient   = a.id_patient
ORDER BY c.id_consultation;

-- Montre la structure réelle de la table admissions
SELECT '=== COLONNES TABLE ADMISSIONS ===' AS info;
DESCRIBE admissions;

-- Montre la structure réelle de la table patients
SELECT '=== COLONNES TABLE PATIENTS ===' AS info;
DESCRIBE patients;
