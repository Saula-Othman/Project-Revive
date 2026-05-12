-- ============================================================
-- REVIVE – Données de test pour le Module 3
-- Base : revive  |  Exécuter dans MySQL Workbench ou WAMP
-- ============================================================

USE revive;

-- ============================================================
-- 1. PATIENTS
--    Colonnes : nom, prenom, date_naissance, num_secu,
--               groupe_sanguin, telephone_urgence
-- ============================================================
INSERT INTO patients (nom, prenom, date_naissance, num_secu, groupe_sanguin, telephone_urgence) VALUES
('Benali',    'Karim',    '1985-03-12', '1850312100001', 'A+', '0612345678'),
('Trabelsi',  'Sonia',    '1992-07-24', '2920724200002', 'B+', '0623456789'),
('Mansouri',  'Ahmed',    '1978-11-05', '1781105300003', 'O+', '0634567890'),
('Chaabane',  'Fatma',    '2001-01-30', '2010130400004', 'AB+','0645678901'),
('Jebali',    'Mohamed',  '1965-09-18', '1650918500005', 'A-', '0656789012'),
('Riahi',     'Leila',    '1990-04-07', '2900407600006', 'O-', '0667890123'),
('Hamdi',     'Youssef',  '1955-12-22', '1551222700007', 'B-', '0678901234'),
('Bouzid',    'Amira',    '2005-06-14', '2050614800008', 'A+', '0689012345');

-- ============================================================
-- 2. PERSONNEL MÉDICAL
--    Colonnes : nom, prenom, role, specialite,
--               identifiant, mot_de_passe
-- ============================================================
INSERT INTO personnel (nom, prenom, role, specialite, identifiant, mot_de_passe) VALUES
('Gharbi',    'Nabil',  'Medecin Urgentiste', 'Cardiologie',       'n.gharbi',    'pass123'),
('Saidi',     'Rania',  'Medecin Urgentiste', 'Neurologie',        'r.saidi',     'pass123'),
('Khelifi',   'Tarek',  'Medecin Urgentiste', 'Traumatologie',     't.khelifi',   'pass123'),
('Bouaziz',   'Ines',   'Medecin Urgentiste', 'Médecine Interne',  'i.bouaziz',   'pass123'),
('Maaloul',   'Slim',   'Infirmier Triage',   NULL,                's.maaloul',   'pass123'),
('Ferchichi', 'Olfa',   'Infirmier Triage',   NULL,                'o.ferchichi', 'pass123');

-- ============================================================
-- 3. ADMISSIONS
--    Colonnes : id_patient, date_heure_arrivee, mode_arrivee,
--               motif_consultation, statut
--    statut ENUM : 'Active' | 'Cloturee'
-- ============================================================
INSERT INTO admissions (id_patient, date_heure_arrivee, mode_arrivee, motif_consultation, statut) VALUES
(1, NOW(),                              'Ambulance',      'Douleurs thoraciques intenses',         'Active'),
(2, NOW(),                              'SMUR',           'Traumatisme crânien suite à chute',     'Active'),
(3, NOW(),                              'Ambulance',      'Détresse respiratoire aiguë',           'Active'),
(4, NOW(),                              'Propres moyens', 'Fracture ouverte membre inférieur',     'Active'),
(5, NOW(),                              'Ambulance',      'Crise hypertensive sévère',             'Active'),
(6, NOW(),                              'Propres moyens', 'Intoxication médicamenteuse',           'Active'),
(7, NOW(),                              'Propres moyens', 'Douleurs abdominales aiguës',           'Active'),
(8, NOW(),                              'SMUR',           'Convulsions fébriles',                  'Active'),
(1, DATE_SUB(NOW(), INTERVAL 2 DAY),   'Ambulance',      'Contrôle post-opératoire',              'Cloturee'),
(3, DATE_SUB(NOW(), INTERVAL 5 DAY),   'Propres moyens', 'Suivi insuffisance cardiaque',          'Cloturee');

-- ============================================================
-- 4. CONSULTATIONS (exemples déjà enregistrés)
-- ============================================================
INSERT INTO consultations (id_admission, id_personnel_medecin, date_heure_debut, date_heure_fin, diagnostic, orientation) VALUES
(1, 1,
 DATE_SUB(NOW(), INTERVAL 3 HOUR), NULL,
 'Syndrome coronarien aigu suspecté. ECG en cours. Troponines élevées à 2.4 ng/mL.',
 'Hospitalisation'),

(2, 2,
 DATE_SUB(NOW(), INTERVAL 5 HOUR), DATE_SUB(NOW(), INTERVAL 4 HOUR),
 'Contusion cérébrale légère. Scanner cérébral normal. Pas de lésion hémorragique.',
 'Sortie'),

(3, 3,
 DATE_SUB(NOW(), INTERVAL 1 DAY),  DATE_SUB(NOW(), INTERVAL 23 HOUR),
 'Pneumonie bilatérale avec hypoxémie (SpO2 88%). Mise sous oxygénothérapie 6L/min.',
 'Hospitalisation'),

(4, 1,
 DATE_SUB(NOW(), INTERVAL 2 DAY),  DATE_SUB(NOW(), INTERVAL 47 HOUR),
 'Fracture ouverte tibia-péroné gauche. Nettoyage et immobilisation provisoire.',
 'Transfert'),

(5, 4,
 DATE_SUB(NOW(), INTERVAL 6 HOUR), NULL,
 'HTA maligne. PA 210/130 mmHg. Traitement antihypertenseur IV en cours.',
 'Hospitalisation');

-- ============================================================
-- 5. ORDONNANCES
-- ============================================================
INSERT INTO ordonnances (id_consultation, medicament, posologie, duree_jours) VALUES
-- Consultation 1 (coronarien)
(1, 'Aspirine 100mg',    '1 comprimé par jour le matin',                        30),
(1, 'Clopidogrel 75mg',  '1 comprimé par jour',                                 30),
(1, 'Héparine IV',       'Perfusion continue selon protocole cardiologie',        3),
-- Consultation 2 (traumatisme crânien)
(2, 'Paracétamol 1g',    '1 comprimé toutes les 6h si douleur (max 4/jour)',      5),
(2, 'Ibuprofène 400mg',  '1 comprimé 3 fois par jour pendant les repas',          3),
-- Consultation 3 (pneumonie)
(3, 'Amoxicilline 1g',   '1 comprimé 3 fois par jour',                           10),
(3, 'Prednisolone 40mg', '1 comprimé le matin à jeun',                            7),
(3, 'Salbutamol spray',  '2 bouffées toutes les 4h en cas de dyspnée',           14),
-- Consultation 4 (fracture)
(4, 'Amoxicilline-Acide clavulanique 1g', '1 comprimé 3 fois par jour pendant les repas', 7),
(4, 'Tramadol 50mg',     '1 gélule toutes les 6h si douleur intense',             5),
-- Consultation 5 (HTA)
(5, 'Nicardipine IV',    'Perfusion IV selon protocole urgences hypertensives',    2),
(5, 'Amlodipine 10mg',   '1 comprimé par jour le soir',                          30);

-- ============================================================
-- Vérification
-- ============================================================
SELECT 'Patients'       AS table_name, COUNT(*) AS nb FROM patients
UNION ALL SELECT 'Personnel',     COUNT(*) FROM personnel
UNION ALL SELECT 'Admissions',    COUNT(*) FROM admissions
UNION ALL SELECT 'Consultations', COUNT(*) FROM consultations
UNION ALL SELECT 'Ordonnances',   COUNT(*) FROM ordonnances;
