package io.contract_testing.contractcase.client;

import com.google.protobuf.StringValue;
import io.contract_testing.contractcase.ContractCaseCoreError;
import io.contract_testing.contractcase.case_boundary.BoundaryFailure;
import io.contract_testing.contractcase.case_boundary.BoundaryFailureKindConstants;
import io.contract_testing.contractcase.case_boundary.ILogPrinter;
import io.contract_testing.contractcase.case_boundary.IResultPrinter;
import io.contract_testing.contractcase.grpc.ContractCaseGrpc.ContractCaseStub;
import io.contract_testing.contractcase.grpc.ContractCaseStream.DefinitionRequest;
import io.contract_testing.contractcase.grpc.ContractCaseStream.DefinitionRequest.Builder;
import io.contract_testing.contractcase.grpc.ContractCaseStream.ResultResponse;
import io.grpc.stub.StreamObserver;
import org.jetbrains.annotations.NotNull;

class RpcForDefinition extends AbstractRpcConnector<DefinitionRequest, Builder> {

  public RpcForDefinition(@NotNull ILogPrinter logPrinter,
      @NotNull IResultPrinter resultPrinter,
      ConfigHandle configHandle) {
    super(
        logPrinter,
        resultPrinter,
        configHandle,
        (testName, invoker) -> new BoundaryFailure(
            BoundaryFailureKindConstants.CASE_CORE_ERROR,
            "runTest isn't valid during contract definition",
            MaintainerLog.CONTRACT_CASE_JAVA_WRAPPER
        )
    );
  }

  @Override
  StreamObserver<DefinitionRequest> createConnection(
      ContractCaseStub asyncStub,
      ContractResponseStreamObserver<DefinitionRequest, DefinitionRequest.Builder> contractResponseStreamObserver
  ) {
    return asyncStub.contractDefinition(contractResponseStreamObserver);
  }

  @Override
  DefinitionRequest setId(Builder builder, StringValue id) {
    return builder.setId(id).build();
  }

  @Override
  DefinitionRequest.Builder makeResponse(ResultResponse response) {
    return DefinitionRequest.newBuilder().setResultResponse(response);
  }

  @Override
  DefinitionRequest.Builder makeInvokeTest(StringValue invokerId) {
    throw new ContractCaseCoreError("makeInvokeTest isn't valid during contract definition");
  }
}