# TiendasOn — AI Coding Rules

## 1. Arquitectura obligatoria

Al crear o modificar features, respetar exactamente:

```text
Screen (Voyager)
 → ViewModel (ScreenModel + StateFlow)
   → UseCase
     → Repository (domain)
       → RepositoryImpl (data)
         → Services (Ktor)
           → DTOs / mappers
```

La UI no llama Repository/Service directamente. El ViewModel no llama Ktor.

## 2. Estructura

Base: `shared/src/commonMain/kotlin/org/example/project/`

```text
presentation/screens/{name}/          → Screen, ViewModel, ScreenState
presentation/screens/{name}/components → componentes del feature
presentation/screens/components/       → componentes compartidos
domain/useCase/{name}/                 → UseCase
domain/repository/{name}/              → Repository interface
data/repositoyImpl/{name}/             → RepositoryImpl + Results
data/remote/{name}/                    → Services + Impl
data/mappers/                          → DTOs, modelos, mappers, enums
data/params/                           → *Params
domain/util/EndPoints.kt               → endpoints
platform/                              → expect/actual
```

`repositoyImpl` es intencional y no debe renombrarse.

## 3. Capas

- **Screen:** Voyager `Screen`, observa `StateFlow`, usa `koinScreenModel()`.
- **ViewModel:** `ScreenModel`, `*ScreenState` en el mismo archivo, `MutableStateFlow`/`asStateFlow`, `screenModelScope`.
- **UseCase:** recibe Repository y expone operaciones `suspend`.
- **Repository:** interface en `domain`; Impl en `data`; coordina Services, Settings y mappers.
- **Services:** interface + Impl usando `HttpClient`; URLs desde `EndPoints`.
- **Results:** sealed class en `data/repositoyImpl/{name}/`.

## 4. Modelos y reutilización

- `*ScreenState` solo en el ViewModel.
- DTOs, modelos, mappers y enums compartidos → `data/mappers/`.
- Nunca colocar DTOs/modelos de red en `presentation/`.
- Antes de crear un DTO, buscar uno existente con la misma estructura JSON.
- Reutilizar modelos, componentes, mappers, params, enums y utilidades antes de duplicar.
- Usar nombres genéricos para DTOs transversales.
- Antes de crear componentes, buscar primero en `presentation/screens/components/` y `{feature}/components/`.
- Si algo existente necesita una pequeña variación, parametrizarlo en lugar de duplicarlo.

## 5. Parámetros

Si una función tiene más de **6 parámetros**, agruparlos en:

```text
data/params/{FunctionName}Params.kt
```

La función debe recibir un único objeto `params`. Solo `private data class` locales pueden permanecer en el archivo del composable.

## 6. Koin / DI

Registrar siempre:

```text
Services       → factory<XServices> { XServicesImpl(get()) }
Repository     → single<XRepository> { XRepositoryImpl(...) }
UseCase        → factory { XUseCases(get()) }
ViewModel      → factory { XViewModel(get()) }
```

Mantener `expect/actual` para APIs de plataforma.

## 7. Navegación

Rutas en `presentation/screens/routes/Routes.kt`.

```kotlin
val LOGIN_SCREEN = LoginScreen()
val MAIN_SCREEN: (LoggedUser) -> Screen = { MainScreen(it) }
```

Con parámetros usar lambda, nunca `fun` como ruta.

## 8. UI / Strings

- Strings siempre mediante `stringResource(Res.string.xxx)`.
- No hardcodear textos visibles.
- Usar `composeResources` para `es/en/pt`.
- Reutilizar componentes existentes.
- No crear `Button`, `OutlinedButton` o `TextButton` de Material si existe un componente del Design System.
- Iconos → `composeResources/drawable/` como vector XML; reutilizar existentes.
- Logos/iconos multicolor → `tint = Color.Unspecified`.

## 9. Tipografía

No definir directamente en `Text`/campos de texto:

```text
fontSize, fontWeight, lineHeight, color, textAlign, textDecoration
```

Usar siempre:

```kotlin
style = tiendasOnTextStyle(...)
```

Los parámetros de layout (`modifier`, `maxLines`, `overflow`, etc.) permanecen en el composable.

## 10. Colores

- No usar `Color(0x...)` directamente en `presentation/`.
- Usar `ColorApp.{token}` o `MaterialTheme.colorScheme`.
- Antes de crear un color, buscar uno existente.
- Los nuevos colores deben agregarse a `ColorApp` con nombres semánticos.

## 11. Dimensiones

Las dimensiones reutilizables (`dp`/`sp`) deben estar en:

```text
presentation/theme/Variables.kt
```

Buscar y reutilizar tokens existentes antes de crear nuevos.

Evitar constantes de layout a nivel de archivo dentro de `presentation/`.

## 12. Código

- No agregar comentarios `//`, `/* */` o KDoc salvo `@Suppress` requerido.
- No dejar `TODO` comentados ni código muerto.
- Mantener funciones pequeñas y responsabilidades claras.
- Evitar sobreingeniería y abstracciones innecesarias.
- Evitar `if (condition) true else false`; usar directamente `condition`.
- Evitar `!!` cuando exista una alternativa segura.
- No inventar APIs, endpoints, métodos o componentes.
- No agregar dependencias si ya existe una solución en el proyecto.
- No cambiar versiones sin necesidad.

## 13. Cambios

Antes de modificar:

```text
Buscar → Entender → Reutilizar → Modificar → Verificar
```

Realizar el menor cambio necesario. No modificar archivos no relacionados ni refactorizar código fuera del alcance solicitado.

## 14. Memoria de reglas de negocio (`MEMORY.md`)

- Mantener en la raíz del repo el archivo `MEMORY.md` como registro acumulativo de **reglas de negocio** (validaciones, flujos, permisos, estados, límites, mensajes funcionales, etc.), no de convenciones de código (esas viven en este `AGENTS.md`).
- **Al iniciar** una tarea de feature o bugfix, leer `MEMORY.md` y respetar lo documentado.
- **Al detectar** una regla de negocio nueva o modificada — en código, requisitos del usuario, comentarios de review o comportamiento de API — **actualizar `MEMORY.md` en el mismo cambio** (mismo PR o entrega de trabajo):
  - Redactar la regla de forma clara y verificable.
  - Indicar contexto (pantalla, flujo, módulo) y origen (archivo, endpoint, conversación).
  - Agrupar bajo el área existente o crear una sección nueva si no encaja.
  - No duplicar: fusionar o editar la entrada previa si es la misma regla.
- No registrar suposiciones no confirmadas; si falta confirmación, no escribir la regla hasta que quede explícita en código o en el pedido del usuario.

## 15. Checklist

Antes de entregar:

- [x] Arquitectura respetada.
- [x] Código existente reutilizado.
- [x] Sin duplicación.
- [x] DTOs/modelos en la ubicación correcta.
- [x] DI registrado.
- [x] Rutas correctas.
- [x] Strings internacionalizados.
- [x] Componentes existentes reutilizados.
- [x] Colores en `ColorApp`.
- [x] Dimensiones en `Variables`.
- [x] Tipografía con `tiendasOnTextStyle`.
- [x] Sin comentarios innecesarios.
- [x] Sin dependencias/versiones innecesarias.
- [x] Imports, tipos y referencias correctos.
- [x] Build/tests verificados cuando sea posible.
- [x] `MEMORY.md` actualizado si apareció o cambió alguna regla de negocio.

**Regla principal: entender primero el proyecto, reutilizar antes de crear y modificar lo mínimo necesario.**
