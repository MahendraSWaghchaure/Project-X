package com.projectx.app.config.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Routes every connection request to one of two logical targets, "primary" or "replica", based on
 * whether the currently active Spring transaction is read-only ({@code @Transactional(readOnly =
 * true)}). {@code TransactionSynchronizationManager.isCurrentTransactionReadOnly()} is set by
 * Spring's transaction interceptor itself at transaction-start time, for every kind of transaction
 * manager (JPA included) - so this is reliable regardless of which service/repository layer
 * actually triggers the connection fetch, and it defaults to {@code false} when there is no
 * active transaction at all (e.g. Flyway migrations, which are also explicitly pointed at the
 * primary datasource directly via {@code @FlywayDataSource} - see DataSourceRoutingConfig - so
 * they never go through this lookup in the first place).
 *
 * <p><b>SAFE-INACTIVE by default.</b> See DataSourceRoutingConfig's javadoc for exactly what
 * target each key maps to today (both map to the same primary HikariDataSource instance - there
 * is no second Postgres instance in this deployment) and what an operator sets to change that.
 */
public class ReplicaRoutingDataSource extends AbstractRoutingDataSource {

    public static final String PRIMARY = "primary";
    public static final String REPLICA = "replica";

    @Override
    protected Object determineCurrentLookupKey() {
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly() ? REPLICA : PRIMARY;
    }
}
