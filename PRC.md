# PRC - Producto, Riesgos y Capacidades (Zapatico Cochinito)

## Resumen ejecutivo

Este PRC documenta el estado real del proyecto en base a inspeccion estatica del codigo actual (Compose + ViewModel + DataStore + Play Games v2), sin ejecutar build ni pruebas.

Hallazgo principal: la integracion de Google Play Games esta funcional en su camino feliz (SDK inicializado, intento de sign-in automatico, prompt manual, leaderboard y submit de score), pero tiene puntos de fragilidad que explican por que "a veces no conecta": no hay reintentos automatizados, el flujo de autenticacion se intenta una sola vez por ciclo de actividad y el manejo de errores es generico (toast + log) sin diagnostico estructurado.

---

## Estado actual implementado (observado en codigo)

### 1) Producto y UX

- Juego ritmico en Jetpack Compose con pantallas: Inicio, Juego, Game Over, Ajustes y Novedades.
- Navegacion con `NavHost` y barra inferior para Inicio/Novedades (Juego se abre por flujo principal).
- Flujo de juego con BPM dinamico, 3 vidas, eventos de `CAMBIA` (inversion de controles), haptics y metronomo opcional.
- Prompt de reseña en Play Store tras game over con umbral y cooldown persistido.
- Soporte de idioma (es-419 / en-US) y tema claro/oscuro.

### 2) Logica de juego y estado

- Estado del juego en `GameViewModel` (`StateFlow<GameUiState>`).
- Persistencia de record y ajustes con `DataStore` via `AppPreferencesRepository`.
- Ajustes editables: dificultad, volumen, metronomo, idioma, tema, nivel de caos de CAMBIA.
- Dificultad progresiva: incremento de BPM por puntaje, topado por `MAX_BPM`.

### 3) Integracion Google Play Games (implementada)

- SDK Play Games v2 declarado en dependencias y servicio dedicado `PlayGamesService`.
- Inicializacion de SDK en `PlayGamesService.init`.
- Intento de autenticacion automatica en `MainActivity.onStart()`.
- Dialogo de conexion manual cuando se requiere sign-in.
- Envio de mejor puntuacion (`submitBestScore`) y apertura de leaderboard (`showLeaderboard`) desde Home y Game Over.
- `games_app_id` y `leaderboard_high_score_id` configurados en resources y `AndroidManifest`.

---

## Evidencia de rutas de integracion Google Play Games

### Dependencias y configuracion

- `app/build.gradle.kts`:61 incluye `play-services-games-v2`.
- `app/src/main/AndroidManifest.xml`:18 define `com.google.android.gms.games.APP_ID` con `@string/games_app_id`.
- `app/src/main/res/values/strings.xml`:3 define `games_app_id` y :4 define `leaderboard_high_score_id`.

### Servicio de integracion

- `app/src/main/java/com/digitalesweb/zapaticocochinito/games/PlayGamesService.kt`:19 inicializa `PlayGamesSdk.initialize(activity)`.
- `.../PlayGamesService.kt`:22 `signInIfNeeded(onSignInRequired)` valida `isAuthenticated`.
- `.../PlayGamesService.kt`:40 `requestUserSignIn()` ejecuta `signIn()` manual.
- `.../PlayGamesService.kt`:51 `submitBestScore(score)` envia score al leaderboard.
- `.../PlayGamesService.kt`:61 `showLeaderboard(onSignInRequired)` abre intent de leaderboard y maneja `SIGN_IN_REQUIRED`.

### Punto de entrada y triggers UI

- `app/src/main/java/com/digitalesweb/zapaticocochinito/MainActivity.kt`:69 instancia `PlayGamesService`.
- `.../MainActivity.kt`:138-142 llama `signInIfNeeded` en `onStart`.
- `.../MainActivity.kt`:104 envia best score via `onBestScoreUpdated`.
- `.../MainActivity.kt`:106-110 abre leaderboard (Home/GameOver) y dispara prompt de sign-in si corresponde.
- `.../MainActivity.kt`:123-133 muestra `PlayGamesSignInDialog` y confirma `requestUserSignIn()`.

