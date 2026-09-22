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

## Autenticación (JWT)

La API usa **Spring Security** con **JWT**. El flujo es:

1. `POST /api/usuarios/registro` → crea la cuenta con rol `USER` y la contraseña hasheada con BCrypt.
2. `POST /api/usuarios/login` → devuelve un **token**.
3. En todos los demás requests se manda ese token en un header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9....
```

El token dura 24 h por defecto (`app.jwt.expiration-ms`) y se firma con `app.jwt.secret`
(variables de entorno `JWT_EXPIRATION_MS` y `JWT_SECRET`). Si la clave cambia, los tokens
emitidos antes dejan de valer.

La API es **stateless**: no hay sesión ni cookies, cada request se identifica sola con su token.
Por eso CSRF está desactivado.

### Roles

| Rol | Qué puede hacer |
|-----|-----------------|
| `USER` | Es el rol por defecto al registrarse. Publica, edita y da de baja **sus** productos, opera **su** carrito y ve **sus** pedidos. |
| `ADMIN` | Además, administra el ABM de categorías y marcas y puede consultar cualquier pedido. |
| `VENDEDOR` | Está definido en el enum `NombreRol` y reservado para más adelante; hoy no se asigna. |

> No hay endpoint para darse rol `ADMIN`: se asigna en la base (tabla `usuario_roles`).

### Matriz de acceso

| Ruta | Método | Quién |
|------|--------|-------|
| `/api/usuarios/registro` | POST | Público |
| `/api/usuarios/login` | POST | Público |
| `/api/productos`, `/api/productos/{id}` | GET | Público |
| `/api/categorias/**`, `/api/marcas/**` | GET | Público |
| `/api/categorias/**`, `/api/marcas/**` | POST / PUT / DELETE | `ADMIN` |
| `/api/usuarios` (listado completo) | GET | `ADMIN` |
| Todo el resto (publicar producto, carrito, checkout, pedidos) | — | Autenticado |

Dentro de lo autenticado hay una segunda regla: **ser el dueño del recurso**. Editar el producto
de otro, mirar el carrito de otro o leer los pedidos de otro dan **403** aunque el token sea válido.

### Respuestas de seguridad

| Código | Cuándo |
|--------|--------|
| **401** | No mandaste token, o el token es inválido, está vencido o el usuario está dado de baja |
| **403** | El token es válido pero el recurso es de otro usuario, o tu rol no alcanza |

Los dos salen con el mismo `ErrorResponse` que el resto de la API: los errores que lanza la
cadena de filtros de Spring Security se redirigen al `GlobalExceptionHandler`.

---

## Modelo (resumen)

- Un producto puede pertenecer a **varias categorías** (N:N). Al crear/actualizar se manda `categoriaIds` (lista). Compatibilidad: si solo llega `categoriaId`, se trata como lista de un elemento.
- Cada producto tiene una **marca** (`marcaId`) y un **vendedor** (`vendedorId` al crear).
- Solo el vendedor puede **modificar** o **dar de baja** el producto: el dueño sale del **token**, no de un query param → **403** si no coincide.
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
| **401** | Login inválido, o falta el token / está vencido / es inválido |
| **403** | El recurso es de otro usuario (`ForbiddenException`) o el rol no alcanza (`AccessDeniedException`) |
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
| `ForbiddenException` | 403 | Operar sobre un producto, carrito o pedido de otro usuario |
| `AccessDeniedException` (Spring Security) | 403 | El rol no alcanza para la ruta |
| `AuthenticationException` (Spring Security) | 401 | Sin token, token inválido o vencido |
| `DuplicateResourceException` | 409 | Email, username, SKU o nombre ya usados |

---

## Categorías

Respuestas con `CategoriaResponse` (`categoriaId`, no `id`).

> Los **GET son públicos**. `POST` y `PUT` piden token con rol **`ADMIN`**: sin token → **401**, con token sin rol → **403**.

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

> Mismas reglas que categorías: **GET público**, `POST` / `PUT` solo **`ADMIN`**.

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

> `GET` es público. `POST`, `PUT` y `DELETE` piden token: el **vendedor es el usuario del token**.

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

Requiere `Authorization: Bearer <token>`. El **vendedor es el usuario autenticado**: ya no se manda
`vendedorId` en el body (si lo mandás, se ignora).

Obligatorios: `nombre`, `sku`, `precio`, `stock`, `categoriaIds` (al menos una), `marcaId`, `imagenes` (al menos una).

```json
{
  "nombre": "MacBook Air 13 M3",
  "descripcion": "Chip M3, 16 GB RAM, 512 GB SSD",
  "precio": 1899999,
  "stock": 8,
  "sku": "MBA-M3-512",
  "categoriaIds": [1, 2],
  "marcaId": 1,
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
| Sin token | **401** |
| Falta nombre / sku / marca / imágenes / categorías | 400 |
| Precio ≤ 0 o stock negativo | 400 |
| Categoría o marca inexistente | 404 |
| SKU duplicado | 409 |

### Actualizar un producto / stock

Actualización **parcial**. Solo el vendedor, identificado por el **token**:  
`PUT /api/productos/{id}`

El producto **no cambia de dueño** desde la API: mandar `vendedorId` en el body no hace nada.

Ejemplos de body: `{"stock": 5}`, `{"precio": 1799999, "stock": 5}`, o reemplazo de `imagenes` / `categoriaIds`.

Si mandás `imagenes`, reemplaza la galería completa. `"imagenes": []` → **400**.

| Situación | Código |
|-----------|--------|
| Sin token o token inválido | **401** |
| No es el vendedor | **403** |
| Producto inexistente | 404 |
| SKU duplicado | 409 |

### Eliminar un producto (baja lógica)

`DELETE /api/productos/{id}` → **204** (con `Authorization: Bearer <token>`)

Marca `activo: false`; sale del catálogo/detalle. Idempotente (ya inactivo → 204).
Sin token → **401**; si el producto es de otro → **403**.

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
  "createdAt": "2026-09-03T21:00:30.882",
  "roles": ["USER"]
}
```

Todo usuario nuevo queda con rol **`USER`**. La contraseña se guarda hasheada con BCrypt
(`PasswordEncoder` declarado como `@Bean` en `SecurityConfig`) y nunca se devuelve.

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
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJwZWRyby...",
  "tipo": "Bearer",
  "expiraEnMs": 86400000,
  "usuario": { "id": 1, "username": "pedro_m", "email": "pedro@uade.edu.ar", "roles": ["USER"], "...": "..." }
}
```

El `token` es lo que hay que guardar: va en el header `Authorization: Bearer <token>` de todos
los endpoints protegidos.

La autenticación la hace el `AuthenticationManager` de Spring Security. Los tres motivos de
fallo —email inexistente, contraseña incorrecta y **usuario dado de baja**— devuelven el mismo
**401**, para no revelar cuál fue.

### Listar / obtener / buscar

| Método | Ruta | Quién |
|--------|------|-------|
| GET | `/api/usuarios` | **`ADMIN`** |
| GET | `/api/usuarios/{id}` | Autenticado |
| GET | `/api/usuarios/buscar?email=...` | Autenticado |
| GET | `/api/usuarios/buscar?username=...` | Autenticado |

Hay que mandar **email o username** (uno de los dos). Sin ninguno → **400**. No existe → **404**.

### Productos del vendedor

`GET /api/usuarios/{id}/productos` → lista de `ProductoResponse` (todos los del vendedor, no solo activos del catálogo público), o **204**. Requiere token. Usuario inexistente → **404**.

### Historial de pedidos

`GET /api/usuarios/{id}/pedidos` → lista de `PedidoResponse`, del más nuevo al más viejo, o **204**.
Es privado: solo lo ve **su dueño** (o un `ADMIN`) → **403** si es de otro. Usuario inexistente → **404**.

---

## Carrito

El carrito activo se obtiene o crea al operar por usuario.

> Todos los endpoints del carrito piden token, y el `{usuarioId}` de la URL **tiene que ser el
> del usuario autenticado**: si no, **403**. Sin token, **401**.

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
| Sin token | **401** |
| El carrito es de otro usuario | **403** |
| Carrito inexistente | 404 |
| Carrito vacío o ya procesado | 400 |
| Stock insuficiente / producto inactivo | 400 |

---

## Pedidos

Requieren token, y además **ser el dueño del pedido** (un `ADMIN` puede ver cualquiera).

| Método | Ruta | Respuesta |
|--------|------|-----------|
| GET | `/api/pedidos/{id}` | **200** con el `PedidoResponse`; es la URL del header `Location` del checkout |
| GET | `/api/pedidos/numero/{numero}` | **200**, busca por el número tipo `PED-1758300000000` |
| GET | `/api/usuarios/{id}/pedidos` | **200** con el historial, o **204** si no compró nada |

| Situación | Código |
|-----------|--------|
| Sin token | **401** |
| El pedido es de otro usuario | **403** |
| Pedido inexistente | 404 |

---

## Flujo sugerido

1. `POST /api/usuarios/registro` (con `username`) → queda con rol `USER`
2. `POST /api/usuarios/login` → **guardar el `token` de la respuesta**
3. De acá en adelante, mandar `Authorization: Bearer <token>` en todos los requests
4. `POST /api/categorias` y `POST /api/marcas` → **requiere un usuario con rol `ADMIN`**
5. `POST /api/productos` con `categoriaIds`, `marcaId` e `imagenes` (el vendedor sale del token)
6. `PUT /api/productos/{id}` / `DELETE /api/productos/{id}` (solo el vendedor → si no, 403)
7. `GET /api/productos` o `GET /api/categorias/{id}/productos` (catálogo, público)
8. `POST /api/carritos/usuarios/{usuarioId}/items` → armar carrito (tiene que ser tu propio id)
9. `POST /api/carritos/{id}/checkout` → **201** + pedido
10. `GET /api/pedidos/{id}` → el pedido que apunta el header `Location`
