package io.contract_testing.contractcase;

import io.contract_testing.contractcase.case_boundary.BoundaryResult;
import io.contract_testing.contractcase.case_boundary.BoundarySuccess;
import io.contract_testing.contractcase.case_boundary.IInvokeCoreTest;
import io.contract_testing.contractcase.case_boundary.IRunTestCallback;
import io.contract_testing.contractcase.client.InternalVerifierClient;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class ContractVerifier {

  private final InternalVerifierClient verifier;

  public ContractVerifier(ContractCaseConfig config) {
    LogPrinter logPrinter = new LogPrinter();

    InternalVerifierClient verification = null;
    try {
      verification = new InternalVerifierClient(
          BoundaryConfigMapper.map(config, "VERIFICATION"),
          new IRunTestCallback() {
            @Override
            public @NotNull BoundaryResult runTest(@NotNull String testName,
                @NotNull IInvokeCoreTest invoker) {
              // TODO replace this with something that knows about JUnit
              try {
                try {
                  BoundaryResultMapper.mapVoid(invoker.verify());
                } catch (Exception e) {
                  System.err.println(e);
                }
                return new BoundarySuccess();
              } catch (Exception e) {
                return BoundaryExceptionMapper.map(e);
              }
            }

          }, logPrinter, logPrinter, new BoundaryVersionGenerator().getVersions()
      );
    } catch (Throwable e) {
      BoundaryCrashReporter.handleAndRethrow(e);
    }
    this.verifier = verification;
  }

  public List<ContractDescription> availableContractDescriptions() {
    try {
      return BoundaryResultMapper.mapListAvailableContracts(this.verifier.availableContractDescriptions());
    } catch (Throwable e) {
      BoundaryCrashReporter.handleAndRethrow(e);
      // This is actually unreachable, since the above method always throws
      return List.of();
    }
  }

  public void runVerification(ContractCaseConfig configOverrides) {
    try {
      this.verifier.runVerification(
          BoundaryConfigMapper.map(configOverrides, "VERIFICATION"));
    } catch (Throwable e) {
      BoundaryCrashReporter.handleAndRethrow(e);
    }
  }
}
