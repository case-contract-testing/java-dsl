package io.contracttesting.contractcase;

public class IndividualFailedTestConfig<T> extends ContractCaseConfig {

  public Trigger<T> trigger;
  public TestErrorResponseFunction testErrorResponse;

}
