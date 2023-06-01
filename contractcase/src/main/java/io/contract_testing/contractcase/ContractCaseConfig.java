package io.contract_testing.contractcase;

import java.util.Map;

public class ContractCaseConfig {


  /**
   * The name of the provider for this contract.
   */
  public String providerName;

  /**
   * The name of the consumer for this contract.
   */
  public String consumerName;


  /**
   * How much should we log during the test?
   *
   * @see LogLevel
   */
  public LogLevel logLevel;

  /**
   * The directory where the contract will be written. If you provide this, ContractCase will
   * generate the filename for you (unless `contractFilename` is specified, in which case this
   * setting is ignored)
   */
  public String contractDir;

  /**
   * The filename where the contract will be written. If you provide this, `contractDir` is ignored
   */
  public String contractFilename;

  /**
   * Whether results should be printed on standard out during the test run
   */
  public Boolean printResults;


  /**
   * Whether the test should throw an error if the matching fails.
   * <p>
   * Note that any configuration errors will still fail the suite regardless of this setting. This
   * includes exceptions thrown during trigger functions, but does not include exceptions thrown by
   * testResponse functions.
   * <p>
   * Default: `true` in contract definition, `false` in contract verification
   */
  public Boolean throwOnFail;

  /**
   * Whether to publish contracts or verification results to the broker
   *
   * @see PublishType
   */
  public PublishType publish;

  /**
   * The base URL for the contract broker
   */
  public String brokerBaseUrl;

  /**
   * The access token to use for the contract broker. Must have CI scope.
   * <p>
   * If this is specified along with brokerBasicAuth, the basic auth is ignored.
   */
  public String brokerCiAccessToken;

  /**
   * The basic authentication username and password to access the contract broker.
   * <p>
   * If this is specified along with brokerCiAccessToken, basic auth credentials are ignored.
   */
  public BrokerBasicAuthCredentials brokerBasicAuth;

  /**
   * The base URL for your real server, if you are testing an http server.
   *
   * @deprecated This will be moved to a config property that allows configuration for arbitrary
   * mocks
   */
  public String baseUrlUnderTest;


  public TriggerGroups triggers;

  /**
   * State setup and teardown handlers for any states this test requires (see (<a
   * href="https://case.contract-testing.io/docs/reference/state-handlers/">writing state
   * handlers</a>)) for more details
   */
  public Map<String, StateHandler> stateHandlers;
}
