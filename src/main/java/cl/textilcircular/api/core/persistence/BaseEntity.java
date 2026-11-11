package cl.textilcircular.api.core.persistence;

import org.hibernate.Hibernate;

/**
 * Identity-based equality that works with Hibernate proxies: two entities are equal only
 * when they are of the same class and share a non-null id.
 */
public abstract class BaseEntity {

    public abstract Long getId();

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
            return false;
        }
        Long id = getId();
        return id != null && id.equals(((BaseEntity) other).getId());
    }

    @Override
    public final int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

    /**
     * Same row, compared by id without initializing lazy proxies. Before the first flush only the
     * same instance matches.
     */
    public static boolean sameEntity(BaseEntity a, BaseEntity b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        Long id = a.getId();
        return id != null && id.equals(b.getId());
    }
}
