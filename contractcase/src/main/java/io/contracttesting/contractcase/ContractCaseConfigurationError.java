package io.contracttesting.contractcase;

import org.jetbrains.annotations.NotNull;

public class ContractCaseConfigurationError extends RuntimeException {

  private final String location;

  public ContractCaseConfigurationError(@NotNull String message, @NotNull String location) {
    super(message);
    this.location = location;
  }

  public String getLocation() {
    return location;
  }
}
