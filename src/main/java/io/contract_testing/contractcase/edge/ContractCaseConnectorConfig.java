package io.contract_testing.contractcase.edge;

import io.contract_testing.contractcase.BrokerBasicAuthCredentials;
import io.contract_testing.contractcase.ContractCaseConfig;
import io.contract_testing.contractcase.LogLevel;
import io.contract_testing.contractcase.PublishType;
import io.contract_testing.contractcase.StateHandler;
import io.contract_testing.contractcase.TriggerGroups;
import java.util.Map;

public class ContractCaseConnectorConfig extends ContractCaseConfig {

  public final ITriggerFunction triggerAndTest;
  private final Map<String, ConnectorStateHandler> connectorStateHandlers;
  /**
   * Triggers are deprecated in the connector config, since they're only included due to the
   * inheritance
   */
  @Deprecated(since = "*")
  public TriggerGroups triggers;

  /**
   * State handlers are deprecated in the connector config, since they're only included due to the
   * inheritance
   */
  @Deprecated(since = "*")
  public Map<String, StateHandler> stateHandlers;

  public final String testRunId;
  public final Map<String, ? extends ITriggerFunction> triggerAndTests;

  protected ContractCaseConnectorConfig(String providerName, String consumerName, LogLevel logLevel,
      String contractDir, String contractFilename, Boolean printResults, Boolean throwOnFail,
      PublishType publish, String brokerBaseUrl, String brokerCiAccessToken,
      BrokerBasicAuthCredentials brokerBasicAuth, String baseUrlUnderTest,
      Map<String, ConnectorStateHandler> stateHandlers, String testRunId,
      Map<String, ? extends ITriggerFunction> triggerAndTests, ITriggerFunction triggerAndTest) {
    super(
        providerName,
        consumerName,
        logLevel,
        contractDir,
        contractFilename,
        printResults,
        throwOnFail,
        publish,
        brokerBaseUrl,
        brokerCiAccessToken,
        brokerBasicAuth,
        baseUrlUnderTest,
        null,
        null
    );
    this.testRunId = testRunId;
    this.triggerAndTests = triggerAndTests;
    this.triggerAndTest = triggerAndTest;
    this.connectorStateHandlers = stateHandlers;

  }


  public static Builder builder() {
    return Builder.aContractCaseConnectorConfig();
  }

  public Map<String, ? extends ITriggerFunction> getTriggerAndTests() {
    return this.triggerAndTests;
  }

  public ITriggerFunction getTriggerAndTest() {
    return this.triggerAndTest;
  }

  public Map<String, ConnectorStateHandler> getConnectorStateHandlers() {
    return this.connectorStateHandlers;
  }

  @Deprecated
  @Override
  public Map<String, StateHandler> getStateHandlers() {
    throw new RuntimeException("This method should not be called");
  }


  @Deprecated
  @Override
  public TriggerGroups getTriggers() {
    throw new RuntimeException("This method should not be called");
  }

  public static final class Builder {

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
    private Map<String, ConnectorStateHandler> stateHandlers;
    private String testRunId;
    private Map<String, ? extends ITriggerFunction> triggerAndTests;
    private ITriggerFunction triggerAndTest;

    private Builder() {
    }

    public static Builder aContractCaseConnectorConfig() {
      return new Builder();
    }

    public Builder providerName(String providerName) {
      this.providerName = providerName;
      return this;
    }

    public Builder consumerName(String consumerName) {
      this.consumerName = consumerName;
      return this;
    }

    public Builder logLevel(LogLevel logLevel) {
      this.logLevel = logLevel;
      return this;
    }

    public Builder contractDir(String contractDir) {
      this.contractDir = contractDir;
      return this;
    }

    public Builder contractFilename(String contractFilename) {
      this.contractFilename = contractFilename;
      return this;
    }

    public Builder printResults(Boolean printResults) {
      this.printResults = printResults;
      return this;
    }

    public Builder throwOnFail(Boolean throwOnFail) {
      this.throwOnFail = throwOnFail;
      return this;
    }

    public Builder publish(PublishType publish) {
      this.publish = publish;
      return this;
    }

    public Builder brokerBaseUrl(String brokerBaseUrl) {
      this.brokerBaseUrl = brokerBaseUrl;
      return this;
    }

    public Builder brokerCiAccessToken(String brokerCiAccessToken) {
      this.brokerCiAccessToken = brokerCiAccessToken;
      return this;
    }

    public Builder brokerBasicAuth(BrokerBasicAuthCredentials brokerBasicAuth) {
      this.brokerBasicAuth = brokerBasicAuth;
      return this;
    }

    public Builder baseUrlUnderTest(String baseUrlUnderTest) {
      this.baseUrlUnderTest = baseUrlUnderTest;
      return this;
    }

    public Builder stateHandlers(Map<String, ConnectorStateHandler> stateHandlers) {
      this.stateHandlers = stateHandlers;
      return this;
    }

    public Builder testRunId(String testRunId) {
      this.testRunId = testRunId;
      return this;
    }

    public Builder triggerAndTests(Map<String, ? extends ITriggerFunction> triggerAndTests) {
      this.triggerAndTests = triggerAndTests;
      return this;
    }

    public Builder triggerAndTest(ITriggerFunction triggerAndTest) {
      this.triggerAndTest = triggerAndTest;
      return this;
    }

    public ContractCaseConnectorConfig build() {
      return new ContractCaseConnectorConfig(
          providerName,
          consumerName,
          logLevel,
          contractDir,
          contractFilename,
          printResults,
          throwOnFail,
          publish,
          brokerBaseUrl,
          brokerCiAccessToken,
          brokerBasicAuth,
          baseUrlUnderTest,
          stateHandlers,
          testRunId,
          triggerAndTests,
          triggerAndTest
      );
    }


  }
}