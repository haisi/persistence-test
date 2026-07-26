/**
 * Optional Hibernate integration: resolves a JPA entity class to its mapped table name via a live
 * {@link jakarta.persistence.EntityManagerFactory}, and entity-aware query assertions built on top of that.
 *
 * <p>Kept as a separate module (rather than folded into {@code query-assertions}) because it's the only
 * part of this project that depends on Hibernate - {@code query-assertions} and everything below it stay
 * usable with plain JDBC or JdbcTemplate.
 */
@NullMarked
package li.selman.persistencetest.hibernate;

import org.jspecify.annotations.NullMarked;
