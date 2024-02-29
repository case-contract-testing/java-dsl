package io.contract_testing.contractcase.client;

import static io.contract_testing.contractcase.client.ConnectorOutgoingMapper.mapRunExampleRequest;
import static io.contract_testing.contractcase.client.ConnectorOutgoingMapper.mapRunRejectingExampleRequest;

import com.fasterxml.jackson.databind.JsonNode;
import io.contract_testing.contractcase.case_boundary.BoundaryFailure;
import io.contract_testing.contractcase.case_boundary.BoundaryFailureKindConstants;
import io.contract_testing.contractcase.case_boundary.BoundaryResult;
import io.contract_testing.contractcase.case_boundary.ContractCaseBoundaryConfig;
import io.contract_testing.contractcase.case_boundary.ILogPrinter;
import io.contract_testing.contractcase.case_boundary.IResultPrinter;
import io.contract_testing.contractcase.grpc.ContractCaseStream.BeginDefinitionRequest;
import io.contract_testing.contractcase.grpc.ContractCaseStream.ContractCaseConfig;
import io.contract_testing.contractcase.grpc.ContractCaseStream.DefinitionRequest;
import io.contract_testing.contractcase.grpc.ContractCaseStream.EndDefinitionRequest;
import io.contract_testing.contractcase.test_equivalence_matchers.base.AnyMatcher;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class InternalDefinerClient {

  private final List<String> parentVersions;

  private final RpcForDefinition rpcConnector;

  private final ConfigHandle configHandle;


  public InternalDefinerClient(final @NotNull ContractCaseBoundaryConfig boundaryConfig,
      final @NotNull ILogPrinter logPrinter,
      final @NotNull IResultPrinter resultPrinter,
      final @NotNull List<String> parentVersions) {
    ResultTypeConstantsCopy.validate();

    this.parentVersions = parentVersions;
    this.configHandle = new ConfigHandle(boundaryConfig);
    this.rpcConnector = new RpcForDefinition(logPrinter, resultPrinter, configHandle);

    // this is only here because we have to be able to map errors into exceptions
    // probably we should call begin outside the constructor to avoid this issue
    RpcBoundaryResultMapper.map(begin(ConnectorOutgoingMapper.mapConfig(boundaryConfig)));
  }

  public @NotNull BoundaryResult endRecord() {
    var result = rpcConnector.executeCallAndWait(DefinitionRequest.newBuilder()
        .setEndDefinition(EndDefinitionRequest.newBuilder().build()));
    rpcConnector.close();

    return result;
  }

  public @NotNull BoundaryResult runExample(final @NotNull JsonNode definition,
      final @NotNull ContractCaseBoundaryConfig runConfig) {
    configHandle.setBoundaryConfig(runConfig);
    return rpcConnector.executeCallAndWait(mapRunExampleRequest(
        definition,
        runConfig
    ));
  }

  public @NotNull BoundaryResult runRejectingExample(final @NotNull JsonNode definition,
      @NotNull ContractCaseBoundaryConfig runConfig) {
    configHandle.setBoundaryConfig(runConfig);
    return rpcConnector.executeCallAndWait(mapRunRejectingExampleRequest(
        definition,
        runConfig
    ));
  }

  public @NotNull BoundaryResult stripMatchers(final @NotNull AnyMatcher matcherOrData) {
    // TODO: Implement this
    return new BoundaryFailure(
        BoundaryFailureKindConstants.CASE_CORE_ERROR,
        "stripMatchers not implemented", // TODO
        MaintainerLog.CONTRACT_CASE_JAVA_WRAPPER
    );
  }

  private BoundaryResult begin(final ContractCaseConfig wireConfig) {
    return rpcConnector.executeCallAndWait(DefinitionRequest.newBuilder()
        .setBeginDefinition(BeginDefinitionRequest.newBuilder()
            .addAllCallerVersions(parentVersions.stream()
                .map(ConnectorOutgoingMapper::map)
                .toList())
            .setConfig(wireConfig)
            .build()));
  }

}
