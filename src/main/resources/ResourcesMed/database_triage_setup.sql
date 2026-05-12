-- ═══════════════════════════════════════════════════════════════════════════
-- Script de création de la table TRIAGE pour le système REVIVE
-- ═══════════════════════════════════════════════════════════════════════════

USE revive;

-- Création de la table triage si elle n'existe pas
CREATE TABLE IF NOT EXISTS triage (
    id_triage INT AUTO_INCREMENT PRIMARY KEY,
    id_admission INT NOT NULL,
    
    -- Constantes vitales
    constantes_ta_sys DECIMAL(5,2) DEFAULT 0,      -- Tension artérielle systolique
    constantes_ta_dia DECIMAL(5,2) DEFAULT 0,      -- Tension artérielle diastolique
    constantes_pouls DECIMAL(5,2) DEFAULT 0,       -- Pouls (bpm)
    constantes_temperature DECIMAL(4,2) DEFAULT 0, -- Température (°C)
    spo2 DECIMAL(5,2) DEFAULT 0,                   -- Saturation en oxygène (%)
    glycemie DECIMAL(5,2) DEFAULT 0,               -- Glycémie
    score_douleur DECIMAL(3,1) DEFAULT 0,          -- Score de douleur (0-10)
    gcs_score INT DEFAULT 15,                      -- Glasgow Coma Scale (3-15)
    frequence_respiratoire INT DEFAULT 0,          -- Fréquence respiratoire (rpm)
    
    -- Évaluation clinique
    symptomes TEXT,                                -- Description des symptômes
    score_calcule INT DEFAULT 0,                   -- Score de triage calculé
    niveau_auto VARCHAR(50),                       -- Niveau automatique (CRITIQUE, URGENT, MODERE, STABLE)
    niveau_final VARCHAR(50),                      -- Niveau final validé par le médecin
    analyse_auto TEXT,                             -- Analyse automatique
    patient_state VARCHAR(50),                     -- État du patient (CRITIQUE, URGENT, MODERE, STABLE)
    
    -- Métadonnées
    date_heure_triage TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Clé étrangère vers admissions
    FOREIGN KEY (id_admission) REFERENCES admissions(id_admission) ON DELETE CASCADE,
    
    -- Index pour améliorer les performances
    INDEX idx_admission (id_admission),
    INDEX idx_niveau (niveau_final),
    INDEX idx_state (patient_state),
    INDEX idx_date (date_heure_triage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ═══════════════════════════════════════════════════════════════════════════
-- Données de test (optionnel - à adapter selon vos besoins)
-- ═══════════════════════════════════════════════════════════════════════════

-- Exemple d'insertion de données de test
-- Assurez-vous que les id_admission existent dans la table admissions

-- Patient CRITIQUE
INSERT INTO triage (
    id_admission, constantes_ta_sys, constantes_ta_dia, constantes_pouls, 
    constantes_temperature, spo2, glycemie, score_douleur, gcs_score, 
    frequence_respiratoire, symptomes, score_calcule, niveau_auto, 
    niveau_final, patient_state, date_heure_triage
) VALUES (
    1, 180, 110, 120, 39.5, 85, 2.5, 9, 10, 28,
    'Douleur thoracique intense, dyspnée sévère, confusion',
    95, 'CRITIQUE', 'CRITIQUE', 'CRITIQUE', NOW()
);

-- Patient URGENT
INSERT INTO triage (
    id_admission, constantes_ta_sys, constantes_ta_dia, constantes_pouls, 
    constantes_temperature, spo2, glycemie, score_douleur, gcs_score, 
    frequence_respiratoire, symptomes, score_calcule, niveau_auto, 
    niveau_final, patient_state, date_heure_triage
) VALUES (
    2, 160, 95, 105, 38.8, 90, 3.2, 7, 13, 24,
    'Fièvre élevée, tachycardie, douleur abdominale aiguë',
    75, 'URGENT', 'URGENT', 'URGENT', NOW()
);

-- Patient MODERE
INSERT INTO triage (
    id_admission, constantes_ta_sys, constantes_ta_dia, constantes_pouls, 
    constantes_temperature, spo2, glycemie, score_douleur, gcs_score, 
    frequence_respiratoire, symptomes, score_calcule, niveau_auto, 
    niveau_final, patient_state, date_heure_triage
) VALUES (
    3, 135, 85, 88, 37.8, 95, 5.5, 5, 15, 18,
    'Céphalées modérées, nausées, vertiges',
    45, 'MODERE', 'MODERE', 'MODERE', NOW()
);

-- Patient STABLE
INSERT INTO triage (
    id_admission, constantes_ta_sys, constantes_ta_dia, constantes_pouls, 
    constantes_temperature, spo2, glycemie, score_douleur, gcs_score, 
    frequence_respiratoire, symptomes, score_calcule, niveau_auto, 
    niveau_final, patient_state, date_heure_triage
) VALUES (
    4, 120, 80, 72, 36.8, 98, 5.0, 2, 15, 16,
    'Consultation de suivi, état général bon',
    15, 'STABLE', 'STABLE', 'STABLE', NOW()
);

-- ═══════════════════════════════════════════════════════════════════════════
-- Vérification
-- ═══════════════════════════════════════════════════════════════════════════

-- Afficher les données de triage avec les informations patient
SELECT 
    t.id_triage,
    t.id_admission,
    p.nom AS nom_patient,
    p.prenom AS prenom_patient,
    t.constantes_ta_sys,
    t.constantes_ta_dia,
    t.constantes_pouls,
    t.constantes_temperature,
    t.spo2,
    t.gcs_score,
    t.symptomes,
    t.score_calcule,
    t.niveau_final,
    t.patient_state,
    t.date_heure_triage
FROM triage t
LEFT JOIN admissions a ON a.id_admission = t.id_admission
LEFT JOIN patients p ON p.id_patient = a.id_patient
ORDER BY t.score_calcule DESC, t.date_heure_triage DESC;
