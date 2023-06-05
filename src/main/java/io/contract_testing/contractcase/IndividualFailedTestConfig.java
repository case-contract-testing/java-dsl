package io.contract_testing.contractcase;

import java.util.Map;

public class IndividualFailedTestConfig<T> extends ContractCaseConfig {

  public final Trigger<T> trigger;
  public final TestErrorResponseFunction testErrorResponse;

  private IndividualFailedTestConfig(String providerName, String consumerName, LogLevel logLevel,
      String contractDir, String contractFilename, Boolean printResults, Boolean throwOnFail,
      PublishType publish, String brokerBaseUrl, String brokerCiAccessToken,
      BrokerBasicAuthCredentials brokerBasicAuth, String baseUrlUnderTest, TriggerGroups triggers,
      Map<String, StateHandler> stateHandlers, Trigger<T> trigger,
      TestErrorResponseFunction testErrorResponse) {
    super(providerName, consumerName, logLevel, contractDir, contractFilename, printResults,
        throwOnFail, publish, brokerBaseUrl, brokerCiAccessToken, brokerBasicAuth, baseUrlUnderTest,
        triggers, stateHandlers);
    this.trigger = trigger;
    this.testErrorResponse = testErrorResponse;
  }

  public static final class IndividualFailedTestConfigBuilder<T> {

    private String providerName;
    private String consumerName;
    private LogLevel logLevel;
    private String contractDir;
    private String contractFilename;
    private Boolean printResults;
    private Boolean throwOnFail;
    private PublishType publish;
    private String brokerBaseUrl;
    private String brokerCiAccessToken;
    private BrokerBasicAuthCredentials brokerBasicAuth;
    private String baseUrlUnderTest;
    private TriggerGroups triggers;
    private Map<String, StateHandler> stateHandlers;
    private Trigger<T> trigger;
    private TestErrorResponseFunction testErrorResponse;

    private IndividualFailedTestConfigBuilder() {
    }

    public static <T> IndividualFailedTestConfigBuilder<T> builder() {
      return new IndividualFailedTestConfigBuilder<T>();
    }

    public IndividualFailedTestConfigBuilder<T> withProviderName(String providerName) {
      this.providerName = providerName;
      return this;
    }

    public IndividualFailedTestConfigBuilder<T> withConsumerName(String consumerName) {
      this.consumerName = consumerName;
      return this;
    }

    public IndividualFailedTestConfigBuilder<T> withLogLevel(LogLevel logLevel) {
      this.logLevel = logLevel;
      return this;
    }

    public IndividualFailedTestConfigBuilder<T> withContractDir(String contractDir) {
      this.contractDir = contractDir;
      return this;
    }

    public IndividualFailedTestConfigBuilder<T> withContractFilename(String contractFilename) {
      this.contractFilename = contractFilename;
      return this;
    }

    public IndividualFailedTestConfigBuilder<T> withPrintResults(Boolean printResults) {
      this.printResults = printResults;
      return this;
    }

    public IndividualFailedTestConfigBuilder<T> withThrowOnFail(Boolean throwOnFail) {
      this.throwOnFail = throwOnFail;
      return this;
    }

    public IndividualFailedTestConfigBuilder<T> withPublish(PublishType publish) {
      this.publish = publish;
      return this;
    }

    public IndividualFailedTestConfigBuilder<T> withBrokerBaseUrl(String brokerBaseUrl) {
      this.brokerBaseUrl = brokerBaseUrl;
      return this;
    }

    public IndividualFailedTestConfigBuilder<T> withBrokerCiAccessToken(
        String brokerCiAccessToken) {
      this.brokerCiAccessToken = brokerCiAccessToken;
      return this;
    }

    public IndividualFailedTestConfigBuilder<T> withBrokerBasicAuth(
        BrokerBasicAuthCredentials brokerBasicAuth) {
      this.brokerBasicAuth = brokerBasicAuth;
      return this;
    }

    public IndividualFailedTestConfigBuilder<T> withBaseUrlUnderTest(String baseUrlUnderTest) {
      this.baseUrlUnderTest = baseUrlUnderTest;
      return this;
    }

    public IndividualFailedTestConfigBuilder<T> withTriggers(TriggerGroups triggers) {
      this.triggers = triggers;
      return this;
    }

    public IndividualFailedTestConfigBuilder<T> withStateHandlers(
        Map<String, StateHandler> stateHandlers) {
      this.stateHandlers = stateHandlers;
      return this;
    }

    public IndividualFailedTestConfigBuilder<T> withTrigger(Trigger<T> trigger) {
      this.trigger = trigger;
      return this;
    }

    public IndividualFailedTestConfigBuilder<T> withTestErrorResponse(
        TestErrorResponseFunction testErrorResponse) {
      this.testErrorResponse = testErrorResponse;
      return this;
    }

    public IndividualFailedTestConfig<T> build() {
      return new IndividualFailedTestConfig<>(providerName, consumerName, logLevel, contractDir,
          contractFilename, printResults, throwOnFail, publish, brokerBaseUrl, brokerCiAccessToken,
          brokerBasicAuth, baseUrlUnderTest, triggers, stateHandlers, trigger, testErrorResponse);
    }
  }
}
