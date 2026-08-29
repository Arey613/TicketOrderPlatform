package com.example.ticketplatform.api.infrastructure.config.persistence;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientConnectionException;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.JDBCConnectionException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
public class ReadQueryExecutor {

  private final TransactionTemplate readReplicaTransactionTemplate;
  private final TransactionTemplate primaryTransactionTemplate;
  private final Counter fallbackCounter;

  ReadQueryExecutor(
      @Qualifier("readReplicaTransactionManager")
          PlatformTransactionManager readReplicaTransactionManager,
      @Qualifier("primaryTransactionManager") PlatformTransactionManager primaryTransactionManager,
      MeterRegistry meterRegistry) {
    this.readReplicaTransactionTemplate = readOnlyTemplate(readReplicaTransactionManager);
    this.primaryTransactionTemplate = readOnlyTemplate(primaryTransactionManager);
    this.fallbackCounter =
        Counter.builder("ticket_order_api.read_replica.fallback")
            .description("Read query fallbacks from read replica to primary")
            .register(meterRegistry);
  }

  public <T> T execute(Supplier<T> readReplicaOperation, Supplier<T> primaryOperation) {
    try {
      return readReplicaTransactionTemplate.execute(status -> readReplicaOperation.get());
    } catch (RuntimeException exception) {
      if (!isFallbackEligible(exception)) {
        throw exception;
      }
      fallbackCounter.increment();
      log.warn("read-replica.fallback");
      return primaryTransactionTemplate.execute(status -> primaryOperation.get());
    }
  }

  private static TransactionTemplate readOnlyTemplate(
      PlatformTransactionManager transactionManager) {
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.setReadOnly(true);
    return transactionTemplate;
  }

  private static boolean isFallbackEligible(RuntimeException exception) {
    Throwable current = exception;
    while (current != null) {
      if (current instanceof CannotGetJdbcConnectionException
          || current instanceof DataAccessResourceFailureException
          || current instanceof CannotCreateTransactionException
          || current instanceof JDBCConnectionException
          || current instanceof SQLTransientConnectionException
          || current instanceof SQLRecoverableException) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }
}
