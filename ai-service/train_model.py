import pandas as pd
import re
import string
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.svm import LinearSVC
from sklearn.pipeline import Pipeline
from sklearn.model_selection import train_test_split, StratifiedKFold, cross_val_score
from sklearn.metrics import classification_report, confusion_matrix
from sklearn.base import BaseEstimator, TransformerMixin
import joblib
import numpy as np

# ─── Türkçe Metin Ön-İşleme ───

# Basit Türkçe stemming / normalizasyon kuralları
TURKISH_SUFFIXES = [
    "dayız", "dayım", "deyiz", "deyim",
    "ıyor", "iyor", "uyor", "üyor",
    "arak", "erek",
    "ları", "leri", "lar", "ler",
    "dan", "den", "tan", "ten",
    "nın", "nin", "nun", "nün",
    "ını", "ini", "unu", "ünü",
    "ımız", "imiz", "umuz", "ümüz",
    "dır", "dir", "dur", "dür",
    "tır", "tir", "tur", "tür",
    "mış", "miş", "muş", "müş",
    "yız", "yiz", "yuz", "yüz",
    "ız", "iz", "uz", "üz",
    "ın", "in", "un", "ün",
    "da", "de", "ta", "te",
    "ım", "im", "um", "üm",
]

# Türkçe karakter → ASCII eşlemeleri (yaygın yazım hataları)
TR_CHAR_MAP = str.maketrans({
    'ı': 'i', 'İ': 'I',
    'ş': 's', 'Ş': 'S',
    'ç': 'c', 'Ç': 'C',
    'ğ': 'g', 'Ğ': 'G',
    'ö': 'o', 'Ö': 'O',
    'ü': 'u', 'Ü': 'U',
})

def normalize_turkish(text):
    """Türkçe metin normalizasyonu"""
    if not isinstance(text, str):
        return ""
    
    # Küçük harfe çevir (Türkçe locale ile)
    text = text.lower()
    # Türkçe İ → i fix
    text = text.replace('İ', 'i').replace('I', 'ı')
    text = text.lower()
    
    # Noktalama ve fazla boşlukları temizle
    text = re.sub(r'[^\w\s]', ' ', text)
    text = re.sub(r'\s+', ' ', text).strip()
    
    # Türkçe karakterleri ASCII'ye normalize et (hem normal hem hatalı yazımları eşle)
    text_ascii = text.translate(TR_CHAR_MAP)
    
    # Her iki versiyonu birleştir: orijinal Türkçe + ASCII normalizeli
    # Bu sayede "enkaz" ve "enkaz" eşleşir, "enkazdayız" ve "enkazdayiz" da eşleşir
    combined = text + " " + text_ascii
    
    return combined

def simple_stem(text):
    """Basit Türkçe suffix stripping"""
    words = text.split()
    stemmed = []
    for word in words:
        if len(word) > 4:  # çok kısa kelimeleri stem'leme
            for suffix in TURKISH_SUFFIXES:
                if word.endswith(suffix) and len(word) - len(suffix) >= 2:
                    word = word[:-len(suffix)]
                    break
        stemmed.append(word)
    return " ".join(stemmed)

class TurkishTextPreprocessor(BaseEstimator, TransformerMixin):
    """Sklearn uyumlu Türkçe metin ön-işleme transformer'ı"""
    
    def fit(self, X, y=None):
        return self
    
    def transform(self, X):
        return [self._preprocess(text) for text in X]
    
    def _preprocess(self, text):
        text = normalize_turkish(text)
        text = simple_stem(text)
        return text


