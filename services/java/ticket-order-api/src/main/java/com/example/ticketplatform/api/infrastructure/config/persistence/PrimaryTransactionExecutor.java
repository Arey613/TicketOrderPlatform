package com.example.ticketplatform.api.infrastructure.config.persistence;

import com.example.ticketplatform.api.application.port.out.TransactionExecutorPort;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
class PrimaryTransactionExecutor implements TransactionExecutorPort {

  private final TransactionTemplate primaryTransactionTemplate;

  PrimaryTransactionExecutor(
      @Qualifier("primaryTransactionManager") PlatformTransactionManager primaryTransactionManager) {
    this.primaryTransactionTemplate = new TransactionTemplate(primaryTransactionManager);
  }

  @Override
  public <T> T executeInPrimaryTransaction(Supplier<T> action) {
    return primaryTransactionTemplate.execute(status -> action.get());
  }
}