### Rutas de UI que llegan a Play Games

- `app/src/main/java/com/digitalesweb/zapaticocochinito/ui/home/HomeScreen.kt`:154-159 boton "Ver marcador global" -> callback `onShowLeaderboard`.
- `app/src/main/java/com/digitalesweb/zapaticocochinito/ui/game/GameOverScreen.kt`:118-123 boton "Revisar marcador" -> callback `onShowLeaderboard`.

---

## Hallazgos de Google Play Games: por que A VECES no conecta

### Causas probables con evidencia en codigo

1. **Intento unico de auto sign-in por ciclo de actividad**  
   Evidencia: `PlayGamesService.hasTriedSignIn` corta reintentos (`PlayGamesService.kt`:16,23-24). Si el primer chequeo falla por red/servicio transitorio, no hay nuevo intento automatico.

2. **Sin estrategia de retry/backoff ni estados de error diferenciados**  
   Evidencia: en `requestUserSignIn` y `signInIfNeeded` se loguea + toast generico (`PlayGamesService.kt`:33,45-47), sin clasificar codigos ni reintentar.

3. **Dependencia de accion manual del usuario cuando falta autenticacion**  
   Evidencia: al fallar auth solo se invoca `onSignInRequired()` (`PlayGamesService.kt`:35), y si el usuario cierra el dialogo en `MainActivity.kt`:129-131, no hay nueva accion proactiva.

4. **Envio de score sin gate explicito de sesion autenticada**  
   Evidencia: `submitBestScore` no valida auth actual antes de `submitScore` (`PlayGamesService.kt`:51-59). Puede derivar en fallas silenciosas (segun comportamiento del cliente SDK).

5. **Manejo parcial de errores en leaderboard**  
   Evidencia: `showLeaderboard` solo trata especialmente `SIGN_IN_REQUIRED`; otros errores se unifican como "no disponible" (`PlayGamesService.kt`:72-79), perdiendo diagnostico fino.

6. **Riesgo externo de configuracion Play Console / SHA no verificable por codigo**  
   Evidencia de dependencia de IDs en app: `strings.xml` (:3-4) + `AndroidManifest.xml`:18-19. Si package/SHA/linked app no coincide en Play Console, la conexion falla intermitente por variante/entorno (debug vs release/internal).

7. **Observabilidad limitada para investigar intermitencias**  
   Evidencia: no hay telemetria estructurada de eventos de auth (solo logs locales), no hay codigos de fallo persistidos, no hay KPI de exito/fallo.

---

## Checklist de diagnostico rapido (Google Play Games)

1. **Build/entorno**
   - Verificar `applicationId` real de la variante instalada (`app/build.gradle.kts`:12).
   - Confirmar que el APK/AAB instalado corresponde al mismo proyecto de Play Console.

2. **IDs y manifiesto**
   - Confirmar `games_app_id` y `leaderboard_high_score_id` en:
     - `app/src/main/res/values/strings.xml`
     - `app/src/main/res/values-b+es+419/strings.xml`
     - `app/src/main/res/values-en-rUS/strings.xml`
   - Verificar `AndroidManifest.xml` meta-data de APP_ID.

3. **Cuenta y dispositivo**
   - Confirmar sesion en Google Play Games en el dispositivo.
   - Confirmar Play Services actualizado.
   - Confirmar conectividad al momento de `onStart`.

4. **Flujo de app**
   - Abrir app y revisar Logcat con tags `PlayGamesService` y `MainActivity`.
   - Verificar si aparece dialogo de sign-in y si el usuario lo acepta.
   - Verificar apertura de leaderboard desde Home y Game Over.

5. **Consola**
   - Validar vinculacion de app de juegos con app Android publicada.
   - Validar huellas SHA (debug/release/internal) registradas segun variante usada.
   - Validar estado del leaderboard (publicado y activo).

---

## Play Games Sidekick (estado y habilitacion)

### Estado actual frente a estandar oficial (actualizado 2026-03-20)

