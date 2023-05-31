package io.contracttesting.contractcase;

import io.contract_testing.contractcase.case_boundary.ContractCaseBoundaryConfig;
import io.contract_testing.contractcase.case_boundary.UserNamePassword;

class BoundaryConfigMapper {

  static ContractCaseBoundaryConfig map(final ContractCaseConfig config,
      final String testRunId) {

    var builder = ContractCaseBoundaryConfig.builder().testRunId(testRunId);

    if (config.brokerBaseUrl != null) {
      builder.brokerBaseUrl(config.brokerBaseUrl);
    }

    if (config.providerName != null) {
      builder.providerName(config.providerName);
    }

    if (config.consumerName != null) {
      builder.consumerName(config.consumerName);
    }

    if (config.logLevel != null) {
      builder.logLevel(config.logLevel.toString());
    }

    if (config.contractDir != null) {
      builder.contractDir(config.contractDir);
    }

    if (config.contractFilename != null) {
      builder.contractFilename(config.contractFilename);
    }

    if (config.printResults != null) {
      builder.printResults(config.printResults);
    }

    if (config.throwOnFail != null) {
      builder.throwOnFail(config.throwOnFail);
    }

    if (config.publish != null) {
      builder.publish(config.publish.toString());
    }
    if (config.brokerCiAccessToken != null) {
      builder.brokerCiAccessToken(config.brokerCiAccessToken);
    }

    if (config.brokerBasicAuth != null) {
      builder.brokerBasicAuth(
          UserNamePassword.builder().password(config.brokerBasicAuth.password())
              .username(config.brokerBasicAuth.username()).build());
    }

    if (config.baseUrlUnderTest != null) {
      builder.baseUrlUnderTest(config.baseUrlUnderTest);
    }

    if (config.triggers != null) {
      builder.triggerAndTests(BoundaryTriggerGroupMapper.map(config.triggers));
    }

    if (config.stateHandlers != null) {
      builder.stateHandlers(BoundaryStateHandlerMapper.map(config.stateHandlers));
    }

    return builder.build();
  }
}


