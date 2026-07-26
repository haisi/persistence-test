package li.selman.persistencetest.hibernate;

/** SPI for resolving a JPA entity class to the name of the table it's mapped to. */
@FunctionalInterface
public interface EntityTableResolver {

    /**
     * Resolves the table {@code entityType} is mapped to.
     *
     * @param entityType a mapped JPA entity class.
     * @return the table name it's mapped to, as Hibernate resolved it (including whatever naming
     *     strategy/physical naming customizations the application configured).
     * @throws IllegalArgumentException if {@code entityType} is not a mapped entity.
     */
    String tableNameOf(Class<?> entityType);
}
