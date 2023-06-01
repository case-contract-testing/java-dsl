package io.contracttesting.contractcase;

import io.contract_testing.contractcase.case_boundary.ContractCaseBoundaryConfig;
import io.contract_testing.contractcase.case_boundary.UserNamePassword;
import org.jetbrains.annotations.NotNull;

class BoundaryConfigMapper {

  static <T> ContractCaseBoundaryConfig mapSuccessExample(
      final IndividualSuccessTestConfig<T> config,
      final String testRunId) {
    var builder = makeBuilder(config, testRunId);

    if (config.trigger != null) {
      if (config.testResponse != null) {
        builder.triggerAndTest(BoundaryTriggerMapper.map(config.trigger, config.testResponse));
      } else {
        throw new ContractCaseConfigurationError(
            "Must specify `testResponse` if you are specifying a `trigger`");
      }
    } else {
      if (config.testResponse != null) {
        throw new ContractCaseConfigurationError(
            "Must specify `trigger` if you are specifying a `testResponse` function");
      }
    }

    return builder.build();
  }

  public static <T> ContractCaseBoundaryConfig mapFailingExample(
      IndividualFailedTestConfig<T> config,
      String testRunId) {
    var builder = makeBuilder(config, testRunId);

    if (config.trigger != null) {
      if (config.testErrorResponse != null) {
        builder.triggerAndTest(BoundaryTriggerMapper.map(config.trigger, config.testErrorResponse));
      } else {
        throw new ContractCaseConfigurationError(
            "Must specify `testErrorResponse` if you are specifying a `trigger`");
      }
    } else {
      if (config.testErrorResponse != null) {
        throw new ContractCaseConfigurationError(
            "Must specify `trigger` if you are specifying a `testErrorResponse` function");
      }
    }
    
    return builder.build();
  }

  static ContractCaseBoundaryConfig map(final ContractCaseConfig config,
      final String testRunId) {

    return makeBuilder(config, testRunId).build();
  }

  @NotNull
  private static ContractCaseBoundaryConfig.Builder makeBuilder(ContractCaseConfig config,
      String testRunId) {
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
    return builder;
  }


}


