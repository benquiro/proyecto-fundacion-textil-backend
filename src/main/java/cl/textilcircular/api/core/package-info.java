/**
 * Shared core used by every feature: the domain model ({@code model}, rich JPA entities that hold the
 * business rules), the Spring Data repositories ({@code repository}) and cross-cutting support
 * ({@code error}, {@code validation}, {@code persistence}, {@code crypto}, {@code config}).
 * The schema lives in {@code src/main/resources/db/migration}.
 *
 * <p>The core never depends on {@code feature}. Something used by a single feature belongs to that
 * feature, not here.
 */
package cl.textilcircular.api.core;
