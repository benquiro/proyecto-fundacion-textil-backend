package cl.textilcircular.api.shared.persistence;

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
}
