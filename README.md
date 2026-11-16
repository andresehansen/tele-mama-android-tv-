# 📺 Mi Tele TV — Aplicación para Android TV / Google TV (12 Canales)

Aplicación de televisión argentina en vivo diseñada específicamente para **Android TV, Google TV, Smart TVs y TV Boxes (Chromecast con Google TV, Xiaomi Mi Box, etc.)**, optimizada con navegación 100% mediante control remoto.

Utiliza la arquitectura robusta probada de **tele-mama**:
- **Streams HLS nativos directos (1080p):** TN y El Trece.
- **API Token Oficial + Cloudflare Worker Proxy:** Telefe.
- **Transmisiones Oficiales YouTube Live (con bypass de ads y auto-heal de ID):** C5N, El Nueve, América TV, A24, TV Pública, LN+, Crónica TV, Canal Rural.
- **Señal Deportiva Directa HD:** TyC Sports 1080p.

---

## 📡 Lista de Canales Incluidos (12 Señales)

### 📺 Aire y Noticias
1. **TN (Todo Noticias)** — HLS Nativo 1080p
2. **Telefe (Canal 11)** — Token API Oficial Mediastream 1080p
3. **C5N (Noticias 24hs)** — YouTube Live Oficial (c5n.com/vivo)
4. **El Trece (Canal 13)** — HLS Nativo 1080p VodGC
5. **El Nueve (Canal 9)** — YouTube Live Oficial
6. **América TV (Canal 2)** — YouTube Live Oficial / Fallback HLS
7. **A24 (América 24)** — YouTube Live Oficial / Fallback HLS
8. **TV Pública (Canal 7)** — YouTube Live Oficial
9. **LN+ (La Nación +)** — YouTube Live Oficial
10. **Crónica TV** — YouTube Live Oficial
11. **Canal Rural** — YouTube Live Oficial

### ⚽ Deportes
12. **TyC Sports** — HLS 1080p Oficial (AR / Internacional)

---

## 🎯 Adaptaciones y Mejoras para Android TV

1. **Reproductor Nativo AndroidX Media3 (ExoPlayer) + Headers HTTP:**
   - Decodificación por hardware de baja latencia para HLS/DASH y audio AC3/EAC3/AAC.
   - Soporte nativo para inyección de cabeceras HTTP (`User-Agent`, `Referer`, `Origin`) extraídas de listas M3U (`#EXTVLCOPT`).

2. **Menú Lateral Vertical de Canales (Estilo DGO / DirecTV GO):**
   - Guía vertical a la izquierda accesible en todo momento para cambiar de canal con un solo clic.

3. **Escudo Nativo Anti-Publicidad y Anti-Popups:**
   - Bloqueo en `WebViewClient` y `WebChromeClient` de dominios de anuncios y ventanas emergentes para acelerar la carga y proteger la navegación con control remoto.

4. **Favoritos (`⭐`) y Memoria de Último Canal Visto (`▶ Último`):**
   - Mantené presionado **OK** (o presioná la tecla **F** / **Botón Rojo**) para fijar canales en **Favoritos**.
   - Al abrir la app, el foco se ubica automáticamente en el último canal que estuviste mirando.

5. **Control de Relación de Aspecto (`📐`):**
   - Alterná entre **Ajustar**, **Rellenar 16:9** y **Zoom** desde el reproductor (o tecla **Z** / **Azul**).

6. **Zapping en Pantalla Completa y Botón Inicio Inteligente:**
   - Mientras se reproduce un canal, presionar **Izquierda / Derecha** (o **Canal +/-**) cambia de canal inmediatamente y muestra el *Toast de Zapping*.
   - Botón **INICIO** para volver limpiamente a la cuadrícula principal de canales.

---

## 🌐 Paso 1: Actualización Instantánea para la TV (GitHub Pages)

La aplicación instalada en tu televisor carga de manera remota:
`https://andresehansen.github.io/tele-mama-android-tv-/`

Al hacer commit y push a la rama `main`:
1. GitHub Pages despliega la última versión automáticamente.
2. En tu Android TV, al abrir la app o presionar el botón **🔄 Actualizar**, la app se actualiza sola sin necesidad de reinstalar el APK.

---

## 🛠️ Paso 2: Compilar nuevo APK (si deseás reinstalar)

### Opción 1: Compilación automática en la nube (GitHub Actions)
El repositorio incluye el workflow en `.github/workflows/build-apk.yml`.
1. Cada push a `main` activa la compilación en GitHub Actions.
2. En la pestaña **Actions** de GitHub, descargás el artefacto **`MiTeleTV-debug.apk`**.

### Opción 2: Instalar en Android TV
- Enviá el `.apk` con la app **Send Files to TV** o mediante un pendrive USB.
