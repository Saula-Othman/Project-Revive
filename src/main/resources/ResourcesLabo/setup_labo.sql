-- ═══════════════════════════════════════════════════════════════════════════
-- REVIVE — Setup Laboratoire (Module 4)
-- Run this in MySQL Workbench / phpMyAdmin on the 'revive' database
-- ═══════════════════════════════════════════════════════════════════════════

USE revive;

-- ── STEP 1: Create examens_demandes table if it doesn't exist ─────────────
CREATE TABLE IF NOT EXISTS examens_demandes (
    id_demande      INT AUTO_INCREMENT PRIMARY KEY,
    id_consultation INT NOT NULL,
    type_examen     VARCHAR(200) NOT NULL,
    date_demande    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    statut          VARCHAR(50)  DEFAULT 'En attente',
    urgent          TINYINT(1)   DEFAULT 0,
    FOREIGN KEY (id_consultation) REFERENCES consultations(id_consultation) ON DELETE CASCADE,
    INDEX idx_consultation (id_consultation),
    INDEX idx_statut       (statut),
    INDEX idx_urgent       (urgent)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── STEP 2: Create resultats table if it doesn't exist ───────────────────
CREATE TABLE IF NOT EXISTS resultats (
    id_resultat        INT AUTO_INCREMENT PRIMARY KEY,
    id_demande         INT NOT NULL,
    compte_rendu_texte TEXT,
    fichier_joint      VARCHAR(500) DEFAULT '',
    etat               VARCHAR(50)  DEFAULT 'Propre',
    score_gravite      INT          DEFAULT 0,
    niveau_gravite     VARCHAR(50)  DEFAULT 'Faible',
    recommandation     TEXT,
    date_resultat      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_demande) REFERENCES examens_demandes(id_demande) ON DELETE CASCADE,
    INDEX idx_demande (id_demande),
    INDEX idx_etat    (etat)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── STEP 3: Add statut_demande, analyses, imageries to consultations ──────
ALTER TABLE consultations
    ADD COLUMN IF NOT EXISTS analyses       TEXT        DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS imageries      TEXT        DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS statut_demande VARCHAR(30) DEFAULT 'Non envoyee';

-- ── STEP 4: Mark some consultations as 'Envoyee' so the dashboard shows ──
-- notifications (picks the first 3 consultations that exist)
UPDATE consultations
SET statut_demande = 'Envoyee',
    analyses  = COALESCE(NULLIF(analyses, ''),  'NFS, CRP, Ionogramme'),
    imageries = COALESCE(NULLIF(imageries, ''), 'Radiographie thoracique')
WHERE id_consultation IN (
    SELECT id_consultation FROM (
        SELECT id_consultation FROM consultations
        ORDER BY id_consultation LIMIT 3
    ) tmp
)
AND (statut_demande IS NULL OR statut_demande IN ('Non envoyee', ''));

-- ── STEP 5: Insert examens_demandes linked to real consultations ──────────
-- Only inserts if examens_demandes is empty
INSERT INTO examens_demandes (id_consultation, type_examen, date_demande, statut, urgent)
SELECT sub.id_consultation, sub.type_examen, sub.date_demande, sub.statut, sub.urgent
FROM (
    SELECT
        (SELECT id_consultation FROM consultations ORDER BY id_consultation LIMIT 1 OFFSET 0) AS id_consultation,
        '[ANALYSE] Numération Formule Sanguine (NFS)'   AS type_examen,
        NOW()                                            AS date_demande,
        'Realise'                                        AS statut,
        0                                                AS urgent
    UNION ALL SELECT
        (SELECT id_consultation FROM consultations ORDER BY id_consultation LIMIT 1 OFFSET 0),
        '[ANALYSE] CRP + Procalcitonine',
        DATE_SUB(NOW(), INTERVAL 2 HOUR),
        'En attente',
        1
    UNION ALL SELECT
        (SELECT id_consultation FROM consultations ORDER BY id_consultation LIMIT 1 OFFSET 1),
        '[IMAGERIE] Radiographie thoracique face',
        DATE_SUB(NOW(), INTERVAL 1 HOUR),
        'Realise',
        1
    UNION ALL SELECT
        (SELECT id_consultation FROM consultations ORDER BY id_consultation LIMIT 1 OFFSET 1),
        '[ANALYSE] Bilan hépatique complet',
        DATE_SUB(NOW(), INTERVAL 3 HOUR),
        'En attente',
        0
    UNION ALL SELECT
        (SELECT id_consultation FROM consultations ORDER BY id_consultation LIMIT 1 OFFSET 2),
        '[ANALYSE] Troponine + D-Dimères',
        NOW(),
        'En attente',
        1
    UNION ALL SELECT
        (SELECT id_consultation FROM consultations ORDER BY id_consultation LIMIT 1 OFFSET 2),
        '[IMAGERIE] Scanner thoracique',
        DATE_SUB(NOW(), INTERVAL 30 MINUTE),
        'Realise',
        0
) sub
WHERE sub.id_consultation IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM examens_demandes);

