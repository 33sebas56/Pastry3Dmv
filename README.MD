# Pastry3D

Pastry3D es una aplicación web de demostración que genera recetas de postres con inteligencia artificial y crea una representación visual en 3D usando modelos GLB reutilizables.

El sistema permite que un usuario escriba un pedido como “flan napolitano”, “pastel de chocolate” o “torta con rosas y fresas”. A partir de ese texto, Gemini genera una receta estructurada y un plan visual. Luego el backend busca modelos 3D disponibles en la biblioteca local y arma una escena usando modelos completos, bases y toppings.

## URL de demostración

http://pastry3d-sebas.duckdns.org

Nota: HTTPS quedó preparado con Caddy, pero durante la demo se mantiene HTTP porque Let’s Encrypt bloqueó temporalmente la emisión del certificado por varios intentos fallidos relacionados con validación DNS de DuckDNS.

## Objetivo del proyecto

El objetivo de Pastry3D es demostrar una arquitectura funcional donde la IA no genera todo desde cero de manera costosa, sino que interpreta el pedido del usuario y reutiliza modelos 3D existentes para construir una escena visual.

La idea central es:

```txt
Pedido del usuario
        ↓
Gemini interpreta receta y plan visual
        ↓
Backend busca modelos GLB disponibles
        ↓
Sistema compone postre 3D
        ↓
Usuario visualiza receta, escena e historial
```

## Funcionalidades principales

- Registro de usuarios.
- Confirmación de cuenta por correo electrónico.
- Inicio de sesión con JWT.
- Generación de recetas con Gemini.
- Historial de recetas por usuario.
- Visualización 3D con modelos GLB.
- Reutilización de modelos locales.
- Composición de bases y toppings.
- Modelos completos para demostración visual.
- Detección de modelos faltantes.
- Módulo experimental de generación externa mediante jobs.
- Despliegue con Docker Compose en máquina virtual.

## Tecnologías usadas

### Backend

- Java 21
- Spring Boot 3
- Spring Security
- JWT
- Spring Data JPA
- PostgreSQL 16
- Flyway
- Java Mail / SMTP
- Gemini API
- Docker

### Frontend

- React
- Vite
- Three.js
- React Three Fiber
- Drei
- React Router
- Lucide React
- CSS personalizado

### Infraestructura

- Ubuntu Server
- Docker Compose
- PostgreSQL en contenedor
- Caddy como proxy
- DuckDNS
- Almacenamiento local para modelos GLB

## Arquitectura general

```txt
Pastry3D
│
├── Frontend React
│   ├── Pantallas de autenticación
│   ├── Formulario de generación
│   ├── Historial
│   └── Visor 3D
│
├── Backend Spring Boot
│   ├── Autenticación
│   ├── Recetas
│   ├── Composición 3D
│   ├── Assets GLB
│   ├── Jobs de generación externa
│   └── Integración con Gemini
│
├── PostgreSQL
│   ├── Usuarios
│   ├── Recetas
│   ├── Composiciones
│   ├── Modelos 3D
│   └── Jobs
│
└── Storage local
    └── Archivos .glb
```

## Arquitectura del backend

El backend sigue una arquitectura por capas:

```txt
Controller → Service → Repository → Database
```

Esta separación permite mantener el código organizado y dividir responsabilidades:

- Controller: recibe peticiones HTTP.
- Service: contiene la lógica de negocio.
- Repository: accede a la base de datos.
- Entity: representa las tablas.
- DTO: controla los datos enviados y recibidos por la API.

### Estructura principal

```txt
back_pastry3d/src/main/java/com/backend/pastry3d

auth/
  controller
  dto
  entity
  jwt
  repository
  service

recipe/
  controller
  dto
  entity
  repository
  service

composition/
  entity
  repository
  service

modelasset/
  controller
  entity
  repository

generation/
  controller
  entity
  repository
  service

ai/
  gemini

adapters/
  fairstack

config/
shared/
```

## Patrones de software aplicados

En la asignatura se trabajan patrones creacionales, estructurales y de comportamiento. En Pastry3D no se implementan todos los patrones existentes, pero sí se aplican varios conceptos de forma real dentro de la arquitectura.

### Tabla de patrones usados

