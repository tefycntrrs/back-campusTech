package com.uade.ecommerce.identidad.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Emisión y validación de los JWT que identifican al usuario en cada request.
 *
 * <p>Un JWT son tres partes separadas por punto: {@code header.payload.firma}. Header y payload
 * son JSON en Base64URL (se pueden leer, no están cifrados) y la firma es un HMAC-SHA256 de
 * {@code header.payload} hecho con una clave que solo conoce el servidor. Por eso el token se
 * puede leer pero no se puede modificar: cambiar un solo carácter invalida la firma.</p>
 *
 * <p>El HMAC se calcula con {@link Mac} del JDK y el JSON con el Jackson que ya usa Spring Boot,
 * así que no hace falta ninguna librería extra.</p>
 *
 * <p>En el token viaja únicamente el <b>subject</b> (el email del usuario). Los roles NO se
 * guardan adentro a propósito: si un admin le saca un rol a alguien, un token viejo seguiría
 * diciendo que lo tiene hasta que expire. {@link JwtAuthenticationFilter} los vuelve a leer de
 * la base en cada request a través del {@code UserDetailsService}.</p>
 */
@Service
public class JwtService {

    /** Algoritmo declarado en el header del token. */
    private static final String ALGORITMO_JWT = "HS256";

    /** Nombre del mismo algoritmo dentro del JDK. */
    private static final String ALGORITMO_MAC = "HmacSHA256";

    /** HMAC-SHA256 necesita una clave de al menos 256 bits para ser segura. */
    private static final int BYTES_MINIMOS_DE_CLAVE = 32;

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final SecretKeySpec clave;
    private final long duracionMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long duracionMs
    ) {
        byte[] bytesDeLaClave = secret.getBytes(StandardCharsets.UTF_8);

        if (bytesDeLaClave.length < BYTES_MINIMOS_DE_CLAVE) {
            throw new IllegalStateException(
                    "app.jwt.secret debe tener al menos " + BYTES_MINIMOS_DE_CLAVE
                            + " caracteres para firmar con " + ALGORITMO_JWT
            );
        }

        this.clave = new SecretKeySpec(bytesDeLaClave, ALGORITMO_MAC);
        this.duracionMs = duracionMs;
    }

    /** Cuánto dura el token, para poder informarlo en la respuesta del login. */
    public long getDuracionMs() {
        return duracionMs;
    }

    /**
     * Arma el token del usuario recién autenticado.
     *
     * @param usuario el {@code UserDetails} que devolvió la autenticación
     * @return el JWT firmado, listo para mandar en el header {@code Authorization: Bearer ...}
     */
    public String generarToken(UserDetails usuario) {
        Instant ahora = Instant.now();

        ObjectNode header = jsonMapper.createObjectNode();
        header.put("alg", ALGORITMO_JWT);
        header.put("typ", "JWT");

        ObjectNode payload = jsonMapper.createObjectNode();
        payload.put("sub", usuario.getUsername());
        payload.put("iat", ahora.getEpochSecond());
        payload.put("exp", ahora.plusMillis(duracionMs).getEpochSecond());

        String contenido = codificar(header) + "." + codificar(payload);

        return contenido + "." + firmar(contenido);
    }

    /**
     * Valida el token y devuelve a quién identifica.
     *
     * <p>No lanza excepción cuando el token es inválido: devuelve un {@code Optional} vacío. Un
     * token roto, vencido o falsificado no es un error del servidor, es simplemente alguien que
     * no está autenticado, y de eso se encarga el filtro devolviendo 401 (ver ítem 30).</p>
     *
     * @param token el JWT tal como vino en el header, ya sin el prefijo "Bearer "
     * @return el subject (el email del usuario) si el token es válido y no venció
     */
    public Optional<String> subjectDeTokenValido(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        String[] partes = token.split("[.]"); // [.] = un punto literal

        if (partes.length != 3) {
            return Optional.empty();
        }

        String contenido = partes[0] + "." + partes[1];

        if (!firmaCoincide(contenido, partes[2])) {
            return Optional.empty();
        }

        // La firma ya está verificada, así que el payload es el que emitió este servidor
        JsonNode payload;
        try {
            payload = jsonMapper.readTree(DECODER.decode(partes[1]));
        } catch (RuntimeException excepcion) {
            return Optional.empty();
        }

        JsonNode expiracion = payload.get("exp");

        if (expiracion == null || expiracion.asLong() <= Instant.now().getEpochSecond()) {
            return Optional.empty();
        }

        JsonNode subject = payload.get("sub");

        if (subject == null || subject.asString().isBlank()) {
            return Optional.empty();
        }

        return Optional.of(subject.asString());
    }

    private String codificar(ObjectNode json) {
        return ENCODER.encodeToString(jsonMapper.writeValueAsBytes(json));
    }

    private String firmar(String contenido) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO_MAC);
            mac.init(clave);

            return ENCODER.encodeToString(mac.doFinal(contenido.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException excepcion) {
            // Si el JDK no tiene HmacSHA256 o la clave no sirve, la app está mal configurada
            throw new IllegalStateException("No se pudo firmar el token", excepcion);
        }
    }

    /**
     * Compara las firmas con {@link MessageDigest#isEqual}, que tarda lo mismo coincidan o no.
     * Un {@code equals} común corta en el primer carácter distinto y ese tiempo de más deja
     * adivinar la firma byte por byte (timing attack).
     */
    private boolean firmaCoincide(String contenido, String firmaRecibida) {
        byte[] esperada = firmar(contenido).getBytes(StandardCharsets.UTF_8);
        byte[] recibida = firmaRecibida.getBytes(StandardCharsets.UTF_8);

        return MessageDigest.isEqual(esperada, recibida);
    }
}
