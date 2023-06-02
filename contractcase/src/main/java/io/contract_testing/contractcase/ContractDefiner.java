package io.contract_testing.contractcase;

import io.contract_testing.contractcase.case_boundary.BoundaryContractDefiner;
import io.contract_testing.contractcase.case_example_mock_types.AnyMockDescriptor;
import org.jetbrains.annotations.NotNull;

public class ContractDefiner {

  private static final String TEST_RUN_ID = "JAVA";
  private final BoundaryContractDefiner definer;


  public ContractDefiner(final @NotNull ContractCaseConfig config) {

    LogPrinter logPrinter = new LogPrinter();

    this.definer = new BoundaryContractDefiner(BoundaryConfigMapper.map(config, TEST_RUN_ID),
        logPrinter,
        logPrinter,
        new BoundaryVersionGenerator().getVersions());
  }

  public <T, M extends AnyMockDescriptor> void runExample(ExampleDefinition<M> definition,
      final @NotNull IndividualSuccessTestConfig<T> additionalConfig) {
    BoundaryResultMapper.map(definer.runExample(BoundaryDefinitionMapper.map(definition),
        BoundaryConfigMapper.mapSuccessExample(additionalConfig, TEST_RUN_ID)));
  }

  public <T, M extends AnyMockDescriptor> void runThrowingExample(ExampleDefinition<M> definition,
      IndividualFailedTestConfig<T> additionalConfig) {
    BoundaryResultMapper.map(definer.runRejectingExample(BoundaryDefinitionMapper.map(definition),
        BoundaryConfigMapper.mapFailingExample(additionalConfig, TEST_RUN_ID)));
  }

}
