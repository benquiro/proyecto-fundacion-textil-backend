/**
 * One package per app feature (for example {@code auth}, {@code directory}, {@code entrepreneur},
 * {@code review}, {@code messages}, {@code content}, {@code metrics}), each holding its controller,
 * service, DTOs and MapStruct mapper.
 *
 * <p>A feature depends only on {@code core}, never on another feature; what two features share moves
 * down to {@code core}. Services coordinate (load, call the entity, save); business rules stay in the
 * entities of {@code core.model}.
 */
package cl.textilcircular.api.feature;
