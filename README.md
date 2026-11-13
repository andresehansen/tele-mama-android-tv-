# 📺 Mi Tele TV — Aplicación para Android TV / Google TV (18 Canales)

Aplicación de televisión argentina en vivo diseñada específicamente para **Android TV, Google TV, Smart TVs y TV Boxes (Chromecast con Google TV, Xiaomi Mi Box, etc.)**, optimizada con navegación 100% mediante control remoto.

Utiliza la arquitectura robusta probada de **tele-mama**:
- **Streams HLS nativos directos (1080p):** TN y El Trece.
- **API Token Oficial + Cloudflare Worker Proxy:** Telefe.
- **Transmisiones Oficiales YouTube Live (con bypass de ads y auto-heal de ID):** C5N, El Nueve, América TV, A24, TV Pública, LN+, Crónica TV, Canal Rural.
- **Señales Deportivas Integradas (Web Embeds & HLS):** ESPN, ESPN 2, Fox Sports, DSports, DAZN F1, ESPN Premium, TNT Sports.

---

## 📡 Lista de Canales Incluidos (18 Señales)

### 📺 Aire y Noticias
1. **TN (Todo Noticias)** — HLS Nativo 1080p
2. **Telefe (Canal 11)** — Token API Oficial
3. **C5N (Noticias 24hs)** — YouTube Live Oficial
4. **El Trece (Canal 13)** — HLS Nativo 1080p
5. **El Nueve (Canal 9)** — YouTube Live Oficial
6. **América TV (Canal 2)** — YouTube Live Oficial / Fallback HLS
7. **A24 (América 24)** — YouTube Live Oficial / Fallback HLS
8. **TV Pública (Canal 7)** — YouTube Live Oficial
9. **LN+ (La Nación +)** — YouTube Live Oficial
10. **Crónica TV** — YouTube Live Oficial
11. **Canal Rural** — YouTube Live Oficial

### ⚽ Deportes y Automovilismo
12. **ESPN** — Web Player / HLS
13. **ESPN 2** — Web Player / HLS
14. **Fox Sports** — Web Player / HLS
15. **DSports (DirecTV Sports)** — Web Player / HLS
16. **DAZN F1 (Fórmula 1)** — Web Player / HLS
17. **ESPN Premium (Fútbol Argentino)** — Web Player / HLS
18. **TNT Sports (Fútbol Argentino)** — Web Player / HLS

---

## 🎯 Adaptaciones y Mejoras para Android TV

1. **Filtro de Categorías con Control:**
   - Botones superiores tipo píldora: `⭐ Todos (18)`, `📺 Aire y Noticias (11)`, `⚽ Deportes y F1 (7)`.
   - Permite filtrar rápidamente con las flechas del control para saltar directamente a los canales de interés.

2. **Navegación nativa con Control Remoto (D-Pad):**
   - Grilla en proporción 16:9 con foco visual de alto contraste (borde amarillo brillante, zoom 1.07x y resplandor).
   - Al iniciar la app, el canal 1 (**TN**) queda enfocado automáticamente; solo basta presionar **OK** en el control para sintonizar.
   - Desplazamiento vertical fluido automático al recorrer filas con la cruceta.
   - **Teclado numérico:** Admite presionar del **1 al 18** en el control remoto para sintonizar directamente.

3. **Zapping en Pantalla Completa:**
   - Mientras se reproduce un canal, presionar **Izquierda / Derecha** (o **Canal +/-**) cambia de canal inmediatamente y muestra un cartel flotante (*Toast de Zapping*).
   - Barra de zapping rápida inferior con carrusel horizontal suave que centra el canal activo.

4. **Botón Atrás (Back) Inteligente:**
   - En reproducción: presionar **Atrás** cierra el video y vuelve a la grilla enfocando la tarjeta del canal.
   - En la grilla principal: solicita confirmación antes de salir.

---

## 🌐 Paso 1: Probar en la Web (GitHub Pages)

Antes de generar el APK, podés probar el funcionamiento en el navegador del televisor o PC:
1. Subí todo el contenido de esta carpeta al repositorio:
   `https://github.com/andresehansen/tele-mama-android-tv-`
2. En GitHub, andá a **Settings > Pages**.
3. En **Build and deployment > Source**, seleccioná **Deploy from a branch**.
4. En **Branch**, elegí `main` (o `master`) y carpeta `/(root)`. Guardá cambios.
5. Accedé al sitio publicado (ej: `https://andresehansen.github.io/tele-mama-android-tv-/`).

---

## 🛠️ Paso 2: ¿Cómo compilar el APK para la TV?

### Opción 1: Compilación automática en la nube (GitHub Actions)
El repositorio ya incluye el workflow en `.github/workflows/build-apk.yml`.
1. Al subir los archivos al repositorio en GitHub, el workflow se iniciará automáticamente.
2. Podés ir a la pestaña **Actions** en tu repositorio de GitHub.
3. Una vez terminado el proceso (demora ~2-3 minutos), en la sección **Artifacts** descargás el archivo comprimido que contiene **`MiTeleTV-debug.apk`**.

### Opción 2: Abrir en Android Studio
1. Abrir **Android Studio**.
2. Seleccionar **Open** y elegir la carpeta `tele-mama-android-tv`.
3. Menú superior: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. El archivo generado estará en: `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📺 ¿Cómo instalar el APK en Android TV / Google TV?

1. **Vía "Send Files to TV" (Recomendado):**
   - Instalar la app gratuita *Send Files to TV* en tu celular y en el televisor (desde Google Play Store).
   - Instalar en el televisor un explorador de archivos (*X-plore File Manager* o *File Commander*).
   - Descargás el `.apk` en el celular y lo enviás al televisor por *Send Files to TV*.
   - Abrís el explorador en la TV, vas a la carpeta `Download` y ejecutás el `.apk` para instalar.

2. **Vía Pendrive USB:**
   - Copiás el archivo `.apk` a un pendrive USB.
   - Conectás el pendrive al puerto USB de la TV o TV Box.
   - Abrís un explorador de archivos en la TV, seleccionás el `.apk` e instalás.
