## URL

```txt
https://extract-lemon-suits-just.trycloudflare.com
```
# Pastry3D

Pastry3D es una aplicación web para generar recetas de repostería con inteligencia artificial y visualizarlas en 3D usando modelos GLB reutilizables. El sistema interpreta el pedido del usuario, busca modelos disponibles en una biblioteca local y arma una composición visual con bases, toppings y decoraciones. Cuando no existe un modelo adecuado, puede crear un job de generación externa para producir un nuevo GLB con Tripo y registrarlo como asset reutilizable.

## Estado actual del proyecto

El proyecto ya cuenta con:

- Frontend en React y Vite.
- Backend en Java 21 con Spring Boot.
- Autenticación con JWT.
- Base de datos PostgreSQL.
- Migraciones con Flyway.
- Integración con Gemini para interpretar pedidos de repostería.
- Biblioteca local de modelos GLB reutilizables.
- Composición de escenas 3D con Three.js / React Three Fiber.
- Jobs de generación externa para modelos faltantes.
- Integración prevista con Tripo para generar modelos 3D desde texto.
- Despliegue en VM Ubuntu con Docker Compose.
- Exposición temporal segura mediante Cloudflare Quick Tunnel.


## Objetivo del proyecto

El objetivo de Pastry3D es demostrar una arquitectura funcional donde la inteligencia artificial no genera todo desde cero de forma costosa, sino que:

1. Interpreta el pedido del usuario.
2. Valida si el texto corresponde a repostería.
3. Genera una receta estructurada.
4. Crea un plan visual 3D.
5. Reutiliza modelos GLB existentes cuando sea posible.
6. Compone una escena 3D con assets locales.
7. Solicita generación externa solo cuando falta un modelo.
8. Guarda los GLB generados para reutilizarlos en futuras solicitudes.

Flujo principal:

```txt
Pedido del usuario
        ↓
Gemini valida e interpreta el pedido
        ↓
Gemini genera receta + plan visual + prompt técnico 3D
        ↓
Backend busca assets GLB locales
        ↓
Si hay modelo suficiente: compone escena 3D
        ↓
Si falta modelo: crea GenerationJob
        ↓
Usuario puede solicitar generación con Tripo
        ↓
Tripo genera modelo 3D
        ↓
Backend descarga el GLB
        ↓
Backend registra el nuevo asset
        ↓
El asset queda disponible para futuras recetas similares
```

## Funcionalidades principales

- Registro de usuarios.
- Inicio de sesión con JWT.
- Visualización y ocultamiento de contraseña en registro.
- Límites de longitud en nombre, correo y contraseña.
- Generación de recetas con Gemini.
- Validación IA para rechazar entradas que no correspondan a postres o repostería.
- Mensajes de error claros para prompts inválidos, ofensivos o no relacionados.
- Historial de recetas por usuario.
- Visualización 3D con modelos GLB.
- Reutilización de modelos locales.
- Composición de bases y toppings.
- Uso de modelos completos cuando existen.
- Detección de modelos faltantes.
- Botón de generación externa cuando no hay modelo apropiado.
- Integración con Tripo para generar GLB desde texto.
- Descarga y almacenamiento local de modelos generados.
- Registro de modelos generados como assets reutilizables.
- Despliegue con Docker Compose en una VM Ubuntu.
- Exposición temporal por HTTPS con Cloudflare Tunnel.

## Modelos GLB considerados

Los modelos GLB físicos no se incluyen necesariamente en el repositorio por tamaño. La base de datos contiene referencias a rutas esperadas. Los modelos demo considerados son:

```txt
birthday_candle.glb
blueberry_topping.glb
caramel_rose_cluster.glb
cheesecake_base_plain.glb
cherry_topping.glb
chocolate_bar_piece.glb
chocolate_cake_complete.glb
chocolate_drizzle.glb
cream_swirl.glb
cupcake_base_vanilla.glb
donut_glazed_complete.glb
flan_napolitano_complete.glb
ice_cream_cup_complete.glb
macaron_topping.glb
milhojas_base_classic.glb
round_cake_base_vanilla.glb
strawberry_cake_complete.glb
strawberry_topping.glb
tart_base_vanilla.glb
```

Rutas esperadas en despliegue:

```txt
/opt/pastry3d/uploads/models/demo/*.glb
/opt/pastry3d/uploads/models/generated/*.glb
```

En Docker, la carpeta se monta desde:

```txt
./back_pastry3d/uploads/models:/opt/pastry3d/uploads/models
```

## Reutilización de modelos generados

Cuando Tripo genera un modelo, el backend debe:

