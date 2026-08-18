# Documentación de la API

API REST de ecommerce de productos tecnológicos (Fase 1). Base URL: `http://localhost:8081`

Los productos siempre pertenecen a una categoría. Primero hay que crear la categoría y después el producto con su `categoriaId`. En las respuestas, el producto solo trae ese id; el detalle de la categoría se consulta por `/api/categorias/{id}`.

---

## Categorías

### Listar categorías

Devuelve todas las categorías. No espera body.

`GET /api/categorias`

**Respuesta 200**

```json
[
  {
    "id": 1,
    "nombre": "Notebooks",
    "descripcion": "Laptops para trabajo, estudio y gaming"
  },
  {
    "id": 2,
    "nombre": "Smartphones",
    "descripcion": "Celulares y accesorios móviles"
  },
  {
    "id": 3,
    "nombre": "Periféricos",
    "descripcion": "Auriculares, teclados, mouse y webcams"
  }
]
```

---

### Obtener una categoría

Trae una categoría por `id`. Si no existe, responde **404**.

`GET /api/categorias/{id}`

Ejemplo: `GET /api/categorias/1`

**Respuesta 200**

```json
{
  "id": 1,
  "nombre": "Notebooks",
  "descripcion": "Laptops para trabajo, estudio y gaming"
}
```

---

### Crear una categoría

Da de alta una categoría. El `id` lo asigna la base; no hace falta mandarlo.

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
  "descripcion": "Laptops para trabajo, estudio y gaming"
}
```

Otros ejemplos de body:

```json
{
  "nombre": "Smartphones",
  "descripcion": "Celulares y accesorios móviles"
}
```

```json
{
  "nombre": "Periféricos",
  "descripcion": "Auriculares, teclados, mouse y webcams"
}
```

**Errores**

| Situación | Código | Mensaje |
|-----------|--------|---------|
| Falta el nombre | 400 | El nombre de la categoría es obligatorio |
| El nombre ya existe | 409 | La categoría ya existe |

El nombre se compara sin importar mayúsculas/minúsculas (`Notebooks` y `notebooks` se consideran la misma).

---

### Listar productos de una categoría

Devuelve los productos de esa categoría. Si el `id` no existe, responde **404**.

`GET /api/categorias/{id}/productos`

Ejemplo: `GET /api/categorias/1/productos`

**Respuesta 200**

```json
[
  {
    "id": 1,
    "nombre": "MacBook Air 13 M3",
    "descripcion": "Chip M3, 16 GB RAM, 512 GB SSD",
    "precio": 1899999.0,
    "stock": 8,
    "categoriaId": 1
  },
  {
    "id": 2,
    "nombre": "Lenovo IdeaPad Gaming 3",
    "descripcion": "Ryzen 7, RTX 4050, 16 GB RAM, 512 GB SSD",
    "precio": 1249999.0,
    "stock": 12,
    "categoriaId": 1
  }
]
```

---

## Productos

### Listar productos

Lista todos los productos. Para ver solo los de una categoría, usar `GET /api/categorias/{id}/productos`.

`GET /api/productos`

**Respuesta 200**

```json
[
  {
    "id": 1,
    "nombre": "MacBook Air 13 M3",
    "descripcion": "Chip M3, 16 GB RAM, 512 GB SSD",
    "precio": 1899999.0,
    "stock": 8,
    "categoriaId": 1
  },
  {
    "id": 3,
    "nombre": "Samsung Galaxy S24",
    "descripcion": "256 GB, 8 GB RAM, cámara 50 MP",
    "precio": 999999.0,
    "stock": 15,
    "categoriaId": 2
  },
  {
    "id": 4,
    "nombre": "Logitech MX Master 3S",
    "descripcion": "Mouse inalámbrico ergonómico, 8000 DPI",
    "precio": 149999.0,
    "stock": 30,
    "categoriaId": 3
  }
]
```

---

### Crear un producto

Crea un producto y lo asigna a una categoría existente. `categoriaId` es obligatorio.

`POST /api/productos`

**Body**

```json
{
  "nombre": "MacBook Air 13 M3",
  "descripcion": "Chip M3, 16 GB RAM, 512 GB SSD",
  "precio": 1899999,
  "stock": 8,
  "categoriaId": 1
}
```

**Respuesta 200**

```json
{
  "id": 1,
  "nombre": "MacBook Air 13 M3",
  "descripcion": "Chip M3, 16 GB RAM, 512 GB SSD",
  "precio": 1899999.0,
  "stock": 8,
  "categoriaId": 1
}
```

Otros ejemplos de body:

```json
{
  "nombre": "Samsung Galaxy S24",
  "descripcion": "256 GB, 8 GB RAM, cámara 50 MP",
  "precio": 999999,
  "stock": 15,
  "categoriaId": 2
}
```

```json
{
  "nombre": "Logitech MX Master 3S",
  "descripcion": "Mouse inalámbrico ergonómico, 8000 DPI",
  "precio": 149999,
  "stock": 30,
  "categoriaId": 3
}
```

**Errores**

| Situación | Código | Mensaje |
|-----------|--------|---------|
| Falta `categoriaId` | 400 | La categoría es obligatoria |
| La categoría no existe | 404 | La categoría no existe |

---

## Consola H2

Para ver las tablas en el navegador: [http://localhost:8081/h2-console](http://localhost:8081/h2-console)

- **JDBC URL:** `jdbc:h2:file:./data/ecommerce`
- **User:** `sa`
- **Password:** (vacío)
