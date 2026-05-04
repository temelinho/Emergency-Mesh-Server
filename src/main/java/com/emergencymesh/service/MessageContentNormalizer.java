package com.emergencymesh.service;

import com.emergencymesh.model.EmergencyRequest;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Uygulamadan gelen mesaj içeriğini parse eder; kategori, öncelik ve temiz açıklama üretir.
 * Uygulama değişikliği gerektirmez - sunucu tarafında tüm formatlar dönüştürülür.
 */
public final class MessageContentNormalizer {

    private MessageContentNormalizer() {}

    public static ParsedEmergency parse(String rawContent) {
        if (rawContent == null) rawContent = "";
        String trimmed = rawContent.trim();

        // Android uygulaması mesajları "Username|||içerik" formatında gönderir.
        // Tag algılama için önce bu mührü soyuyoruz.
        if (trimmed.contains("|||")) {
            String[] parts = trimmed.split("\\|\\|\\|", 2);
            if (parts.length == 2) {
                trimmed = parts[1].trim();
            }
        }
        ParsedEmergency result = new ParsedEmergency();

        // 1) !1, !2, !3... etiket formatı (mevcut)
        if (trimmed.length() >= 2 && trimmed.startsWith("!")) {
            String tag = trimmed.substring(0, 2);
            ParsedEmergency fromTag = fromTag(tag, trimmed);
            if (fromTag != null) return fromTag;
        }

        // 2) Başta veya tek başına rakam: "4", "4 yangın var", "4- yangın"
        Matcher numMatcher = Pattern.compile("^[!]?(\\d)\\s*[-.:]?\\s*(.*)$", Pattern.CASE_INSENSITIVE).matcher(trimmed);
        if (numMatcher.matches()) {
            String num = numMatcher.group(1);
            String rest = numMatcher.group(2).trim();
            ParsedEmergency fromNum = fromNumericTag(num, rest);
            if (fromNum != null) return fromNum;
        }

        // 3) Anahtar kelimeler (Türkçe / İngilizce)
        ParsedEmergency fromKeyword = fromKeywords(trimmed);
        if (fromKeyword != null) return fromKeyword;

        // 4) Hiçbir etiketle eşleşmeyen serbest metinleri analiz et (Smart NLP Fallback)
        return analyzeManualMessage(trimmed);
    }

