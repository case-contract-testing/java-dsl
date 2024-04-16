package io.contract_testing.contractcase.client;

import static io.contract_testing.contractcase.client.ConnectorOutgoingMapper.mapRunExampleRequest;
import static io.contract_testing.contractcase.client.ConnectorOutgoingMapper.mapRunRejectingExampleRequest;

import com.fasterxml.jackson.databind.JsonNode;
import io.contract_testing.contractcase.LogPrinter;
import io.contract_testing.contractcase.edge.ConnectorFailure;
import io.contract_testing.contractcase.edge.ConnectorFailureKindConstants;
import io.contract_testing.contractcase.edge.ConnectorResult;
import io.contract_testing.contractcase.edge.ContractCaseConnectorConfig;
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


  public InternalDefinerClient(final @NotNull ContractCaseConnectorConfig boundaryConfig,
      final @NotNull LogPrinter logPrinter,
      final @NotNull List<String> parentVersions) {
    this.parentVersions = parentVersions;
    this.configHandle = new ConfigHandle(boundaryConfig);
    this.rpcConnector = new RpcForDefinition(logPrinter, configHandle);

    // this is only here because we have to be able to map errors into exceptions
    // probably we should call begin outside the constructor to avoid this issue
    RpcConnectorResultMapper.map(begin(ConnectorOutgoingMapper.mapConfig(boundaryConfig)));
  }

  public @NotNull ConnectorResult endRecord() {
    var result = rpcConnector.executeCallAndWait(DefinitionRequest.newBuilder()
        .setEndDefinition(EndDefinitionRequest.newBuilder().build()), "endRecord");
    rpcConnector.close();

    return result;
  }

  public @NotNull ConnectorResult runExample(final @NotNull JsonNode definition,
      final ContractCaseConnectorConfig runConfig) {
    configHandle.setConnectorConfig(runConfig);
    return rpcConnector.executeCallAndWait(mapRunExampleRequest(
        definition,
        runConfig
    ), "runExample");
  }

  public @NotNull ConnectorResult runRejectingExample(final @NotNull JsonNode definition,
      ContractCaseConnectorConfig runConfig) {
    configHandle.setConnectorConfig(runConfig);
    return rpcConnector.executeCallAndWait(
        mapRunRejectingExampleRequest(
            definition,
            runConfig
        ), "runRejectingExample");
  }

  public @NotNull ConnectorResult stripMatchers(final @NotNull AnyMatcher matcherOrData) {
    // TODO: Implement this
    return new ConnectorFailure(
        ConnectorFailureKindConstants.CASE_CORE_ERROR,
        "stripMatchers not implemented", // TODO
        MaintainerLog.CONTRACT_CASE_JAVA_WRAPPER
    );
  }

  private ConnectorResult begin(final ContractCaseConfig wireConfig) {
    return rpcConnector.executeCallAndWait(DefinitionRequest.newBuilder()
        .setBeginDefinition(BeginDefinitionRequest.newBuilder()
            .addAllCallerVersions(parentVersions.stream()
                .map(ConnectorOutgoingMapper::map)
                .toList())
            .setConfig(wireConfig)
            .build()), "begin");
  }

}
