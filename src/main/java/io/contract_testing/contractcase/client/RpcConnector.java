package io.contract_testing.contractcase.client;

import io.contract_testing.contractcase.ContractCaseCoreError;
import io.contract_testing.contractcase.case_boundary.BoundaryFailure;
import io.contract_testing.contractcase.case_boundary.BoundaryFailureKindConstants;
import io.contract_testing.contractcase.case_boundary.ILogPrinter;
import io.contract_testing.contractcase.case_boundary.IResultPrinter;
import io.contract_testing.contractcase.grpc.ContractCaseGrpc;
import io.contract_testing.contractcase.grpc.ContractCaseGrpc.ContractCaseStub;
import io.contract_testing.contractcase.grpc.ContractCaseStream.BoundaryResult;
import io.contract_testing.contractcase.grpc.ContractCaseStream.DefinitionRequest;
import io.contract_testing.contractcase.grpc.ContractCaseStream.DefinitionRequest.Builder;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;

public class RpcConnector {

  private static final int DEFAULT_PORT = 50200;

  private final StreamObserver<DefinitionRequest> requestObserver;
  private final ConcurrentMap<String, CompletableFuture<BoundaryResult>> responseFutures = new ConcurrentHashMap<String, CompletableFuture<BoundaryResult>>();
  private final AtomicInteger nextId = new AtomicInteger();

  private Status errorStatus;


  public RpcConnector(@NotNull ILogPrinter logPrinter, @NotNull IResultPrinter resultPrinter,
      ConfigHandle configHandle) {
    // TODO: Allow configuration of the port
    ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", DEFAULT_PORT)
        .usePlaintext()
        .build();
    ContractCaseStub asyncStub = ContractCaseGrpc.newStub(channel);
    requestObserver = createConnection(asyncStub,new ContractResponseStreamObserver(this, logPrinter, resultPrinter, configHandle));
  }

  private StreamObserver<DefinitionRequest> createConnection(ContractCaseStub asyncStub,
      ContractResponseStreamObserver contractResponseStreamObserver) {
    return asyncStub.contractDefinition(contractResponseStreamObserver);
  }

  io.contract_testing.contractcase.case_boundary.BoundaryResult executeCallAndWait(Builder builder) {
    var id = "" + nextId.getAndIncrement();
    if (errorStatus != null) {
      return new BoundaryFailure(
          BoundaryFailureKindConstants.CASE_CONFIGURATION_ERROR,
          "ContractCase's internal connection failed before execution: " + errorStatus,
          MaintainerLog.CONTRACT_CASE_JAVA_WRAPPER
      );
    }

    var future = new CompletableFuture<BoundaryResult>();
    responseFutures.put(id, future);
    requestObserver.onNext(builder.setId(ConnectorOutgoingMapper.map(id)).build());

    try {
      return ConnectorIncomingMapper.mapBoundaryResult(future.get(60, TimeUnit.SECONDS));
    } catch (TimeoutException e) {
      if (errorStatus != null) {
        return new BoundaryFailure(
            BoundaryFailureKindConstants.CASE_CONFIGURATION_ERROR,
            "ContractCase's internal connection failed while waiting for a request '" + id + "':"
                + errorStatus,
            MaintainerLog.CONTRACT_CASE_JAVA_WRAPPER
        );
      }
      return new BoundaryFailure(
          BoundaryFailureKindConstants.CASE_CONFIGURATION_ERROR,
          "Timed out waiting for internal connection to ContractCase for message '" + id + "'",
          MaintainerLog.CONTRACT_CASE_JAVA_WRAPPER
      );
    } catch (ExecutionException e) {
      return new BoundaryFailure(
          BoundaryFailureKindConstants.CASE_CORE_ERROR,
          "Failed waiting for a response '" + id + "':" + e.getMessage(),
          MaintainerLog.CONTRACT_CASE_JAVA_WRAPPER
      );
    } catch (InterruptedException e) {
      return new BoundaryFailure(
          BoundaryFailureKindConstants.CASE_CONFIGURATION_ERROR,
          "ContractCase was interrupted during its run. This isn't really a configuration error, it usually happens if a user killed the run.",
          MaintainerLog.CONTRACT_CASE_JAVA_WRAPPER
      );
    }
  }

  void completeWait(String id, BoundaryResult result) {
    MaintainerLog.log("Completing wait for: " + id);

    var future = responseFutures.get(id);
    if (future == null) {
      throw new ContractCaseCoreError(
          "There was no future with id '" + id + "'. This is a bug in the wrapper or the boundary.",
          MaintainerLog.CONTRACT_CASE_JAVA_WRAPPER
      );
    }
    responseFutures.get(id).complete(result);
  }

  void sendResponse(DefinitionRequest.Builder builder, String id) {
    requestObserver.onNext(builder.setId(ConnectorOutgoingMapper.map(id)).build());
  }

  public void setErrorStatus(Status errorStatus) {
    this.errorStatus = errorStatus;
  }

  public void close() {
    requestObserver.onCompleted();
  }
}