package io.contract_testing.contractcase;

import io.contract_testing.contractcase.case_example_mock_types.mocks.base.AnyMockDescriptor;
import io.contract_testing.contractcase.client.InternalDefinerClient;
import org.jetbrains.annotations.NotNull;

public class ContractDefiner {

  private static final String TEST_RUN_ID = "JAVA";
  private final InternalDefinerClient definer;


  public ContractDefiner(final @NotNull ContractCaseConfig config) {
    LogPrinter logPrinter = new LogPrinter();
    InternalDefinerClient definer = null;
    try {
      definer = new InternalDefinerClient(BoundaryConfigMapper.map(config, TEST_RUN_ID),
          logPrinter,
          logPrinter,
          new BoundaryVersionGenerator().getVersions());
    } catch (Throwable e) {
      BoundaryCrashReporter.handleAndRethrow(e);
    }
    this.definer = definer;
  }

  public void endRecord() {
    BoundaryResultMapper.map(this.definer.endRecord());
  }

  public <T, M extends AnyMockDescriptor> void runExample(ExampleDefinition<M> definition,
      final @NotNull IndividualSuccessTestConfig<T> additionalConfig) {
    try {
      BoundaryResultMapper.map(definer.runExample(definition.toJSON(),
          BoundaryConfigMapper.mapSuccessExample(additionalConfig, TEST_RUN_ID)));
    } catch (Throwable e) {
      BoundaryCrashReporter.handleAndRethrow(e);
    }
  }

  public <M extends AnyMockDescriptor> void runExample(ExampleDefinition<M> definition) {
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

  public <M extends AnyMockDescriptor> void runThrowingExample(ExampleDefinition<M> definition) {
    this.runThrowingExample(
        definition,
        IndividualFailedTestConfig
            .IndividualFailedTestConfigBuilder
            .builder()
            .build());
  }

}
