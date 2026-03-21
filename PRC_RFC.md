# Zapatico Cochinito - PRC & RFC

> **Versión:** 1.0  
> **Estado:** Estable  
> **Fecha:** Marzo 2026  
> **Autor:** DigitalesWeb

---

## Resumen Ejecutivo

Zapatico Cochinito es un minijuego rítmico que moderniza la ronda tradicional latinoamericana "Zapatico, cochinito, cambia de piecito". El juego evalúa la coordinación y reflejos del jugador mediante secuencias de ritmo que se aceleran progresivamente, con una mecánica especial de inversión de controles.

---

## 1. Producto

### 1.1 Descripción

| Campo | Valor |
|-------|-------|
| **Nombre** | Zapatico Cochinito |
| **Plataforma** | Android (API 24+) |
| **Lanzamiento** | 2024 |
| **Licencia** | MIT |

### 1.2 Propuesta de Valor

Juego de ritmo simple, accesible y nostálgico que combina:
- Curva de aprendizaje natural (reflejos)
- Dificultad progresiva (BPM dinámico)
- Mecánica única de inversión de controles ("¡CAMBIA!")
- Competencia social mediante leaderboard

### 1.3 Usuarios Objetivo

- Jugadores casuales de 5-65 años
- Fans de juegos de ritmo simples (ej. Piano Tiles, Flappy Bird)
- Usuarios nostálgicos de rondas infantiles latinoamericanas

---

## 2. Requisitos del Producto

### 2.1 Requisitos Funcionales

| ID | Requisito | Prioridad | Estado |
|----|-----------|-----------|--------|
| RF-01 | Sistema de prompts aleatorios IZQ/DER | Crítica | Implementado |
| RF-02 | Detección de input táctil por pie | Crítica | Implementado |
| RF-03 | Sistema de puntuación (10 pts/acierto) | Crítica | Implementado |
| RF-04 | Sistema de 3 vidas | Crítica | Implementado |
| RF-05 | Mecánica "¡CAMBIA!" con inversión | Alta | Implementado |
| RF-06 | Aceleración progresiva de BPM | Alta | Implementado |
| RF-07 | Persistencia de high score local | Alta | Implementado |
| RF-08 | Leaderboard global (Google Play Games) | Media | Implementado |
| RF-09 | 3 niveles de dificultad | Media | Implementado |
| RF-10 | Sonido metrónomo configurable | Baja | Implementado |

### 2.2 Requisitos No Funcionales

| ID | Requisito | Meta |
|----|-----------|------|
| RNF-01 | Tiempo de respuesta táctil | < 50ms |
| RNF-02 | Latencia de audio | < 16ms |
| RNF-03 | Tamaño APK | < 15MB |
| RNF-04 | Arranque en frío | < 2s |
| RNF-05 | Compatibilidad | Android 7.0+ (API 24) |

---

## 3. Especificación Técnica

### 3.1 Stack Tecnológico

| Componente | Tecnología | Versión |
|------------|------------|---------|
| Lenguaje | Kotlin | 1.9.x |
| UI Framework | Jetpack Compose | BOM 2024.02.00 |
| Arquitectura | MVVM + StateFlow | - |
| Navegación | Navigation Compose | 2.7.7 |
| Persistencia | DataStore Preferences | 1.0.0 |
| Audio | ToneGenerator | Android SDK |
| Gaming | Google Play Games SDK | - |

### 3.2 Estructura de Directorios

```
app/src/main/java/com/digitalesweb/zapaticocochinito/
├── MainActivity.kt                    # Entry point + navegación
├── model/AppModels.kt                 # Enums, data classes, estados
├── viewmodel/
│   ├── AppViewModel.kt                # Settings global
│   └── GameViewModel.kt               # Lógica de juego
├── ui/
│   ├── home/HomeScreen.kt             # Pantalla inicio
│   ├── game/
│   │   ├── GameScreen.kt              # Juego activo
│   │   ├── GameOverScreen.kt          # Fin de partida
│   │   └── ReviewPromptDialog.kt      # Dialog valoración
│   ├── settings/SettingsScreen.kt      # Configuración
│   ├── notifications/NotificationsScreen.kt
│   ├── navigation/ZapaticoDestinations.kt
│   └── theme/{Theme,Color,Type}.kt
├── data/AppPreferencesRepository.kt    # DataStore wrapper
├── games/PlayGamesService.kt          # Google Play Games
├── di/ServiceLocator.kt               # DI simple
└── util/LocaleUtils.kt                # Localización
```

### 3.3 Modelos de Datos

