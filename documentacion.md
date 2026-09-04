# Documentación de la API

API REST de ecommerce de productos tecnológicos (Fase 1). Base URL: `http://localhost:8081`

Base de datos: **MySQL** (credenciales por variables de entorno `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).

Para probar sin MySQL hay un perfil con H2 embebida:
`mvnw spring-boot:run -Dspring-boot.run.profiles=h2` (la base queda en `target/h2/ecommerce.mv.db`).

Los productos pertenecen a una **categoría** y a una **marca**. Primero hay que crear categoría y marca; después el producto con `categoriaId` y `marcaId`. En las respuestas, el producto expone `categoriaId` y el objeto `marca`.

---

## Códigos de respuesta

Todos los endpoints devuelven `ResponseEntity`, así que el código HTTP es explícito:

| Código | Cuándo |
|--------|--------|
| 200 OK | GET y PUT con resultado |
| 201 Created | POST: devuelve el recurso creado y el header `Location` |
| 204 No Content | GET de lista cuando todavía no hay elementos (cuerpo vacío) |
| 400 Bad Request | Datos inválidos (`ArgumentInvalidException` o `@Valid`) |
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

Si la categoría no existe → **404**.

**Respuesta 200**

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
    "marca": {
      "id": 1,
      "nombre": "Apple",
      "activo": true
    },
    "createdAt": "2026-09-03T19:00:00",
    "updatedAt": "2026-09-03T19:00:00",
    "categoriaId": 1
  }
]
```

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

### Listar productos

Lista todos los productos. Para filtrar por categoría: `GET /api/categorias/{id}/productos`.

`GET /api/productos`

**Respuesta 200**

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
    "marca": {
      "id": 1,
      "nombre": "Apple",
      "activo": true
    },
    "createdAt": "2026-09-03T19:00:00",
    "updatedAt": "2026-09-03T19:00:00",
    "categoriaId": 1
  }
]
```

---

### Obtener un producto

`GET /api/productos/{id}`

Si no existe → **404**. Si el id no es numérico → **400**.

**Respuesta 200**

```json
{
  "id": 1,
  "nombre": "MacBook Air 13 M3",
  "descripcion": "Chip M3, 16 GB RAM, 512 GB SSD",
  "precio": 1899999.00,
  "stock": 8,
  "sku": "MBA-M3-512",
  "activo": true,
  "marca": {
    "id": 1,
    "nombre": "Apple",
    "activo": true
  },
  "createdAt": "2026-09-03T19:00:00",
  "updatedAt": "2026-09-03T19:00:00",
  "categoriaId": 1
}
```

---

### Crear un producto

`categoriaId`, `marcaId` y `sku` son obligatorios (categoría y marca deben existir; el SKU es único).

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
  "marcaId": 1
}
```

**Respuesta 201 Created**

Header `Location` con la URL del recurso creado.

```json
{
  "id": 1,
  "nombre": "MacBook Air 13 M3",
  "descripcion": "Chip M3, 16 GB RAM, 512 GB SSD",
  "precio": 1899999.00,
  "stock": 8,
  "sku": "MBA-M3-512",
  "activo": true,
  "marca": {
    "id": 1,
    "nombre": "Apple",
    "activo": true
  },
  "createdAt": "2026-09-03T19:00:00",
  "updatedAt": "2026-09-03T19:00:00",
  "categoriaId": 1
}
```

**Errores**

| Situación | Código | Mensaje |
|-----------|--------|---------|
| Falta `nombre` | 400 | El nombre del producto es obligatorio |
| Falta `sku` | 400 | El SKU es obligatorio |
| Falta `categoriaId` | 400 | La categoría es obligatoria |
| Falta `marcaId` | 400 | La marca es obligatoria |
| `precio` nulo o menor o igual a 0 | 400 | El precio debe ser mayor a 0 |
| `stock` nulo o negativo | 400 | El stock no puede ser negativo |
| Categoría inexistente | 404 | La categoría con id X no existe |
| Marca inexistente | 404 | La marca con id X no existe |
| SKU ya usado por otro producto | 409 | Ya existe un producto con ese SKU |

---

### Actualizar un producto

Actualización parcial: solo se modifican los campos que mandes. El resto queda igual.

`PUT /api/productos/{id}`

**Body (ejemplo: solo precio y stock)**

```json
{
  "precio": 1799999,
  "stock": 5
}
```

**Body (ejemplo: cambiar categoría y desactivar)**

```json
{
  "categoriaId": 2,
  "activo": false
}
```

**Respuesta 200**: el producto actualizado (`updatedAt` se refresca solo).

| Situación | Código |
|-----------|--------|
| Producto inexistente | 404 |
| Categoría/marca inexistente | 404 |
| SKU vacío | 400 |
| SKU duplicado en otro producto | 409 |

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
4. `POST /api/productos` (con `categoriaId` y `marcaId`)
5. `PUT /api/productos/{id}` / `PUT /api/categorias/{id}` / `PUT /api/marcas/{id}` cuando haga falta
6. `GET /api/productos` o `GET /api/categorias/{id}/productos`