1. Descargar el archivo GLB antes de que expire la URL externa.
2. Guardarlo localmente en `uploads/models/generated/`.
3. Crear un registro en `model_assets`.
4. Asignar un `assetKey` estable.
5. Guardar etiquetas y alias para encontrarlo después.

Ejemplo:

```txt
Usuario pide: pan de bono
        ↓
No existe asset local
        ↓
Se genera con Tripo
        ↓
Se guarda como generated/pan_de_bono_<id>.glb
        ↓
Se registra:
assetKey: pan_de_bono
category: complete
dessertType: pan_de_bono
tags: pan de bono, pandebono, queso, colombiano, pan colombiano, ai-generated, tripo
```

Si otro usuario pide algo parecido, por ejemplo “quiero un pandebono”, el sistema puede encontrar el asset registrado y reutilizarlo sin gastar créditos de Tripo.

Advertencia: si se borra la base de datos, los archivos GLB pueden seguir existiendo en disco, pero la aplicación perderá los registros que permiten encontrarlos. Antes de resetear PostgreSQL después de generar modelos reales, conviene respaldar `model_assets` o volver a sembrar esos assets.

## Tecnologías usadas

### Backend

- Java 21.
- Spring Boot 3.
- Spring Security.
- JWT.
- Spring Data JPA.
- PostgreSQL 16.
- Flyway.
- Gemini API.
- Tripo API.
- Java Mail / SMTP opcional.
- Docker.

### Frontend

- React.
- Vite.
- Three.js.
- React Three Fiber.
- Drei.
- React Router.
- Lucide React.
- CSS personalizado.

### Infraestructura

- Ubuntu Server.
- Docker Compose.
- PostgreSQL en contenedor.
- Caddy como proxy HTTP local.
- Cloudflare Quick Tunnel para HTTPS temporal.
- Almacenamiento local para modelos GLB.

## Arquitectura general

```txt
Pastry3D
│
├── Frontend React
│   ├── Login y registro
│   ├── Formulario de generación
│   ├── Historial
│   ├── Resultado de receta
│   └── Visor 3D
│
├── Backend Spring Boot
│   ├── Autenticación
│   ├── Validación de prompts
│   ├── Recetas
│   ├── Composición 3D
│   ├── Assets GLB
│   ├── Jobs de generación externa
│   ├── Integración con Gemini
│   └── Integración con Tripo
│
├── PostgreSQL
│   ├── Usuarios
│   ├── Recetas
│   ├── Composiciones
│   ├── Modelos 3D
│   └── Jobs de generación
│
└── Storage local
    ├── demo/*.glb
    └── generated/*.glb
```

## Arquitectura del backend

El backend sigue una arquitectura por capas:

```txt
Controller → Service → Repository → Database
```

Responsabilidades:

- `Controller`: recibe peticiones HTTP y devuelve respuestas de API.
- `Service`: contiene lógica de negocio.
- `Repository`: accede a PostgreSQL.
- `Entity`: representa tablas.
- `DTO`: controla datos enviados y recibidos.
- `Adapter / Client`: encapsula APIs externas como Gemini y Tripo.

Estructura principal:

```txt
back_pastry3d/src/main/java/com/backend/pastry3d

adapters/
  fairstack/
  tripo/

aio/
  gemini/

auth/
  controller/
  dto/
  entity/
  jwt/
  repository/
  service/

composition/
  controller/
  entity/
  repository/
  service/

config/

favorite/

generation/
  controller/
  entity/
  repository/
  service/

history/

modelasset/
  controller/
  entity/
  repository/

recipe/
  controller/
  dto/
  entity/
  repository/
  service/

shared/
```

## Integración con Gemini

Gemini se usa para:

- Validar que el texto del usuario sea un pedido de repostería.
- Rechazar entradas sin relación con postres.
- Rechazar contenido ofensivo, obsceno o claramente inválido.
- Generar receta estructurada.
- Proponer un plan visual.
- Crear un prompt técnico para generación 3D externa.

No se necesita una segunda API key de Gemini para el flujo de Tripo. La misma key puede usarse para validación, receta y prompt técnico.

Ejemplo de respuesta esperada cuando el usuario escribe algo inválido:

```txt
Aquí solo se generan postres y piezas de repostería. Describe un pastel, flan, cupcake, tarta, topping o decoración comestible.
```

## Integración con Tripo

Tripo se usa como proveedor externo para generar modelos 3D cuando no existe un GLB local apropiado.

Flujo recomendado:

```txt
Frontend pulsa “Generar con Tripo”
        ↓
POST /api/generation-jobs/recipes/{recipeId}/start-tripo
        ↓
Backend crea task en Tripo
        ↓
Tripo devuelve task_id
        ↓
Backend guarda GenerationJob
        ↓
Frontend consulta /sync
        ↓
Backend consulta estado en Tripo
        ↓
Si termina correctamente, descarga GLB
        ↓
Guarda archivo local
        ↓
Registra ModelAsset
```

