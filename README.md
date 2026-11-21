# 📺 AndyTV — Reproductor personal para Android TV / Google TV

Reproductor de uso **personal y privado** para Android TV, Google TV, Smart TVs y TV Boxes, con navegación completa por control remoto.

> [!IMPORTANT]
> **Aviso legal.** AndyTV es un proyecto personal, sin fines de lucro, para uso privado del autor y un grupo reducido de allegados. **No está afiliado, patrocinado ni avalado** por ninguno de los canales o emisoras que aparecen en la grilla. Todas las marcas, nombres y logos pertenecen a sus respectivos titulares y se usan únicamente para identificar cada señal.
>
> AndyTV **no aloja, almacena ni vende contenido audiovisual**: solo organiza y reproduce transmisiones que los propios canales publican en internet. No se distribuye en tiendas de aplicaciones ni se comercializa.
>
> **Pedidos de baja:** si sos titular de derechos y querés que se quite una señal, abrí un *issue* en este repositorio y se retira a la brevedad.

## 🚀 Características
- **Grilla modular (`channels.json`):** señales, números y categorías desacoplados del APK; se actualizan sin recompilar.
- **Doble motor de reproducción:**
  - **AndroidX Media3 (ExoPlayer)** nativo con decodificación por hardware para HLS.
  - **Reproductor web** (Hls.js) y **embeds de YouTube Live**.
- **Múltiples fuentes por canal** con cambio manual (tecla `S` / amarillo) y respaldo automático si una falla.
- **Guía de programación (EPG)** aproximada: *Ahora* / *Luego* en cada tarjeta y barra OSD en el reproductor.
- **Zapping con CH+ / CH-** del control remoto, recall al último canal (`0` / `R` / verde), teclado numérico multidígito y favoritos.
- **Lista M3U propia:** permite cargar una lista personal del usuario (como cualquier reproductor genérico tipo VLC).

## 📋 Grilla
| # | Canal | Categoría |
|---|---|---|
| 1 | TN | Noticias |
| 2 | Telefe | Aire |
| 3 | C5N | Noticias |
| 4 | El Trece | Aire |
| 5 | El Nueve | Aire |
| 6 | América TV | Aire |
| 7 | A24 | Noticias |
| 8 | TV Pública | Aire |
| 9 | LN+ | Noticias |
| 10 | Crónica | Noticias |
| 11 | Canal Rural | Aire |
| 12 | TyC Sports | Deportes |
| 13 | Canal 26 | Noticias |
| 14 | FIFA+ | Deportes |
| 15 | Quiero Música TV | Entretenimiento |

La disponibilidad de cada señal depende de su emisora y puede cambiar sin aviso.

## 🔄 Actualización remota
La app instalada carga la versión publicada en `https://andresehansen.github.io/tele-mama-android-tv-/` y, si no hay conexión, usa la copia empaquetada en el APK. El botón **🔄 Actualizar** fuerza la recarga de `channels.json`.

## 🧩 Fuentes comunitarias
La sincronización automática con listas comunitarias (iptv-org) está **deshabilitada**. Las fuentes se curan manualmente en `channels.json`.

## 📄 Licencias
- Código propio: [MIT](LICENSE).
- [hls.js](https://github.com/video-dev/hls.js) — Apache License 2.0.
- [AndroidX Media3](https://github.com/androidx/media) — Apache License 2.0.
