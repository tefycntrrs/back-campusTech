# Documentación de la API

API REST de ecommerce de productos tecnológicos (Fase 1). Base URL: `http://localhost:8081`

Base de datos: **MySQL** (credenciales por variables de entorno `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).

Para probar sin MySQL hay un perfil con H2 embebida:
`mvnw spring-boot:run -Dspring-boot.run.profiles=h2` (la base queda en `target/h2/ecommerce.mv.db`).

Los productos pertenecen a una **categoría** y a una **marca**. Primero hay que crear categoría y marca; después el producto con `categoriaId` y `marcaId`. En las respuestas, el producto expone `categoriaId` / `categoriaNombre` y `marcaId` / `marcaNombre`.

Cada producto tiene un **usuario vendedor** (el que lo publica, `usuarioId` al crear). Solo ese usuario puede **modificar el producto** (incluido el stock) y **darlo de baja**; las dos operaciones piden `usuarioId` como query param y responden **403** si no coincide con el vendedor. En las respuestas el producto expone `vendedorId` / `vendedorNombre`.

El **catálogo** (`GET /api/productos` y `GET /api/categorias/{id}/productos`) devuelve **solo productos activos**, ordenados **alfabéticamente por nombre**. Un producto dado de baja desaparece del catálogo y del detalle (**404**).

Cada producto tiene una **galería de imágenes** (al menos una, obligatoria al publicar). En esta fase solo se guardan **URLs**: no hay carga ni almacenamiento de archivos. La galería se maneja entera desde el producto (no tiene endpoints propios).

---

## Códigos de respuesta

Todos los endpoints devuelven `ResponseEntity`, así que el código HTTP es explícito:

| Código | Cuándo |
|--------|--------|
| 200 OK | GET y PUT con resultado |
| 201 Created | POST: devuelve el recurso creado y el header `Location` |
| 204 No Content | GET de lista sin elementos, o DELETE aplicado (cuerpo vacío) |
| 400 Bad Request | Datos inválidos (`ArgumentInvalidException` o `@Valid`) |
| 403 Forbidden | El recurso existe pero pertenece a otro usuario (`ForbiddenException`) |
| 404 Not Found | Recurso inexistente (`ResourceNotFoundException`) |
| 409 Conflict | Valor único repetido (`DuplicateResourceException`) |
| 500 Internal Server Error | Error inesperado |

## Formato de errores

Ningún controller arma respuestas de error: todas las excepciones las captura el
`GlobalExceptionHandler` (`@RestControllerAdvice`) y salen con este cuerpo:

```json
{
  "timestamp": "2026-09-03T21:00:39.699",
  "status": 404,
  "error": "Not Found",
  "message": "El usuario con id 9999 no existe",
  "path": "/api/usuarios/9999"
}
```

Cuando el error es por campos concretos se agrega el objeto `errores` con el detalle campo por campo:

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
    "fechaNacimiento": "La fecha de nacimiento debe ser anterior a hoy",
    "nombre": "El nombre es obligatorio"
  }
}
```

### Excepciones personalizadas

| Excepción | HTTP | Se usa en |
|-----------|------|-----------|
| `ResourceNotFoundException` | 404 | Base de `ProductoNotFoundException`, `CategoriaNotFoundException`, `MarcaNotFoundException`, `UsuarioNotFoundException` |
| `ArgumentInvalidException` | 400 | Reglas de negocio: campos obligatorios, precio, stock, edad |
| `ForbiddenException` | 403 | Modificar o eliminar una publicación de producto que es de otro usuario |
| `DuplicateResourceException` | 409 | Email, SKU o nombre ya usados |

---

## Categorías

### Listar categorías

`GET /api/categorias`

**Respuesta 200**

```json
[
  {
    "id": 1,
    "nombre": "Notebooks",
    "descripcion": "Laptops para trabajo, estudio y gaming",
    "activo": true
  }
]
```

---

### Obtener una categoría

`GET /api/categorias/{id}`

Si no existe → **404**.

**Respuesta 200**

```json
{
  "id": 1,
  "nombre": "Notebooks",
  "descripcion": "Laptops para trabajo, estudio y gaming",
  "activo": true
}
```

---

### Crear una categoría

`POST /api/categorias`

**Body**

```json
{
  "nombre": "Notebooks",
  "descripcion": "Laptops para trabajo, estudio y gaming"
}
```

**Respuesta 201 Created**

Header `Location` con la URL del recurso creado.

```json
{
  "id": 1,
  "nombre": "Notebooks",
  "descripcion": "Laptops para trabajo, estudio y gaming",
  "activo": true
}
```

**Errores**

| Situación | Código | Mensaje |
|-----------|--------|---------|
| Falta el nombre | 400 | El nombre de la categoría es obligatorio |
| El nombre ya existe | 409 | La categoría ya existe |

El nombre se compara sin importar mayúsculas/minúsculas.

---

### Actualizar una categoría

`PUT /api/categorias/{id}`

**Body**

```json
{
  "nombre": "Notebooks y Ultrabooks",
  "descripcion": "Laptops para trabajo, estudio y gaming",
  "activo": true
}
```

**Respuesta 200**

```json
{
  "id": 1,
  "nombre": "Notebooks y Ultrabooks",
  "descripcion": "Laptops para trabajo, estudio y gaming",
  "activo": true
}
```

Si el id no existe → **404**. Si el nombre ya lo usa otra categoría → **409**.

---

### Listar productos de una categoría

`GET /api/categorias/{id}/productos`

Si la categoría no existe → **404**. Devuelve **solo productos activos**, ordenados **alfabéticamente por nombre**.

**Respuesta 200**: misma estructura que `GET /api/productos` (ver la sección Productos), o **204 No Content** si la categoría no tiene productos activos.

---

## Marcas

### Listar marcas

`GET /api/marcas`

**Respuesta 200**

```json
[
  {
    "id": 1,
    "nombre": "Apple",
    "activo": true
  },
  {
    "id": 2,
    "nombre": "Samsung",
    "activo": true
  }
]
```

---

### Obtener una marca

`GET /api/marcas/{id}`

Si no existe → **404**.

**Respuesta 200**

```json
{
  "id": 1,
  "nombre": "Apple",
  "activo": true
}
```

---

### Crear una marca

`POST /api/marcas`

**Body**

```json
{
  "nombre": "Apple"
}
```

**Respuesta 201 Created**

Header `Location` con la URL del recurso creado.

```json
{
  "id": 1,
  "nombre": "Apple",
  "activo": true
}
```

**Errores**

| Situación | Código | Mensaje |
|-----------|--------|---------|
| Falta el nombre | 400 | El nombre de la marca es obligatorio |
| El nombre ya existe | 409 | La marca ya existe |

---

### Actualizar una marca

`PUT /api/marcas/{id}`

**Body**

```json
{
  "nombre": "Apple Inc.",
  "activo": true
}
```

**Respuesta 200**

```json
{
  "id": 1,
  "nombre": "Apple Inc.",
  "activo": true
}
```

Si el id no existe → **404**. Si el nombre ya lo usa otra marca → **409**.

---

## Productos

### Listar productos (catálogo)

Devuelve **solo productos activos**, ordenados **alfabéticamente por nombre**. Para filtrar por categoría: `GET /api/categorias/{id}/productos` (mismos criterios).

`GET /api/productos`

**Respuesta 200** con la lista, o **204 No Content** si no hay productos activos.

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
    "categoriaId": 1,
    "categoriaNombre": "Notebooks",
    "marcaId": 1,
    "marcaNombre": "Apple",
    "vendedorId": 1,
    "vendedorNombre": "Pedro Marzano",
    "imagenes": [
      { "id": 1, "url": "https://cdn.ejemplo.com/mba-m3/frente.jpg", "orden": 0, "principal": true },
      { "id": 2, "url": "https://cdn.ejemplo.com/mba-m3/lateral.jpg", "orden": 1, "principal": false }
    ],
    "createdAt": "2026-09-03T19:00:00",
    "updatedAt": "2026-09-03T19:00:00"
  }
]
```

`imagenes` viene ordenada por `orden` ascendente y siempre trae exactamente una imagen con `principal: true` (la portada).

---

### Obtener un producto (detalle)

`GET /api/productos/{id}`

Detalle completo: descripción, galería de imágenes y datos del vendedor.

Si no existe, o si fue dado de baja → **404**. Si el id no es numérico → **400**.

**Respuesta 200**: mismo objeto que en el listado.

---

### Crear un producto

`categoriaId`, `marcaId`, `usuarioId`, `sku` e `imagenes` (al menos una) son obligatorios. Categoría, marca y usuario deben existir; el SKU es único. El `usuarioId` es el **vendedor**: el único que después va a poder modificar o dar de baja el producto.

`POST /api/productos`

**Body**

```json
{
  "nombre": "MacBook Air 13 M3",
  "descripcion": "Chip M3, 16 GB RAM, 512 GB SSD",
  "precio": 1899999,
  "stock": 8,
  "sku": "MBA-M3-512",
  "categoriaId": 1,
  "marcaId": 1,
  "usuarioId": 1,
  "imagenes": [
    { "url": "https://cdn.ejemplo.com/mba-m3/frente.jpg", "principal": true },
    { "url": "https://cdn.ejemplo.com/mba-m3/lateral.jpg" }
  ]
}
```

De cada imagen solo `url` es obligatoria. `orden` (entero) y `principal` (boolean) son opcionales:

- Si no mandás `orden`, se usa la posición en la lista (0, 1, 2…).
- Si ninguna imagen trae `principal: true`, la primera queda como portada. Si mandás varias, se respeta solo la primera.

**Respuesta 201 Created**

Header `Location` con la URL del recurso creado. El cuerpo es el mismo objeto que devuelve `GET /api/productos/{id}`.

**Errores**

| Situación | Código | Mensaje |
|-----------|--------|---------|
| Falta `nombre` | 400 | El nombre del producto es obligatorio |
| Falta `sku` | 400 | El SKU es obligatorio |
| Falta `categoriaId` | 400 | La categoría es obligatoria |
| Falta `marcaId` | 400 | La marca es obligatoria |
| Falta `usuarioId` | 400 | El usuario que publica el producto es obligatorio |
| Usuario inexistente | 404 | El usuario con id X no existe |
| `imagenes` vacío o ausente | 400 | El producto debe tener al menos una imagen |
| Una imagen sin `url` | 400 | Cada imagen necesita una url |
| `precio` nulo o menor o igual a 0 | 400 | El precio debe ser mayor a 0 |
| `stock` nulo o negativo | 400 | El stock no puede ser negativo |
| Categoría inexistente | 404 | La categoría con id X no existe |
| Marca inexistente | 404 | La marca con id X no existe |
| SKU ya usado por otro producto | 409 | Ya existe un producto con ese SKU |

---

### Actualizar un producto / gestión de stock

Actualización parcial: solo se modifican los campos que mandes. El resto queda igual.
Sirve también para la **gestión de stock** (mandás solo `stock`).

**Solo el usuario vendedor** puede actualizar el producto: `usuarioId` va como query param
y tiene que coincidir con el vendedor. Si no coincide → **403**.

`PUT /api/productos/{id}?usuarioId={usuarioId}`

**Body (ejemplo: gestión de stock)**

```json
{
  "stock": 5
}
```

**Body (ejemplo: precio y stock)**

```json
{
  "precio": 1799999,
  "stock": 5
}
```

**Body (ejemplo: reemplazar la galería)**

```json
{
  "imagenes": [
    { "url": "https://cdn.ejemplo.com/mba-m3/nueva-portada.jpg", "principal": true },
    { "url": "https://cdn.ejemplo.com/mba-m3/detalle.jpg" }
  ]
}
```

Si mandás `imagenes`, **reemplaza la galería completa** (borra las anteriores). Si no mandás el campo, la galería queda como estaba. Mandar `"imagenes": []` da **400** (un producto no puede quedar sin imágenes).

**Respuesta 200**: el producto actualizado (`updatedAt` se refresca solo).

| Situación | Código |
|-----------|--------|
| Falta `usuarioId`, o el usuario no existe | 400 / 404 |
| El producto es de otro usuario | 403 |
| Producto inexistente | 404 |
| Categoría/marca inexistente | 404 |
| SKU vacío | 400 |
| SKU duplicado en otro producto | 409 |
| `imagenes` presente pero vacío, o una imagen sin `url` | 400 |

---

### Eliminar un producto

**Baja lógica**: el producto se marca como inactivo (`activo: false`), sale del catálogo y
del detalle, pero no se borra de la base para no romper los carritos que ya lo referencian.

**Solo el usuario vendedor** puede darlo de baja: `usuarioId` va como query param.

`DELETE /api/productos/{id}?usuarioId={usuarioId}`

**Respuesta 204 No Content** (sin cuerpo). Es idempotente: dar de baja algo ya inactivo
vuelve a responder 204.

| Situación | Código |
|-----------|--------|
| Falta `usuarioId`, o el usuario no existe | 400 / 404 |
| El producto es de otro usuario | 403 |
| Producto inexistente | 404 |

---

## Usuarios

El usuario se registra con **fecha de nacimiento** y **sexo**. La contraseña se guarda
codificada con `passwordEncoder.encode()` (BCrypt) y nunca se devuelve en las respuestas.

Valores válidos de `sexo`: `MASCULINO`, `FEMENINO`, `OTRO`, `PREFIERO_NO_DECIR`
(se aceptan en minúsculas). Formato de `fechaNacimiento`: `yyyy-MM-dd`.

### Registrar un usuario

`POST /api/usuarios/registro`

**Body**

```json
{
  "nombre": "Pedro",
  "apellido": "Marzano",
  "email": "pedro@uade.edu.ar",
  "password": "password123",
  "fechaNacimiento": "1999-05-20",
  "sexo": "MASCULINO"
}
```

**Respuesta 201 Created**

Header `Location: http://localhost:8081/api/usuarios/1`