def train():
    # Load dataset
    df = pd.read_csv("emergency_dataset.csv")
    
    print(f"Dataset size: {len(df)}")
    print(f"Label distribution:\n{df['label'].value_counts().sort_index()}")
    print()
    
    # Split for validation (stratified)
    X_train, X_test, y_train, y_test = train_test_split(
        df['text'], df['label'], 
        test_size=0.2, 
        random_state=42,
        stratify=df['label']  # Dengeli bölme
    )
    
    # Geliştirilmiş Pipeline:
    # 1. TurkishTextPreprocessor - Metin temizleme, normalizasyon, basit stemming
    # 2. TfidfVectorizer - Kelime + karakter n-gram, sublinear TF
    # 3. LinearSVC - Dengeli sınıf ağırlıkları
    model = Pipeline([
        ('preprocessor', TurkishTextPreprocessor()),
        ('tfidf', TfidfVectorizer(
            ngram_range=(1, 3),         # unigram + bigram + trigram
            max_features=15000,          # Daha fazla özellik (5000'den artırıldı)
            sublinear_tf=True,           # TF logaritmik ölçekleme (sık kelime dominasyonunu azaltır)
            min_df=2,                    # En az 2 belgede geçen kelimeler
            analyzer='word',             # Kelime bazlı
            strip_accents=None,          # Türkçe karakterleri koru
        )),
        ('clf', LinearSVC(
            class_weight='balanced',     # Dengesiz sınıflar için otomatik ağırlık
            max_iter=5000,               # Daha fazla iterasyon (2000'den artırıldı)
            C=1.0,                       # Regularizasyon
        ))
    ])
    
    # Cross-validation
    print("Running 5-fold stratified cross-validation...")
    cv = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
    cv_scores = cross_val_score(model, df['text'], df['label'], cv=cv, scoring='f1_weighted')
    print(f"CV F1 Scores: {cv_scores}")
    print(f"CV Mean F1: {cv_scores.mean():.4f} (+/- {cv_scores.std():.4f})")
    print()
    
    # Train on train split
    print("Training final model...")
    model.fit(X_train, y_train)
    
    # Evaluate
    y_pred = model.predict(X_test)
    
    label_names = ['LOW (0)', 'MEDIUM (1)', 'HIGH (2)', 'EMERGENCY (3)']
    
    print("\n═══ Model Evaluation Report ═══")
    print(classification_report(y_test, y_pred, target_names=label_names))
    
    print("\n═══ Confusion Matrix ═══")
    cm = confusion_matrix(y_test, y_pred)
    print(f"{'':>15} Predicted →")
    print(f"{'':>15} {'LOW':>8} {'MED':>8} {'HIGH':>8} {'EMRG':>8}")
    for i, row in enumerate(cm):
        print(f"Actual {label_names[i]:>8} {row[0]:>8} {row[1]:>8} {row[2]:>8} {row[3]:>8}")
    
    # Quick sanity check with known inputs
    print("\n═══ Sanity Check ═══")
    test_inputs = [
        ("iyiyim", "LOW"),
        ("enkaz altındayız", "EMERGENCY"),
        ("su lazım", "MEDIUM"),
        ("bacağım kırıldı", "HIGH"),
        ("güvendeyiz", "LOW"),
        ("yrdm edin enkazdyız", "EMERGENCY"),
        ("yangın var", "HIGH"),
        ("battaniye lazım", "MEDIUM"),
        ("bina çöktü", "EMERGENCY"),
        ("her sey iyi", "LOW"),
    ]
    
    priority_map = {0: "LOW", 1: "MEDIUM", 2: "HIGH", 3: "EMERGENCY"}
    all_passed = True
    for text, expected in test_inputs:
        pred = int(model.predict([text])[0])
        pred_label = priority_map[pred]
        status = "✓" if pred_label == expected else "✗"
        if pred_label != expected:
            all_passed = False
        print(f"  {status} '{text}' → {pred_label} (expected: {expected})")
    
    if all_passed:
        print("\n  ✅ All sanity checks passed!")
    else:
        print("\n  ⚠️ Some sanity checks failed - review model performance")
    
    # Save model
    joblib.dump(model, "emergency_model.pkl")
    print(f"\nModel trained and saved to emergency_model.pkl")

if __name__ == "__main__":
    train()
