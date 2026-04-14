from fastapi import FastAPI
from pydantic import BaseModel
import joblib
import re

import os
from sklearn.base import BaseEstimator, TransformerMixin

# ─── Türkçe Metin Ön-İşleme (Model Unpickling İçin Gerekli) ───

TURKISH_SUFFIXES = [
    "dayız", "dayım", "deyiz", "deyim",
    "ıyor", "iyor", "uyor", "üyor",
    "arak", "erek", "ları", "leri", "lar", "ler",
    "dan", "den", "tan", "ten", "nın", "nin", "nun", "nün",
    "ını", "ini", "unu", "ünü", "ımız", "imiz", "umuz", "ümüz",
    "dır", "dir", "dur", "dür", "tır", "tir", "tur", "tür",
    "mış", "miş", "muş", "müş", "yız", "yiz", "yuz", "yüz",
    "ız", "iz", "uz", "üz", "ın", "in", "un", "ün",
    "da", "de", "ta", "te", "ım", "im", "um", "üm",
]

TR_CHAR_MAP = str.maketrans({
    'ı': 'i', 'İ': 'I', 'ş': 's', 'Ş': 'S', 'ç': 'c', 'Ç': 'C',
    'ğ': 'g', 'Ğ': 'G', 'ö': 'o', 'Ö': 'O', 'ü': 'u', 'Ü': 'U',
})

def normalize_turkish(text):
    if not isinstance(text, str): return ""
    text = text.lower().replace('İ', 'i').replace('I', 'ı').lower()
    text = re.sub(r'[^\w\s]', ' ', text)
    text = re.sub(r'\s+', ' ', text).strip()
    return text + " " + text.translate(TR_CHAR_MAP)

def simple_stem(text):
    words = text.split()
    stemmed = []
    for word in words:
        if len(word) > 4:
            for suffix in TURKISH_SUFFIXES:
                if word.endswith(suffix) and len(word) - len(suffix) >= 2:
                    word = word[:-len(suffix)]
                    break
        stemmed.append(word)
    return " ".join(stemmed)

class TurkishTextPreprocessor(BaseEstimator, TransformerMixin):
    def fit(self, X, y=None): return self
    def transform(self, X): return [self._preprocess(t) for t in X]
    def _preprocess(self, text): return simple_stem(normalize_turkish(text))

# ─── /Türkçe Metin Ön-İşleme ───

app = FastAPI(title="Emergency Mesh AI Triage Service")

# Load model (pipeline includes TurkishTextPreprocessor internally)
try:
    BASE_DIR = os.path.dirname(os.path.abspath(__file__))
    model_path = os.path.join(BASE_DIR, "emergency_model.pkl")
    model = joblib.load(model_path)
    print(f"[AI-SERVICE] Model loaded successfully from {model_path}.")
except Exception as e:
    model = None
    print(f"[AI-SERVICE] Model load failed: {e}")

class Message(BaseModel):
    text: str

# ─── Heuristic Keyword Engine ───
# Anahtar kelime tabanlı acil durum tespiti (ML'den önce çalışır)

EMERGENCY_KEYWORDS = {
    # EMERGENCY (3) - Hayati tehlike, enkaz, göçük
    3: [
        "enkaz", "göçük", "gocuk", "gömülü", "gomulu",
        "nefes alamıyorum", "nefes alamiyorum", "nefes yok",
        "altındayız", "altindayiz", "altındayım", "altindayim",
        "bina çöktü", "bina coktu", "bina yıkıldı", "bina yikildi",
        "çöktü", "coktu", "sıkıştı", "sikisti",
        "hava bitiyor", "hava yok",
        "gömüldük", "gomulduk",
    ],
    # HIGH (2) - Ciddi yaralanma, yangın
    2: [
        "yangın", "yangin", "yanıyor", "yaniyor",
        "kanama", "kan akıyor", "kan akiyor",
        "bayıldı", "bayildi", "bilinci kapandı",
        "kalp krizi", "kalp durması",
        "bıçaklandı", "bicaklandi",
        "patlama", "patladi",
        "zehirlenme",
    ],
}

def check_heuristic(text_lower):
    """Anahtar kelime tabanlı kontrol - kesin eşleşmelerde ML'yi bypass eder"""
    for priority_label, keywords in sorted(EMERGENCY_KEYWORDS.items(), reverse=True):
        for kw in keywords:
            if kw in text_lower:
                return priority_label, kw
    return None, None


@app.post("/predict")
async def predict(message: Message):
    text_lower = message.text.lower().strip()
    # Türkçe karakter normalizasyonu (yazım hatasız arama için)
    text_normalized = text_lower
    for tr_char, ascii_char in [('ı','i'), ('ş','s'), ('ç','c'), ('ğ','g'), ('ö','o'), ('ü','u')]:
        text_normalized = text_normalized.replace(tr_char, ascii_char)
    
    combined_text = text_lower + " " + text_normalized
    
    print(f"[AI-LOG] Received: {text_lower}")
    
    # 1. Heuristic Override (Kesin eşleşmeler için)
    heuristic_label, matched_keyword = check_heuristic(combined_text)
    if heuristic_label is not None:
        priority_map = {0: "LOW", 1: "MEDIUM", 2: "HIGH", 3: "EMERGENCY"}
        print(f"[AI-LOG] HEURISTIC MATCH: '{matched_keyword}' → {priority_map[heuristic_label]}")
        return {
            "text": message.text,
            "priority": priority_map[heuristic_label],
            "label": heuristic_label,
            "method": "heuristic"
        }

    if not model:
        return {"priority": "UNKNOWN", "score": 0, "error": "Model not loaded"}
    
    # 2. ML Prediction
    # Not: model pipeline kendi içinde TurkishTextPreprocessor uygular
    # Bu yüzden ham metni doğrudan model'e verebiliriz
    prediction = int(model.predict([message.text])[0])
    
    priority_map = {
        0: "LOW",
        1: "MEDIUM",
        2: "HIGH",
        3: "EMERGENCY"
    }
    
    res = {
        "text": message.text,
        "priority": priority_map.get(prediction, "LOW"),
        "label": prediction,
        "method": "ml"
    }
    print(f"[AI-LOG] ML RESULT: {res['priority']}")
    return res

@app.get("/health")
def health():
    return {"status": "ok", "model_loaded": model is not None}

if __name__ == "__main__":
    import uvicorn
    import argparse
    
    parser = argparse.ArgumentParser(description="Emergency Mesh AI Service")
    parser.add_argument("--host", type=str, default="0.0.0.0", help="Host to run the service on")
    parser.add_argument("--port", type=int, default=8030, help="Port to run the service on")
    args = parser.parse_args()
    
    print(f"[AI-SERVICE] Starting on {args.host}:{args.port}")
    uvicorn.run(app, host=args.host, port=args.port)
