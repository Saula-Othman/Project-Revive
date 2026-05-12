-- ═══════════════════════════════════════════════════════════════════════════
-- Migration SQL pour la fonctionnalité Email Post-Consultation
-- REVIVE Module 3 — Urgences
-- ═══════════════════════════════════════════════════════════════════════════

-- ── 1. Ajout colonne email dans patients ──────────────────────────────────
-- Permet de stocker l'adresse email du patient pour l'envoi automatique

ALTER TABLE patients 
ADD COLUMN IF NOT EXISTS email VARCHAR(255) DEFAULT NULL
COMMENT 'Adresse email du patient pour envoi recap consultation';

-- ── 2. Ajout colonne icd_code dans consultations ──────────────────────────
-- Stocke le code ICD-10 du diagnostic pour récupérer infos MedlinePlus

ALTER TABLE consultations 
ADD COLUMN IF NOT EXISTS icd_code VARCHAR(20) DEFAULT NULL
COMMENT 'Code ICD-10 du diagnostic (ex: J18.9 pour pneumonie)';

-- ── 3. Vérification colonnes constantes vitales dans admissions ───────────
-- Ces colonnes doivent déjà exister, mais on les crée si absentes

ALTER TABLE admissions 
ADD COLUMN IF NOT EXISTS constances_pouls INT DEFAULT NULL
COMMENT 'Pouls en battements par minute';

ALTER TABLE admissions 
ADD COLUMN IF NOT EXISTS constances_temperature DECIMAL(4,1) DEFAULT NULL
COMMENT 'Temperature corporelle en degres Celsius';

ALTER TABLE admissions 
ADD COLUMN IF NOT EXISTS spo2 INT DEFAULT NULL
COMMENT 'Saturation en oxygene en pourcentage';

ALTER TABLE admissions 
ADD COLUMN IF NOT EXISTS glycemie DECIMAL(4,2) DEFAULT NULL
COMMENT 'Glycemie en g/L';

ALTER TABLE admissions 
ADD COLUMN IF NOT EXISTS score_douleur INT DEFAULT NULL
COMMENT 'Score de douleur sur 10';

ALTER TABLE admissions 
ADD COLUMN IF NOT EXISTS frequence_respiratoire INT DEFAULT NULL
COMMENT 'Frequence respiratoire par minute';

ALTER TABLE admissions 
ADD COLUMN IF NOT EXISTS gcs_score INT DEFAULT NULL
COMMENT 'Score de Glasgow (conscience)';

ALTER TABLE admissions 
ADD COLUMN IF NOT EXISTS motif_consultation TEXT DEFAULT NULL
COMMENT 'Motif initial de consultation aux urgences';

-- ── 4. Données de test (optionnel) ────────────────────────────────────────
-- Ajout d'emails de test pour quelques patients existants

-- UPDATE patients SET email = 'patient1@test.com' WHERE id_patient = 1;
-- UPDATE patients SET email = 'patient2@test.com' WHERE id_patient = 2;
-- UPDATE patients SET email = 'patient3@test.com' WHERE id_patient = 3;

-- ── 5. Index pour performance ──────────────────────────────────────────────

CREATE INDEX IF NOT EXISTS idx_consultations_icd 
ON consultations(icd_code);

CREATE INDEX IF NOT EXISTS idx_patients_email 
ON patients(email);

-- ═══════════════════════════════════════════════════════════════════════════
-- Migration terminée
-- ═══════════════════════════════════════════════════════════════════════════

-- Vérification :
-- SELECT COUNT(*) FROM patients WHERE email IS NOT NULL;
-- SELECT COUNT(*) FROM consultations WHERE icd_code IS NOT NULL;
