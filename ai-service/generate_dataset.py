import pandas as pd
import random
import re

def generate_dataset():
    # Labels: 
    # 3 = EMERGENCY (Black) - Life-threatening, trapped, active disaster
    # 2 = HIGH (Red) - Serious injury, active fire, accident
    # 1 = MEDIUM (Orange) - Logistics, shelter, minor injury, needs supplies
    # 0 = LOW (Yellow/Green) - Safe, status updates, general info
    
    categories = {
        3: [
            # Enkaz / Göçük
            "Enkaz altındayız yardım edin nefes alamıyoruz",
            "Bina çöktü, {n} kişi göçük altında kaldı",
            "Göçük altındayız, yerimiz belli değil, sesimizi duyun",
            "Nefes almakta zorlanıyorum, etraf kapalı, hava bitiyor",
            "Kalbim duruyor, acil yardım lazım",
            "Ağır yaralı var, bilinci tamamen kapalı",
            "Enkazdan sesler geliyor ama ulaşamıyoruz",
            "Bebek enkaz altında kaldı, çok acil ekip lazım",
            "Patlama oldu, bina yıkıldı, insanlar altında",
            "Sıkıştık dışarı çıkamıyoruz, tavan çöküyor",
            "Sesimi duyan var mı enkaz altındayım",
            "Duvar üstümüze yıkıldı, hareket edemiyoruz",
            "Hava çok az kaldı, acil kurtarma",
            "Bilinç kaybı var, kanama durmuyor, enkazdayız",
            "Kızım göçük altında, lütfen yardım edin",
            "Enkaz altında sıkışan bir çocuk var",
            "Balkon çöktü, insanlar altında kaldı",
            "Binamız yıkıldı, acil arama kurtarma",
            "Enkazdan yardım çığlıkları geliyor",
            "Merdivenler çöktü, üst katta mahsuruz ve bina sallanıyor",
            # Yeni eklenen gerçekçi mesajlar
            "yardım edin altında kaldık çıkamıyoruz",
            "3 katlı bina tamamen çöktü içinde aile var",
            "depremde göçük altında kaldık lütfen gelin",
            "beton altındayız hava yok acil",
            "çocuklarım enkazda çıkaramıyorum",
            "bina yıkıldı ailem içerde yardım",
            "nefes alamıyorum gömüldüm yardım edin",
            "çöken binanın altındayız su basıyor",
            "sıkıştım bacaklarım hareket etmiyor enkaz",
            "binamız çöktü komşular altında kaldı",
            "apartman yıkıldı insanlar bağırıyor alttan",
            "göçük var 5 kişi altında kurtarın",
            "enkaz altinda sesler geliyor cocuk sesi",
            "nefesim kesilyor hava yok yardim",
            "gömüldük altındayız acil ekip",
            "duvarlar üstümüze çöktü çıkamıyoruz",
            "bina sallanarak yıkıldı mahsur kaldık",
            "bodrum kata sıkıştık su doluyor",
            "tavandan beton düştü yaralıyız çıkamıyoruz",
            "deprem sonrası bina çöktü kurtarın bizi",
        ],
        2: [
            # Yangın ve ciddi yaralanma
            "Büyük bir yangın var, itfaiye hemen gelmeli",
            "Ağır kanama var, turnike yapıyoruz ama durmuyor",
            "Bacağım kırıldı, açık yara var, hareket edemiyorum",
            "Komşumuz bayıldı, nefesi zayıf, acil ambulans",
            "Mutfakta tüp patladı, yaralılar var",
            "Çok kan kaybediyorum, yardım edin lütfen",
            "Astım krizine girdi, ilaçları yok, nefes alamıyor",
            "Çatıda mahsur kaldık, alevler her yeri sardı",
            "Trafik kazası oldu, araçta sıkışanlar çıkarılamıyor",
            "Kimyasal koku var, herkes öksürüyor ve bayılanlar var",
            "Kafa travması var, burnundan kan geliyor",
            "Vücudunda ciddi yanıklar var, acil müdahale",
            "Ayağına demir saplandı, çok acı çekiyor",
            "İç kanama şüphesi var, karnı çok sertleşti",
            "Alevler binayı sardı, mahsur kaldık",
            "Gaz kaçağı kokusu var, patlamadan korkuyoruz",
            "Kavga çıktı, birisi bıçaklandı, durumu ağır",
            "Şok geçiriyor, elleri ayakları buz kesti",
            "Yüksekten düştü, beli kırılmış olabilir",
            "Zehirlenme belirtileri var, kusuyor",
            # Yeni eklenen gerçekçi mesajlar
            "yangın çıktı daire yanıyor yardım",
            "bacağından çok kan akıyor durduramıyoruz",
            "çocuk düştü kafası yarıldı bayıldı",
            "araba kazası var yaralılar çıkarılamıyor",
            "bıçaklama olayı var acil ambulans",
            "binanın alt katı yanıyor dumanlar çıkıyor",
            "kalp krizi geçiriyor acil müdahale şart",
            "kolunu kırdı kemik dışarı çıktı",
            "tüp patladı mutfak yanıyor",
            "elektrik çarptı yere yığıldı",
            "boğulma tehlikesi var suda birisi var",
            "ateşli silah sesi var yaralı olabilir",
            "ciddi alerji reaksiyonu şişti nefes alamıyor",
            "araç devrildi içindekiler sıkıştı",
            "merdivenden yuvarlandı bilinci kapanıyor",
            "karbonmonoksit zehirlenmesi olabilir herkes baygın",
            "köpek saldırdı kolunu parçaladı",
            "ağır yaralı var kanama çok fazla ambulans",
            "yüzünde ciddi yanık var acil hastane",
            "trafik kazası {n} araç birbirine girdi yaralılar var",
        ],
        1: [
            # Lojistik / malzeme ihtiyacı
            "Su bitti, çocuklar çok susadı, acil su lazım",
            "Erzak lazım, yiyecek bir şeyimiz kalmadı",
            "Hava çok soğuk, çadır veya battaniye istiyoruz",
            "Şarjım bitmek üzere, hiçbir cihaz çalışmıyor, elektrik yok",
            "Ekmek ve mama ihtiyacımız var, bebekler aç",
            "Barınacak yerimiz yok, sokaktayız, üşüyoruz",
            "Bebek bezi ve hijyen paketi gerekiyor acilen",
            "Elektrik kesintisi uzun süredir devam ediyor, karanlıktayız",
            "Temiz su hattı patladı, içecek suyumuz kalmadı",
            "Kandaki şekerim düştü, yemek lazım, tansiyon düştü",
            "İlaçlarım bitti, düzenli kullanmam gerekiyor",
            "Isınma sorunu var, odun veya kömür lazım",
            "Çadırımız su alıyor, yeni bir yere geçmemiz gerek",
            "Yağmurun altında kaldık, battaniye gönderin",
            "Engelli bir vatandaş var, tekerlekli sandalye lazım",
            "Süt ve çocuk bezi ihtiyacımız çok fazla",
            "Koyunlar telef oldu, yem lazım",
            "Burada çok toz var, maske gönderir misiniz",
            "Battaniye ve kışlık kıyafet yardımı istiyoruz",
            "Aydınlatma için fener veya pil lazım",
            # Yeni eklenen gerçekçi mesajlar
            "yiyecek kalmadı çocuklar aç",
            "içecek su yok acil su gönderin",
            "ilaç lazım tansiyon ilacı bitti",
            "üşüyoruz battaniye yok çadır yok",
            "bebek maması lazım acil",
            "elektrik yok karanlıkta kalıyoruz",
            "tuvalet ihtiyacı var portatif tuvalet lazım",
            "yaşlı hastamız var ilaçları bitmek üzere",
            "hijyen malzemesi lazım kadın pedi yok",
            "çadırımız yırtıldı yağmur yağıyor ıslanıyoruz",
            "su bulamıyoruz çeşmeler akmıyor",
            "kıyafet lazım üstümüzdekilerle kaldık",
            "powerbank lazım telefonlar ölmek üzere",
            "ambulans lazım ama acil değil hafif yaralanma var",
            "soba lazım donuyoruz gece çok soğuk",
            "çölyak hastası var glütensiz un lazım",
            "diyabet ilacı bulmamız lazım acilen",
            "dezenfektan ve eldiven lazım temizlik için",
            "barınak arıyoruz kalabalık aileyiz",
            "jeneratör lazım elektrik gelmiyor",
        ],
        0: [
            # Güvenli / bilgi
            "Ben ve ailem güvendeyiz, iyiyiz şükür",
            "Her şey yolunda, evimiz sağlam, hasar yok",
            "Güvenli bölgeye ulaştık, burası güvenli",
            "Diğerlerine yardım ediyorum, durumum gayet iyi",
            "Yardım tırları geldi, dağıtım yapılıyor",
            "Merhabalar, koordinasyonu sağlıyoruz, sıkıntı yok",
            "Burada durum şu an sakin, panik yapmayın",
            "Güvenli alan burası, toplanma yerindeyiz",
            "Sadece bilgi almak istiyorum, son durum nedir",
            "Görevliler burada, her şey kontrol altında",
            "Biz iyiyiz, maraş merkezdeyiz, yıkım yok burada",
            "Haberleşme sağlandı, iletişime geçebilirsiniz",
            "Şu an güvendeyiz, bir ihtiyacımız yok",
            "Ekipler ulaştı, her şey yolunda görünüyor",
            "Yollar açık, ulaşım sağlanabiliyor",
            "Sadece sarsıntıyı hissettik ama iyiyiz",
            "Korktuk ama bir hasarımız yok",
            "Teşekkürler, biz güvendeyiz",
            "Durum güncellemesi: her yer sakin",
            # Yeni eklenen gerçekçi mesajlar
            "iyiyiz sorun yok",
            "güvendeyiz merak etmeyin",
            "biz sağız burası güvenli",
            "hasar yok evde kaldık",
            "ailem yanımda hepimiz iyiyiz",
            "yıkım yok bizim tarafta",
            "şükür bir şey olmadı",
            "çadır kuruldu yerleştik",
            "yardım geldi teşekkürler",
            "merhaba bilgi almak istiyorum",
            "artçı deprem hissettik ama sorun yok",
            "komşularla birlikte toplanma alanındayız",
            "internet geldi iletişim sağlandı",
            "akrabalarımız da güvende haber aldık",
            "durum normal panik yok",
            "elektrik geldi her şey düzeliyor",
            "yol açıldı ulaşım var",
            "tahliye tamamlandı güvendeyiz",
            "hiçbir sıkıntımız yok sağ olun",
            "herkes iyi burda endişelenmeyin",
            # İngilizce mesajlar (mesh ağında olabilir)
            "Safe, we are fine here in this camp",
            "We are ok no damage",
            "All good here everyone is safe",
            "Help arrived thanks",
            "No injuries reported here",
        ]
    }

    # Çeşitli prefix/suffix - daha az ama daha gerçekçi
    prefixes = ["", "Lütfen ", "Acil ", "ACİL: ", "YARDIM ", ""]
    suffixes = ["", "!", "!!!", " lütfen", " acil", ""]
    
    # Yazım hatası simülasyonu - Türkçe'de yaygın hatalar
    typo_map = {
        "ı": "i", "İ": "I", "ş": "s", "Ş": "S",
        "ç": "c", "Ç": "C", "ğ": "g", "Ğ": "G",
        "ö": "o", "Ö": "O", "ü": "u", "Ü": "U"
    }

    def make_typo(text):
        """Türkçe karakter hatalarını simüle eder"""
        result = list(text)
        for i, ch in enumerate(result):
            if ch in typo_map and random.random() > 0.5:
                result[i] = typo_map[ch]
        return "".join(result)

    def remove_spaces(text):
        """Bazı boşlukları kaldır (hızlı yazma simülasyonu)"""
        words = text.split()
        if len(words) <= 2:
            return text
        # Rastgele 1-2 boşluğu kaldır
        result = []
        for i, w in enumerate(words):
            result.append(w)
            if i < len(words) - 1 and random.random() > 0.85:
                # boşluk ekleme (birleşik yaz)
                pass
            elif i < len(words) - 1:
                result.append(" ")
        return "".join(result)

    expanded_data = []

    for label, texts in categories.items():
        for base_text in texts:
            # Orijinal metin + temel varyasyonlar
            for pre in prefixes:
                for suf in suffixes:
                    n = random.randint(1, 10)
                    text = base_text.replace("{n}", str(n))
                    final_text = f"{pre}{text}{suf}"
                    expanded_data.append({"text": final_text, "label": label})
                    
                    # Lowercase varyasyon
                    if random.random() > 0.6:
                        expanded_data.append({"text": final_text.lower(), "label": label})
                    
                    # UPPERCASE varyasyon (panik mesajları)
                    if random.random() > 0.85:
                        expanded_data.append({"text": final_text.upper(), "label": label})
                    
                    # Yazım hatalı varyasyon (Türkçe karakter olmadan)
                    if random.random() > 0.6:
                        expanded_data.append({"text": make_typo(final_text), "label": label})

    # Kısa mesajlar ve edge case'ler (gerçek kullanıcı davranışı)
    short_messages = [
        # EMERGENCY kısa
        ("enkaz", 3), ("göçük", 3), ("altındayız", 3), ("gömüldük", 3),
        ("nefes yok", 3), ("bina çöktü", 3), ("kurtarın", 3),
        ("enkaz altındayım", 3), ("çıkamıyoruz", 3), ("hava bitiyor", 3),
        ("enkaz yardım", 3), ("gocuk altinda", 3), ("altindayiz yardim", 3),
        ("sesimizi duyun enkazdayiz", 3), ("bina yikildi", 3),
        
        # HIGH kısa
        ("yangın", 2), ("kan", 2), ("ambulans", 2), ("yaralı", 2),
        ("kaza", 2), ("yanıyor", 2), ("bayıldı", 2), ("kırıldı", 2),
        ("acil ambulans", 2), ("kan akıyor", 2), ("yangın var", 2),
        ("bıçaklandı", 2), ("düştü yaralı", 2), ("patladı", 2),
        ("ciddi yaralı", 2), ("kanama durmuyor", 2),
        
        # MEDIUM kısa
        ("su lazım", 1), ("aç", 1), ("battaniye", 1), ("ilaç", 1),
        ("yemek lazım", 1), ("su yok", 1), ("elektrik yok", 1),
        ("mama lazım", 1), ("çadır lazım", 1), ("üşüyoruz", 1),
        ("erzak", 1), ("su istiyoruz", 1), ("yiyecek yok", 1),
        ("ilaç bitti", 1), ("barınak lazım", 1),
        
        # LOW kısa  
        ("iyiyim", 0), ("güvendeyiz", 0), ("tamam", 0), ("iyiyiz", 0),
        ("sorun yok", 0), ("güvende", 0), ("hasar yok", 0), ("sağız", 0),
        ("teşekkürler", 0), ("merak etmeyin", 0), ("ok", 0), ("burdayız", 0),
        ("problem yok", 0), ("her sey iyi", 0), ("iyiyiz sağolun", 0),
    ]
    
    for text, label in short_messages:
        # Her kısa mesajdan birkaç varyasyon
        expanded_data.append({"text": text, "label": label})
        expanded_data.append({"text": text.upper(), "label": label})
        expanded_data.append({"text": text.capitalize(), "label": label})
        expanded_data.append({"text": make_typo(text), "label": label})

    # Karışık / belirsiz mesajlar (modelin öğrenmesi gereken zor vakalar)
    ambiguous = [
        ("güvendeyiz ama su lazım", 1),  # Güvende ama ihtiyaç var → MEDIUM
        ("iyiyiz sadece yiyecek lazım", 1),
        ("yaralı yok ama barınak lazım", 1),
        ("güvendeyiz elektrik yok", 1),
        ("biz iyiyiz ama komşu enkaz altında", 3),
        ("hasar yok ama yangın kokusu geliyor", 2),
        ("iyiyiz ama yaşlı komşu bayıldı", 2),
        ("hafif yara var ama ciddi değil", 1),
        ("bina çatladı ama ayakta güvende miyiz", 2),
        ("yardım geldi ama ilaç eksik", 1),
        ("kurtarıldık ama yaralıyız", 2),
        ("tahliye olduk şimdi güvendeyiz", 0),
        ("sarsıntı hissettik korktuk ama sorun yok", 0),
        ("deprem oldu ama bina sağlam iyiyiz", 0),
    ]
    for text, label in ambiguous:
        expanded_data.append({"text": text, "label": label})
        expanded_data.append({"text": text.upper(), "label": label})
        expanded_data.append({"text": make_typo(text), "label": label})

    # Shuffle
    random.shuffle(expanded_data)
    
    # Limit dataset size
    df = pd.DataFrame(expanded_data)
    if len(df) > 8000:
        # Dengeli örnekleme: her sınıftan eşit sayıda al
        balanced = df.groupby('label').apply(
            lambda x: x.sample(n=min(len(x), 2000), random_state=42)
        ).reset_index(drop=True)
        df = balanced.sample(frac=1, random_state=42).reset_index(drop=True)

    df.to_csv("emergency_dataset.csv", index=False)
    print(f"Dataset generated with {len(df)} samples.")
    print(f"Distribution:\n{df['label'].value_counts().sort_index()}")

if __name__ == "__main__":
    generate_dataset()