- **Estado:** **parcial**.
- **Cumple base tecnica de PGS v2 en app:** si (SDK v2, `APP_ID`, auth y leaderboard integrados).
- **No se observa requisito clave de Sidekick para guideline Level Up 2026:** implementacion de **Achievements** (el proyecto hoy usa solo leaderboard).
- **No hay API runtime especifica obligatoria de Sidekick en el cliente Android:** la habilitacion principal de overlay se hace desde **Play Console** sobre AABs.

### Requisitos oficiales Sidekick (fuente Android Developers)

- Sidekick se agrega a juegos mediante Play Console en release de testing/produccion; no se habilita por una API dedicada en el codigo del juego.
- Requisitos de uso en dispositivo: Android 13+, juego instalado desde Play Store y usuario con Gamer Profile.
- Para exponer el modulo de logros en Sidekick, el juego debe implementar Achievements de Play Games Services.
- En testing, el toggle de Sidekick se activa desde Developer options de Play Store en el dispositivo tester.
- Para cumplir guideline de Level Up (hito julio 2026), Google indica integrar Sidekick + Achievements.

### Brecha detectada en este repositorio

1. **Achievements no implementados en codigo**
   - Evidencia: no hay uso de `AchievementsClient` ni llamadas de unlock/increment en `app/src/main/java/com/digitalesweb/zapaticocochinito/`.
2. **No hay IDs de achievements en recursos**
   - Evidencia: `app/src/main/res/values/strings.xml` solo define `games_app_id` y `leaderboard_high_score_id`.
3. **Habilitacion Sidekick depende de consola/release, no verificable solo con codigo**
   - Evidencia: no existen metadatos Android ni dependencia SDK especifica de Sidekick distinta de PGS v2.

### Checklist operativo para habilitar Sidekick en produccion

1. **Preparar base PGS (ya casi lista en este repo)**
   - Confirmar `games_app_id` y leaderboard en resources + manifest.
   - Mantener `play-services-games-v2` actualizado.

2. **Agregar Achievements (pendiente en proyecto)**
   - Crear achievements en Play Console (minimo set inicial).
   - Publicarlos (no en draft) y mapear IDs en recursos Android.
   - Implementar llamadas en app (`unlock`/`increment`) en hitos de juego.

3. **Activar Sidekick en Play Console (obligatorio)**
   - En release interna/cerrada, marcar **Add Play Games Sidekick to app bundles you upload**.
   - Opcional recomendado: en **Testing > Advanced settings > Play Games Sidekick**, habilitar auto-add para nuevos AAB.

4. **Validar en dispositivo tester**
   - Instalar build desde Play Store (track de testing).
   - Activar Play Store developer options (tap 7 veces en version) y encender toggle **Play Games Sidekick**.
   - Verificar overlay en partida + seccion de achievements/beneficios.

5. **Promocion a produccion**
   - Promover release que ya incluye Sidekick.
   - Confirmar que el checklist de UX Level Up quede cubierto (Sidekick + Achievements).

### Riesgos y limitaciones

- **Riesgo de cumplimiento:** sin Achievements, Sidekick queda limitado y no cubre completamente guideline Level Up.
- **Riesgo de rollout:** si la instalacion no proviene de Play Store o el tester no activa toggle, no se vera overlay.
- **Riesgo de seguridad/compatibilidad:** Sidekick introduce libs nativas adicionales; si hay anti-tamper externo, requiere prueba dedicada por variante.

---

## Plan de mitigacion Play Games (diagnostico + acciones)

### Fase 1 - Quick wins (1-2 dias)

- Registrar estado de autenticacion en memoria de UI (autenticado / pendiente / fallo / no intentado).
- Reintentar `signInIfNeeded` cuando:
  - vuelve conectividad,
  - `onStart` posterior,
  - usuario toca leaderboard y no hay sesion.
- Enriquecer logs con `statusCode` en todos los fallos (`ApiException`), no solo en leaderboard.
- Agregar mensaje UX contextual: "Sin conexion", "Sesion requerida", "Servicio no disponible".

### Fase 2 - Robustez (3-5 dias)