La API de Tripo trabaja de forma asíncrona. No se debe esperar que el modelo esté listo inmediatamente después de crear la tarea.

Variables recomendadas:

```env
TRIPO_ENABLED=true
TRIPO_BASE_URL=https://api.tripo3d.ai/v2/openapi
TRIPO_API_KEY=replace-with-real-key
TRIPO_MODEL_VERSION=v3.1-20260211
TRIPO_TEXTURE=true
TRIPO_PBR=true
TRIPO_TEXTURE_QUALITY=standard
TRIPO_GEOMETRY_QUALITY=standard
TRIPO_FACE_LIMIT=30000
TRIPO_AUTO_SIZE=true
TRIPO_EXPORT_UV=true
TRIPO_SMART_LOW_POLY=false
TRIPO_QUAD=false
TRIPO_NEGATIVE_PROMPT=low quality, blurry, deformed, broken geometry, extra objects, plate, table, hands, people, text, logo, watermark
TRIPO_TIMEOUT_SECONDS=180
```

Para conservar créditos durante pruebas, se recomienda no activar inicialmente opciones caras como textura detallada, geometría detallada, quad remesh o smart low-poly.

## Patrones de software aplicados

| Patrón | Tipo | Aplicación en Pastry3D |
|---|---|---|
| Adapter | Estructural | `GeminiClient`, `TripoClient` y `FairStackClient` encapsulan APIs externas. |
| Facade | Estructural | `RecipeService`, `CompositionService` y `GenerationJobService` simplifican flujos complejos. |
| Composite | Estructural | La escena 3D combina bases, toppings y decoraciones como partes de una composición. |
| Proxy | Estructural | Caddy recibe tráfico HTTP y lo envía al frontend; Nginx del frontend puede enrutar `/api`. |
| State | Comportamiento | Jobs y recetas cambian de estado: pendiente, listo, fallido, generando o pendiente de modelo. |
| Strategy | Comportamiento | El backend decide entre usar modelo completo, componer con assets o generar un nuevo modelo. |
| Singleton | Creacional | Los servicios Spring son beans administrados por el contenedor. |

## Configuración de entorno

Crear un archivo `.env` en la raíz del despliegue.

Ejemplo base:

```env
POSTGRES_DB=pastry3d
POSTGRES_USER=pastry3d
POSTGRES_PASSWORD=change-me

DB_URL=jdbc:postgresql://postgres:5432/pastry3d
DB_USERNAME=pastry3d
DB_PASSWORD=change-me

JWT_SECRET=replace-with-a-long-random-secret-minimum-64-characters
JWT_EXPIRATION_MS=86400000

APP_ADMIN_SEED_ENABLED=true
APP_ADMIN_EMAIL=admin@pastry3d.local
APP_ADMIN_PASSWORD=Admin12345

APP_MAIL_ENABLED=false
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM=no-reply@pastry3d.local
APP_MAIL_REGISTER_CODE_EXPIRATION_MINUTES=30

GEMINI_ENABLED=true
GEMINI_API_KEY=replace-with-gemini-key
GEMINI_MODEL=gemini-1.5-flash

TRIPO_ENABLED=true
TRIPO_BASE_URL=https://api.tripo3d.ai/v2/openapi
TRIPO_API_KEY=replace-with-tripo-key
TRIPO_MODEL_VERSION=v3.1-20260211
TRIPO_TEXTURE=true
TRIPO_PBR=true
TRIPO_TEXTURE_QUALITY=standard
TRIPO_GEOMETRY_QUALITY=standard
TRIPO_FACE_LIMIT=30000
TRIPO_AUTO_SIZE=true
TRIPO_EXPORT_UV=true
TRIPO_SMART_LOW_POLY=false
TRIPO_QUAD=false
TRIPO_TIMEOUT_SECONDS=180

FAIRSTACK_ENABLED=false
FAIRSTACK_BASE_URL=
FAIRSTACK_API_KEY=

STORAGE_UPLOADS=/opt/pastry3d/uploads/models
APP_CORS_ALLOWED_ORIGINS=https://extract-lemon-suits-just.trycloudflare.com,http://20.220.27.142,http://127.0.0.1:80
APP_PUBLIC_BASE_URL=https://extract-lemon-suits-just.trycloudflare.com

VITE_API_BASE_URL=/api
PUBLIC_DOMAIN=localhost
JPA_SHOW_SQL=false
```

No subir `.env` real al repositorio.

## Despliegue con Docker Compose

Desde la raíz del proyecto en la VM:

