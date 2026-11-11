# Guía para implementar una feature del backend

La base ya está lista: cuentas, seguridad, errores, catálogos, WebSocket y las entidades con sus reglas.
Tu trabajo en cada feature es **conectar el contrato con el dominio**: un controlador que cumple
`docs/api/openapi.yaml` y un servicio que usa las entidades que ya existen.

Usa como referencia el paquete `feature/account`: es una feature completa hecha con estas mismas reglas.

## Camino rápido

1. Lee tu sección en `docs/api/openapi.yaml` (rutas, cuerpos, códigos de respuesta y textos de error).
2. Revisa cómo lo usa la app (`../Proyecto-Textil-Circular/Proyecto-Fundacion-Textil`): si el contrato y la
   app no coinciden, avisa antes de programar. La app manda.
3. Crea `feature/<nombre>/` con controlador, servicio y DTOs (ver [ejemplo](#ejemplo-contenidos)).
4. Escribe un test de API por regla del contrato y corre `./mvnw test`.
5. Revisa el [checklist](#checklist-antes-del-pr) y abre el PR.

## Dónde va cada cosa

| Qué | Dónde | Ya existe |
|-----|-------|-----------|
| Reglas de negocio (estados, validaciones, permisos del dominio) | `core/model/<área>/` (entidades) | Sí, casi todas |
| Consultas a la base de datos | `core/repository/` | Sí, agrega métodos si faltan |
| Tablas | `src/main/resources/db/migration/` | Sí (V1–V10) |
| Endpoint HTTP | `feature/<nombre>/<Nombre>Controller.java` | **Tú** |
| Orquestación (buscar, llamar a la entidad, armar la respuesta) | `feature/<nombre>/<Nombre>Service.java` | **Tú** |
| Cuerpos de petición y respuesta | `feature/<nombre>/<Nombre>Dtos.java` (records) | **Tú** |

Tres reglas de dependencias:

- `core` nunca importa nada de `feature`.
- Una feature nunca importa otra feature. Si dos la necesitan, va en `core`.
- El servicio **coordina**; la regla vive en la entidad. Si escribes un `if` de negocio en el servicio,
  pregúntate si la entidad ya lo hace (por ejemplo `Business.vote`, `Business.isOwnedBy`, `OutreachContent.delete`).

## Recorrido de una petición

```
App ──HTTP──▶ Spring Security ──▶ Controller ──▶ Service ──▶ Entidad (reglas) ──▶ Repository ──▶ PostgreSQL
                │                    │              │
                │ valida el JWT      │ /api/v1 se   │ @Transactional: si algo lanza una
                │ y la sesión        │ agrega solo  │ excepción, no se guarda nada
                ▼                    ▼              ▼
              401 si falla     ApiExceptionHandler convierte cada excepción en el error del contrato
```

## Ejemplo: contenidos

Dos operaciones de `/contents`: listar (pública) y eliminar (solo administradores).

```java
// feature/content/ContentController.java
@RestController
@RequiredArgsConstructor
class ContentController {

    private final ContentService contents;

    @Operation(summary = "Listar contenidos de difusión", tags = "Contents")
    @GetMapping("/contents")                       // sin /api/v1: WebConfig lo agrega
    ContentPage list(@RequestParam(defaultValue = "0") int page,
                     @RequestParam(defaultValue = "20") int size) {
        return contents.list(page, size);
    }

    @Operation(summary = "Eliminar un contenido (administrador)", tags = "Contents")
    @DeleteMapping("/contents/{contentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)          // el mismo código que dice el contrato
    void delete(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable long contentId) {
        contents.delete(Caller.of(jwt), contentId); // quién llama sale del token, nunca del cuerpo
    }
}
```

```java
// feature/content/ContentService.java
@Service
@RequiredArgsConstructor
@Transactional
class ContentService {

    private final OutreachContentRepository contents;
    private final UserAccountRepository accounts;
    private final Clock clock;                      // nunca Instant.now(): los tests controlan el tiempo

    void delete(Caller caller, long contentId) {
        UserAccount admin = accounts.findById(caller.userId()).orElseThrow(UnauthorizedException::new);
        OutreachContent content = contents.findByIdAndDeletedAtIsNull(contentId)
                .orElseThrow(NotFoundException::new);            // → 404 NOT_FOUND
        content.delete(admin, clock.instant());                  // la regla está en la entidad
        // sin save(): la entidad ya está cargada y JPA guarda el cambio al terminar la transacción
    }
}
```

No hace falta proteger `DELETE /contents/*` en el código: `SecurityConfig` ya exige `ADMINISTRATOR`.

## Reglas que no se negocian

| Regla | Por qué |
|-------|---------|
| El usuario sale de `Caller.of(jwt)`, nunca del cuerpo ni de la URL | Si no, cualquiera actúa en nombre de otro |
| En un `PUT`, todos los campos del DTO llevan `@JsonProperty(required = true)` | El contrato exige el cuerpo completo; un campo ausente debe dar 400, no borrar datos sin querer |
| El código de éxito es el del contrato (`@ResponseStatus`); si hay dos (200/201), usa `ResponseEntity` | La app falla si recibe 204 donde espera un cuerpo |
| Las respuestas son records y nunca exponen entidades | Controlas exactamente qué sale y con qué nombres. Para convertir entidad → record usa lo que prefieras: un método `of(...)` en el record (como `Profile.of` en `feature/account`) o un mapper de MapStruct (ya está configurado en el `pom.xml`) |
| Las listas que crecen se paginan: `page` desde 0, `size` 20 por defecto, máximo 50 | Contrato común; copia la forma de `UserPage` |
| Fechas con `Clock`, no `Instant.now()` | Tests deterministas |
| Nada de servicios externos (Google, push, almacenamiento en la nube, pagos): se simulan | Es un requisito del proyecto |
| Una tabla nueva o un cambio de esquema es una migración nueva `V<n>__descripcion.sql`; nunca edites una existente | Flyway ya aplicó las anteriores en otras bases |

## Errores: 

Lanza la excepción; `ApiExceptionHandler` arma el cuerpo del contrato (`code`, `detail`, `errors`).

| Situación | Lanza | Respuesta |
|-----------|-------|-----------|
| Dato inválido en un campo | `new ValidationException("campo", "Mensaje para el usuario")` | 400 `VALIDATION` |
| No existe o no es visible para quien llama | `new NotFoundException()` | 404 `NOT_FOUND` |
| Existe pero no le corresponde (no es dueño, no participa) | `new ForbiddenException()` | 403 `FORBIDDEN` |
| Choca con el estado actual (ya votó, ya existe) | `new ConflictException("CODIGO", "Mensaje")` | 409 con ese `code` |

Para validar texto, correos, URLs o precios usa `core/validation/Validators`: ya lanza `ValidationException`
con el mensaje correcto. Los mensajes van en español, porque la app los muestra tal cual.

## Seguridad: 

| Lo hace Spring Security, no lo repitas | Lo haces tú en el servicio |
|----------------------------------------|----------------------------|
| Validar el JWT y que la sesión siga activa | Comprobar que quien llama es dueño del recurso (`isOwnedBy`) |
| Rechazar con 401 a quien no tiene token | Comprobar que participa en la conversación |
| Exigir `ADMINISTRATOR` en `/admin/**` y en las escrituras de `/contents` | Reglas que dependen de datos (emprendimiento aprobado, ya votó...) |

Todo endpoint nuevo queda protegido por defecto. Si el contrato lo marca público (`security: []`),
agrégalo al `permitAll` de `SecurityConfig`.

## Tiempo real (solo chat)

Para avisar a un usuario conectado, inyecta `RealtimeSessions` y llama a:

```java
realtime.send(destinatarioId, "message.created", datos);   // tipos y datos: x-websocket en el contrato
```

Llámalo **después** de guardar. No bloquea y no falla si el usuario no está conectado. La app se pone al día
por REST al reconectarse, así que no guardes eventos pendientes.