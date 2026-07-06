# Ganot – Akademik Asistan

Ganot, üniversite öğrencilerinin vize, final, devamsızlık sınırları ve sınav takvimleri arasındaki akademik süreçlerini tek bir noktadan yönetebilmeleri için tasarlanmış, minimalist ve yüksek performanslı bir mobil asistandır. 

Eski ve karmaşık hesaplama tablolarının aksine, tamamen kullanıcı deneyimine odaklanan yapısıyla dönem ve genel not ortalamalarını (GANO) hatasız bir şekilde takip etmenizi sağlar.

## Öne Çıkan Özellikler

* **Akıllı GANO & Not Hesaplama:** Üniversitelerin harf notu sistemlerine (AA, BA...) ve vize/final etki oranlarına uygun, dinamik ve hatasız hesaplama motoru.
* **Gelişmiş Devamsızlık Sayaçları:** Ders bazlı devamsızlık sınırlarının takibi ve sınıra yaklaşma durumlarında akıllı uyarı mekanizmaları.
* **Merkezi Akademik Takvim:** Vize, final, büt ve proje teslim tarihlerini tek bir ekranda organize eden takvim modülü.
* **Benzersiz Kod ile Ders Paylaşımı:** Sınıf arkadaşları arasında ders programlarını veya harf sistemlerini tek tek elle girmek yerine, üretilen benzersiz kodlar aracılığıyla saniyeler içinde veri aktarımı.
* ** %100 Gizlilik ve Yerel Veri Güvenliği ** Kullanıcı verileri hiçbir uzak sunucuya yedeklenmez; tüm akademik geçmiş cihaz üzerinde yerel ve güvenli veri tabanlarında şifreli olarak saklanır.

## Mağazalar & Sürümler

Uygulamanın güncel üretim (production) sürümlerine aşağıdaki resmi mağazalardan ulaşabilirsiniz:
* **Huawei AppGallery:** [Canlı Sürümü İncele](https://appgallery.huawei.com)
* **Google Play Store:** *Yakında*
* **Apple App Store:** *Yakında*

## Teknik Mimari ve Teknolojiler

Proje, çoklu platform desteğini en yüksek performansla sunabilmek adına modern mobil teknolojilerle inşa edilmiştir:

* **Core Architecture:** Kotlin Multiplatform (KMP) & Compose Multiplatform
* **Language:** Kotlin / Dart (Legacy core)
* **Data Persistence:** Platform-agnostic encrypted local storage (Yerel şifreli veri tabanı mimarisi)
* **Target Platforms:** Android (API 24+), iOS (15.0+)

## Yerel Kurulum (Setup)

Projeyi kendi yerel geliştirme ortamınızda derlemek için aşağıdaki adımları takip edebilirsiniz:

1. Projeyi bilgisayarınıza klonlayın:

```bash
git clone https://github.com/RieStudio/Ganot.git
```