- Introducir un `PlayGamesAuthManager` (o estado en ViewModel) para separar UI de side effects de auth.
- Implementar politica de retry con backoff exponencial acotado para chequeo de auth.
- Gatear `submitBestScore` por estado auth y encolar score local para envio diferido.
- Crear event tracing minimo (timestamp + origen + resultado + codigo) en almacenamiento local para soporte.

### Fase 3 - Operacion y soporte (1 sprint)

- KPI dashboard simple (Crashlytics/Analytics o logger propio) para:
  - tasa de sign-in exitoso,
  - tasa de apertura leaderboard,
  - error codes top N.
- Script/checklist de release para validar IDs/SHA/leaderboard antes de publicar.
- Pruebas manuales estandarizadas en matriz de dispositivos/cuentas.

---

## Roadmap de funcionalidades futuras (priorizado)

### Corto plazo (0-1 sprint)

1. Historial local de partidas (score, BPM maximo, duracion).
2. Modo practica sin perder vidas.
3. Mejor feedback de errores Play Games (toasts contextuales + CTA de reconexion).

### Medio plazo (1-2 sprints)

1. Desafio diario (ya anticipado por copy en `NotificationsScreen`).
2. Misiones semanales y progresion por objetivos.
3. Compartir score con deeplink de la app.

### Largo plazo (2+ sprints)

1. Liga semanal online real (backend + temporadas).
2. Cosmeticos/skins desbloqueables.
3. Eventos en vivo y ranking por regiones.

---

## Migracion a iOS (evaluacion y plan accionable)

### Enfoque recomendado y tradeoffs

- **Recomendacion base: KMP + UI nativa (Compose en Android, SwiftUI en iOS).** Permite reutilizar logica de juego, reglas de dominio y parte de persistencia, manteniendo UX nativa y menor riesgo de performance para mecanicas ritmicas.
- **Flutter (alternativa valida):** acelera salida multiplataforma con una sola UI y alto control visual; tradeoff: mayor costo de migracion inicial desde Compose, reescritura de UI completa y puente para servicios de plataforma.
- **React Native (alternativa con menor recomendacion para este caso):** rapido para equipos JS/TS, pero en juego ritmico puede exigir modulos nativos para timing/haptics/audio preciso; aumenta complejidad de integracion.
- **Nativo iOS puro (Swift/SwiftUI):** maxima calidad nativa y menor lock-in tecnologico, pero duplicacion de logica y mayor costo de mantenimiento evolutivo frente a Android.
- **Criterio pragmatico de seleccion inicial:** si el objetivo es iOS sin comprometer sensacion de juego, priorizar KMP; si el objetivo es una experiencia UI 100% compartida con velocidad comercial, evaluar Flutter con POC tecnico previo.

### Portabilidad: que se puede reutilizar y que no

- **Altamente portable (capa dominio/datos):** reglas de BPM, scoring, vidas, eventos de CAMBIA, casos de uso y contratos de repositorio (si se desacoplan de Android).
- **Portable con adaptacion:** persistencia actual basada en `DataStore` (migrable a `multiplatform-settings`, SQLDelight o wrapper expect/actual).
- **No portable de forma directa:** UI Jetpack Compose, navegacion `NavHost`, ciclo de vida de `MainActivity`, haptics Android y componentes especificos de Play Services.
- **Requiere reemplazo en iOS:** Google Play Games (leaderboard/auth) por Game Center (o proveedor cross-platform), review prompt de Play Store por `SKStoreReviewController`.
- **Riesgo de acoplamiento actual:** `MainActivity` concentra integraciones; conviene extraer puertos de plataforma antes de migrar para evitar reescritura mezclada con deuda tecnica.

### Roadmap de migracion por fases

