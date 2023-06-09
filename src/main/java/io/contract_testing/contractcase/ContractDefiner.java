package io.contract_testing.contractcase;

import io.contract_testing.contractcase.case_boundary.BoundaryContractDefiner;
import io.contract_testing.contractcase.case_example_mock_types.AnyMockDescriptor;
import org.jetbrains.annotations.NotNull;

public class ContractDefiner {

  private static final String TEST_RUN_ID = "JAVA";
  private final BoundaryContractDefiner definer;


  public ContractDefiner(final @NotNull ContractCaseConfig config) {
    LogPrinter logPrinter = new LogPrinter();
    BoundaryContractDefiner definer = null;
    try {
      definer = new BoundaryContractDefiner(BoundaryConfigMapper.map(config, TEST_RUN_ID),
          logPrinter,
          logPrinter,
          new BoundaryVersionGenerator().getVersions());
    } catch (Throwable e) {
      BoundaryCrashReporter.handleAndRethrow(e);
    }
    this.definer = definer;
  }

  public <T, M extends AnyMockDescriptor> void runExample(ExampleDefinition<M> definition,
      final @NotNull IndividualSuccessTestConfig<T> additionalConfig) {
    try {
      BoundaryResultMapper.map(definer.runExample(BoundaryDefinitionMapper.map(definition),
          BoundaryConfigMapper.mapSuccessExample(additionalConfig, TEST_RUN_ID)));
    } catch (Throwable e) {
      BoundaryCrashReporter.handleAndRethrow(e);
    }
  }

  public <T, M extends AnyMockDescriptor> void runExample(ExampleDefinition<M> definition) {
    this.runExample(
        definition,
        IndividualSuccessTestConfig
            .IndividualSuccessTestConfigBuilder
            .builder()
            .build());
  }

  public <T, M extends AnyMockDescriptor> void runThrowingExample(ExampleDefinition<M> definition,
      IndividualFailedTestConfig<T> additionalConfig) {
    try {
      BoundaryResultMapper.map(definer.runRejectingExample(BoundaryDefinitionMapper.map(definition),
          BoundaryConfigMapper.mapFailingExample(additionalConfig, TEST_RUN_ID)));
    } catch (Throwable e) {
      BoundaryCrashReporter.handleAndRethrow(e);
    }
  }

  public <T, M extends AnyMockDescriptor> void runThrowingExample(ExampleDefinition<M> definition) {
    this.runThrowingExample(
        definition,
        IndividualFailedTestConfig
            .IndividualFailedTestConfigBuilder
            .builder()
            .build());
  }

}
