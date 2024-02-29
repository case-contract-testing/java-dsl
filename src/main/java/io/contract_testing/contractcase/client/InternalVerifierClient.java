package io.contract_testing.contractcase.client;

import io.contract_testing.contractcase.case_boundary.BoundaryResult;
import io.contract_testing.contractcase.case_boundary.ContractCaseBoundaryConfig;
import io.contract_testing.contractcase.case_boundary.ILogPrinter;
import io.contract_testing.contractcase.case_boundary.IResultPrinter;
import io.contract_testing.contractcase.case_boundary.IRunTestCallback;
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

  public InternalVerifierClient(
      @NotNull ContractCaseBoundaryConfig boundaryConfig,
      @NotNull IRunTestCallback callback,
      @NotNull ILogPrinter logPrinter,
      @NotNull IResultPrinter resultPrinter,
      @NotNull List<String> parentVersions) {
    
    ResultTypeConstantsCopy.validate();
    this.parentVersions = parentVersions;
    this.rpcConnector = new RpcForVerification(
        logPrinter,
        resultPrinter,
        new ConfigHandle(boundaryConfig),
        callback
    );

    // this is only here because we have to be able to map errors into exceptions
    // probably we should call begin outside the constructor to avoid this issue
    RpcBoundaryResultMapper.map(begin(ConnectorOutgoingMapper.mapConfig(boundaryConfig)));
  }

  public @NotNull BoundaryResult availableContractDescriptions() {
    return rpcConnector.executeCallAndWait(VerificationRequest.newBuilder()
        .setAvailableContractDefinitions(AvailableContractDefinitions.newBuilder()));
  }

  public @NotNull BoundaryResult runVerification(@NotNull ContractCaseBoundaryConfig configOverrides) {
    return rpcConnector.executeCallAndWait(VerificationRequest.newBuilder().setRunVerification(
            RunVerification.newBuilder()
                .setConfig(
                    ConnectorOutgoingMapper.mapConfig(configOverrides)
                )
        )
    );
  }

  private BoundaryResult begin(ContractCaseConfig wireConfig) {
    return rpcConnector.executeCallAndWait(VerificationRequest.newBuilder()
        .setBeginVerification(BeginVerificationRequest.newBuilder()
            .addAllCallerVersions(
                parentVersions.stream()
                    .map(ConnectorOutgoingMapper::map)
                    .toList())
            .setConfig(wireConfig)
            .build()));
  }


}
