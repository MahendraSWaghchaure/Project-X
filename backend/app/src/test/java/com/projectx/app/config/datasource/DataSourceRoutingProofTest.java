package com.projectx.app.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import com.projectx.notification.dto.InquiryDtos.CreateInquiryRequest;
import com.projectx.notification.service.InquiryService;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * End-to-end proof, against the real Spring context and the real (local, test) Postgres database
 * - not just the isolated routing-logic unit test (ReplicaRoutingDataSourceTest) - that wiring
 * DataSourceRoutingConfig into the actual application does not introduce a write-then-read
 * visibility gap. This is the real risk of this kind of change and the thing that actually needs
 * proving rather than asserting: today "replica" and "primary" resolve to the exact same
 * HikariDataSource instance (see DataSourceRoutingConfig - no spring.datasource.replica.url is
 * configured anywhere in this test run), so a write committed through a plain (non-readOnly)
 * @Transactional method MUST be immediately visible to a @Transactional(readOnly = true) method
 * called right after - if it weren't, that would mean the routing/bean wiring itself is broken,
 * not that real replication lag exists (there is no second database here to lag).
 *
 * <p>Deliberately reuses two already-existing, unrelated production methods
 * (InquiryService.submit() - plain @Transactional - and InquiryService.countNew() -
 * @Transactional(readOnly = true)) rather than inventing a throwaway table, so this exercises the
 * exact same annotation-driven routing every other read/write pair in the app goes through.
 */
@SpringBootTest
@ActiveProfiles("test")
class DataSourceRoutingProofTest {

    @Autowired
    private InquiryService inquiryService;

    @Autowired
    private DataSource dataSource;

    @Test
    void routingDataSourceIsWiredAsThePrimaryBean() {
        // Confirms DataSourceRoutingConfig's @Primary dataSource() bean is actually what JPA/JDBC
        // see everywhere else in the app, not some other DataSource left over from autoconfiguration.
        assertThat(dataSource).isInstanceOf(ReplicaRoutingDataSource.class);
    }

    @Test
    void writeIsImmediatelyVisibleToReadOnlyTransactionRightAfter() {
        long before = inquiryService.countNew();

        // submit() is a plain (non-readOnly) @Transactional method - routed to "primary" - and has
        // fully committed by the time it returns (Spring's transactional proxy commits before
        // control returns to the caller).
        inquiryService.submit(new CreateInquiryRequest(
                "Routing Test " + UUID.randomUUID(), "routing-test@example.com", "Proving read-after-write visibility", null));

        // countNew() is @Transactional(readOnly = true) - routed to "replica". With no real
        // replica configured, "replica" IS "primary" (same pool, same instance) - so this must see
        // the row just committed above, immediately, every time.
        long after = inquiryService.countNew();

        assertThat(after).isEqualTo(before + 1);
    }
}
