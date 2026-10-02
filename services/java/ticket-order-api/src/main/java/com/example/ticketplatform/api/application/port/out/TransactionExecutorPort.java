package com.example.ticketplatform.api.application.port.out;

import java.util.function.Supplier;

public interface TransactionExecutorPort {

  <T> T executeInPrimaryTransaction(Supplier<T> action);
}
