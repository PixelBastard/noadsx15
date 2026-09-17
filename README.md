# NoAds X15 Standalone 3.0

Aplicație Android standalone. Nu necesită Raspberry Pi, server, cont sau abonament.

## Ce s-a schimbat față de prototipul 1.x
- descarcă și compilează local liste mari, nu 18 domenii hardcodate;
- folosește surse separate pentru reclame/trackere și popup domains;
- allowlist și blocklist personale;
- actualizare atomică: lista veche rămâne dacă downloadul eșuează;
- upstream DNS cu fallback;
- teste unitare înainte de build;
- UI cu status, statistici, actualizare și test DNS.

## Prima utilizare
1. Instalează APK-ul.
2. Lasă DNS privat pe Automat/Oprit.
3. Deschide aplicația și apasă Actualizează listele în timp ce protecția este oprită.
4. Verifică să apară cel puțin 20.000 domenii.
5. Activează protecția și acceptă dialogul VPN Android.

## Limite
Este filtrare DNS system-wide. Blochează multe reclame, trackere și popup-uri care depind de domenii externe, dar nu poate modifica vizual alte aplicații și nu elimină universal reclamele YouTube/same-origin.
