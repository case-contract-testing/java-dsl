package io.contract_testing.contractcase;

import io.contract_testing.contractcase.case_boundary.BoundaryContractVerifier;
import io.contract_testing.contractcase.case_boundary.BoundaryResult;
import io.contract_testing.contractcase.case_boundary.BoundarySuccess;
import io.contract_testing.contractcase.case_boundary.IInvokeCoreTest;
import io.contract_testing.contractcase.case_boundary.IRunTestCallback;
import org.jetbrains.annotations.NotNull;

public class ContractVerifier {

  private final BoundaryContractVerifier verifier;

  public ContractVerifier(ContractCaseConfig config) {
    LogPrinter logPrinter = new LogPrinter();
    this.verifier = new BoundaryContractVerifier(BoundaryConfigMapper.map(config, "VERIFICATION"),
        new IRunTestCallback() {
          @Override
          public @NotNull BoundaryResult runTest(@NotNull String testName,
              @NotNull IInvokeCoreTest invoker) {
            // TODO replace this with something that knows about JUnit
            try {
              try {
                BoundaryResultMapper.map(invoker.verify());
              } catch (Exception e) {
                System.err.println(e);
              }
              return new BoundarySuccess();
            } catch (Exception e) {
              return BoundaryExceptionMapper.map(e);
            }
          }

        }, logPrinter, logPrinter, new BoundaryVersionGenerator().getVersions());
  }

  public void runVerification(ContractCaseConfig configOverrides) {
    this.verifier.runVerification(
        BoundaryConfigMapper.map(configOverrides, "VERIFICATION"));
  }
}
