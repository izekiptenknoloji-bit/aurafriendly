# Aura Friendly

Client-only bir Fabric Minecraft mod'u:

- **Kısayollar (macro):** 12 tuşa istediğin metni/komutu atayabilirsin (`/home`, `/spawn`, düz mesaj vb.). Sunucuda kurulu olması gerekmez, herhangi bir sunucuda çalışır.
- **Arkadaş sistemi:** Email/şifre ile Aura Friendly hesabı, gerçek zamanlı online durumu ve arkadaş ekleme/kabul etme (Firebase Realtime Database üzerinden).
- **Oto-güncelleme:** Oyun açılışında GitHub Releases'teki en son sürümü kontrol eder, varsa arka planda indirir.

## Sürümler

Bu repo iki ayrı Minecraft sürümünü paralel olarak barındırır:

- `mc1.21.11/` — Minecraft 1.21.11 için
- `mc1.21.4/` — Minecraft 1.21.4 için

Her klasör bağımsız bir Gradle/Fabric Loom projesidir.

## Geliştirme

Her iki proje de JDK 21 gerektirir.

```
cd mc1.21.11
./gradlew build
```

Derlenen jar `build/libs/aurafriendly-<versiyon>.jar` olarak çıkar.

## Kurulum (oyuncu için)

1. [Fabric Loader](https://fabricmc.net/use/) ve uyumlu [Fabric API](https://modrinth.com/mod/fabric-api) sürümünü kur.
2. [Releases](../../releases) sekmesinden Minecraft sürümüne uygun jar'ı indir (`aurafriendly-<versiyon>-mc<mcsurumu>.jar`).
3. `.minecraft/mods` klasörüne koy.