-- ── STEP 6: Insert resultats for 'Realise' examens that have none ─────────
INSERT INTO resultats (id_demande, compte_rendu_texte, fichier_joint, etat, score_gravite, niveau_gravite, recommandation, date_resultat)
SELECT
    ed.id_demande,
    CASE
        WHEN ed.type_examen LIKE '%NFS%' THEN
            'Hémoglobine 13.2 g/dL (normale). Leucocytes 8200/mm3. Plaquettes 245000/mm3. Aucune anomalie détectée.'
        WHEN ed.type_examen LIKE '%Radiographie%' OR ed.type_examen LIKE '%Radio%' THEN
            'Opacités alvéolaires diffuses au niveau du lobe inférieur droit, compatibles avec un foyer de condensation pulmonaire. Pas d''épanchement pleural.'
        WHEN ed.type_examen LIKE '%Scanner%' THEN
            'Pas de lésion parenchymateuse significative. Silhouette cardiaque normale. Pas d''épanchement.'
        ELSE
            'Résultats dans les limites normales pour l''âge du patient. Aucune anomalie significative détectée.'
    END AS compte_rendu_texte,
    '' AS fichier_joint,
    CASE WHEN ed.type_examen LIKE '%Radiographie%' THEN 'Grave' ELSE 'Propre' END AS etat,
    CASE WHEN ed.type_examen LIKE '%Radiographie%' THEN 65 ELSE 20 END AS score_gravite,
    CASE WHEN ed.type_examen LIKE '%Radiographie%' THEN 'Élevé' ELSE 'Faible' END AS niveau_gravite,
    CASE WHEN ed.type_examen LIKE '%Radiographie%'
         THEN 'Antibiothérapie IV recommandée. Contrôle radiologique à 48h.'
         ELSE 'Suivi de routine. Réévaluation si symptômes persistent.'
    END AS recommandation,
    NOW() AS date_resultat
FROM examens_demandes ed
WHERE ed.statut = 'Realise'
  AND NOT EXISTS (SELECT 1 FROM resultats r WHERE r.id_demande = ed.id_demande);

-- ── VERIFICATION ──────────────────────────────────────────────────────────
SELECT 'examens_demandes' AS table_name, COUNT(*) AS total FROM examens_demandes
UNION ALL SELECT 'resultats',   COUNT(*) FROM resultats
UNION ALL SELECT 'consultations Envoyee', COUNT(*) FROM consultations WHERE statut_demande = 'Envoyee';

-- Show what the dashboard will display
SELECT '=== EXAMENS DU JOUR ===' AS info;
SELECT ed.id_demande, ed.type_examen, ed.statut, ed.urgent,
       DATE(ed.date_demande) AS date_demande
FROM examens_demandes ed
WHERE DATE(ed.date_demande) = CURDATE();

SELECT '=== DEMANDES MEDECINS (Envoyee) ===' AS info;
SELECT c.id_consultation, c.analyses, c.imageries, c.statut_demande
FROM consultations c
WHERE c.statut_demande = 'Envoyee';
