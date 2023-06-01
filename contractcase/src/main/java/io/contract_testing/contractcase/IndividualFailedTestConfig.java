package io.contract_testing.contractcase;

public class IndividualFailedTestConfig<T> extends ContractCaseConfig {

  public Trigger<T> trigger;
  public TestErrorResponseFunction testErrorResponse;

}
