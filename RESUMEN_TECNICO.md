# Zapatico Cochinito - Resumen Técnico del Juego

## Descripción General
Aplicación móvil Android que implementa el juego tradicional latinoamericano "Zapatico, cochinito, cambia de piecito" como un minijuego rítmico interactivo.

## Flujo del Juego

### 1. Pantalla Inicial
- Muestra "Toca para comenzar"
- Estadísticas en cero (Puntos: 0, Vidas: 3, Racha: 0)
- Al tocar cualquier botón de pie, el juego comienza

### 2. Durante el Juego

#### Ciclo Principal
```
1. El juego muestra un pie (IZQUIERDO o DERECHO)
2. El jugador tiene 1.5 segundos (inicialmente) para tocar el botón correcto
3. Evaluación del toque:
   - CORRECTO: +10 puntos, racha +1, continúa el juego
   - INCORRECTO: -1 vida, racha = 0, continúa (si quedan vidas)
   - SIN RESPUESTA: -1 vida, racha = 0, continúa (si quedan vidas)
4. Si quedan vidas, vuelve al paso 1
5. Si vidas = 0, muestra pantalla de Game Over
```

#### Mecánica "¡CAMBIA!"
- Aparece con 20% de probabilidad en lugar del pie normal
- Duración: 2 segundos en pantalla
- Efecto: Invierte los controles durante los próximos 3-5 toques
  - Si muestra "PIE IZQUIERDO", debes tocar el botón DERECHO
  - Si muestra "PIE DERECHO", debes tocar el botón IZQUIERDO
- Animación visual: texto parpadeante en color naranja

#### Progresión de Dificultad
- **Inicio**: 1500ms por turno
- **Cada 10 aciertos consecutivos**: -50ms
- **Mínimo**: 800ms por turno
- Fórmula: `tiempo = max(800ms, 1500ms - (racha ÷ 10 × 50ms))`

### 3. Game Over
- Muestra "¡Juego Terminado!"
- Muestra puntuación final
- Botón "Jugar de Nuevo" para reiniciar

## Arquitectura del Código

### MainActivity.kt
Actividad principal que contiene toda la lógica del juego.

#### Variables de Estado
```kotlin
- score: Int              // Puntuación actual
- lives: Int              // Vidas restantes (máximo 3)
- streak: Int             // Racha de aciertos consecutivos
- isGameActive: Boolean   // Si el juego está en curso
- isInverted: Boolean     // Si los controles están invertidos (CAMBIA activo)
- currentFoot: Int        // Pie actual (0=izquierdo, 1=derecho)
- currentInterval: Long   // Tiempo actual para responder (en ms)
```

#### Métodos Principales

**startGame()**
- Inicializa el estado del juego
- Resetea puntuación, vidas, racha
- Llama a scheduleNextFoot()

**scheduleNextFoot()**
- Decide aleatoriamente si mostrar CAMBIA (20%) o un pie normal
- Programa el siguiente turno
- Ajusta dificultad basada en racha

**showCambia()**
- Muestra el texto "¡CAMBIA!" con animación
- Activa el flag isInverted
- Programa que los controles vuelvan a la normalidad después de 3-5 toques

**showCurrentFoot()**
- Muestra el pie a tocar (considerando inversión si aplica)
- Cambia el color del texto según el pie
- Inicia temporizador para detectar respuesta tardía

**onFootTapped(tappedFoot: Int)**
- Verifica si el toque es correcto (considerando inversión)
- Llama a onCorrectTap() o onIncorrectTap()

**onCorrectTap()**
- Suma puntos (+10)
- Incrementa racha
- Muestra retroalimentación visual (flash verde)
- Programa siguiente turno

**onIncorrectTap()**
- Resta vida (-1)
- Resetea racha a 0
- Muestra retroalimentación visual (flash rojo)
- Si vidas = 0, llama a endGame()

**onMissedTap()**
- Se ejecuta cuando el temporizador expira sin respuesta
- Misma lógica que onIncorrectTap()

**endGame()**
- Detiene todos los temporizadores
- Muestra pantalla de Game Over con puntuación final

**resetGame()**
- Oculta pantalla de Game Over
- Vuelve al estado inicial

### activity_main.xml
Layout con componentes visuales organizados verticalmente.