```bash
cd /opt/pastry3d/app

docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

Verificar contenedores:

```bash
docker compose -f docker-compose.prod.yml --env-file .env ps
```

Ver logs:

```bash
docker compose -f docker-compose.prod.yml --env-file .env logs --tail=120 backend
docker compose -f docker-compose.prod.yml --env-file .env logs --tail=120 frontend
docker compose -f docker-compose.prod.yml --env-file .env logs --tail=120 caddy
```

Validar localmente:

```bash
curl -I http://127.0.0.1:80
curl -i http://127.0.0.1:80/actuator/health
```

Respuesta esperada para frontend:

```txt
HTTP/1.1 200 OK
```

## Cloudflare Quick Tunnel

El túnel expone la app local por HTTPS sin requerir DuckDNS ni dominio propio.

Instalar:

```bash
cd /tmp
wget https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64.deb
sudo dpkg -i cloudflared-linux-amd64.deb
sudo apt install -y tmux
```

Crear sesión persistente:

```bash
tmux new -s pastry-tunnel
```

Dentro de tmux:

```bash
cloudflared tunnel --protocol http2 --url http://127.0.0.1:80 2>&1 | tee -a /opt/pastry3d/logs/cloudflared.log
```

Salir sin apagar el túnel:

```txt
CTRL + B
D
```

Volver a entrar:

```bash
tmux attach -t pastry-tunnel
```

Ver URL generada:

```bash
grep -o 'https://[-a-zA-Z0-9]*\.trycloudflare\.com' /opt/pastry3d/logs/cloudflared.log | tail -1
```

Validar:

```bash
curl -I https://extract-lemon-suits-just.trycloudflare.com
```

Respuesta esperada:

```txt
HTTP/2 200
```

## Reset de base de datos antes de pruebas con Tripo

Si todavía no se han generado modelos reales con Tripo, se puede borrar la base sin perder assets generados.

Comando destructivo:

```bash
cd /opt/pastry3d/app

docker compose -f docker-compose.prod.yml --env-file .env down

docker volume rm app_pastry3d_postgres_data

docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

Si el volumen tiene otro nombre, revisar con:

```bash
docker volume ls | grep pastry
```

Advertencia: después de generar GLB reales con Tripo, no borrar la base sin respaldar `model_assets`, porque la app perdería la referencia a los archivos generados.

## Comandos útiles de operación

Revisar estado general:

```bash
docker compose -f docker-compose.prod.yml --env-file .env ps
pgrep -af cloudflared
tmux ls
```

Revisar URL de túnel:

```bash
grep -o 'https://[-a-zA-Z0-9]*\.trycloudflare\.com' /opt/pastry3d/logs/cloudflared.log | tail -1
```

Revisar modelos disponibles:

```bash
find /opt/pastry3d/app/back_pastry3d/uploads/models -type f -name "*.glb"
```

Reiniciar solo backend/frontend/caddy sin borrar datos:

```bash
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

## Validaciones de entrada

### Prompt de postre

- Máximo recomendado: 100 caracteres.
- Debe describir un postre, pastel, flan, cupcake, topping, decoración o producto de repostería.
- No debe aceptar insultos, obscenidades, contenido violento o texto sin relación con repostería.

### Registro

- Nombre máximo: 80 caracteres.
- Correo máximo: 80 caracteres.
- Contraseña máxima: 80 caracteres.
- Debe existir botón para ver u ocultar contraseña.

## Limitaciones conocidas

- La URL `trycloudflare.com` es temporal.
- Si se reinicia la VM, puede cambiar la URL del túnel.
- Los modelos GLB no se versionan necesariamente en Git por tamaño.
- La calidad de modelos generados por Tripo puede variar según el prompt.
- Las opciones avanzadas de Tripo pueden consumir más créditos.
- Si se borra PostgreSQL, la app pierde el índice de modelos generados aunque los archivos sigan en disco.
- La generación 3D externa debe ejecutarse desde backend, nunca desde frontend, para no exponer claves API.

## Recomendación de demo

Para una demostración estable:

1. Mantener la VM encendida.
2. Mantener `cloudflared` corriendo en tmux.
3. Usar solo la URL de Cloudflare Tunnel.
4. Evitar DuckDNS durante la evaluación.
5. Probar login antes de la demo.
6. Probar una receta con modelo existente.
7. Probar una receta sin modelo para mostrar el botón de Tripo.
8. No gastar créditos en múltiples generaciones si no es necesario.
9. Conservar logs del backend abiertos durante la prueba.

## Licencia y uso académico

Pastry3D es un proyecto de demostración académica y técnica. Su objetivo es mostrar una arquitectura funcional de IA aplicada a composición 3D para repostería, integrando modelos locales reutilizables y generación externa controlada.