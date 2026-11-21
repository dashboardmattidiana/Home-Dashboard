# Heimboard · Familien-Dashboard (Android TV + Smartphone-Web-App)

Ein ruhiges Mid-Century-Modern-Familien-Dashboard für **Philips Google TV (Android 14, 16:9)** und **Smartphone-Web-App (GitHub Pages)** mit Echtzeit-Synchronisation über **Firebase Firestore**.

## Struktur des Repositories

- `app/` — Native Android-TV-App (Kotlin, Jetpack Compose, `LEANBACK_LAUNCHER`, `FLAG_KEEP_SCREEN_ON`, 3 rotierende Bento-Layouts alle 15 Minuten, Burn-in-Risikominderung & Ruhemodus, Open-Meteo Wetter-API, Coil Disk-Cache + lokale Fallback-Artworks).
- `web/` — Mobile Web-App für Smartphones (HTML/CSS/ESM-JS ohne Build-Schritt, direkter GitHub-Pages-Deploy, LocalStorage-Profilumschalter für beide Partner, Open-Meteo Stadtsuche).
- `firestore.rules` & `firebase-blueprint.json` — Abgesicherte Firestore-Sicherheitsregeln.
- `.github/workflows/build-apk.yml` — Baut automatisch `Heimboard-TV.apk` und veröffentlicht sie unter dem Release-Tag `latest-tv-apk`.
- `.github/workflows/deploy-pages.yml` — Veröffentlicht `web/` automatisch auf GitHub Pages.

## Einrichtung

1. **GitHub Pages aktivieren**: In den Repository-Einstellungen unter *Pages → Source* auf **GitHub Actions** stellen.
2. **Firebase Auth Domain freigeben**: In der Firebase Console (`com-aistudio-heimboard-f-4b456`) unter *Authentication → Settings → Authorized domains* deine GitHub-Pages-Domain (`<dein-github-name>.github.io`) hinzufügen, damit Google Sign-In in der Web-App funktioniert.
3. **APK auf dem Philips Google TV installieren**:
   - Im GitHub-Repository unter *Releases → `latest-tv-apk`* die `Heimboard-TV.apk` herunterladen (z. B. mit der App *Downloader* auf Google TV über `https://github.com/<USER>/<REPO>/releases/download/latest-tv-apk/Heimboard-TV.apk`).
   - App starten und einmalig mit demselben Google-Konto anmelden.
4. **Namen & Wohnort anpassen**: In der Web-App unter dem Tab **Brett → Haushalt & Wetterort** eure Namen (`partnerA`, `partnerB`) und euren Wohnort speichern – TV und Web-App synchronisieren sofort.