| Patrón | Tipo | Aplicación en Pastry3D |
|---|---|---|
| Adapter | Estructural | `GeminiClient` y `FairStackClient` aíslan la comunicación con APIs externas. El resto del sistema no depende directamente del formato interno de esos proveedores. |
| Facade | Estructural | Servicios como `RecipeService`, `CompositionService` y `GenerationJobService` simplifican operaciones complejas para los controllers. |
| Composite | Estructural | La escena 3D se construye combinando una base con varios toppings. Cada elemento forma parte de una composición visual. |
| Proxy | Estructural | Caddy funciona como proxy de entrada hacia el frontend. También se usa `/api` para ocultar la ruta interna del backend al usuario. |
| State | Comportamiento | Recetas, composiciones y jobs cambian de estado: `PENDING`, `READY`, `FAILED`, `PENDING_MANUAL_MODEL`, `GENERATING_MODEL`. |
| Strategy | Comportamiento | El backend decide entre usar un modelo completo, componer con assets existentes o crear un job pendiente para generación externa. |
| Singleton | Creacional | Los servicios administrados por Spring Boot funcionan como beans compartidos por el contenedor, normalmente con una única instancia durante la ejecución. |

## Relación con los patrones vistos en clase

### Patrones creacionales

Los patrones creacionales se enfocan en la forma de crear objetos. En Pastry3D se evidencia principalmente:

#### Singleton

Spring Boot administra servicios como:

```txt
RecipeService
CompositionService
GenerationJobService
AuthService
GeminiClient
```

Estos servicios son beans reutilizados por la aplicación. No se crean manualmente en cada clase, sino que Spring los inyecta cuando son necesarios.

#### Builder, uso conceptual

Aunque no se implementa una clase formal llamada `Builder`, el sistema construye objetos complejos paso a paso, especialmente:

```txt
RecipeGenerateResponse
RecipeComposition
SceneJson
VisualPlan
```

Por ejemplo, para generar una receta se construyen varias partes:

```txt
receta
ingredientes
pasos
plan visual
assets requeridos
escena 3D
```

No se afirma como patrón Builder formal, sino como una construcción progresiva de objetos complejos.

### Patrones estructurales

Los patrones estructurales son los más visibles en el proyecto porque Pastry3D conecta frontend, backend, base de datos, modelos GLB, servicios externos y proxy.

#### Adapter

Se usa para adaptar APIs externas al sistema interno.

Ejemplo:

```txt
GeminiClient
FairStackClient
```

El backend no necesita saber todos los detalles de la API externa. Solo llama métodos como:

```txt
generateRecipePlan()
startTextTo3D()
```

Esto permite cambiar el proveedor externo sin reescribir toda la lógica del sistema.

#### Facade

Los servicios funcionan como fachadas porque ocultan operaciones internas complejas.

Ejemplo en `RecipeService`:

```txt
recibir prompt
llamar Gemini
guardar receta
buscar modelos
crear composición
devolver respuesta al frontend
```

El controller no necesita conocer cada paso interno. Solo llama al servicio.

#### Composite

La composición 3D funciona como un patrón Composite conceptual.

Una escena puede estar formada por:

```txt
base de torta
rosa decorativa
fresas
arándanos
crema
salsa de chocolate
```

Cada pieza es un asset, pero visualmente todas forman un único postre.

#### Proxy

Caddy actúa como proxy de entrada.

El usuario entra a:

```txt
http://pastry3d-sebas.duckdns.org
```

Pero internamente Caddy envía la petición al contenedor frontend.

Además, el frontend consume:

```txt
/api
/uploads
```

sin exponer directamente los detalles internos del backend.

### Patrones de comportamiento

Estos patrones aparecen en la gestión de estados y decisiones del sistema.

#### State

El sistema cambia de comportamiento según el estado de una receta o job.

Estados usados:

```txt
PENDING
READY
FAILED
PENDING_MANUAL_MODEL
GENERATING_MODEL
```

Ejemplo:

```txt
Si la receta está READY:
    se muestra la escena 3D.

Si está PENDING_MANUAL_MODEL:
    se muestra que faltan modelos.

Si el job está FAILED:
    se muestra el error del proveedor externo.
```

#### Strategy

El backend decide qué estrategia aplicar según los modelos disponibles.

Estrategias principales:

```txt
Usar modelo completo
Componer base + toppings
Crear job pendiente de generación externa
```

Ejemplo:

```txt
flan napolitano
→ usa modelo completo

torta con rosas y fresas
→ usa base + toppings

postre con dragón dorado
→ detecta modelo faltante y crea job pendiente
```

## Patrones no implementados formalmente

Aunque el PDF incluye más patrones, no todos se implementan directamente en Pastry3D.