```json
{
  "id": 1,
  "nombre": "Pedro",
  "apellido": "Marzano",
  "email": "pedro@uade.edu.ar",
  "fechaNacimiento": "1999-05-20",
  "edad": 27,
  "sexo": "MASCULINO",
  "activo": true,
  "createdAt": "2026-09-03T21:00:30.882"
}
```

**Errores**

| Situación | Código | Mensaje |
|-----------|--------|---------|
| Falta el nombre / apellido | 400 | El nombre es obligatorio |
| Email con formato inválido | 400 | El email no tiene un formato válido |
| Contraseña de menos de 8 caracteres | 400 | La contraseña debe tener entre 8 y 72 caracteres |
| Fecha de nacimiento futura o nula | 400 | La fecha de nacimiento debe ser anterior a hoy |
| Menor de 13 años | 400 | El usuario debe tener al menos 13 años |
| `sexo` con un valor que no existe | 400 | sexo debe ser uno de: MASCULINO, FEMENINO, OTRO, PREFIERO_NO_DECIR |
| Email ya registrado | 409 | Ya existe Usuario con email 'x@y.com' |

Los errores de campos vienen con el detalle en el objeto `errores`.

---

### Listar usuarios

`GET /api/usuarios`

**Respuesta 200** con la lista, o **204 No Content** si todavía no hay usuarios.

```json
[
  {
    "id": 1,
    "nombre": "Pedro",
    "apellido": "Marzano",
    "email": "pedro@uade.edu.ar",
    "fechaNacimiento": "1999-05-20",
    "edad": 27,
    "sexo": "MASCULINO",
    "activo": true,
    "createdAt": "2026-09-03T21:00:30.882"
  }
]
```

---

### Obtener un usuario

`GET /api/usuarios/{id}`

Si no existe → **404**.

---

### Buscar un usuario por email

`GET /api/usuarios/buscar?email=pedro@uade.edu.ar`

Si no existe → **404**.

---

## Flujo sugerido

1. `POST /api/usuarios/registro`
2. `POST /api/categorias`
3. `POST /api/marcas`
4. `POST /api/productos` (con `categoriaId`, `marcaId`, `usuarioId` y al menos una imagen en `imagenes`)
5. `PUT /api/productos/{id}?usuarioId=...` para editar el producto o su stock (solo el vendedor)
6. `DELETE /api/productos/{id}?usuarioId=...` para dar de baja la publicación (solo el vendedor)
7. `GET /api/productos` o `GET /api/categorias/{id}/productos` para ver el catálogo (activos, alfabético)
8. `GET /api/productos/{id}` para el detalle
