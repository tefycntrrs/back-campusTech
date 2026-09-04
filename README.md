# ecommerce-backend-group1
API REST de ecommerce construida con Java y Spring Boot (Fase 1: Backend).

La documentación de los endpoints está en [documentacion.md](documentacion.md) y hay una
colección lista para importar en Postman en `collection_postman.json`.

## Requisitos

- JDK 21 o superior
- MySQL (o el perfil `h2` para probar sin instalar nada)

## Cómo correrlo

Con MySQL (variables de entorno):

```bash
DB_URL=jdbc:mysql://localhost:3306/ecommerce DB_USERNAME=root DB_PASSWORD=secreto ./mvnw spring-boot:run
```

Con H2 embebida, para probar rápido sin MySQL:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

La API queda en `http://localhost:8081`.

## Tests

```bash
./mvnw test
```

Los tests corren contra H2 en memoria, no hace falta MySQL.
