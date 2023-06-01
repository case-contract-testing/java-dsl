package io.contract_testing.contractcase;

import org.jetbrains.annotations.NotNull;

public class ContractCaseCoreError extends RuntimeException {

  private final String location;

  public ContractCaseCoreError(@NotNull String message, @NotNull String location) {
    super(message);
    this.location = location;
  }

  public String getLocation() {
    return location;
  }
}
