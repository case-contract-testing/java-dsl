package io.contracttesting.contractcase;

public class IndividualSuccessTestConfig<T> extends ContractCaseConfig {

  public Trigger<T> trigger;
  public TestResponseFunction<T> testResponse;

}