#### Estructura
```
ConstraintLayout (pantalla completa)
├── LinearLayout (statsBar) - Barra superior
│   ├── TextView (scoreText) - "Puntos: X"
│   ├── TextView (livesText) - "Vidas: X"
│   └── TextView (streakText) - "Racha: X"
├── FrameLayout (gameArea) - Área central del juego
│   ├── TextView (currentFootText) - Muestra el pie o mensaje inicial
│   └── TextView (cambiaText) - Muestra "¡CAMBIA!" (oculto por defecto)
├── LinearLayout (footButtons) - Botones de pie
│   ├── Button (leftFootButton) - "PIE IZQUIERDO" (verde)
│   └── Button (rightFootButton) - "PIE DERECHO" (azul)
└── LinearLayout (gameOverLayout) - Overlay de Game Over (oculto por defecto)
    ├── TextView (gameOverText) - "¡Juego Terminado!"
    ├── TextView (finalScoreText) - "Puntuación Final: X"
    └── Button (playAgainButton) - "Jugar de Nuevo"
```

## Colores y Diseño

### Paleta de Colores
- **Background**: #F3E5F5 (lavanda claro)
- **Primary**: #6200EE (morado)
- **Left Foot**: #4CAF50 (verde)
- **Right Foot**: #2196F3 (azul)
- **Correct**: #4CAF50 (verde)
- **Incorrect**: #F44336 (rojo)
- **Cambia**: #FF5722 (naranja brillante)

### Animaciones
- **Botones**: Flash de color al tocar (200ms)
- **CAMBIA**: Parpadeo entre blanco y naranja (300ms × 3 repeticiones)

## Características Técnicas

### Tecnologías Utilizadas
- **Lenguaje**: Kotlin 1.9.0
- **UI**: Android Views (sin Jetpack Compose)
- **Build System**: Gradle 8.0
- **Android SDK**: 
  - minSdk: 24 (Android 7.0)
  - targetSdk: 34 (Android 14)
  - compileSdk: 34

### Dependencias
```gradle
- androidx.core:core-ktx:1.12.0
- androidx.appcompat:appcompat:1.6.1
- com.google.android.material:material:1.10.0
- androidx.constraintlayout:constraintlayout:2.1.4
```

### Threading
- Utiliza `Handler` con `Looper.getMainLooper()` para temporizadores
- Todos los Runnables se cancelan apropiadamente en `onDestroy()`

## Casos de Prueba Sugeridos

### Casos Básicos
1. ✓ Iniciar juego toca botón
2. ✓ Tocar pie correcto suma puntos
3. ✓ Tocar pie incorrecto resta vida
4. ✓ No tocar nada resta vida
5. ✓ Perder 3 vidas termina el juego

### Casos CAMBIA
6. ✓ CAMBIA invierte controles
7. ✓ Tocar correcto durante inversión suma puntos
8. ✓ Tocar incorrecto durante inversión resta vida
9. ✓ CAMBIA termina después de 3-5 toques

### Casos de Dificultad
10. ✓ Racha de 10 acelera el juego
11. ✓ El tiempo mínimo es 800ms
12. ✓ Perder racha resetea a 0

### Casos UI
13. ✓ Estadísticas se actualizan correctamente
14. ✓ Colores de botones cambian en flash
15. ✓ Game Over muestra puntuación final
16. ✓ Jugar de nuevo reinicia el estado

## Mejoras Futuras Posibles

### Funcionalidad
- [ ] Efectos de sonido y música
- [ ] Vibración háptica al tocar
- [ ] Múltiples niveles de dificultad
- [ ] Modo de práctica sin perder vidas
- [ ] Logros y recompensas

### Persistencia
- [ ] Guardar puntuación máxima (SharedPreferences)
- [ ] Historial de partidas
- [ ] Estadísticas globales (total de partidas, promedio, etc.)

### Social
- [ ] Tabla de clasificación en línea
- [ ] Compartir puntuación en redes sociales
- [ ] Modo multijugador local

### UI/UX
- [ ] Tutorial interactivo para nuevos usuarios
- [ ] Temas visuales alternativos
- [ ] Modo oscuro
- [ ] Personalización de colores
- [ ] Animaciones más elaboradas

### Accesibilidad
- [ ] Soporte para lectores de pantalla
- [ ] Ajuste de tamaños de texto
- [ ] Modo para daltonismo
- [ ] Ajuste de velocidad del juego

## Conclusión

Zapatico Cochinito es una implementación completa y funcional del juego tradicional como app Android. El código es limpio, bien estructurado y fácil de mantener. La lógica del juego es sólida y la interfaz es intuitiva y atractiva. La aplicación está lista para ser compilada e instalada en dispositivos Android con API 24 o superior.
