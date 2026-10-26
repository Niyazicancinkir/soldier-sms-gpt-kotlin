# Asker GPT

İnternetsiz tuşlu telefonu **Gemini yapay zekasına** ve ailenin **Telegram grubuna** bağlayan Android SMS köprüsü.

Evde duran eski bir Android telefon, gelen SMS'leri dinler. Normal mesajları Gemini'ye sorar ve cevabı SMS olarak geri gönderir. `AILE.` ile başlayan mesajları ise Telegram'daki aile grubuna iletir. Aile de Telegram'dan askere SMS gönderebilir.

## Nasıl çalışır?

```
Tuşlu telefon ──SMS──▶ Android köprü ──▶ "AILE." ile mi başlıyor?
                         (Wi-Fi)            ├─ Hayır ─▶ Gemini API ─▶ yanıt SMS (≤160 karakter, ASCII)
                                            └─ Evet  ─▶ Telegram aile grubu

Telegram grubu ──"/asker mesaj"──▶ Android köprü ──yerel SMS──▶ Tuşlu telefon
```

| Dosya | Görevi |
|---|---|
| `SmsReceiver.kt` | Gelen SMS'i yakalar; sadece izinli numarayı işler, `AILE.` önekine göre Gemini ya da Telegram'a yönlendirir |
| `GeminiClient.kt` | Gemini API çağrısı, model yedeklemesi (fallback) |
| `TelegramClient.kt` / `TelegramPollingService.kt` | Aile grubuna mesaj gönderme; gruptan gelen `/asker` komutlarını dinleyen ön plan servisi |
| `SmsQueueManager.kt` | Giden SMS'leri sıraya koyar (aralarında 1,5 sn) |
| `SmsLoopGuard.kt` | Köprünün kendi gönderdiği SMS'i tekrar soru sanmasını engeller (sonsuz döngü koruması) |
| `TextSanitizer.kt` | Türkçe karakterleri ASCII'ye çevirir (tuşlu telefonda 160 karakter sınırı için) |
| `AppPreferences.kt` | Ayarları `EncryptedSharedPreferences` ile şifreli saklar |

## Kurulum

1. Projeyi Android Studio ile aç ve köprü olacak Android telefona yükle (min. Android 7.0 / API 24).
2. Uygulamada şunları gir:
   - **Gemini API anahtarı** ([Google AI Studio](https://aistudio.google.com/apikey))
   - **İzinli numara**: tuşlu telefonun numarası (sadece bu numaradan gelen SMS'ler işlenir)
   - **Telegram Bot Token** ([@BotFather](https://t.me/BotFather)) ve **aile grubunun chat ID'si**
3. SMS izinlerini ver. Android'in pil optimizasyonunu (Doze) bu uygulama için kapat ki servis arka planda uyumasın.
4. Köprü telefonu Wi-Fi'a ve şarja bağlı bırak.

API anahtarları koda gömülü değildir; uygulama ekranından girilir ve cihazda şifreli tutulur.

## Kullanım

| Kimden | Ne yazar | Ne olur |
|---|---|---|
| Asker (SMS) | `Fenerbahce maci ne oldu?` | Gemini'nin kısa cevabı SMS olarak döner |
| Asker (SMS) | `AILE. Annecim nasilsiniz` | Mesaj Telegram aile grubuna düşer |
| Aile (Telegram grubu) | `/asker Iyiyiz oglum` | Mesaj askere SMS olarak gider |

## Maliyet ve güvenlik notları

- Sistem bedava değildir: köprü cihazın interneti, yerel hattan giden SMS'ler ve Gemini API kotası kullanılır. Yurt dışındaki aile için uluslararası arama/SMS ücreti yoktur.
- Askeri kurallara ve birliğinizin telefon kullanım kurallarına uyun. Konum, birlik veya görevle ilgili bilgi paylaşmayın.
- Bu proje kişisel/eğitim amaçlıdır; Google Play'in SMS izin politikaları nedeniyle mağazada yayınlanması ek onay gerektirebilir.
