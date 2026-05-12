-- ═══════════════════════════════════════════════════════════════════════════
-- Données de test pour la fonctionnalité Email Post-Consultation
-- REVIVE Module 3 — Urgences
-- ═══════════════════════════════════════════════════════════════════════════

-- ⚠️  ATTENTION : Ces données sont pour TEST uniquement
-- Ne pas exécuter en production sans adaptation

-- ── 1. Mise à jour emails patients existants ──────────────────────────────
-- Remplacez les IDs par vos vrais IDs de patients

UPDATE patients SET email = 'test.patient1@gmail.com' WHERE id_patient = 1;
UPDATE patients SET email = 'test.patient2@gmail.com' WHERE id_patient = 2;
UPDATE patients SET email = 'test.patient3@gmail.com' WHERE id_patient = 3;

-- ── 2. Ajout codes ICD-10 aux consultations existantes ────────────────────
-- Exemples de codes ICD-10 courants

-- Pneumonie
UPDATE consultations SET icd_code = 'J18.9' 
WHERE diagnostic LIKE '%pneumonie%' AND icd_code IS NULL;

-- Hypertension
UPDATE consultations SET icd_code = 'I10' 
WHERE diagnostic LIKE '%hypertension%' AND icd_code IS NULL;

-- Diabète
UPDATE consultations SET icd_code = 'E11' 
WHERE diagnostic LIKE '%diabete%' AND icd_code IS NULL;

-- Gastrite
UPDATE consultations SET icd_code = 'K29' 
WHERE diagnostic LIKE '%gastrite%' AND icd_code IS NULL;

-- Infection respiratoire
UPDATE consultations SET icd_code = 'J06.9' 
WHERE diagnostic LIKE '%infection%' AND diagnostic LIKE '%respiratoire%' AND icd_code IS NULL;

-- Fracture
UPDATE consultations SET icd_code = 'S52' 
WHERE diagnostic LIKE '%fracture%' AND icd_code IS NULL;

-- ── 3. Création patient de test complet ───────────────────────────────────

-- Patient test
INSERT INTO patients (nom, prenom, email, telephone, date_naissance, sexe, adresse, cin)
VALUES ('TESTEUR', 'Jean', 'votre.email.test@gmail.com', '+216 12 345 678', '1985-05-15', 'M', '123 Rue Test, Tunis', '12345678');

SET @test_patient_id = LAST_INSERT_ID();

-- Admission test avec constantes vitales
INSERT INTO admissions (
    id_patient, 
    date_heure_arrivee, 
    mode_arrivee, 
    motif_consultation,
    constances_pouls,
    constances_temperature,
    spo2,
    glycemie,
    score_douleur,
    frequence_respiratoire,
    gcs_score,
    statut
)
VALUES (
    @test_patient_id,
    NOW(),
    'Ambulance',
    'Douleur thoracique intense avec dyspnee',
    95,           -- Pouls élevé
    38.2,         -- Fièvre
    92,           -- SpO2 bas (attention)
    1.15,         -- Glycémie légèrement élevée
    7,            -- Douleur importante
    24,           -- Fréquence respiratoire élevée
    15,           -- GCS normal
    'Active'
);

SET @test_admission_id = LAST_INSERT_ID();

-- Consultation test avec code ICD
INSERT INTO consultations (
    id_admission,
    id_personnel_medecin,
    date_heure_debut,
    diagnostic,
    icd_code,
    orientation,
    analyses,
    imageries,
    statut_demande
)
VALUES (
    @test_admission_id,
    1,  -- Remplacez par un vrai ID médecin
    NOW(),
    'Pneumonie communautaire aigue avec insuffisance respiratoire moderee',
    'J18.9',
    'Hospitalisation',
    'NFS, CRP, Procalcitonine',
    'Radio thorax face + profil',
    'Envoyee'
);

SET @test_consultation_id = LAST_INSERT_ID();

-- Ordonnances test
INSERT INTO ordonnances (id_consultation, medicament, posologie, duree_jours)
VALUES 
    (@test_consultation_id, 'Amoxicilline 1g', '3 fois par jour', 7),
    (@test_consultation_id, 'Paracetamol 1g', 'Toutes les 6h si fievre', 5),
    (@test_consultation_id, 'Ventoline (Salbutamol)', '2 bouffees 3 fois par jour', 10);

-- ── 4. Vérification des données ───────────────────────────────────────────

SELECT 
    'PATIENT TEST' AS Type,
    p.id_patient AS ID,
    CONCAT(p.prenom, ' ', p.nom) AS Nom,
    p.email AS Email,
    'OK' AS Statut
FROM patients p
WHERE p.id_patient = @test_patient_id;

SELECT 
    'ADMISSION TEST' AS Type,
    a.id_admission AS ID,
    a.motif_consultation AS Motif,
    CONCAT(a.constances_pouls, ' bpm') AS Pouls,
    CONCAT(a.constances_temperature, '°C') AS Temperature,
    CONCAT(a.spo2, '%') AS SpO2,
    'OK' AS Statut
FROM admissions a
WHERE a.id_admission = @test_admission_id;

SELECT 
    'CONSULTATION TEST' AS Type,
    c.id_consultation AS ID,
    c.diagnostic AS Diagnostic,
    c.icd_code AS 'Code ICD',
    c.orientation AS Orientation,
    'OK' AS Statut
FROM consultations c
WHERE c.id_consultation = @test_consultation_id;

SELECT 
    'ORDONNANCES TEST' AS Type,
    COUNT(*) AS Nombre,
    GROUP_CONCAT(o.medicament SEPARATOR ', ') AS Medicaments,
    'OK' AS Statut
FROM ordonnances o
WHERE o.id_consultation = @test_consultation_id;

-- ── 5. Instructions pour tester ───────────────────────────────────────────

SELECT '
═══════════════════════════════════════════════════════════════════════════
  DONNÉES DE TEST CRÉÉES AVEC SUCCÈS
═══════════════════════════════════════════════════════════════════════════

POUR TESTER L''ENVOI D''EMAIL :

1. Configurez config.properties avec votre Gmail App Password
2. Remplacez "votre.email.test@gmail.com" par votre vraie adresse
3. Lancez l''application REVIVE
4. Allez dans la liste des consultations
5. Trouvez la consultation de "Jean TESTEUR"
6. Cliquez sur le bouton "✓ Clôturer"
7. Vérifiez votre boîte email (peut prendre 10-30 secondes)

CONSULTATION TEST :
- Patient    : Jean TESTEUR
- Diagnostic : Pneumonie communautaire
- Code ICD   : J18.9
- Ordonnances: 3 médicaments
- Constantes : Toutes remplies avec valeurs réalistes

L''email contiendra :
✅ Informations MedlinePlus sur la pneumonie
✅ Constantes vitales avec code couleur
✅ 3 ordonnances
✅ Conseils pratiques en français
✅ Signes d''alerte pour retour en urgence

═══════════════════════════════════════════════════════════════════════════
' AS Instructions;

-- ── 6. Nettoyage (optionnel) ──────────────────────────────────────────────
-- Décommentez pour supprimer les données de test

/*
DELETE FROM ordonnances WHERE id_consultation = @test_consultation_id;
DELETE FROM consultations WHERE id_consultation = @test_consultation_id;
DELETE FROM admissions WHERE id_admission = @test_admission_id;
DELETE FROM patients WHERE id_patient = @test_patient_id;

SELECT 'Données de test supprimées' AS Statut;
*/
