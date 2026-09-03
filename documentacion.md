# Documentación de la API

API REST de ecommerce de productos tecnológicos (Fase 1). Base URL: `http://localhost:8081`

Base de datos: **MySQL** (credenciales por variables de entorno `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).

Los productos pertenecen a una **categoría** y a una **marca**. Primero hay que crear categoría y marca; después el producto con `categoriaId` y `marcaId`. En las respuestas, el producto expone `categoriaId` y el objeto `marca`.

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

**Respuesta 200**

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

### Crear una marca

`POST /api/marcas`

**Body**

```json
{
  "nombre": "Apple"
}
```

**Respuesta 200**

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

**Errores**

| Situación | Código | Mensaje |
|-----------|--------|---------|
| Falta `categoriaId` | 400 | La categoría es obligatoria |
| Falta `marcaId` | 400 | La marca es obligatoria |
| Categoría inexistente | 404 | La categoría con id X no existe |
| Marca inexistente | 404 | La marca con id X no existe |

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

## Flujo sugerido

1. `POST /api/categorias`
2. `POST /api/marcas`
3. `POST /api/productos` (con `categoriaId` y `marcaId`)
4. `PUT /api/productos/{id}` / `PUT /api/categorias/{id}` / `PUT /api/marcas/{id}` cuando haga falta
5. `GET /api/productos` o `GET /api/categorias/{id}/productos`
