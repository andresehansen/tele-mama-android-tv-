# 📺 AndyTV — Aplicación para Android TV / Google TV

Aplicación de televisión argentina en vivo diseñada específicamente para **Android TV, Google TV, Smart TVs y TV Boxes (Chromecast con Google TV, Xiaomi Mi Box, etc.)**, optimizada con navegación 100% mediante control remoto.

## 🚀 Características Principales
- **Arquitectura Modular con `channels.json`:** Las señales, números y categorías se gestionan de forma desacoplada en JSON. Podés actualizar la grilla de canales sin recompilar el APK.
- **Doble Motor de Reproducción:**
  - **AndroidX Media3 (ExoPlayer) Nativo:** Decodificación por hardware de alto rendimiento habilitada por defecto en Android TV para transmisiones HLS directas con mínimo consumo.
  - **Player Web Seguro & YouTube Live:** Integración oficial con escudos anti-popups y sin publicidad intrusiva.
- **Guía de Programación Electrónica (EPG) en Vivo:**
  - **En Pantalla de Inicio:** Vista previa de programa en vivo actual (*Ahora*) y siguiente (*Luego*) con horario en cada tarjeta.
  - **Barra OSD Inferior en el Reproductor:** Despliega canal, nombre del programa, horario, barra de progreso en tiempo real y próximo programa (accesible con flecha abajo, tecla INFO o al cambiar de canal).
- **Función Último Canal (Recall):**
  - Botón dedicado `↩ Último` en la barra superior.
  - Atajo rápido con tecla `0`, `R` o botón Verde del control remoto para alternar al instante entre dos señales.
- **Retomar Último Canal al Iniciar:** Al abrir la aplicación, sintoniza automáticamente la última señal vista emulando un televisor tradicional.
- **Teclado Numérico Multidígito Mejorado:**
  - Buffer de 1.5 segundos con confirmación visual en pantalla para sintonizar canales de 1 y 2 dígitos (ej: canal 14).
- **Navegación 100% Optimizada para Control Remoto:**
  - **Flechas Laterales (◄ / ►):** Acceso instantáneo a los botones superiores (Inicio, Recall, Cambiar Señal, Ajustar Relación de Aspecto, Favoritos).
  - **Flechas Arriba / Abajo (▲ / ▼):** Navegación fluida y natural entre canales de la guía lateral.
  - **Botón Atrás (Back):** Oculta la guía y los menús volviendo a pantalla completa.
- **Contadores Dinámicos:** Los filtros de categorías (Todos, Favoritos, Aire, Noticias, Deportes) se calculan automáticamente desde la lista de canales.

---

## 📋 Lista de Canales Incluidos (14 Señales Oficiales)

### 📡 Aire y Noticias
1. **TN (Todo Noticias)** — HLS Nativo 1080p Artear
2. **Telefe (Canal 11)** — Token API Oficial Mediastream 1080p
3. **C5N (Noticias 24hs)** — YouTube Live Oficial
4. **El Trece (Canal 13)** — HLS Nativo 1080p VodGC
5. **El Nueve (Canal 9)** — YouTube Live Oficial
6. **América TV (Canal 2)** — HLS HD Oficial Google DAI / YouTube Live
7. **A24 (América 24)** — YouTube Live Oficial / Fallback HLS
8. **TV Pública (Canal 7)** — YouTube Live Oficial
9. **LN+ (La Nación +)** — YouTube Live Oficial
10. **Crónica TV** — YouTube Live Oficial
11. **Canal Rural** — YouTube Live Oficial
13. **Canal 26** — YouTube Live Oficial 24/7

### ⚽ Deportes
12. **TyC Sports** — HLS 1080p Oficial (AR / Internacional)
14. **FIFA+** — HLS FAST Oficial (Español / Global)

---

## 🔄 Actualización Remota OTA (Over-The-Air)
La aplicación instalada en tu televisor consulta automáticamente la versión actualizada alojada en GitHub:
`https://andresehansen.github.io/tele-mama-android-tv-/`

Al presionar el botón **🔄 Actualizar** en el menú superior o abrir la aplicación, se descargan los canales más recientes desde `channels.json` sin necesidad de reinstalar.
