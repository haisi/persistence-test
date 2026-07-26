package li.selman.persistencetest.hibernate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "order_item")
class OrderItem {

    @Id
    private @Nullable Long id;

    // Mapped for realism; read by Hibernate via reflection, not by this test's Java code.
    @SuppressWarnings("unused")
    private @Nullable Long customerId;

    protected OrderItem() {}
}
