# Control cu două butoane

- `PORNEȘTE TOT`: activează logica Auto Skip/Close și pornește filtrarea DNS.
- `OPREȘTE TOT`: oprește VPN-ul și face serviciul de Accesibilitate complet inactiv.

## Limită Android
La prima utilizare, Android nu permite unei aplicații să-și acorde singură accesul VPN sau Accesibilitate. Utilizatorul trebuie să confirme fiecare permisiune în ecranul de sistem. După aceea, controlul zilnic se face din cele două butoane.

Android nu permite aplicației să dezactiveze programatic autorizarea serviciului de Accesibilitate. `OPREȘTE TOT` dezactivează intern orice acțiune a serviciului, deci acesta rămâne autorizat dar nu mai procesează sau apasă nimic.