```kotlin
// Configuración de usuario
data class AppSettings(
    val difficulty: Difficulty = Difficulty.Normal,
    val volume: Float = 0.7f,
    val metronomeEnabled: Boolean = true,
    val theme: AppTheme = AppTheme.Light,
    val language: AppLanguage = AppLanguage.SpanishLatam,
    val cambiaChaosLevel: CambiaChaosLevel = CambiaChaosLevel.Standard
)

// Estado del juego
data class GameUiState(
    val currentPrompt: GamePrompt = GamePrompt.Left,
    val expectedFoot: Foot = Foot.Left,
    val showCambia: Boolean = false,
    val invertActive: Boolean = false,
    val score: Int = 0,
    val lives: Int = 3,
    val isRunning: Boolean = false,
    val isGameOver: Boolean = false,
    val beat: Long = 0L,
    val currentBpm: Int = 90
)
```

### 3.4 Enums Principales

```kotlin
enum class Difficulty(val bpm: Int) {
    Kid(70),      // Para principiantes
    Normal(90),   // Estándar
    Pro(120)      // Experto
}

enum class Foot { Left, Right }

enum class CambiaChaosLevel(val probMult: Float, val durMult: Float) {
    Relaxed(0.6f, 0.8f),
    Standard(1.0f, 1.0f),
    Frenzy(1.45f, 1.25f)
}
```

### 3.5 Constantes del Juego

| Constante | Valor | Descripción |
|-----------|-------|-------------|
| `POINTS_PER_HIT` | 10 | Puntos por acierto |
| `MAX_LIVES` | 3 | Vidas iniciales |
| `MAX_BPM` | 200 | BPM máximo |
| `BPM_INCREMENT` | 4 | Aumento por step |
| `HITS_PER_BPM_STEP` | 6 | Aciertos para aumentar BPM |
| `CAMBIA_TRIGGER_PROBABILITY` | 0.22 | Probabilidad base de CAMBIA |
| `CAMBIA_DURATION_BEATS` | 6 | Duración base de CAMBIA |
| `CAMBIA_ANNOUNCE_BEATS` | 2 | Beats de anuncio antes de CAMBIA |

---

## 4. Mecánicas de Juego

### 4.1 Flujo Principal

```
[Inicio] → [HomeScreen]
                ↓
          [Presionar JUGAR]
                ↓
          [GameScreen: countdown 3-2-1]
                ↓
          [Bucle de juego]
          ┌─────────────────────────────┐
          │ 1. Emitir beat (BPM)        │
          │ 2. Generar prompt aleatorio │
          │ 3. Esperar input del jugador│
          │ 4. Validar (acierto/error)  │
          │ 5. ¿Vidas = 0? → Game Over  │
          │ 6. ¿CAMBIA activo?          │
          │ 7. ¿Trigger CAMBIA?         │
          │ 8. Actualizar BPM           │
          │ 9. Volver a 1               │
          └─────────────────────────────┘
                ↓
          [GameOverScreen]
                ↓
          [Guardar high score]
          [Enviar a Play Games]
```

### 4.2 Sistema de Puntuación

```
Score = Aciertos × 10

High Score = max(Score actual, High Score guardado)

Envío a Play Games: Solo si Score > High Score anterior
```

### 4.3 Mecánica "¡CAMBIA!"

```
Fase 1: ANUNCIO (2 beats)
├── Mostrar "¡CAMBIA!" en pantalla
├── Pulso visual de alerta
└── Audio de advertencia

Fase 2: ACTIVO (3-10 beats según nivel)
├── Controles invertidos (IZQ ↔ DER)
├── Indicador visual activo
└── Dificultad incrementada

Fase 3: FIN
├── Restaurar controles normales
└── Resetear indicador
```

### 4.4 Escalado de Dificultad

```
currentBPM = min(baseBpm + (hits / 6) × 4, 200)

Ejemplo con dificultad Normal (base = 90):
- 0-5 hits:   90 BPM
- 6-11 hits:  94 BPM
- 12-17 hits: 98 BPM
- ...
- 180+ hits:  200 BPM (máximo)
```

---

## 5. Pantallas y Navegación

### 5.1 Mapa de Navegación

```
BottomNavigationBar
├── Inicio (HomeScreen)
├── Novedades (NotificationsScreen)
└── Juego (GameScreen)

Desde HomeScreen:
├── [JUGAR] → GameScreen
├── [CONFIGURACIÓN] → SettingsScreen
└── [LEADERBOARD] → Google Play Games

Desde GameScreen:
└── [SALIR] → HomeScreen

Desde GameOverScreen:
├── [JUGAR DE NUEVO] → GameScreen
├── [LEADERBOARD] → Google Play Games
└── [INICIO] → HomeScreen
```

### 5.2 Pantallas

