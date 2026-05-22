# Pastry3D Backend - ejecución local/VM

## 1. Levantar PostgreSQL

```bash
docker compose up -d postgres
```

## 2. Crear carpeta para modelos demo

Local:

```bash
mkdir -p uploads/models/demo
```

VM recomendada:

```bash
sudo mkdir -p /opt/pastry3d/uploads/models/demo
sudo chown -R $USER:$USER /opt/pastry3d
```

Coloca los GLB iniciales con estos nombres para que el seed funcione:

```text
round_cake_base_vanilla.glb
caramel_rose.glb
strawberry_topping.glb
birthday_candle.glb
```

## 3. Ejecutar backend

```bash
./mvnw spring-boot:run
```

O en VM:

```bash
export STORAGE_UPLOADS=/opt/pastry3d/uploads/models
export APP_CORS_ALLOWED_ORIGINS=http://localhost:5173,https://TU-DOMINIO.duckdns.org
export JWT_SECRET=CAMBIA_ESTE_SECRETO_LARGO_DE_64_CARACTERES_MINIMO
./mvnw spring-boot:run
```

## 4. Usuario admin demo

Por defecto se crea:

```text
admin@pastry3d.local
Admin12345
```

Cambia estas variables en VM:

```bash
APP_ADMIN_EMAIL=tu_correo
APP_ADMIN_PASSWORD=tu_password_seguro
```

## 5. Flujo principal del front

```http
POST /api/auth/register
POST /api/auth/login
GET /api/auth/me
POST /api/recipes/generate
GET /api/recipes
GET /api/recipes/{id}
GET /api/models
GET /api/compositions/recipe/{recipeId}
```

Si el usuario pide un modelo existente por piezas, devuelve `READY` y `COMPOSE_SCENE`.
Si faltan piezas y FairStack está apagado, devuelve `PENDING_MANUAL_MODEL`.
