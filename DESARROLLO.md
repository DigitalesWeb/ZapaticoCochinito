# Zapatico Cochinito - Instrucciones de Desarrollo

## Descripción
Zapatico Cochinito es un minijuego rítmico divertido y nostálgico, inspirado en la ronda tradicional "Zapatico, cochinito, cambia de piecito". Pon a prueba tu coordinación, reflejos y sentido del ritmo tocando el pie correcto al compás del juego.

## Características del Juego

### 🎮 Mecánicas de Juego
- **Toca el pie correcto**: El juego te indicará qué pie tocar (izquierdo o derecho)
- **Mantén el ritmo**: Tienes un tiempo limitado para tocar el pie correcto
- **¡CAMBIA!**: Ocasionalmente aparece esta palabra que invierte los controles temporalmente
- **Sistema de puntuación**: Gana 10 puntos por cada acierto
- **Racha perfecta**: Mantén una secuencia de aciertos consecutivos
- **Sistema de vidas**: Comienzas con 3 vidas, pierdes una por error o falta de respuesta
- **Dificultad progresiva**: El ritmo se acelera conforme avanzas

### 🎯 Objetivo
Mantener la racha más larga posible de aciertos al ritmo, acumulando la mayor puntuación sin perder las 3 vidas.

## Requisitos de Desarrollo

### Software Necesario
- **Android Studio** (Arctic Fox o superior)
- **JDK** 11 o superior
- **Android SDK** con API Level 24 o superior
- **Gradle** 8.0 (incluido en el proyecto)

### Requisitos Mínimos del Dispositivo
- Android 7.0 (API 24) o superior
- Pantalla táctil

## Cómo Compilar y Ejecutar

### Opción 1: Usando Android Studio

1. **Clonar el repositorio**
   ```bash
   git clone https://github.com/DigitalesWeb/ZapaticoCochinito.git
   cd ZapaticoCochinito
   ```

2. **Abrir en Android Studio**
   - Abrir Android Studio
   - Seleccionar "Open an Existing Project"
   - Navegar al directorio clonado y seleccionarlo

3. **Sincronizar Gradle**
   - Android Studio automáticamente sincronizará las dependencias
   - Esperar a que termine la sincronización

4. **Ejecutar en Emulador o Dispositivo**
   - Conectar un dispositivo Android con USB debugging habilitado, o
   - Crear un emulador Android desde AVD Manager
   - Hacer clic en el botón "Run" (▶️)

### Opción 2: Usando Línea de Comandos

1. **Compilar el APK de Debug**
   ```bash
   ./gradlew assembleDebug
   ```

2. **Instalar en Dispositivo Conectado**
   ```bash
   ./gradlew installDebug
   ```

3. **Compilar APK de Release**
   ```bash
   ./gradlew assembleRelease
   ```
   El APK estará en: `app/build/outputs/apk/release/`

## Cómo Capturar Logs en Dispositivo Físico

Cuando ejecutes la app en una tablet o teléfono real, puedes revisar los `Log.d` agregados usando cualquiera de los siguientes métodos:

### Con Android Studio
1. Conecta el dispositivo mediante USB y verifica que aparezca en la lista de dispositivos (`Run > Select Device`).
2. Abre **Logcat** desde la parte inferior de Android Studio.
3. En el desplegable de filtros, elige tu aplicación (por `package`) y selecciona el nivel **Debug** para que se muestren los mensajes registrados con `Log.d`.
4. Si necesitas enfocarte en una etiqueta concreta (por ejemplo, `AppViewModel`), escribe el nombre en el campo de búsqueda de Logcat.

### Con ADB en la línea de comandos
1. Asegúrate de tener `adb` instalado y que el dispositivo esté autorizado (`adb devices`).
2. Ejecuta el comando:
   ```bash
   adb logcat | grep AppViewModel
   ```
   Cambia `AppViewModel` por la etiqueta que deseas observar (`AppPreferencesRepository`, `MainActivity`, etc.).
3. Para guardar los logs en un archivo mientras reproduces el problema, usa:
   ```bash
   adb logcat -v time > logs.txt
   ```
   Luego abre `logs.txt` con tu editor favorito y busca las entradas relevantes.

> **Nota:** Si Logcat muestra demasiada información, añade filtros por nivel (`*:S AppViewModel:D`) o limpia el buffer antes de comenzar (`adb logcat -c`).

## Estructura del Proyecto

```
ZapaticoCochinito/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/digitalesweb/zapaticocochinito/
│   │       │   └── MainActivity.kt          # Lógica principal del juego
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   │   └── activity_main.xml    # Diseño de la interfaz
│   │       │   ├── values/
│   │       │   │   ├── strings.xml          # Textos en español
│   │       │   │   ├── colors.xml           # Paleta de colores
│   │       │   │   └── themes.xml           # Tema de la app
│   │       │   └── mipmap-*/                # Iconos de la app
│   │       └── AndroidManifest.xml          # Configuración de la app
│   └── build.gradle                         # Configuración de compilación
├── gradle/                                  # Gradle Wrapper
├── build.gradle                             # Configuración raíz
├── settings.gradle                          # Configuración de proyecto
└── README.md                                # Este archivo
```

## Detalles Técnicos

### Tecnologías Utilizadas
- **Lenguaje**: Kotlin
- **UI Framework**: Android Views con ViewBinding
- **Arquitectura**: Activity con manejo de estado en memoria
- **Animaciones**: ObjectAnimator para efectos visuales
- **Threading**: Handler con Looper para temporizadores del juego

### Componentes Principales

#### MainActivity.kt
La actividad principal contiene toda la lógica del juego:
- Gestión del estado del juego (activo, terminado)
- Sistema de puntuación y vidas
- Temporizadores para el ritmo del juego
- Lógica de inversión de controles (CAMBIA)
- Animaciones y retroalimentación visual

#### activity_main.xml
Layout con:
- Barra superior con estadísticas (puntos, vidas, racha)
- Área central que muestra el pie actual a tocar
- Indicador "¡CAMBIA!" con animación
- Dos botones grandes para pie izquierdo y derecho
- Pantalla de Game Over con puntuación final

### Características Implementadas
- ✅ Detección de toques correctos/incorrectos
- ✅ Sistema de puntuación con rachas
- ✅ Sistema de vidas (3 máximo)
- ✅ Modo "CAMBIA" que invierte controles
- ✅ Aceleración progresiva del ritmo
- ✅ Retroalimentación visual con colores
- ✅ Pantalla de Game Over
- ✅ Opción de jugar de nuevo
- ✅ Orientación forzada a vertical

### Mecánicas de Dificultad
- **Intervalo base**: 1.5 segundos por pie
- **Aceleración**: Se reduce 50ms cada 10 aciertos consecutivos
- **Intervalo mínimo**: 800ms (máxima dificultad)
- **Probabilidad de CAMBIA**: 20% en cada turno
- **Duración de inversión**: 3-5 toques después de aparecer CAMBIA

## Posibles Mejoras Futuras

- 🔊 Agregar efectos de sonido y música de fondo
- 🏆 Sistema de puntuación máxima persistente
- 📊 Estadísticas y gráficas de progreso
- 🎨 Más temas visuales y personalizaciones
- 🌐 Tabla de clasificación en línea
- 🎵 Sincronización con música real
- 👥 Modo multijugador
- 🏅 Sistema de logros

## Licencia
Este proyecto está bajo licencia MIT. Ver archivo LICENSE para más detalles.

## Contribuciones
Las contribuciones son bienvenidas. Por favor, abre un issue o pull request para sugerencias y mejoras.
