# Zapatico Cochinito - Instrucciones de Desarrollo

## Descripción
Zapatico Cochinito es un minijuego rítmico construido íntegramente con **Jetpack Compose** y **ViewModels**. El proyecto moderniza la dinámica tradicional de “Zapatico, cochinito, cambia de piecito” manteniendo un núcleo ligero y fácilmente extensible. Esta guía describe la arquitectura vigente, las herramientas necesarias y los puntos de extensión más comunes.

## Características del Juego

### 🎮 Mecánicas principales
- **Golpes precisos**: cada acierto entrega **10 puntos** y mantiene la racha activa.
- **Metrónomo dinámico**: el BPM base depende de la dificultad (70/90/120). Cada 6 aciertos consecutivos incrementan el tempo en 4 BPM hasta un máximo de 200 BPM.
- **Modo ¡CAMBIA!**: invierte temporalmente los lados correctos. Su frecuencia y duración se adaptan según el “modo de caos” configurado para los jugadores Pro.
- **Vidas limitadas**: dispones de 3 oportunidades. Al agotarse se muestra la pantalla de Game Over con la puntuación final.
- **Marcador global**: la app sincroniza el mejor puntaje con Google Play Juegos y permite abrir el leaderboard desde Inicio o Game Over.

### ⚙️ Ajustes personalizables
- **Dificultad**: `Niño`, `Normal` y `Pro` ajustan el BPM base.
- **Modo CAMBIA avanzado** (solo en `Pro`): controles para suavizar o intensificar la frecuencia/duración de CAMBIA.
- **Volumen y metrónomo**: slider de volumen y switch para activar el pulso sonoro.
- **Tema e idioma**: selector claro/oscuro y alternancia entre español LatAm e inglés (vía AppCompatDelegate + AppCompatLocaleManager).

## Requisitos de Desarrollo

### Software
- **Android Studio Flamingo (o superior)** con soporte para Compose.
- **JDK 17** (incluido en el proyecto mediante Gradle Wrapper).
- **SDK Android** API 24 mínimo, objetivo 34.

### Dispositivo
- Android 7.0+ con pantalla táctil.
- Recomendable habilitar 120 Hz para probar la aceleración del tempo.

## Cómo Compilar y Ejecutar

### Android Studio
1. Clona el repositorio y ábrelo en Android Studio (`File > Open`).
2. Espera la sincronización de Gradle.
3. Ejecuta el módulo `app` sobre un emulador o dispositivo físico.

### Línea de comandos
```bash
./gradlew assembleDebug   # Genera APK debug
./gradlew installDebug    # Instala en dispositivo conectado
./gradlew bundleRelease   # Genera AAB + mapping y símbolos nativos
```

## Arquitectura Técnica

```
MainActivity
 ├─ PlayGamesService (sign-in + leaderboard)
 ├─ AppViewModel (DataStore + ajustes)
 │   └─ AppPreferencesRepository (Preferences DataStore)
 └─ GameViewModel (lógica de ritmo y puntuación)

UI (Compose)
 ├─ home/        → HomeScreen (botón play + leaderboard)
 ├─ game/        → GameScreen & GameOverScreen
 ├─ notifications/→ Feed estático de novedades
 └─ settings/    → Ajustes generales + modo CAMBIA avanzado
```

- **Estado reactivo**: `GameViewModel` y `AppViewModel` exponen `StateFlow`. `collectAsStateWithLifecycle` asegura que la UI se actualice solo en estados activos.
- **Persistencia**: las preferencias (dificultad, metronomo, idioma, tema, modo CAMBIA) se guardan en `AppPreferencesRepository` usando DataStore.
- **Integración Play Games**: `PlayGamesService` inicializa el SDK, intenta el sign-in automático y expone `submitBestScore` y `showLeaderboard`.
- **Internacionalización**: `AppLanguage` encapsula los recursos `values-b+es+419` y `values-en-rUS` y actualiza `AppLocales` dinámicamente.

## Estructura del Proyecto

```
app/src/main/java/com/digitalesweb/zapaticocochinito/
├── MainActivity.kt                 # Host Compose + navegación
├── games/PlayGamesService.kt       # Integración Google Play Juegos
├── data/AppPreferencesRepository.kt# DataStore Preferences
├── model/AppModels.kt              # AppSettings, enums y UI state
├── ui/
│   ├── home/HomeScreen.kt          # Portada animada con vista previa sonora
│   ├── game/GameScreen.kt          # Juego y GameOver
│   ├── notifications/…             # Feed de novedades y compartir app
│   └── settings/SettingsScreen.kt  # Ajustes, incluyendo modo CAMBIA avanzado
└── viewmodel/
    ├── AppViewModel.kt             # Sincroniza ajustes
    └── GameViewModel.kt            # Lógica de puntuación, CAMBIA y BPM
```

Los recursos se localizan en `res/values/`, `values-b+es+419/` y `values-en-rUS/`, mientras que la configuración de Play Juegos vive en `res/values/strings.xml`.

## Flujo del Juego

1. **Inicio**: `HomeScreen` muestra la racha máxima guardada y ejecuta un micro-preview de tono (6 pulsos) para no saturar al usuario.
2. **Juego**: `GameScreen` arranca cuando se presiona “Jugar” y escucha los beats emitidos por `GameViewModel.onBeat()`.
3. **CAMBIA**:
   - Probabilidad base 22 %, ajustada por el modo seleccionado (`Relaxed`, `Standard`, `Frenzy`).
   - Duración base 6 beats, escalada con el multiplicador y acotada entre 3 y 10 beats.
4. **Final**: al perder todas las vidas se dispara `GameOverScreen`, se actualiza DataStore y se sincroniza el marcador global.
5. **Leaderboard**: disponible desde Inicio y Game Over mediante `PlayGamesService.showLeaderboard()`.

## Notas de Integración
- Para apuntar a otro proyecto de Google Play Juegos modifica `games_app_id` y `leaderboard_high_score_id` en `strings.xml` (y sus variantes regionales).
- Los paquetes del leaderboard deben configurarse en Play Console para que `showLeaderboard` abra la UI nativa.
- `POINTS_PER_HIT` está centralizado en `GameViewModel` para mantener la coherencia con la documentación.

## Registro y Depuración
- Usa Logcat con filtro `GameViewModel`, `AppViewModel` y `PlayGamesService`.
- Para revisar DataStore en local, ejecuta `adb shell run-as com.digitalesweb.zapaticocochinito ls files/datastore/`.

## Pruebas Recomendadas
1. **Escenario base**: iniciar partida, acertar 6 golpes y confirmar el incremento de BPM.
2. **CAMBIA Relaxed**: en Ajustes → Pro seleccionar `Suave` y comprobar menor frecuencia.
3. **CAMBIA Frenzy**: seleccionar `Insano`, verificar más inversiones y duración ampliada.
4. **Marcador**: tras superar un récord, abrir el leaderboard desde Inicio.
5. **Persistencia**: reiniciar la app y confirmar que dificultad, idioma, tema y modo CAMBIA se recuerdan.
6. **Localización**: alternar idioma y validar los nuevos textos del feed de novedades.

## Mantenimiento
- Ejecuta `./gradlew lint` antes de subir cambios a producción.
- Añade nuevos textos en los tres archivos de `values` para mantener la paridad lingüística.
- Cuando ajustes la lógica de puntuación, sincroniza la documentación (`README.md`, `DESARROLLO.md`) para evitar discrepancias.

¡Feliz desarrollo! Si encuentras oportunidades de mejora, abre un issue o PR documentando el impacto en la jugabilidad.
