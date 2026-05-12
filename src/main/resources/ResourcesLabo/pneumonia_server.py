"""
=============================================================
  REVIVE — Serveur IA Détection Pneumonie (DenseNet121)
  API Flask locale — localhost:5000
  Pas de ngrok — fonctionne entièrement en local
=============================================================

INSTRUCTIONS :
1. Placez ce fichier dans le même dossier que votre modèle .keras
2. Installez les dépendances :
      pip install flask tensorflow pillow numpy
3. Lancez le serveur :
      python pneumonia_server.py
4. Le serveur démarre sur http://localhost:5000
5. Lancez ensuite l'application JavaFX REVIVE

MODÈLE ATTENDU :
  - Fichier : pneumonia_model.keras (ou .h5)
  - Architecture : DenseNet121
  - Input : (224, 224, 3) — images RGB
  - Output : probabilité de pneumonie (sigmoid)
  - Dataset : Chest X-Ray Pneumonia (Kaggle)
=============================================================
"""

import os
import io
import numpy as np
from flask import Flask, request, jsonify
from PIL import Image
import tensorflow as tf

# ── Configuration
MODEL_PATH  = "pneumonia_model.keras"   # Chemin vers votre modèle
IMG_SIZE    = (224, 224)                # Taille d'entrée DenseNet121
THRESHOLD   = 0.5                       # Seuil de classification
PORT        = 5000
HOST        = "127.0.0.1"              # Localhost uniquement (sécurité)

# ── Chargement du modèle au démarrage
print("⏳ Chargement du modèle DenseNet121...")
try:
    model = tf.keras.models.load_model(MODEL_PATH)
    print(f"✅ Modèle chargé : {MODEL_PATH}")
    print(f"   Input shape  : {model.input_shape}")
    print(f"   Output shape : {model.output_shape}")
except Exception as e:
    print(f"❌ Erreur chargement modèle : {e}")
    print(f"   Vérifiez que '{MODEL_PATH}' existe dans le dossier courant.")
    model = None

app = Flask(__name__)


def preprocess_image(image_bytes):
    """
    Prétraitement de l'image pour DenseNet121.
    - Conversion en RGB
    - Redimensionnement à 224x224
    - Normalisation [0, 1]
    - Ajout de la dimension batch
    """
    img = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    img = img.resize(IMG_SIZE, Image.LANCZOS)
    arr = np.array(img, dtype=np.float32) / 255.0
    arr = np.expand_dims(arr, axis=0)  # (1, 224, 224, 3)
    return arr


@app.route("/predict", methods=["POST"])
def predict():
    """
    Endpoint principal de prédiction.

    Entrée  : image (multipart/form-data, champ 'file')
    Sortie  : JSON { "prediction": "NORMAL"|"PNEUMONIA", "score": float }
    """
    if model is None:
        return jsonify({"error": "Modèle non chargé. Vérifiez le fichier .keras"}), 500

    if "file" not in request.files:
        return jsonify({"error": "Aucun fichier fourni. Utilisez le champ 'file'."}), 400

    file = request.files["file"]
    if file.filename == "":
        return jsonify({"error": "Fichier vide."}), 400

    # Vérifier l'extension
    allowed = {".jpg", ".jpeg", ".png"}
    ext = os.path.splitext(file.filename.lower())[1]
    if ext not in allowed:
        return jsonify({"error": f"Format non supporté : {ext}. Utilisez JPG ou PNG."}), 400

    try:
        image_bytes = file.read()
        arr = preprocess_image(image_bytes)

        # ── Prédiction
        prediction_raw = model.predict(arr, verbose=0)
        score = float(prediction_raw[0][0])  # Probabilité de pneumonie

        # ── Classification
        if score >= THRESHOLD:
            prediction = "PNEUMONIA"
        else:
            prediction = "NORMAL"

        print(f"📊 Prédiction : {prediction} (score={score:.4f})")

        return jsonify({
            "prediction": prediction,   # "NORMAL" ou "PNEUMONIA"
            "score":      round(score, 4)
        })

    except Exception as e:
        print(f"❌ Erreur prédiction : {e}")
        return jsonify({"error": f"Erreur lors de l'analyse : {str(e)}"}), 500


@app.route("/health", methods=["GET"])
def health():
    """Vérification que le serveur est actif."""
    return jsonify({
        "status":  "ok",
        "modele":  MODEL_PATH,
        "charge":  model is not None,
        "version": "REVIVE Pneumonia AI v2.0"
    })


if __name__ == "__main__":
    print("\n" + "=" * 55)
    print("  🏥 REVIVE — Serveur IA Détection Pneumonie")
    print("  🧠 Modèle : DenseNet121")
    print(f"  🌐 URL    : http://{HOST}:{PORT}")
    print(f"  📋 Endpoint : http://{HOST}:{PORT}/predict")
    print(f"  ❤  Health  : http://{HOST}:{PORT}/health")
    print("=" * 55)
    print("\n  ✅ Serveur prêt — Lancez l'application JavaFX\n")

    app.run(host=HOST, port=PORT, debug=False)