| Pantalla | Archivo | Líneas | Responsabilidad |
|----------|---------|--------|-----------------|
| Home | HomeScreen.kt | 288 | Presentación, preview sonoro, acceso rápido |
| Game | GameScreen.kt | 563 | Juego activo, input, feedback visual/háptico |
| Game Over | GameOverScreen.kt | 193 | Resultados, opciones de continuación |
| Settings | SettingsScreen.kt | 686 | Configuración completa |
| Notifications | NotificationsScreen.kt | 195 | Novedades, compartir app |
| Review Dialog | ReviewPromptDialog.kt | 70 | Solicitud de valoración |

---

## 6. Persistencia de Datos

### 6.1 DataStore Preferences

| Clave | Tipo | Default | Sincronizado |
|-------|------|---------|--------------|
| `difficulty` | String | "Normal" | Sí |
| `volume` | Float | 0.7 | Sí |
| `metronome` | Boolean | true | Sí |
| `theme` | String | "Light" | Sí |
| `language` | String | "es-419" | Sí |
| `high_score` | Int | 0 | Sí |
| `cambia_chaos` | String | "Standard" | Sí |
| `rating_disabled` | Boolean | false | Sí |
| `rating_remind_after` | Long | 0L | Sí |

### 6.2 Criterios de Sincronización

- High score se sincroniza con Play Games tras cada partida
- Preferencias se guardan en cada cambio (debounced)
- Rating prompt: 10 días de recordatorio si se rechaza

---

## 7. Integraciones Externas

### 7.1 Google Play Games

| Configuración | Valor |
|---------------|-------|
| App ID | `630863606670` |
| Leaderboard ID | `Cgkljueak64SEAIQAQ` |

**Funcionalidades:**
- Sign-in automático al iniciar
- Submit high score tras partida
- Mostrar leaderboard global

### 7.2 Audio

- **ToneGenerator** con `TONE_PROP_BEEP`
- Stream: `STREAM_MUSIC`
- Volumen configurable (0.0 - 1.0)

### 7.3 Haptics

- `HapticFeedbackType.LongPress` al presionar botones de pie

---

## 8. Localización

### 8.1 Idiomas Soportados

| Código | Variante | Estado |
|--------|----------|--------|
| `es-419` | Español Latinoamericano | Completo |
| `en-US` | Inglés Estadounidense | Completo |

### 8.2 Recursos Localizados

- `res/values/strings.xml` (valores por defecto)
- `res/values-b+es+419/strings.xml`
- `res/values-en-rUS/strings.xml`

---

## 9. Métricas de Éxito

### 9.1 KPIs del Producto

| Métrica | Meta | Actual |
|---------|------|--------|
| Retención D1 | > 40% | - |
| Retención D7 | > 20% | - |
| Sesiones por usuario/semana | > 3 | - |
| Tiempo medio por sesión | 2-5 min | - |
| Tasa de completación | > 50% | - |

### 9.2 Métricas Técnicas

| Métrica | Umbral Aceptable |
|---------|-----------------|
| ANR rate | < 0.1% |
| Crash rate | < 0.5% |
| Frame rate | 60 FPS |
| Memory usage | < 100MB |

---

## 10. Roadmap

### 10.1 v1.x - Estable

- [x] Mecánicas core de juego
- [x] Integración Play Games
- [x] Temas claro/oscuro
- [x] Localización ES/EN
- [x] Sistema de valoración

### 10.2 v2.0 - Posibles Mejoras

- [ ] Modo cooperativo local
- [ ] Logros/Trofeos
- [ ] Temas visuales desbloqueables
- [ ] Sonidos personalizados
- [ ] Modo Zen (sin CAMBIA)

### 10.3 v2.x - Expansión

- [ ] Estadísticas detalladas
- [ ] Modo diario/desafío
- [ ] Tutorial interactivo
- [ ] Animaciones de personajes

---

## 11. Apendices

### 11.1 Glosario

| Término | Definición |
|---------|------------|
| BPM | Beats Per Minute - velocidad del juego |
| CAMBIA | Mecánica que invierte los controles temporalmente |
| High Score | Mejor puntuación del jugador |
| Leaderboard | Tabla de puntuaciones global |
| Prompt | Indicación visual de qué pie tocar |

### 11.2 Referencias

- [README.md](README.md) - Documentación de usuario
- [DESARROLLO.md](DESARROLLO.md) - Guía de desarrollo
- [Play Console](https://play.google.com/console) - Configuración de publicación

### 11.3 Changelog

| Versión | Fecha | Cambios |
|---------|-------|---------|
| 1.0 | Mar 2026 | Versión inicial del PRC/RFC |

---

## 12. Aprobaciones

| Rol | Nombre | Fecha | Firma |
|-----|--------|-------|-------|
| Product Owner | | | |
| Tech Lead | | | |
| QA Lead | | | |

---

*Documento generado para参考 y desarrollo futuro del proyecto Zapatico Cochinito.*
