package cl.textilcircular.api.support;

import cl.textilcircular.api.core.model.account.Entrepreneur;
import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.model.business.Business;
import cl.textilcircular.api.core.model.business.ReviewDecision;
import cl.textilcircular.api.core.model.reference.AgeRange;
import cl.textilcircular.api.core.model.reference.Commune;
import cl.textilcircular.api.core.model.reference.Gender;
import cl.textilcircular.api.core.model.reference.TextileService;
import jakarta.persistence.EntityManager;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static cl.textilcircular.api.support.Fixtures.ENCODER;
import static cl.textilcircular.api.support.Fixtures.NOW;

/** Builds committed-shape data through the real entities, so repository tests start from valid rows. */
public class PersistedData {

    private static final AtomicLong SEQ = new AtomicLong(500);

    private final EntityManager em;

    public PersistedData(EntityManager em) {
        this.em = em;
    }

    public UserAccount customer() {
        UserAccount account = UserAccount.registerCustomer("cliente" + SEQ.incrementAndGet() + "@b.cl", "Demo1234", "Cliente", ENCODER, NOW);
        em.persist(account);
        return account;
    }

    public UserAccount administrator() {
        UserAccount account = UserAccount.registerAdministrator("admin" + SEQ.incrementAndGet() + "@b.cl", "Demo1234", "Admin", ENCODER, NOW);
        em.persist(account);
        return account;
    }

    public Entrepreneur entrepreneur() {
        long n = SEQ.incrementAndGet();
        UserAccount account = UserAccount.registerEntrepreneur("emprendedora" + n + "@b.cl", "Demo1234", null, ENCODER, NOW);
        em.persist(account);
        Entrepreneur entrepreneur = Entrepreneur.create(account, Fixtures.rutFor(n), "Ana", "Pérez",
                em.getReference(AgeRange.class, 3L), em.getReference(Gender.class, 1L));
        em.persist(entrepreneur);
        return entrepreneur;
    }

    public Business pendingBusiness(String name, String description, long communeId, Long... serviceIds) {
        Business business = Business.register(entrepreneur(), name, description, null,
                em.find(Commune.class, communeId), services(serviceIds));
        em.persist(business);
        return business;
    }

    public Business approvedBusiness(String name, String description, long communeId, Long... serviceIds) {
        Business business = pendingBusiness(name, description, communeId, serviceIds);
        business.vote(administrator(), ReviewDecision.APPROVED, null, null, NOW);
        business.vote(administrator(), ReviewDecision.APPROVED, null, null, NOW);
        return business;
    }

    public List<TextileService> services(Long... ids) {
        return Arrays.stream(ids).map(id -> em.find(TextileService.class, id)).toList();
    }

    public void flushAndClear() {
        em.flush();
        em.clear();
    }
}
