# DarHoney — Momento 3

Diario gastronómico compartido para parejas (Android, Kotlin, Firebase).

## Requisitos cumplidos (rúbrica)

| Pilar | Implementación |
|--------|----------------|
| **Auth + RBAC + sesión** | Firebase Auth (email, Google, Facebook), roles `ADMIN` / `STANDARD` en Firestore, `SessionPreferences` + auto-login, logout con limpieza de sesión y caché Glide |
| **Firestore en tiempo real** | `addSnapshotListener` en repositorios, CRUD de restaurantes, visitas y wishlist |
| **Evidencia fotográfica** | Galería (`GetMultipleContents`), cámara (`TakePicture` + FileProvider), **Firebase Storage** con barra de progreso |
| **Mapas + GPS** | Google Maps, marcadores, zoom, **vista satélite**, **InfoWindow** nativo, Fused Location + Geocoder |
| **Notificaciones** | Canal local, aviso al guardar visita, `POST_NOTIFICATIONS`, servicio **FCM** (`DarHoneyMessagingService`) |

## Usuarios de prueba

| Rol | Cómo obtenerlo |
|-----|----------------|
| **Administrador** | Registrarse con correo que comience con `admin@` (ej. `admin@darhoney.test`) |
| **Estándar** | Cualquier otro correo (ej. `pareja@darhoney.test`) |

Los administradores pueden eliminar restaurantes y eliminar cuenta desde Estadísticas. Los usuarios estándar no ven esas acciones.

## Configuración local

1. Clonar el repositorio y abrir en Android Studio.
2. Crear `local.properties` (no subir al repo):

```properties
sdk.dir=C\:\\Users\\TU_USUARIO\\AppData\\Local\\Android\\Sdk
MAPS_API_KEY=tu_clave_maps
GOOGLE_WEB_CLIENT_ID=cliente_web_oauth_de_firebase
FACEBOOK_APP_ID=...
FACEBOOK_CLIENT_TOKEN=...
```

3. Descargar `google-services.json` desde Firebase (proyecto `darhoney-6e6de`) en `app/google-services.json`.
4. Firebase Console: Auth (Email, Google, Facebook), Firestore, Storage; Maps API habilitada.
5. Meta Developers: Key Hash de firma debug/release para Facebook Login.

## Ejecución

```bash
./gradlew :app:assembleDebug
```

Instalar el APK en emulador o dispositivo API 24+.

## Entrega

- Código fuente en GitHub (rama principal con historial de commits).
- APK de debug o release firmado según indique el docente.
- Este README con usuarios de prueba y pasos de configuración.

## Estructura relevante

- `ui/auth/LoginActivity` — login, validación `TextInputLayout.setError`, auto-login
- `utils/SessionPreferences` — sesión persistente
- `data/repository/PhotoStorageRepository` — subida a Storage
- `notifications/` — canal, notificaciones locales y FCM
- `res/layout-land/` — layouts para orientación horizontal
