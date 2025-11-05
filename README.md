# ZapaticoCochinito
Zapatico Cochinito es un minijuego rítmico divertido y nostálgico, inspirado en la ronda tradicional "Zapatico, cochinito, cambia de piecito". Pon a prueba tu coordinación, reflejos y sentido del ritmo tocando el pie correcto al compás del juego. ¡Pero cuidado! Cuando aparezca ¡CAMBIA!, los lados se invierten y el desafío se intensifica.

## 🎮 Cómo Jugar

1. **Toca para comenzar**: Presiona cualquier botón de pie para iniciar el juego
2. **Lee la indicación**: El juego te mostrará qué pie tocar (IZQUIERDO o DERECHO)
3. **Toca el botón correcto**: Tienes tiempo limitado para tocar el pie indicado
4. **¡Cuidado con CAMBIA!**: Cuando aparece esta palabra, los controles se invierten temporalmente
5. **Mantén tu racha**: Cada acierto suma 10 puntos y aumenta tu racha
6. **No pierdas tus vidas**: Empiezas con 3 vidas, cada error o tiempo agotado te resta una vida
7. **El ritmo se acelera**: Mientras mejor juegues, más rápido se vuelve el juego
8. **Comparte tu récord**: Abre el marcador global de Google Play Juegos desde la pantalla de inicio o al terminar una partida

## 📱 Instalación

### Para Usuarios
1. Descarga el APK desde la sección [Releases](https://github.com/DigitalesWeb/ZapaticoCochinito/releases)
2. Instala en tu dispositivo Android (requiere Android 7.0 o superior)
3. ¡Disfruta jugando!

### Para Desarrolladores
Consulta [DESARROLLO.md](DESARROLLO.md) para instrucciones detalladas de compilación y desarrollo.

## ✨ Características

- 🎯 Juego de ritmo simple e intuitivo
- 🔄 Mecánica especial "¡CAMBIA!" que invierte los controles
- 📊 Sistema de puntuación con rachas (10 puntos por golpe correcto)
- ❤️ Sistema de vidas (3 vidas)
- ⚡ Dificultad progresiva
- 🎨 Interfaz colorida y atractiva
- 🏆 Integración con Google Play Juegos y acceso directo al marcador global
- 🌀 Controles avanzados de CAMBIA para personalizar el desafío en modo Pro
- 📱 Optimizado para dispositivos móviles en orientación vertical

## ⚙️ Configuración de Google Play Juegos

El repositorio ya está configurado con los identificadores oficiales de Zapatico Cochinito:

- `games_app_id = 630863606670`
- `leaderboard_high_score_id = Cgkljueak64SEAIQAQ`

Si necesitas apuntar a otro proyecto o tablero de puntuaciones, actualiza los valores anteriores en `app/src/main/res/values/strings.xml` siguiendo estos pasos:

1. Ingresa en [Google Play Console](https://play.google.com/console) y selecciona tu proyecto.
2. Abre **Servicios de juegos de Google Play → Configuración y gestión → Configuración**. En la tarjeta **Información general** copia el valor **ID del juego** (numérico) y úsalo como `games_app_id`.
3. Dentro de la misma sección entra en **Marcadores (Leaderboards)** y selecciona tu tabla de puntuaciones. El campo **ID del recurso** (por ejemplo `Cgkljueak64SEAIQAQ`) es el que debes colocar en `leaderboard_high_score_id`.
4. Sincroniza el proyecto y verifica desde un dispositivo con Play Games instalado que el inicio de sesión y el envío de puntuaciones funcionen correctamente.

## 🧾 Archivos para la consola de Google Play

Al compilar la versión de distribución se generan automáticamente los artefactos que resuelven las advertencias de **desofuscación** y **símbolos nativos** en la consola:

- `app/build/outputs/mapping/release/mapping.txt`: súbelo en la sección **Depuración > Archivos de desofuscación** del App Bundle correspondiente.
- `app/build/outputs/native-debug-symbols/release/native-debug-symbols.zip`: súbelo en **Depuración > Símbolos nativos** para facilitar el análisis de ANR y fallos en dispositivos con código nativo.

Para obtener ambos archivos ejecuta:

```bash
./gradlew bundleRelease
```

Después de subir un nuevo App Bundle, adjunta ambos archivos en la Play Console para que los reportes de fallos y ANR muestren stack traces legibles.

## 🎯 Objetivo

Mantén la racha más larga posible de aciertos al ritmo, acumulando la mayor puntuación sin perder las 3 vidas.

## 🛠️ Tecnologías

- **Plataforma**: Android (API 24+)
- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose
- **Arquitectura**: ViewModel + StateFlow + Navigation Compose
- **Mínimo**: Android 7.0 (Nougat)
- **Target**: Android 14

## 📄 Licencia

Este proyecto está bajo licencia MIT.

## 🤝 Contribuciones

Las contribuciones son bienvenidas. Por favor, abre un issue o pull request para sugerencias y mejoras.

---

Hecho con ❤️ para preservar y modernizar las tradiciones latinoamericanas
