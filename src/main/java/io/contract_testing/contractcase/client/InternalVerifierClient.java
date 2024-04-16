package io.contract_testing.contractcase.client;

import io.contract_testing.contractcase.LogPrinter;
import io.contract_testing.contractcase.case_boundary.BoundaryResult;
import io.contract_testing.contractcase.case_boundary.ContractCaseBoundaryConfig;
import io.contract_testing.contractcase.edge.ConnectorResult;
import io.contract_testing.contractcase.edge.ConnectorResultTypeConstants;
import io.contract_testing.contractcase.edge.RunTestCallback;
import io.contract_testing.contractcase.grpc.ContractCaseStream.AvailableContractDefinitions;
import io.contract_testing.contractcase.grpc.ContractCaseStream.BeginVerificationRequest;
import io.contract_testing.contractcase.grpc.ContractCaseStream.ContractCaseConfig;
import io.contract_testing.contractcase.grpc.ContractCaseStream.RunVerification;
import io.contract_testing.contractcase.grpc.ContractCaseStream.VerificationRequest;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class InternalVerifierClient {

  private final List<String> parentVersions;
  private final RpcForVerification rpcConnector;
  private final ConfigHandle configHandle;

  private static final int VERIFY_TIMEOUT_SECONDS = 60 * 30;

  public InternalVerifierClient(
      @NotNull ContractCaseBoundaryConfig boundaryConfig,
      @NotNull RunTestCallback callback,
      @NotNull LogPrinter logPrinter,
      @NotNull List<String> parentVersions) {

    ConnectorResultTypeConstants.validate();
    this.parentVersions = parentVersions;
    this.configHandle = new ConfigHandle(boundaryConfig);
    this.rpcConnector = new RpcForVerification(
        logPrinter,
        configHandle,
        callback
    );

    // this is only here because we have to be able to map errors into exceptions
    // probably we should call begin outside the constructor to avoid this issue
    RpcBoundaryResultMapper.map(begin(ConnectorOutgoingMapper.mapConfig(boundaryConfig)));
  }

  public @NotNull BoundaryResult availableContractDescriptions() {
    return ConnectorResult.toBoundaryResult(rpcConnector.executeCallAndWait(
        VerificationRequest.newBuilder()
            .setAvailableContractDefinitions(AvailableContractDefinitions.newBuilder()),
        "availableContractDescriptions"
    ));
  }

  public @NotNull BoundaryResult runVerification(@NotNull ContractCaseBoundaryConfig configOverrides) {
    MaintainerLog.log("Verification run");
    configHandle.setBoundaryConfig(configOverrides);
    var response = rpcConnector.executeCallAndWait(VerificationRequest.newBuilder()
        .setRunVerification(
            RunVerification.newBuilder()
                .setConfig(
                    ConnectorOutgoingMapper.mapConfig(configOverrides)
                )
        ), "runVerification", VERIFY_TIMEOUT_SECONDS
    );
    MaintainerLog.log("Response from verification was: " + response.getResultType());
    return ConnectorResult.toBoundaryResult(response);
  }

  private BoundaryResult begin(ContractCaseConfig wireConfig) {
    MaintainerLog.log("Beginning verification setup");
    return ConnectorResult.toBoundaryResult(rpcConnector.executeCallAndWait(VerificationRequest.newBuilder()
        .setBeginVerification(BeginVerificationRequest.newBuilder()
            .addAllCallerVersions(
                parentVersions.stream()
                    .map(ConnectorOutgoingMapper::map)
                    .toList())
            .setConfig(wireConfig)
            .build()), "begin"));
  }


}