No se afirma uso formal de:

```txt
Abstract Factory
Factory Method
Prototype
Bridge
Decorator
Flyweight
Chain of Responsibility
Command
Interpreter
Mediator
Iterator
Memento
Observer
Template Method
Visitor
```

Algunos podrían agregarse en versiones futuras, pero no conviene decir que ya están implementados si no existe una estructura clara de código que lo demuestre.

## Patrones arquitectónicos adicionales

Además de los patrones clásicos, Pastry3D usa patrones comunes en desarrollo backend.

| Patrón | Aplicación |
|---|---|
| Layered Architecture | Separación en controller, service, repository y entity. |
| Repository Pattern | Acceso a datos mediante interfaces JPA como `RecipeRepository`, `ModelAssetRepository` y `GenerationJobRepository`. |
| DTO Pattern | Separación entre entidades internas y respuestas enviadas al frontend. |
| Migration Pattern | Control de cambios de base de datos con Flyway. |
| Job Pattern | Registro y seguimiento de procesos externos con `GenerationJob`. |
| Externalized Configuration | Uso de variables de entorno para claves, base de datos, SMTP, Gemini y dominio público. |

## Flujo de generación de receta

```txt
1. El usuario escribe un pedido.
2. El frontend envía el prompt al backend.
3. Gemini genera:
   - receta
   - tipo de postre
   - ingredientes
   - pasos
   - plan visual
   - assets requeridos
4. El backend busca modelos GLB disponibles.
5. Si encuentra un modelo completo, lo reutiliza.
6. Si encuentra base y toppings, compone una escena.
7. Si falta un modelo, registra un job pendiente.
8. El usuario ve receta, escena 3D e historial.
```

## Tipos de modelos 3D

Los modelos se guardan en:

```txt
back_pastry3d/uploads/models/demo
```

### Modelos completos

Estos modelos se usan para mostrar postres ya generados o preparados como pieza completa.

```txt
flan_napolitano_complete.glb
chocolate_cake_complete.glb
strawberry_cake_complete.glb
donut_glazed_complete.glb
ice_cream_cup_complete.glb
```

Uso esperado:

```txt
flan napolitano
pastel de chocolate
pastel de fresa
dona glaseada
copa de helado
```

### Bases combinables

Estos modelos sirven como estructura principal para composiciones dinámicas.

```txt
round_cake_base_vanilla.glb
milhojas_base_classic.glb
cupcake_base_vanilla.glb
cheesecake_base_plain.glb
tart_base_vanilla.glb
```

### Toppings y decoraciones

```txt
caramel_rose_cluster.glb
strawberry_topping.glb
blueberry_topping.glb
cherry_topping.glb
cream_swirl.glb
birthday_candle.glb
macaron_topping.glb
chocolate_bar_piece.glb
chocolate_drizzle.glb
```

Ejemplos de composición:

```txt
torta con rosas y fresas
→ round_cake_base_vanilla + caramel_rose_cluster + strawberry_topping

torta con cerezas y arándanos
→ round_cake_base_vanilla + cherry_topping + blueberry_topping

milhojas con rosas azules
→ milhojas_base_classic + caramel_rose_cluster con color azul
```

## Base de datos

La base de datos usa PostgreSQL y se versiona con Flyway.

Las migraciones están en:

```txt
back_pastry3d/src/main/resources/db/migration
```

Migraciones principales:

```txt
V1__init_schema.sql
V2__add_auth_confirmation.sql
V3__add_model_assets.sql
V4__add_recipe_history.sql
V5__add_generation_jobs.sql
V6__seed_demo_model_assets.sql
V7__create_compositions_table.sql
V8__seed_showcase_complete_models_and_toppings.sql
```

## Entidades principales

```txt
User
Recipe
RecipeComposition
ModelAsset
ModelColorVariant
GenerationJob
Favorite
```

## Estados usados

### Recetas y composiciones

```txt
PENDING
READY
FAILED
PENDING_MANUAL_MODEL
```

### Jobs de generación externa

```txt
PENDING_MANUAL_MODEL
GENERATING_MODEL
READY
FAILED
```

## Integración con Gemini

Gemini se usa para interpretar el pedido del usuario y generar una salida estructurada.

Gemini devuelve:

```txt
recipe
visualPlan
dessertType
requiredAssets
modelPrompt
```

Ejemplo:

