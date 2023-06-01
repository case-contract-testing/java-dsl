package io.contract_testing.contractcase;

public class IndividualSuccessTestConfig<T> extends ContractCaseConfig {

  public Trigger<T> trigger;
  public TestResponseFunction<T> testResponse;

}
