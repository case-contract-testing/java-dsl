package io.contract_testing.contractcase.client;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.GeneratedMessageV3.Builder;
import com.google.protobuf.StringValue;
import io.contract_testing.contractcase.ContractCaseCoreError;
import io.contract_testing.contractcase.case_boundary.BoundaryFailure;
import io.contract_testing.contractcase.case_boundary.BoundaryFailureKindConstants;
import io.contract_testing.contractcase.case_boundary.ILogPrinter;
import io.contract_testing.contractcase.case_boundary.IResultPrinter;
import io.contract_testing.contractcase.case_boundary.IRunTestCallback;
import io.contract_testing.contractcase.grpc.ContractCaseGrpc;
import io.contract_testing.contractcase.grpc.ContractCaseGrpc.ContractCaseStub;
import io.contract_testing.contractcase.grpc.ContractCaseStream.BoundaryResult;
import io.contract_testing.contractcase.grpc.ContractCaseStream.ResultResponse;
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

abstract class AbstractRpcConnector<T extends AbstractMessage, B extends Builder<B>> {

  private static final int DEFAULT_PORT = 50200;

  private final StreamObserver<T> requestObserver;
  private final ConcurrentMap<String, CompletableFuture<BoundaryResult>> responseFutures = new ConcurrentHashMap<String, CompletableFuture<BoundaryResult>>();
  private final AtomicInteger nextId = new AtomicInteger();
  private Status errorStatus;


  public AbstractRpcConnector(
      @NotNull ILogPrinter logPrinter,
      @NotNull IResultPrinter resultPrinter,
      @NotNull ConfigHandle configHandle,
      @NotNull IRunTestCallback runTestCallback) {
    this.requestObserver = createConnection(
        ContractCaseGrpc.newStub(
            ManagedChannelBuilder
                // TODO: Allow configuration of the port
                .forAddress("localhost", DEFAULT_PORT)
                .usePlaintext()
                .build()),
        new ContractResponseStreamObserver<>(
            this,
            logPrinter,
            resultPrinter,
            configHandle,
            runTestCallback
        )
    );
  }

  abstract StreamObserver<T> createConnection(ContractCaseStub asyncStub,
      ContractResponseStreamObserver<T, B> contractResponseStreamObserver);

  abstract T setId(B builder, StringValue id);

  abstract B makeResponse(ResultResponse response);

  abstract B makeInvokeTest(StringValue invokerId);

  io.contract_testing.contractcase.case_boundary.BoundaryResult executeCallAndWait(B builder) {
    final var id = "" + nextId.getAndIncrement();
    if (errorStatus != null) {
      return new BoundaryFailure(
          BoundaryFailureKindConstants.CASE_CONFIGURATION_ERROR,
          "ContractCase's internal connection failed before execution: " + errorStatus,
          MaintainerLog.CONTRACT_CASE_JAVA_WRAPPER
      );
    }

    var future = new CompletableFuture<BoundaryResult>();
    responseFutures.put(id, future);
    requestObserver.onNext(setId(builder, ConnectorOutgoingMapper.map(id)));

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

    final var future = responseFutures.get(id);
    if (future == null) {
      throw new ContractCaseCoreError(
          "There was no future with id '" + id + "'. This is a bug in the wrapper or the boundary.",
          MaintainerLog.CONTRACT_CASE_JAVA_WRAPPER
      );
    }
    responseFutures.get(id).complete(result);
  }

  void sendResponse(B builder, String id) {
    requestObserver.onNext(setId(builder, ConnectorOutgoingMapper.map(id)));
  }

  void sendResponse(ResultResponse response, String id) {
    sendResponse(makeResponse(response), id);
  }

  public void setErrorStatus(Status errorStatus) {
    this.errorStatus = errorStatus;
  }

  public void close() {
    requestObserver.onCompleted();
  }


}