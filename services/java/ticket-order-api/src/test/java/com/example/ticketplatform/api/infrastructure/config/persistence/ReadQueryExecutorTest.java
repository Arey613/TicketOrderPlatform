package com.example.ticketplatform.api.infrastructure.config.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

class ReadQueryExecutorTest {

  @Test
  void executesOnlyReadReplicaOperationWhenItSucceeds() {
    CountingTransactionManager readReplicaTransactionManager = new CountingTransactionManager();
    CountingTransactionManager primaryTransactionManager = new CountingTransactionManager();
    ReadQueryExecutor executor =
        new ReadQueryExecutor(
            readReplicaTransactionManager,
            primaryTransactionManager,
            new SimpleMeterRegistry());

    String result = executor.execute(() -> "replica", () -> "primary");

    assertThat(result).isEqualTo("replica");
    assertThat(readReplicaTransactionManager.beginCount).isEqualTo(1);
    assertThat(primaryTransactionManager.beginCount).isZero();
  }

  @Test
  void fallsBackToPrimaryWhenReadReplicaConnectionFails() {
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    CountingTransactionManager readReplicaTransactionManager = new CountingTransactionManager();
    CountingTransactionManager primaryTransactionManager = new CountingTransactionManager();
    ReadQueryExecutor executor =
        new ReadQueryExecutor(
            readReplicaTransactionManager, primaryTransactionManager, meterRegistry);

    String result =
        executor.execute(
            () -> {
              throw new CannotGetJdbcConnectionException("replica unavailable");
            },
            () -> "primary");

    assertThat(result).isEqualTo("primary");
    assertThat(readReplicaTransactionManager.beginCount).isEqualTo(1);
    assertThat(primaryTransactionManager.beginCount).isEqualTo(1);
    assertThat(meterRegistry.counter("ticket_order_api.read_replica.fallback").count())
        .isEqualTo(1);
  }

  @Test
  void doesNotFallbackForSqlQueryFailures() {
    CountingTransactionManager readReplicaTransactionManager = new CountingTransactionManager();
    CountingTransactionManager primaryTransactionManager = new CountingTransactionManager();
    ReadQueryExecutor executor =
        new ReadQueryExecutor(
            readReplicaTransactionManager,
            primaryTransactionManager,
            new SimpleMeterRegistry());

    assertThatThrownBy(
            () ->
                executor.execute(
                    () -> {
                      throw new BadSqlGrammarException(
                          "findEvents", "SELECT invalid", new SQLException("bad query"));
                    },
                    () -> "primary"))
        .isInstanceOf(BadSqlGrammarException.class);

    assertThat(readReplicaTransactionManager.beginCount).isEqualTo(1);
    assertThat(primaryTransactionManager.beginCount).isZero();
  }

  private static final class CountingTransactionManager extends AbstractPlatformTransactionManager {

    private int beginCount;

    @Override
    protected Object doGetTransaction() {
      return new Object();
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
      beginCount++;
      assertThat(definition.isReadOnly()).isTrue();
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {}

    @Override
    protected void doRollback(DefaultTransactionStatus status) {}
  }
}
