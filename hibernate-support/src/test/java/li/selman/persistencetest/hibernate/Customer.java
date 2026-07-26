package li.selman.persistencetest.hibernate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "customer")
class Customer {

    @Id
    private @Nullable Long id;

    // Mapped for realism (a JPA entity with only an @Id would be a poor fixture); read by Hibernate via
    // reflection, not by this test's Java code.
    @SuppressWarnings("unused")
    private @Nullable String name;

    protected Customer() {}
}
