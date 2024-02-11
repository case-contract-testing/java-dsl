package io.contract_testing.contractcase.client;

import static io.contract_testing.contractcase.client.ConnectorIncomingMapper.mapMatchErrorRequest;
import static io.contract_testing.contractcase.client.ConnectorIncomingMapper.mapMessageErrorRequest;
import static io.contract_testing.contractcase.client.ConnectorIncomingMapper.mapPrintableTestTitle;
import static io.contract_testing.contractcase.client.ConnectorOutgoingMapper.mapResultResponse;
import static io.contract_testing.contractcase.client.ConnectorOutgoingMapper.mapResult;
import static io.contract_testing.contractcase.client.ConnectorOutgoingMapper.mapRunExampleRequest;
import static io.contract_testing.contractcase.client.ConnectorOutgoingMapper.mapRunRejectingExampleRequest;

import com.fasterxml.jackson.databind.JsonNode;
import io.contract_testing.contractcase.ContractCaseCoreError;
import io.contract_testing.contractcase.case_boundary.BoundaryFailure;
import io.contract_testing.contractcase.case_boundary.BoundaryFailureKindConstants;
import io.contract_testing.contractcase.case_boundary.BoundaryMockDefinition;
import io.contract_testing.contractcase.case_boundary.BoundaryResult;
import io.contract_testing.contractcase.case_boundary.BoundarySuccess;
import io.contract_testing.contractcase.case_boundary.BoundarySuccessWithAny;
import io.contract_testing.contractcase.case_boundary.BoundarySuccessWithMap;
import io.contract_testing.contractcase.case_boundary.ContractCaseBoundaryConfig;
import io.contract_testing.contractcase.case_boundary.ILogPrinter;
import io.contract_testing.contractcase.case_boundary.IResultPrinter;
import io.contract_testing.contractcase.case_boundary.ITriggerFunction;
import io.contract_testing.contractcase.grpc.ContractCaseGrpc;
import io.contract_testing.contractcase.grpc.ContractCaseGrpc.ContractCaseStub;
import io.contract_testing.contractcase.grpc.ContractCaseStream;
import io.contract_testing.contractcase.grpc.ContractCaseStream.BeginDefinitionRequest;
import io.contract_testing.contractcase.grpc.ContractCaseStream.ContractCaseConfig;
import io.contract_testing.contractcase.grpc.ContractCaseStream.DefinitionRequest;
import io.contract_testing.contractcase.grpc.ContractCaseStream.DefinitionRequest.Builder;
import io.contract_testing.contractcase.grpc.ContractCaseStream.DefinitionResponse;
import io.contract_testing.contractcase.grpc.ContractCaseStream.EndDefinitionRequest;
import io.contract_testing.contractcase.grpc.ContractCaseStream.ResultResponse;
import io.contract_testing.contractcase.grpc.ContractCaseStream.ResultSuccess;
import io.contract_testing.contractcase.test_equivalence_matchers.base.AnyMatcher;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InternalDefinerClient {

  public static final String CONTRACT_CASE_JAVA_WRAPPER = "ContractCase Java DSL";
  private static final int DEFAULT_PORT = 50200;
  public static final String CONTRACT_CASE_TRIGGER_AND_TEST = "ContractCase::TriggerAndTest";
  private final ILogPrinter logPrinter;
  private final IResultPrinter resultPrinter;
  private final List<String> parentVersions;
  private final StreamObserver<DefinitionRequest> requestObserver;

  private final ConcurrentMap<String, CompletableFuture<ContractCaseStream.BoundaryResult>> responseFutures = new ConcurrentHashMap<>();

  private final AtomicInteger nextId = new AtomicInteger();
  private ContractCaseBoundaryConfig boundaryConfig;

  private Status errorStatus;


  public InternalDefinerClient(final @NotNull ContractCaseBoundaryConfig boundaryConfig,
      final @NotNull ILogPrinter logPrinter,
      final @NotNull IResultPrinter resultPrinter,
      final @NotNull List<String> parentVersions) {
    ResultTypeConstantsCopy.validate();

    // TODO: Error handling in each case

    ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", DEFAULT_PORT)
        .usePlaintext()
        .build();
    ContractCaseStub asyncStub = ContractCaseGrpc.newStub(channel);
    requestObserver = createConnection(asyncStub);
    this.logPrinter = logPrinter;
    this.resultPrinter = resultPrinter;
    this.parentVersions = parentVersions;

    this.boundaryConfig = boundaryConfig;

    BoundaryResultMapper.map(begin(ConnectorOutgoingMapper.mapConfig(this.boundaryConfig)));
  }


  private BoundaryResult executeCallAndWait(Builder builder) {
    var id = "" + nextId.getAndIncrement();
    if (errorStatus != null) {
      return new BoundaryFailure(BoundaryFailureKindConstants.CASE_CONFIGURATION_ERROR,
          "ContractCase's internal connection failed before execution: " + errorStatus,
          CONTRACT_CASE_JAVA_WRAPPER);
    }

    var future = new CompletableFuture<ContractCaseStream.BoundaryResult>();
    responseFutures.put(id, future);
    requestObserver.onNext(builder.setId(ConnectorOutgoingMapper.map(id)).build());

    try {
      return mapBoundaryResult(future.get(60, TimeUnit.SECONDS));
    } catch (TimeoutException e) {
      if (errorStatus != null) {
        return new BoundaryFailure(BoundaryFailureKindConstants.CASE_CONFIGURATION_ERROR,
            "ContractCase's internal connection failed while waiting for a request '" + id + "':"
                + errorStatus,
            CONTRACT_CASE_JAVA_WRAPPER);
      }
      return new BoundaryFailure(BoundaryFailureKindConstants.CASE_CONFIGURATION_ERROR,
          "Timed out waiting for internal connection to ContractCase for message '" + id + "'",
          CONTRACT_CASE_JAVA_WRAPPER);
    } catch (ExecutionException e) {
      return new BoundaryFailure(BoundaryFailureKindConstants.CASE_CORE_ERROR,
          "Failed waiting for a response '" + id + "':" + e.getMessage(),
          CONTRACT_CASE_JAVA_WRAPPER);
    } catch (InterruptedException e) {
      return new BoundaryFailure(BoundaryFailureKindConstants.CASE_CONFIGURATION_ERROR,
          "ContractCase was interrupted during its run. This isn't really a configuration error, it usually happens if a user killed the run.",
          CONTRACT_CASE_JAVA_WRAPPER);
    }
  }

  private void completeWait(String id, ContractCaseStream.BoundaryResult result) {
    maintainerLog("Completing wait for: " + id);

    var future = responseFutures.get(id);
    if (future == null) {
      throw new ContractCaseCoreError(
          "There was no future with id '" + id + "'. This is a bug in the wrapper or the boundary.",
          CONTRACT_CASE_JAVA_WRAPPER);
    }
    responseFutures.get(id).complete(result);
  }

  private void maintainerLog(String s) {
    /*System.err.println(s);*/
  }

  private void sendResponse(Builder builder, String id) {
    requestObserver.onNext(builder.setId(ConnectorOutgoingMapper.map(id)).build());
  }

  private BoundaryResult mapBoundaryResult(ContractCaseStream.BoundaryResult wireBoundaryResult) {
    if (wireBoundaryResult == null) {
      throw new ContractCaseCoreError(
          "There was a null boundaryResult. This is probably a bug in the connector server library.",
          CONTRACT_CASE_JAVA_WRAPPER);
    }
    var resultType = wireBoundaryResult.getValueCase();
    switch (resultType) {
      case FAILURE -> {
        var wireFailure = wireBoundaryResult.getFailure();
        if (wireFailure == null) {
          throw new ContractCaseCoreError(
              "undefined wireFailure in a boundary result. This is probably an error in the connector server library.",
              CONTRACT_CASE_JAVA_WRAPPER);
        }
        return new BoundaryFailure(ConnectorIncomingMapper.map(wireFailure.getKind()),
            ConnectorIncomingMapper.map(wireFailure.getMessage()),
            ConnectorIncomingMapper.map(wireFailure.getLocation()));
      }
      case SUCCESS -> {
        return new BoundarySuccess();
      }
      case SUCCESS_HAS_ANY -> {
        var wireWithAny = wireBoundaryResult.getSuccessHasAny();
        if (wireWithAny == null) {
          throw new ContractCaseCoreError(
              "undefined wire with any in a boundary result. This is probably an error in the connector server library.",
              CONTRACT_CASE_JAVA_WRAPPER);
        }
        return new BoundarySuccessWithAny(ConnectorIncomingMapper.map(wireWithAny.getPayload()));
      }
      case SUCCESS_HAS_MAP -> {
        var wireWithMap = wireBoundaryResult.getSuccessHasMap();
        if (wireWithMap == null) {
          throw new ContractCaseCoreError(
              "undefined wire with map in a boundary result. This is probably an error in the connector server library.",
              CONTRACT_CASE_JAVA_WRAPPER);
        }
        return new BoundarySuccessWithMap(ConnectorIncomingMapper.map(wireWithMap.getMap()));
      }
      case VALUE_NOT_SET -> throw new ContractCaseCoreError(
          "There was an unset boundaryResult. This is probably a bug in the connector server library.",
          CONTRACT_CASE_JAVA_WRAPPER);
      default -> throw new ContractCaseCoreError(
          "There was a boundary result type that we didn't understand '" + resultType
              + "'. This is probably a bug in the connector server library.",
          CONTRACT_CASE_JAVA_WRAPPER);
    }
  }

  private BoundaryResult begin(ContractCaseConfig wireConfig) {
    return executeCallAndWait(DefinitionRequest.newBuilder()
        .setBeginDefinition(BeginDefinitionRequest.newBuilder()
            .addAllCallerVersions(parentVersions.stream()
                .map(ConnectorOutgoingMapper::map)
                .toList())
            .setConfig(wireConfig)
            .build()));
  }


  public @NotNull BoundaryResult endRecord() {
    var result = executeCallAndWait(DefinitionRequest.newBuilder()
        .setEndDefinition(EndDefinitionRequest.newBuilder().build()));
    requestObserver.onCompleted();

    return result;
  }

  public @NotNull BoundaryResult runExample(JsonNode definition,
      @NotNull ContractCaseBoundaryConfig runConfig) {
    this.boundaryConfig = runConfig;
    return executeCallAndWait(mapRunExampleRequest(
        definition,
        runConfig));
  }


  public @NotNull BoundaryResult runRejectingExample(@NotNull JsonNode definition,
      @NotNull ContractCaseBoundaryConfig runConfig) {
    this.boundaryConfig = runConfig;
    return executeCallAndWait(mapRunRejectingExampleRequest(
        definition,
        runConfig));
  }



  public @NotNull BoundaryResult stripMatchers(@NotNull AnyMatcher matcherOrData) {
    return new BoundaryFailure(BoundaryFailureKindConstants.CASE_CORE_ERROR,
        "stripMatchers not implemented", // TODO
        CONTRACT_CASE_JAVA_WRAPPER);
  }


  private StreamObserver<DefinitionRequest> createConnection(ContractCaseStub asyncStub) {
    return asyncStub.contractDefinition(new StreamObserver<>() {
      @Override
      public void onNext(DefinitionResponse note) {
        /* For when we receive messages from the server */
        final var requestId = ConnectorIncomingMapper.map(note.getId());
        switch (note.getKindCase()) {
          case RUN_STATE_HANDLER -> {
            final var stateHandlerRunRequest = note.getRunStateHandler();
            // TODO Implement this properly
            sendResponse(DefinitionRequest.newBuilder()
                .setResultResponse(ResultResponse.newBuilder()
                    .setResult(ContractCaseStream.BoundaryResult.newBuilder()
                        .setSuccess(ResultSuccess.newBuilder().build())
                        .build())), requestId);
          }
          case LOG_REQUEST -> {
            final var logRequest = note.getLogRequest();
            sendResponse(DefinitionRequest.newBuilder()
                    .setResultResponse(ResultResponse.newBuilder()
                        .setResult(mapResult(logPrinter.log(ConnectorIncomingMapper.map(logRequest.getLevel()),
                            ConnectorIncomingMapper.map(logRequest.getTimestamp()),
                            ConnectorIncomingMapper.map(logRequest.getVersion()),
                            ConnectorIncomingMapper.map(logRequest.getTypeString()),
                            ConnectorIncomingMapper.map(logRequest.getLocation()),
                            ConnectorIncomingMapper.map(logRequest.getMessage()),
                            ConnectorIncomingMapper.map(logRequest.getAdditional()))))),
                requestId);
          }
          case PRINT_MATCH_ERROR_REQUEST -> {
            final var printMatchErrorRequest = note.getPrintMatchErrorRequest();
            sendResponse(mapResultResponse(resultPrinter.printMatchError(mapMatchErrorRequest(
                printMatchErrorRequest))), requestId);
          }
          case PRINT_MESSAGE_ERROR_REQUEST -> {
            sendResponse(mapResultResponse(resultPrinter.printMessageError(
                    mapMessageErrorRequest(
                        note.getPrintMessageErrorRequest()))),
                requestId);
          }
          case PRINT_TEST_TITLE_REQUEST -> {
            final var printTestTitleRequest = note.getPrintTestTitleRequest();
            sendResponse(mapResultResponse(resultPrinter.printTestTitle(mapPrintableTestTitle(
                printTestTitleRequest))), requestId);
          }
          case TRIGGER_FUNCTION_REQUEST -> {
            var triggerFunctionRequest = note.getTriggerFunctionRequest();
            var handle = ConnectorIncomingMapper.map(triggerFunctionRequest.getTriggerFunction()
                .getHandle());
            if (handle == null) {
              throw new ContractCaseCoreError(
                  "Received a trigger request message with a null trigger handle",
                  "Java Internal Connector");
            }

            ITriggerFunction trigger = getTriggerFunction(handle);

            sendResponse(DefinitionRequest.newBuilder()
                .setResultResponse(ResultResponse.newBuilder()
                    .setResult(mapResult(trigger.trigger(ConnectorIncomingMapper.map(
                        triggerFunctionRequest.getConfig()))
                    ))), requestId);
          }
          case RESULT_RESPONSE -> {
            completeWait(requestId,
                note.getResultResponse().getResult());
          }
          case KIND_NOT_SET -> {
            throw new ContractCaseCoreError("Received a message with no kind set",
                "Java Internal Connector");
          }
        }
      }

      @Override
      public void onError(Throwable t) {
        Status status = Status.fromThrowable(t);
        if (Status.Code.UNAVAILABLE.equals(status.getCode())) {
          System.err.println(
              "ContractCase was unable to contact its internal server.\n"
                  + "   This is either a conflict while starting the server,\n"
                  + "   or a bug in ContractCase. Please see the rest of the\n"
                  + "   log output for details.\n\n"
                  + "   If you are unable to resolve this locally,\n"
                  + "   please open an issue here:"
                  + "   https://github.com/case-contract-testing/contract-case");
        } else {
          System.err.println("ContractCase failed: " + status);
        }
        errorStatus = status;
      }

      @Override
      public void onCompleted() {
      }
    });
  }

  @NotNull
  private ITriggerFunction getTriggerFunction(String handle) {
    ITriggerFunction trigger = getTriggerInternal(handle);
    if (trigger == null) {
      throw new ContractCaseCoreError(
          "Unable to trigger the function with handle '" + handle + "', as it is null");
    }
    return trigger;
  }

  @Nullable
  private ITriggerFunction getTriggerInternal(String handle) {
    if (handle.equals(CONTRACT_CASE_TRIGGER_AND_TEST)) {
      return boundaryConfig.getTriggerAndTest();
    }

    var triggerMap = boundaryConfig.getTriggerAndTests();
    if (triggerMap == null) {
      throw new ContractCaseCoreError(
          "Unable to trigger the function with handle '" + handle
              + "', as the entire trigger map is null");
    }
    return triggerMap.get(handle);
  }

}