```txt
Usuario:
torta con rosas y fresas

Gemini:
- receta de torta
- dessertType: round_cake
- requiredAssets:
  - caramel_rose
  - strawberry_topping
```

## Generación externa experimental

El sistema tiene preparada una integración para generación externa de modelos 3D mediante jobs.

El flujo preparado es:

```txt
1. El backend detecta que falta un modelo.
2. Se crea un job pendiente.
3. El usuario puede intentar generar desde cero.
4. El proveedor externo responde.
5. Si se obtiene un GLB, se guarda localmente.
6. El modelo queda disponible para futuras composiciones.
```

Durante el desarrollo se probó FairStack, pero su endpoint 3D respondió que la modalidad `threeD` no estaba habilitada en la API activa. Por eso esta función queda como módulo experimental y desacoplado. La funcionalidad principal del proyecto no depende de ese proveedor.

## Variables de entorno principales

El archivo `.env` configura:

```txt
PostgreSQL
JWT
SMTP
Gemini
Proveedor externo experimental
Storage local de GLB
CORS
Dominio público
```

Ejemplo:

```env
PUBLIC_DOMAIN=pastry3d-sebas.duckdns.org
APP_PUBLIC_BASE_URL=http://pastry3d-sebas.duckdns.org
APP_CORS_ALLOWED_ORIGINS=http://pastry3d-sebas.duckdns.org
VITE_API_BASE_URL=/api
```

## Despliegue

El despliegue se realiza con Docker Compose:

```bash
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

Verificación básica:

```bash
docker ps
curl http://localhost:8080/actuator/health
curl -I http://localhost
```

## Servicios en Docker

```txt
postgres
backend
frontend
caddy
```

En la configuración actual, Caddy expone el acceso público. Backend y PostgreSQL quedan dentro de la red Docker.

## Prompts recomendados para la demo

```txt
flan napolitano
pastel de chocolate
pastel de fresa
dona glaseada
copa de helado
torta con rosas y fresas
torta con cerezas y arándanos
milhojas con rosas azules
```

## Cómo argumentar el proyecto

Pastry3D no busca reemplazar por completo el modelado 3D profesional. Su propósito es demostrar una arquitectura en la que la IA interpreta el pedido del usuario y el sistema reutiliza assets 3D disponibles para construir una experiencia visual.

El valor principal está en:

```txt
interpretación con IA
reutilización de modelos
composición visual
historial personalizado
arquitectura modular
posibilidad de conectar proveedores externos
```

Argumento recomendado:

```txt
Pastry3D demuestra cómo una IA puede transformar un pedido textual en una receta y en una escena visual 3D. En lugar de generar todos los modelos desde cero, el sistema reutiliza modelos GLB existentes, lo cual es más realista para una aplicación web de demostración. Cuando un modelo no existe, el sistema no se rompe: registra un job pendiente y deja preparada la integración con proveedores externos.
```

## Estado actual

La aplicación está funcional para demostración:

- Frontend desplegado.
- Backend desplegado.
- Base de datos funcionando.
- SMTP funcionando.
- Gemini funcionando.
- Modelos GLB cargando correctamente.
- Historial funcionando.
- Composición 3D funcional.
- Modelos completos de demostración cargados.
- Toppings combinables funcionando.
- Proveedor externo preparado como módulo experimental.

## Limitaciones conocidas

- La generación externa de modelos 3D depende de disponibilidad real del proveedor.
- Algunos modelos GLB pueden requerir calibración visual adicional.
- Las posiciones de toppings son aproximadas.
- HTTPS está preparado, pero en la demo actual puede mantenerse HTTP por validación temporal de certificado.
- La calidad visual depende de los modelos GLB cargados en la biblioteca.

## Futuras mejoras

- Ajustar coordenadas por tipo de postre.
- Agregar más bases y toppings reutilizables.
- Permitir administración visual de modelos GLB.
- Conectar otro proveedor real de generación texto a 3D.
- Agregar panel de administración de assets.
- Mejorar materiales y recoloración avanzada.
- Implementar HTTPS definitivo cuando DuckDNS y Let’s Encrypt validen correctamente.
- Añadir métricas de uso y recetas más populares.

## Conclusión

Pastry3D demuestra una solución funcional basada en inteligencia artificial, arquitectura por capas, patrones de diseño y visualización 3D.

El sistema integra backend, frontend, base de datos, modelos GLB, autenticación, correo y despliegue en VM, manteniendo una estructura modular que permite ampliar proveedores, modelos y reglas visuales sin reescribir toda la aplicación.