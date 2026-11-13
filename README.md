# 📺 Mi Tele TV — Aplicación para Android TV / Google TV (18 Canales)

Aplicación de televisión en vivo por IPTV diseñada específicamente para **Android TV, Google TV, Smart TVs y TV Boxes (Chromecast, Xiaomi Mi Box, etc.)**, optimizada con navegación 100% mediante control remoto.

---

## 📡 Lista de Canales Incluidos (18 Señales)

### 📺 Aire y Noticias
1. **TN (Todo Noticias)**
2. **Telefe (Canal 11)**
3. **C5N (Noticias 24hs)**
4. **El Trece (Canal 13)**
5. **El Nueve (Canal 9)**
6. **América TV (Canal 2)**
7. **A24 (América 24)**
8. **TV Pública (Canal 7)**
9. **LN+ (La Nación +)**
10. **Crónica TV**
11. **Canal Rural (El campo en vivo)**

### ⚽ Deportes y Automovilismo
12. **ESPN**
13. **ESPN 2**
14. **Fox Sports**
15. **DSports (DirecTV Sports)**
16. **DAZN F1 (Fórmula 1)**
17. **ESPN Premium (Fútbol Argentino)**
18. **TNT Sports (Fútbol Argentino)**

---

## 🎯 Adaptaciones y Mejoras para Android TV

1. **Filtro de Categorías con Control:**
   - Botones tipo píldora arriba: `⭐ Todos (18)`, `📺 Aire y Noticias (11)`, `⚽ Deportes y F1 (7)`.
   - Permite filtrar rápidamente con las flechas del control para no tener que recorrer toda la grilla si solo se busca fútbol o noticias.

2. **Navegación nativa con Control Remoto (D-Pad):**
   - Grilla en proporción 16:9 con foco visual de alto contraste (borde amarillo brillante, zoom 1.07x y resplandor).
   - Al iniciar la app, el canal 1 (**TN**) queda enfocado automáticamente; solo basta presionar **OK** en el control para empezar a ver televisión.
   - Desplazamiento vertical fluido automático al bajar o subir de fila con la cruceta.
   - **Teclado numérico:** Admite presionar del **1 al 18** en el control remoto para sintonizar directamente.

3. **Zapping en Pantalla Completa:**
   - Mientras se reproduce un canal, presionar **Izquierda / Derecha** (o **Canal +/-**) cambia de canal inmediatamente y muestra un cartel flotante (*Toast de Zapping*).
   - Barra de zapping rápida inferior con carrusel horizontal suave que centra el canal activo.

4. **Botón Atrás (Back) Inteligente:**
   - En reproducción: presionar **Atrás** cierra el video y vuelve a la grilla enfocando la tarjeta del canal.
   - En la grilla principal: solicita confirmación ("*Presione Atrás de nuevo para salir*").

5. **Configuración de listas M3U adicionales:**
   - Botón `⚙️ Lista M3U` para ingresar URLs de listas privadas (ej. listas de proveedores con canales premium adicionales).

---

## 🛠️ ¿Cómo compilar el APK?

### Opción 1: Abrir en Android Studio (Recomendada)
1. Abrir **Android Studio**.
2. Seleccionar **Open** y elegir la carpeta `tele-mama-android-tv`.
3. Menú superior: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. El archivo generado estará en: `app/build/outputs/apk/debug/app-debug.apk`.

### Opción 2: Compilación automática mediante GitHub Actions
Subir esta carpeta a un repositorio en GitHub:
1. El archivo `.github/workflows/build-apk.yml` compilará el APK en la nube de GitHub.
2. Descargás el archivo **MiTeleTV-debug-apk** desde la pestaña **Actions**.

---

## 📺 ¿Cómo instalar el APK en la Tele?

1. **Vía "Send Files to TV" (Recomendado):**
   - Instalar *Send Files to TV* en celular y en la TV.
   - Enviar el `.apk` a la TV e instalarlo con *X-plore* o *File Commander*.
2. **Vía Pendrive USB:**
   - Copiar el `.apk` al pendrive y abrirlo en la TV.