    private static ParsedEmergency analyzeManualMessage(String text) {
        String lower = text.toLowerCase(new Locale("tr", "TR"));
        ParsedEmergency r = new ParsedEmergency();
        r.isEmergency = true; // Tüm mesajlar admin panelde görünsün
        r.cleanContent = text;
        r.type = EmergencyRequest.EmergencyType.OTHER; // Varsayılan tür

        int score = 0;

        // Kritik Durumlar (Kombinasyonlar ve Ağırlıklar)
        if (containsAny(lower, "bilinç", "baygın", "bayıldı", "nefes", "solunum", "kalp")) score += 60;
        if (containsAny(lower, "kan", "kanama", "fışkırıyor", "kesik", "yaralandı")) score += 50;
        if (containsAny(lower, "enkaz", "göçük", "altında", "sıkıştı", "çıkamıyor")) score += 70;
        if (containsAny(lower, "yangın", "alev", "patlama", "gaz kaçağı", "duman")) score += 50;

        // Hassas Gruplar (Hafif artı puan)
        if (containsAny(lower, "bebek", "çocuk", "hamile", "yaşlı")) score += 20;

        // Öncelik Belirleme (AI servis çalışıyorsa AI kararı bunu geçersiz kılabilir)
        if (score >= 100) {
            r.priority = EmergencyRequest.EmergencyPriority.EMERGENCY; // Siyah Kod
        } else if (score >= 50) {
            r.priority = EmergencyRequest.EmergencyPriority.HIGH; // Kırmızı Kod
        } else if (score >= 30) {
            r.priority = EmergencyRequest.EmergencyPriority.MEDIUM; // Turuncu Kod
        } else {
            r.priority = EmergencyRequest.EmergencyPriority.LOW; // Sarı/Yeşil Kod
        }

        // Akıllı Tür Müdahalesi (Eğer kelime geçiyorsa tipi güncelle)
        if (containsAny(lower, "yangın", "itfaiye", "alev")) r.type = EmergencyRequest.EmergencyType.FIRE;
        else if (containsAny(lower, "enkaz", "göçük", "altında")) r.type = EmergencyRequest.EmergencyType.RESCUE_DEBRIS;
        else if (containsAny(lower, "kan", "bilinç", "kalp", "hasta", "yaralı")) r.type = EmergencyRequest.EmergencyType.MEDICAL;
        else if (containsAny(lower, "su", "yemek", "erzak", "aç", "susuz")) r.type = EmergencyRequest.EmergencyType.FOOD;
        else if (containsAny(lower, "polis", "güvenlik", "saldırı", "kavga")) r.type = EmergencyRequest.EmergencyType.POLICE;

        return r;
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String k : keywords) {
            if (text.contains(k)) return true;
        }
        return false;
    }

    private static ParsedEmergency fromTag(String tag, String raw) {
        String rest = raw.substring(2).trim();
        ParsedEmergency r = new ParsedEmergency();
        r.isEmergency = true;
        r.cleanContent = rest.isEmpty() ? defaultDescription(tag) : rest;

        switch (tag) {
            case "!1": r.type = EmergencyRequest.EmergencyType.SAFE; r.priority = EmergencyRequest.EmergencyPriority.LOW; break;
            case "!2": r.type = EmergencyRequest.EmergencyType.MEDICAL; r.priority = EmergencyRequest.EmergencyPriority.HIGH; break;
            case "!3": r.type = EmergencyRequest.EmergencyType.RESCUE_DEBRIS; r.priority = EmergencyRequest.EmergencyPriority.EMERGENCY; break;
            case "!4": r.type = EmergencyRequest.EmergencyType.FIRE; r.priority = EmergencyRequest.EmergencyPriority.HIGH; break;
            case "!5": r.type = EmergencyRequest.EmergencyType.SUPPLY_WATER; r.priority = EmergencyRequest.EmergencyPriority.MEDIUM; break;
            case "!6": r.type = EmergencyRequest.EmergencyType.SHELTER; r.priority = EmergencyRequest.EmergencyPriority.MEDIUM; break;
            case "!7": r.type = EmergencyRequest.EmergencyType.DANGER; r.priority = EmergencyRequest.EmergencyPriority.HIGH; break;
            case "!8": r.type = EmergencyRequest.EmergencyType.POWER_OUTAGE; r.priority = EmergencyRequest.EmergencyPriority.MEDIUM; break;
            case "!9": r.type = EmergencyRequest.EmergencyType.OTHER; r.priority = EmergencyRequest.EmergencyPriority.MEDIUM; break;
            default: return null;
        }
        return r;
    }

    private static ParsedEmergency fromNumericTag(String num, String rest) {
        String tag = "!" + num;
        return fromTag(tag, tag + " " + rest);
    }

    private static ParsedEmergency fromKeywords(String content) {
        if (content.length() < 2) return null;
        String lower = content.toLowerCase(Locale.ROOT).trim();
        ParsedEmergency r = new ParsedEmergency();
        r.isEmergency = true;
        r.cleanContent = content;

        if (matches(lower, "güvendeyim", "güvenli", "safe", "iyiyim", "iyi")) {
            r.type = EmergencyRequest.EmergencyType.SAFE;
            r.priority = EmergencyRequest.EmergencyPriority.LOW;
            return r;
        }
        if (matches(lower, "tıbbi", "medical", "sağlık", "health", "hasta", "yaralı", "ambulans")) {
            r.type = EmergencyRequest.EmergencyType.MEDICAL;
            r.priority = EmergencyRequest.EmergencyPriority.HIGH;
            return r;
        }
        if (matches(lower, "enkaz", "debris", "rescue_debris", "altında", "trapped")) {
            r.type = EmergencyRequest.EmergencyType.RESCUE_DEBRIS;
            r.priority = EmergencyRequest.EmergencyPriority.EMERGENCY;
            return r;
        }
        if (matches(lower, "yangın", "fire", "ateş")) {
            r.type = EmergencyRequest.EmergencyType.FIRE;
            r.priority = EmergencyRequest.EmergencyPriority.HIGH;
            return r;
        }
        if (matches(lower, "su", "water", "erzak", "supply", "yiyecek", "food")) {
            r.type = matches(lower, "yiyecek", "food", "erzak") ? EmergencyRequest.EmergencyType.FOOD : EmergencyRequest.EmergencyType.SUPPLY_WATER;
            r.priority = EmergencyRequest.EmergencyPriority.MEDIUM;
            return r;
        }
        if (matches(lower, "barınma", "shelter", "evsiz", "ev")) {
            r.type = EmergencyRequest.EmergencyType.SHELTER;
            r.priority = EmergencyRequest.EmergencyPriority.MEDIUM;
            return r;
        }
        if (matches(lower, "tehlike", "danger", "tehlikeli", "acil")) {
            r.type = EmergencyRequest.EmergencyType.DANGER;
            r.priority = EmergencyRequest.EmergencyPriority.HIGH;
            return r;
        }
        if (matches(lower, "şarj", "charger", "power", "elektrik", "batarya")) {
            r.type = EmergencyRequest.EmergencyType.POWER_OUTAGE;
            r.priority = EmergencyRequest.EmergencyPriority.MEDIUM;
            return r;
        }
        if (matches(lower, "kaza", "accident", "trafik")) {
            r.type = EmergencyRequest.EmergencyType.ACCIDENT;
            r.priority = EmergencyRequest.EmergencyPriority.HIGH;
            return r;
        }
        if (matches(lower, "polis", "police", "güvenlik", "security")) {
            r.type = EmergencyRequest.EmergencyType.POLICE;
            r.priority = EmergencyRequest.EmergencyPriority.HIGH;
            return r;
        }
        if (matches(lower, "kurtarma", "rescue", "yardım", "help")) {
            r.type = EmergencyRequest.EmergencyType.RESCUE;
            r.priority = EmergencyRequest.EmergencyPriority.HIGH;
            return r;
        }

        return null;
    }

    private static boolean matches(String text, String... keywords) {
        for (String k : keywords) {
            if (text.contains(k)) return true;
        }
        return false;
    }

    private static String defaultDescription(String tag) {
        switch (tag) {
            case "!1": return "Güvendeyim";
            case "!2": return "Tıbbi Yardım Talebi";
            case "!3": return "Enkaz Altındayım";
            case "!4": return "Yangın İhbarı";
            case "!5": return "Erzak/Su İhtiyacı";
            case "!6": return "Barınma İhtiyacı";
            case "!7": return "Genel Tehlike";
            case "!8": return "Şarj Bitiyor";
            case "!9": return "Diğer Acil Durum";
            default: return "Acil yardım talebi";
        }
    }

    public static class ParsedEmergency {
        public boolean isEmergency;
        public EmergencyRequest.EmergencyType type;
        public EmergencyRequest.EmergencyPriority priority;
        public String cleanContent;
    }
}
