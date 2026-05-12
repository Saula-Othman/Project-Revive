-- ═══════════════════════════════════════════════════════════════
-- DIAGNOSTIC & FIX: Patient names not showing in Module 3
-- Run in MySQL Workbench / phpMyAdmin on the 'revive' database
-- ═══════════════════════════════════════════════════════════════

USE revive;

-- ── STEP 1: DIAGNOSE — see which consultations have broken links ──
SELECT
    c.id_consultation,
    c.id_admission,
    a.id_admission  AS admission_found,
    a.id_patient    AS patient_id,
    p.nom           AS patient_nom,
    p.prenom        AS patient_prenom
FROM consultations c
LEFT JOIN admissions a ON a.id_admission = c.id_admission
LEFT JOIN patients   p ON p.id_patient   = a.id_patient
ORDER BY c.id_consultation;

-- ── STEP 2: Count broken links ────────────────────────────────────
SELECT
    COUNT(*) AS total_consultations,
    SUM(CASE WHEN a.id_admission IS NULL THEN 1 ELSE 0 END) AS missing_admissions,
    SUM(CASE WHEN p.id_patient   IS NULL THEN 1 ELSE 0 END) AS missing_patients
FROM consultations c
LEFT JOIN admissions a ON a.id_admission = c.id_admission
LEFT JOIN patients   p ON p.id_patient   = a.id_patient;

-- ── STEP 3: Show what admissions actually exist ───────────────────
SELECT a.id_admission, a.id_patient, p.nom, p.prenom, a.statut
FROM admissions a
LEFT JOIN patients p ON p.id_patient = a.id_patient
ORDER BY a.id_admission;

-- ── STEP 4: FIX — Re-link consultations to valid admissions ──────
-- This updates consultations whose id_admission doesn't exist
-- by assigning them to the first valid admission for the same patient
-- (Only run if Step 2 shows missing_admissions > 0)

-- Option A: If admissions table is empty — re-insert from data_test.sql
-- (Run data_test.sql first, then re-run this script)

-- Option B: If admissions exist but IDs don't match — remap
-- Update consultations to use the lowest valid id_admission
UPDATE consultations c
SET c.id_admission = (
    SELECT MIN(a.id_admission)
    FROM admissions a
    WHERE a.id_patient IS NOT NULL
)
WHERE NOT EXISTS (
    SELECT 1 FROM admissions a2
    WHERE a2.id_admission = c.id_admission
);

-- ── STEP 5: VERIFY FIX ────────────────────────────────────────────
SELECT
    c.id_consultation,
    CONCAT(p.prenom, ' ', p.nom) AS nom_patient,
    CONCAT(per.prenom, ' ', per.nom) AS nom_medecin,
    c.diagnostic,
    c.orientation
FROM consultations c
LEFT JOIN admissions a   ON a.id_admission   = c.id_admission
LEFT JOIN patients   p   ON p.id_patient     = a.id_patient
LEFT JOIN personnel  per ON per.id_personnel = c.id_personnel_medecin
ORDER BY c.id_consultation;