1. **Fase 0 - Descubrimiento (1-2 semanas):** inventario de dependencias Android-only, definicion de arquitectura objetivo (KMP/Flutter/RN/nativo) y POC de timing de input + audio + haptics en iOS.
2. **Fase 1 - Desacople (1 sprint):** separar dominio de framework Android, crear interfaces de plataforma (auth, leaderboard, review, haptics, audio), mover logica de juego a modulo testeable.
3. **Fase 2 - Nucleo compartido (1-2 sprints):** portar estado, reglas y persistencia a capa compartida; mantener Android funcionando como baseline de paridad funcional.
4. **Fase 3 - Cliente iOS MVP (1-2 sprints):** implementar UI iOS y adaptadores de plataforma (Game Center, almacenamiento, review), enfocando Inicio/Juego/Game Over.
5. **Fase 4 - Paridad y hardening (1 sprint):** test de latencia percibida, calibracion de dificultad, analitica comparada Android vs iOS, correcciones de UX y publicacion limitada (TestFlight).

### Riesgos tecnicos y de producto

- **Precision temporal:** diferencias de scheduler/audio/haptics entre plataformas pueden afectar la jugabilidad ritmica.
- **Paridad de servicios:** no existe equivalencia 1:1 entre Play Games y Game Center (IDs, login UX, politicas), riesgo de experiencia inconsistente.
- **Sobrecosto de mantenimiento:** durante transicion habra doble superficie (Android + iOS + capa compartida), con mayor costo de QA y release.
- **Desalineacion de roadmap:** migrar temprano puede frenar features de retencion (desafio diario/misiones) si no se protege capacidad del equipo.
- **Riesgo de producto:** sin validacion de demanda iOS, la migracion puede consumir sprint budget sin impacto proporcional en crecimiento.

### Criterio de decision (cuando conviene migrar y cuando no)

- **Conviene migrar ahora si:**
  - iOS representa una oportunidad comercial validada (lista de espera, feedback de comunidad, objetivos de negocio claros).
  - El equipo puede sostener 20-30% de capacidad extra temporal para paridad y QA multiplataforma.
  - Se completa un POC con resultado aceptable de latencia, estabilidad y experiencia de leaderboard/login.
- **Conviene postergar si:**
  - Aun no estan estabilizados los fundamentos del loop actual (telemetria, robustez de auth, retencion D1/D7).
  - No hay propietario tecnico claro de la arquitectura multiplataforma.
  - El costo de oportunidad afecta hitos de producto de corto plazo con mayor ROI.
- **Regla operativa sugerida:** iniciar migracion solo con gate de negocio + gate tecnico aprobados; si uno falla, mantener foco Android y reevaluar en 1-2 sprints.

---

## Oportunidades de mejora (quick wins vs refactor)

### Quick wins

- Eliminar dependencia no usada `play-services-auth` si no se utiliza en codigo (`app/build.gradle.kts`:62).
- Unificar mensajes de error y agregar codigos de causa.
- Alinear documentacion tecnica antigua (`RESUMEN_TECNICO.md`) con arquitectura actual Compose.
- Agregar pruebas unitarias para reglas de score/BPM/CAMBIA en `GameViewModel`.

### Refactor estructural

- Separar modulo de integraciones externas (Play Games, Play Store review) de `MainActivity`.
- Introducir casos de uso (`SignInPlayGamesUseCase`, `SubmitScoreUseCase`, `OpenLeaderboardUseCase`).
- Formalizar modelo de estado de sesion (sealed class) consumido por UI.
- Definir capa de observabilidad (eventos de dominio + errores tecnicos).

---

## KPIs sugeridos

1. **Play Games sign-in success rate** = inicios de sesion exitosos / intentos de sign-in.
2. **Leaderboard open success rate** = aperturas exitosas / taps en "ver marcador".
3. **Score submit success rate** = envios aceptados / envios intentados.
4. **Tiempo medio a sesion autenticada** desde `onStart`.
5. **Top 5 statusCode de fallos** por version de app.
6. **Retencion D1/D7** segmentada por usuarios conectados vs no conectados a Play Games.
7. **Conversion a review** = prompts mostrados -> clic en calificar.

---

## Riesgos y supuestos

- Este documento se basa en inspeccion estatica; no valida comportamiento en runtime ni configuracion real de Play Console.
- La intermitencia de conexion puede depender de factores externos (cuenta, red, Play Services, huellas SHA) que no son inferibles solo por codigo.
- El repositorio tiene cambios locales no relacionados en curso; no fueron modificados ni revertidos.

