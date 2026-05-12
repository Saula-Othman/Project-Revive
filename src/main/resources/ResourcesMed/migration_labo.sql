-- Migration Module 4 — Laboratoire & Imagerie
-- Executer dans phpMyAdmin sur la base revive

USE revive;

ALTER TABLE consultations
    ADD COLUMN IF NOT EXISTS analyses       TEXT          DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS imageries      TEXT          DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS statut_demande VARCHAR(30)   DEFAULT 'Non envoyee';

-- Verification
DESCRIBE consultations;
