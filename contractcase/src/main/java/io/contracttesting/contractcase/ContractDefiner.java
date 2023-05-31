package io.contracttesting.contractcase;

import io.contract_testing.contractcase.case_boundary.BoundaryContractDefiner;
import org.jetbrains.annotations.NotNull;

public class ContractDefiner {


  private static final String TEST_RUN_ID = "JAVA";
  private final LogPrinter logPrinter;
  private final BoundaryContractDefiner definer;

  public ContractDefiner(final @NotNull ContractCaseConfig config) {

    this.logPrinter = new LogPrinter();

    // TODO figure out how to get versions
    this.definer = new BoundaryContractDefiner(BoundaryConfigMapper.map(config, TEST_RUN_ID),
        logPrinter,
        logPrinter,
        BoundaryVersionGenerator.VERSIONS);
  }

  public void runExample(ExampleDefinition definition,
      final @NotNull ContractCaseConfig additionalConfig) {
    BoundaryResultMapper.map(definer.runExample(BoundaryDefinitionMapper.map(definition),
        BoundaryConfigMapper.map(additionalConfig, TEST_RUN_ID)));
  }

  public void runThrowingExample(ExampleDefinition definition,
      ContractCaseConfig additionalConfig) {
    BoundaryResultMapper.map(definer.runRejectingExample(BoundaryDefinitionMapper.map(definition),
        BoundaryConfigMapper.map(additionalConfig, TEST_RUN_ID)));
  }

}
