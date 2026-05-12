-- ============================================================
-- MODULE 5 — MATÉRIEL & LOGISTIQUE
-- Créer les tables manquantes dans la base "revive"
-- Exécuter dans MySQL Workbench
-- ============================================================

USE revive;

-- Ajouter la colonne localisation à salles si elle n'existe pas
ALTER TABLE salles ADD COLUMN IF NOT EXISTS localisation VARCHAR(255) DEFAULT NULL;

-- ── Table matériel d'urgence ──────────────────────────────────
CREATE TABLE IF NOT EXISTS materiel_urgence (
    id_materiel              INT AUTO_INCREMENT PRIMARY KEY,
    id_salle                 INT DEFAULT NULL,
    nom                      VARCHAR(150) NOT NULL,
    date_derniere_maintenance DATE DEFAULT NULL,
    etat                     VARCHAR(50)  NOT NULL DEFAULT 'Fonctionnel',
    quantite                 INT          NOT NULL DEFAULT 1,
    CONSTRAINT fk_mat_salle FOREIGN KEY (id_salle)
        REFERENCES salles(id_salle) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Table ambulances ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ambulances (
    id_ambulance             INT AUTO_INCREMENT PRIMARY KEY,
    numero_serie             VARCHAR(100) NOT NULL UNIQUE,
    marque                   VARCHAR(100) DEFAULT NULL,
    modele                   VARCHAR(100) DEFAULT NULL,
    annee_fabrication        INT          DEFAULT NULL,
    etat                     VARCHAR(50)  NOT NULL DEFAULT 'Disponible',
    km_total                 DOUBLE       NOT NULL DEFAULT 0,
    date_derniere_vidange    DATE         DEFAULT NULL,
    km_derniere_vidange      DOUBLE       DEFAULT 0,
    date_derniers_pneus      DATE         DEFAULT NULL,
    km_derniers_pneus        DOUBLE       DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Table trajets ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS trajets (
    id_trajet                INT AUTO_INCREMENT PRIMARY KEY,
    id_ambulance             INT          NOT NULL,
    localisation_depart      VARCHAR(255) DEFAULT NULL,
    localisation_urgence     VARCHAR(255) DEFAULT NULL,
    distance_km              DOUBLE       NOT NULL DEFAULT 0,
    duree_minutes            INT          NOT NULL DEFAULT 0,
    date_trajet              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statut                   VARCHAR(50)  NOT NULL DEFAULT 'Terminé',
    CONSTRAINT fk_trajet_amb FOREIGN KEY (id_ambulance)
        REFERENCES ambulances(id_ambulance) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Table alertes maintenance ─────────────────────────────────
CREATE TABLE IF NOT EXISTS alertes_maintenance (
    id_alerte                INT AUTO_INCREMENT PRIMARY KEY,
    id_ambulance             INT          NOT NULL,
    type_maintenance         VARCHAR(100) NOT NULL,
    priorite                 VARCHAR(50)  NOT NULL DEFAULT 'Moyenne',
    description              TEXT         DEFAULT NULL,
    km_actuel                DOUBLE       DEFAULT 0,
    km_recommande            DOUBLE       DEFAULT 0,
    date_generation          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statut                   VARCHAR(50)  NOT NULL DEFAULT 'En attente',
    date_resolution          DATETIME     DEFAULT NULL,
    CONSTRAINT fk_alerte_amb FOREIGN KEY (id_ambulance)
        REFERENCES ambulances(id_ambulance) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SELECT 'Tables Module 5 créées avec succès.' AS statut;
