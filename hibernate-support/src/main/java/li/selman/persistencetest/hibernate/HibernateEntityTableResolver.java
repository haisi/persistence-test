package li.selman.persistencetest.hibernate;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.MappingMetamodel;

/**
 * Resolves entity-to-table mappings by asking a live Hibernate {@link MappingMetamodel} - the actual
 * resolved mapping, including whatever {@code PhysicalNamingStrategy} or {@code ImplicitNamingStrategy} the
 * application configured - rather than re-deriving table names from {@code @Table} annotations, which
 * would silently be wrong for any entity relying on a custom naming strategy.
 */
public final class HibernateEntityTableResolver implements EntityTableResolver {

    private final MappingMetamodel metamodel;

    /** @param entityManagerFactory must be backed by Hibernate (unwrapped via {@code unwrap}). */
    public HibernateEntityTableResolver(EntityManagerFactory entityManagerFactory) {
        SessionFactoryImplementor sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
        this.metamodel = sessionFactory.getMappingMetamodel();
    }

    @Override
    public String tableNameOf(Class<?> entityType) {
        if (!metamodel.isEntityClass(entityType)) {
            throw new IllegalArgumentException(entityType.getName() + " is not a mapped JPA entity");
        }
        return metamodel.getEntityDescriptor(entityType).getTableName();
    }
}
