-- Vérifier les admissions existantes
SELECT 
    a.id_admission,
    a.id_patient,
    a.statut,
    a.motif_consultation,
    p.nom,
    p.prenom,
    p.date_naissance
FROM admissions a
LEFT JOIN patients p ON p.id_patient = a.id_patient
ORDER BY a.id_admission;

-- Vérifier les patients existants
SELECT * FROM patients ORDER BY id_patient;