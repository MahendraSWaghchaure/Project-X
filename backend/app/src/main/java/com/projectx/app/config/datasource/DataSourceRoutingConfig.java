package com.projectx.app.config.datasource;

import com.zaxxer.hikari.HikariDataSource;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.flyway.autoconfigure.FlywayDataSource;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Read/write datasource routing - prep for a real Postgres read replica that does not exist yet
 * in this deployment (no budget/infra decision has been made for provisioning one; this is
 * application-side plumbing only). <b>SAFE-INACTIVE by default</b>: with no
 * {@code spring.datasource.replica.url} configured (the current and only real state), the
 * "replica" routing target below IS the exact same {@link HikariDataSource} instance as
 * "primary" - so every read and every write still goes through the one connection pool this app
 * has always used, byte-for-byte the same as before this class existed. See
 * {@code DataSourceRoutingProofTest} (app module test) for the actual proof that a write and a
 * read-only query in the same test run still see each other's data correctly - the real risk this
 * kind of change can introduce, which is not something to merely assert.
 *
 * <p>Every {@code @Transactional(readOnly = true)} method in the whole application is routed to
 * the "replica" target by {@link ReplicaRoutingDataSource}; everything else (every write, and
 * anything outside a transaction at all, e.g. Flyway) goes to "primary".
 *
 * <p><b>To turn on real replica routing once a second Postgres instance exists</b>, set:
 * <ul>
 *   <li>{@code spring.datasource.replica.url} (e.g. via a {@code DB_REPLICA_URL} env var) - the
 *       replica's own JDBC URL. Setting this alone activates a second, real Hikari connection
 *       pool and starts routing every read-only transaction to it.
 *   <li>{@code spring.datasource.replica.username} / {@code .password} if the replica needs
 *       different credentials from the primary (both are bound from the same
 *       {@link DataSourceProperties} shape the primary datasource uses).
 * </ul>
 * Flyway and every write always go to the primary regardless - a real Postgres streaming replica
 * physically rejects writes, so this matches that reality rather than merely approximating it.
 */
@Configuration
public class DataSourceRoutingConfig {

    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * The one real, writable datasource - built exactly the way Spring Boot's own
     * {@code DataSourceAutoConfiguration} would have built the app's single {@code dataSource}
     * bean before this class existed (same {@code spring.datasource.*} URL/credentials, same
     * {@code spring.datasource.hikari.*} pool settings - maximum-pool-size, minimum-idle,
     * connection-timeout, leak-detection-threshold, see application.yml). Declaring any
     * {@code DataSource} bean here makes Spring Boot's own autoconfiguration back off
     * automatically (it's conditional on no such bean existing), so nothing double-configures.
     *
     * <p>Marked {@code @FlywayDataSource} so migrations always run directly against this pool,
     * never through the routing layer below - correct regardless of transaction state in
     * principle (no active transaction defaults to "primary" anyway, see
     * ReplicaRoutingDataSource), but explicit rather than relying on that default for something
     * this important to get right.
     */
    @Bean
    @FlywayDataSource
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource primaryDataSource(DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.replica")
    public DataSourceProperties replicaDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * Deliberately NOT annotated {@code @ConfigurationProperties} on this method itself - unlike
     * {@code primaryDataSource()} above, this one can return the EXACT SAME instance as
     * {@code primaryDataSource} (the SAFE-INACTIVE case), and {@code @ConfigurationProperties} on
     * a {@code @Bean} method re-binds its properties onto whatever instance that method returns -
     * which would silently overwrite the primary pool's own hikari settings in that case. Once a
     * real replica exists and its own {@code spring.datasource.replica.hikari.*} pool sizing
     * matters, add a dedicated bean method that unconditionally builds a brand new
     * {@code HikariDataSource} (never one that might return {@code primaryDataSource}) and
     * annotate THAT one instead - the same pattern {@code primaryDataSource()} already uses
     * safely because it never has this ambiguity.
     *
     * <p>{@code @Qualifier("primaryDataSource")} is required, not optional, on the second
     * parameter below - with three {@code DataSource} beans in play and one of them
     * ({@code dataSource} itself) marked {@code @Primary}, Spring's autowiring resolves
     * {@code @Primary} BEFORE falling back to by-name matching, so an unqualified
     * {@code DataSource primaryDataSource} parameter here would actually resolve to the
     * {@code dataSource} bean - which depends on this bean - a circular reference. (Caught by
     * ReplicaRoutingDataSourceTest/DataSourceRoutingProofTest failing to even load the context
     * when this was first written without the qualifier.)
     */
    @Bean
    public DataSource replicaDataSource(DataSourceProperties replicaDataSourceProperties,
                                         @Qualifier("primaryDataSource") DataSource primaryDataSource) {
        String replicaUrl = replicaDataSourceProperties.getUrl();
        if (replicaUrl == null || replicaUrl.isBlank()) {
            // SAFE-INACTIVE: no replica configured - "replica" IS "primary", see class javadoc.
            return primaryDataSource;
        }
        return replicaDataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    /**
     * The bean every other piece of the application (JPA, JdbcTemplate, anything that just wants
     * "the datasource") actually sees - {@code @Primary} so it wins over the two concrete pools
     * above whenever Spring has to pick one {@code DataSource} for an unqualified injection point.
     */
    @Bean
    @Primary
    public DataSource dataSource(@Qualifier("primaryDataSource") DataSource primaryDataSource,
                                  @Qualifier("replicaDataSource") DataSource replicaDataSource) {
        ReplicaRoutingDataSource routingDataSource = new ReplicaRoutingDataSource();
        routingDataSource.setTargetDataSources(Map.of(
                ReplicaRoutingDataSource.PRIMARY, primaryDataSource,
                ReplicaRoutingDataSource.REPLICA, replicaDataSource));
        routingDataSource.setDefaultTargetDataSource(primaryDataSource);
        return routingDataSource;
    }
}
