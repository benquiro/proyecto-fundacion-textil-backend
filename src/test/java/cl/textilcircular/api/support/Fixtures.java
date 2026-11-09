package cl.textilcircular.api.support;

import cl.textilcircular.api.account.Entrepreneur;
import cl.textilcircular.api.account.UserAccount;
import cl.textilcircular.api.reference.AgeRange;
import cl.textilcircular.api.reference.Commune;
import cl.textilcircular.api.reference.Gender;
import cl.textilcircular.api.reference.Region;
import cl.textilcircular.api.reference.TextileService;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/** In-memory entities for unit tests; ids are set by reflection because only the database assigns them. */
public final class Fixtures {

    public static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);
    public static final Instant NOW = Instant.parse("2026-11-09T12:00:00Z");

    private static final AtomicLong IDS = new AtomicLong(1);

    private Fixtures() {
    }

    public static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    public static UserAccount customer() {
        return withId(UserAccount.registerCustomer("c" + IDS.get() + "@b.cl", "Demo1234", "Cliente", ENCODER, NOW),
                IDS.getAndIncrement());
    }

    public static UserAccount administrator() {
        return withId(UserAccount.registerAdministrator("a" + IDS.get() + "@b.cl", "Demo1234", "Admin", ENCODER, NOW),
                IDS.getAndIncrement());
    }

    public static Entrepreneur entrepreneur() {
        long id = IDS.getAndIncrement();
        UserAccount account = withId(UserAccount.registerEntrepreneur("e" + id + "@b.cl", "Demo1234", null, ENCODER, NOW), id);
        Entrepreneur entrepreneur = Entrepreneur.create(account, rutFor(id), "Ana", "Pérez", ageRange(3L), gender(1L));
        return withId(entrepreneur, id);
    }

    public static TextileService service(long id) {
        TextileService service = instantiate(TextileService.class);
        ReflectionTestUtils.setField(service, "id", id);
        ReflectionTestUtils.setField(service, "name", "Servicio " + id);
        ReflectionTestUtils.setField(service, "active", true);
        return service;
    }

    public static TextileService retiredService(long id) {
        TextileService service = service(id);
        ReflectionTestUtils.setField(service, "active", false);
        return service;
    }

    public static Commune commune(long id) {
        Region region = withId(instantiate(Region.class), 1L);
        Commune commune = withId(instantiate(Commune.class), id);
        ReflectionTestUtils.setField(commune, "region", region);
        return commune;
    }

    public static AgeRange ageRange(long id) {
        return withId(instantiate(AgeRange.class), id);
    }

    public static Gender gender(long id) {
        return withId(instantiate(Gender.class), id);
    }

    /** Valid RUT whose body is derived from {@code seed}. */
    public static String rutFor(long seed) {
        String body = String.valueOf(10_000_000 + seed);
        return body + "-" + cl.textilcircular.api.shared.validation.Validators.checkDigit(body);
    }

    private static <T> T instantiate(Class<T> type) {
        try {
            return BeanUtils.instantiateClass(type.getDeclaredConstructor());
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }
}
