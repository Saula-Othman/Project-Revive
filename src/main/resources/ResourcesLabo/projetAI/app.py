# -*- coding: utf-8 -*-
from flask import Flask, request, jsonify
import tensorflow as tf
import numpy as np
from PIL import Image
import io

app = Flask(__name__)

model = tf.keras.models.load_model("modele_pneumonia_epoch6.keras")
print("Modele charge avec succes !")

@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"}), 200

@app.route("/predict", methods=["POST"])
def predict():
    try:
        if "file" in request.files:
            file = request.files["file"]
            image_bytes = file.read()
        elif request.data:
            image_bytes = request.data
        else:
            return jsonify({"error": "Aucun fichier recu"}), 400

        image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
        image = image.resize((224, 224))

        img_array = np.array(image, dtype=np.float32) / 255.0
        img_array = np.expand_dims(img_array, axis=0)

        prediction = model.predict(img_array)
        score = float(prediction[0][0])

        result = "PNEUMONIA" if score > 0.5 else "NORMAL"

        return jsonify({
            "prediction": result,
            "score": round(score, 4)
        })

    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == "__main__":
    print("API Flask demarree sur http://localhost:5000")
    app.run(host="0.0.0.0", port=5000, debug=False)