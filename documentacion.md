# Documentación de la API

API REST de ecommerce de productos tecnológicos (Fase 1). Base URL: `http://localhost:8081`

Base de datos: **MySQL** (credenciales por variables de entorno `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).

Para probar sin MySQL hay un perfil con H2 embebida:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

(la base queda en `target/h2/ecommerce.mv.db`).

Colección Postman: `collection_postman.json`.

---

## Modelo (resumen)

- Un producto puede pertenecer a **varias categorías** (N:N). Al crear/actualizar se manda `categoriaIds` (lista). Compatibilidad: si solo llega `categoriaId`, se trata como lista de un elemento.
- Cada producto tiene una **marca** (`marcaId`) y un **vendedor** (`vendedorId` al crear).
- Solo el vendedor puede **modificar** o **dar de baja** el producto: en PUT/DELETE se manda `usuarioId` como query param → **403** si no coincide.
- En las respuestas de producto: `categoriaIds`, `marcaId` / `marcaNombre`, `vendedorId` / `vendedorUsername`, más la galería `imagenes`.
- El **catálogo** (`GET /api/productos` y `GET /api/categorias/{id}/productos`) devuelve **solo productos activos**, ordenados **A–Z por nombre**. Un producto dado de baja no aparece en catálogo ni en detalle (**404**).
- Cada producto tiene al menos una **imagen** (URLs; no hay upload de archivos).
- Categorías y marcas exponen `categoriaId` / `marcaId` (no `id`).
- Hay **carrito** por usuario y **checkout** que crea un **pedido** (transaccional: si falla el stock de un ítem, no se descuenta ninguno).

---

## Códigos de respuesta

| Código | Cuándo |
|--------|--------|
| **200** | GET ok, PUT ok, login ok, operaciones sobre carrito (agregar / eliminar ítem / vaciar) |
| **201** | POST create (usuario, producto, categoría, marca) + **checkout** + header `Location` |
| **204** | Listas vacías, DELETE producto (baja lógica) |
| **400** | Validación / reglas de negocio (`ArgumentInvalidException` o `@Valid`) |
| **401** | Login inválido (`CredencialesInvalidasException`) |
| **403** | No es dueño del producto (`ForbiddenException`) |
| **404** | Recurso inexistente (también producto inactivo en catálogo/detalle) |
| **409** | Duplicados (email, username, sku, nombre categoría/marca) |
| **500** | Error inesperado |

## Formato de errores

Las excepciones las captura el `GlobalExceptionHandler` (`@RestControllerAdvice`):

```json
{
  "timestamp": "2026-09-03T21:00:39.699",
  "status": 404,
  "error": "Not Found",
  "message": "El usuario con id 9999 no existe",
  "path": "/api/usuarios/9999"
}
```

Con detalle por campo:

```json
{
  "timestamp": "2026-09-03T21:00:31.178",
  "status": 400,
  "error": "Bad Request",
  "message": "Hay campos inválidos en la petición",
  "path": "/api/usuarios/registro",
  "errores": {
    "email": "El email no tiene un formato válido",
    "password": "La contraseña debe tener entre 8 y 72 caracteres",
    "username": "El username es obligatorio"
  }
}
```

### Excepciones personalizadas

| Excepción | HTTP | Uso |
|-----------|------|-----|
| `ResourceNotFoundException` | 404 | Producto, categoría, marca, usuario, carrito, etc. |
| `ArgumentInvalidException` | 400 | Reglas de negocio / campos obligatorios |
| `CredencialesInvalidasException` | 401 | Login fallido |
| `ForbiddenException` | 403 | Modificar/eliminar producto de otro usuario |
| `DuplicateResourceException` | 409 | Email, username, SKU o nombre ya usados |

---

## Categorías

Respuestas con `CategoriaResponse` (`categoriaId`, no `id`).

### Listar categorías

`GET /api/categorias` → **200** o **204** si no hay ninguna.

```json
[
  {
    "categoriaId": 1,
    "nombre": "Notebooks",
    "descripcion": "Laptops para trabajo, estudio y gaming",
    "activo": true
  }
]
```

### Obtener una categoría

`GET /api/categorias/{id}` → **200**, o **404** si no existe.

### Crear una categoría

`POST /api/categorias` → **201** + `Location: /api/categorias/{id}`

```json
{
  "nombre": "Notebooks",
  "descripcion": "Laptops para trabajo, estudio y gaming"
}
```

| Situación | Código |
|-----------|--------|
| Falta el nombre | 400 |
| El nombre ya existe (case-insensitive) | 409 |

### Actualizar una categoría

`PUT /api/categorias/{id}`

```json
{
  "nombre": "Notebooks y Ultrabooks",
  "descripcion": "Laptops para trabajo, estudio y gaming",
  "activo": true
}
```

→ **200**. Id inexistente → **404**. Nombre duplicado → **409**.

### Listar productos de una categoría

`GET /api/categorias/{id}/productos`

Categoría inexistente → **404**. Solo **activos**, orden A–Z. Cuerpo = lista de `ProductoResponse` (igual que el catálogo), o **204** si no hay.

---

## Marcas

Respuestas con `MarcaResponse` (`marcaId`, no `id`).

### Listar / obtener / crear / actualizar

| Método | Ruta |
|--------|------|
| GET | `/api/marcas` |
| GET | `/api/marcas/{id}` |
| POST | `/api/marcas` |
| PUT | `/api/marcas/{id}` |

**Body create/update**

```json
{
  "nombre": "Apple",
  "activo": true
}
```

**Respuesta ejemplo**

```json
{
  "marcaId": 1,
  "nombre": "Apple",
  "activo": true
}
```

Mismas reglas: nombre obligatorio (400), duplicado (409), id inexistente (404). Create → **201** + `Location`.

---

## Productos

### Listar productos (catálogo)

`GET /api/productos` → activos, A–Z, o **204**.

```json
[
  {
    "id": 1,
    "nombre": "MacBook Air 13 M3",
    "descripcion": "Chip M3, 16 GB RAM, 512 GB SSD",
    "precio": 1899999.00,
    "stock": 8,
    "sku": "MBA-M3-512",
    "activo": true,
    "categoriaIds": [1, 2],
    "marcaId": 1,
    "marcaNombre": "Apple",
    "vendedorId": 1,
    "vendedorUsername": "pedro_m",
    "imagenes": [
      { "id": 1, "url": "https://cdn.ejemplo.com/mba-m3/frente.jpg", "orden": 0, "principal": true },
      { "id": 2, "url": "https://cdn.ejemplo.com/mba-m3/lateral.jpg", "orden": 1, "principal": false }
    ],
    "createdAt": "2026-09-03T19:00:00",
    "updatedAt": "2026-09-03T19:00:00"
  }
]
```

`imagenes` ordenada por `orden`; siempre hay exactamente una con `principal: true`.

### Obtener un producto

`GET /api/productos/{id}` → **200**, o **404** si no existe / está inactivo. Id no numérico → **400**.

### Crear un producto

`POST /api/productos` → **201** + `Location: /api/productos/{id}`

Obligatorios: `nombre`, `sku`, `precio`, `stock`, `categoriaIds` (al menos una), `marcaId`, `vendedorId`, `imagenes` (al menos una).

```json
{
  "nombre": "MacBook Air 13 M3",
  "descripcion": "Chip M3, 16 GB RAM, 512 GB SSD",
  "precio": 1899999,
  "stock": 8,
  "sku": "MBA-M3-512",
  "categoriaIds": [1, 2],
  "marcaId": 1,
  "vendedorId": 1,
  "imagenes": [
    { "url": "https://cdn.ejemplo.com/mba-m3/frente.jpg", "principal": true },
    { "url": "https://cdn.ejemplo.com/mba-m3/lateral.jpg" }
  ]
}
```

Notas:

- Se acepta el viejo `categoriaId` (singular) si no mandás `categoriaIds`.
- De cada imagen solo `url` es obligatoria. Sin `orden` → posición en la lista. Sin `principal: true` → la primera es portada.

| Situación | Código |
|-----------|--------|
| Falta nombre / sku / marca / vendedor / imágenes / categorías | 400 |
| Precio ≤ 0 o stock negativo | 400 |
| Categoría, marca o vendedor inexistente | 404 |
| SKU duplicado | 409 |

### Actualizar un producto / stock

Actualización **parcial**. Solo el vendedor:  
`PUT /api/productos/{id}?usuarioId={usuarioId}`

Ejemplos de body: `{"stock": 5}`, `{"precio": 1799999, "stock": 5}`, o reemplazo de `imagenes` / `categoriaIds`.

Si mandás `imagenes`, reemplaza la galería completa. `"imagenes": []` → **400**.

| Situación | Código |
|-----------|--------|
| Falta `usuarioId` / usuario inexistente | 400 / 404 |
| No es el vendedor | **403** |
| Producto inexistente | 404 |
| SKU duplicado | 409 |

### Eliminar un producto (baja lógica)

`DELETE /api/productos/{id}?usuarioId={usuarioId}` → **204**

Marca `activo: false`; sale del catálogo/detalle. Idempotente (ya inactivo → 204). Mismas reglas de dueño (**403**).

---

## Usuarios

Registro con **username**, **fecha de nacimiento** y **sexo**. Contraseña con BCrypt; nunca se devuelve.

Valores de `sexo`: `MASCULINO`, `FEMENINO`, `OTRO`, `PREFIERO_NO_DECIR`.  
`fechaNacimiento`: `yyyy-MM-dd`.

### Registrar

`POST /api/usuarios/registro` → **201** + `Location`

```json
{
  "nombre": "Pedro",
  "apellido": "Marzano",
  "username": "pedro_m",
  "email": "pedro@uade.edu.ar",
  "password": "password123",
  "fechaNacimiento": "1999-05-20",
  "sexo": "MASCULINO"
}
```

**Respuesta**

```json
{
  "id": 1,
  "nombre": "Pedro",
  "apellido": "Marzano",
  "username": "pedro_m",
  "email": "pedro@uade.edu.ar",
  "fechaNacimiento": "1999-05-20",
  "edad": 27,
  "sexo": "MASCULINO",
  "activo": true,
  "createdAt": "2026-09-03T21:00:30.882"
}
```

| Situación | Código |
|-----------|--------|
| Validación de campos (`@Valid`) | 400 |
| Menor de 13 años | 400 |
| Email o username ya usados | 409 |

### Login

`POST /api/usuarios/login` → **200**, o **401** si email/contraseña no coinciden.

```json
{
  "email": "pedro@uade.edu.ar",
  "password": "password123"
}
```

```json
{
  "mensaje": "Login exitoso",
  "usuario": { "id": 1, "username": "pedro_m", "email": "pedro@uade.edu.ar", "...": "..." }
}
```

### Listar / obtener / buscar

| Método | Ruta |
|--------|------|
| GET | `/api/usuarios` |
| GET | `/api/usuarios/{id}` |
| GET | `/api/usuarios/buscar?email=...` |
| GET | `/api/usuarios/buscar?username=...` |

Hay que mandar **email o username** (uno de los dos). Sin ninguno → **400**. No existe → **404**.

### Productos del vendedor

`GET /api/usuarios/{id}/productos` → lista de `ProductoResponse` (todos los del vendedor, no solo activos del catálogo público), o **204**. Usuario inexistente → **404**.

---

## Carrito

El carrito activo se obtiene o crea al operar por usuario.

### Agregar ítem

`POST /api/carritos/usuarios/{usuarioId}/items` → **200**

```json
{
  "productoId": 1,
  "cantidad": 2
}
```

Valida producto activo, stock > 0, cantidad ≥ 1 y que la suma en carrito no supere el stock.

### Obtener carrito

`GET /api/carritos/usuarios/{usuarioId}` → **200**

```json
{
  "id": 1,
  "estado": "ACTIVO",
  "usuarioId": 1,
  "guestToken": null,
  "items": [
    {
      "id": 10,
      "productoId": 1,
      "productoNombre": "MacBook Air 13 M3",
      "cantidad": 2,
      "stockDisponible": 8,
      "precioReferencia": 1899999.00,
      "subtotal": 3799998.00
    }
  ],
  "total": 3799998.00,
  "createdAt": "2026-09-09T20:00:00",
  "updatedAt": "2026-09-09T20:05:00"
}
```

### Eliminar un ítem / vaciar

| Método | Ruta | Respuesta |
|--------|------|-----------|
| DELETE | `/api/carritos/usuarios/{usuarioId}/items/{itemId}` | **200** carrito actualizado |
| DELETE | `/api/carritos/usuarios/{usuarioId}/items` | **200** carrito vacío |

### Checkout

`POST /api/carritos/{id}/checkout` → **201 Created**

Header: `Location: /api/pedidos/{id}`

Revalida stock de **todos** los ítems; si uno falla → **400** y **no descuenta stock de ninguno** (transacción). Crea `Pedido` + `DetallePedido`, descuenta stock y deja el carrito en estado finalizado.

```json
{
  "id": 1,
  "numero": "PED-...",
  "estado": "CONFIRMADO",
  "usuarioId": 1,
  "detalles": [
    {
      "id": 1,
      "productoId": 1,
      "productoNombre": "MacBook Air 13 M3",
      "cantidad": 2,
      "precioUnitario": 1899999.00,
      "subtotal": 3799998.00
    }
  ],
  "subtotal": 3799998.00,
  "total": 3799998.00,
  "createdAt": "2026-09-09T20:10:00"
}
```

| Situación | Código |
|-----------|--------|
| Carrito inexistente | 404 |
| Carrito vacío o ya procesado | 400 |
| Stock insuficiente / producto inactivo | 400 |

> Nota: el header `Location` apunta a `/api/pedidos/{id}`; en esta fase **no hay** `GET /api/pedidos/{id}` implementado (el pedido ya viene en el body del checkout).

---

## Flujo sugerido

1. `POST /api/usuarios/registro` (con `username`)
2. `POST /api/usuarios/login` (opcional, para verificar credenciales)
3. `POST /api/categorias` y `POST /api/marcas`
4. `POST /api/productos` con `categoriaIds`, `marcaId`, `vendedorId` e `imagenes`
5. `PUT /api/productos/{id}?usuarioId=...` / `DELETE ...?usuarioId=...` (solo el vendedor)
6. `GET /api/productos` o `GET /api/categorias/{id}/productos` (catálogo)
7. `POST /api/carritos/usuarios/{usuarioId}/items` → armar carrito
8. `POST /api/carritos/{id}/checkout` → **201** + pedido
