package io.contract_testing.contractcase.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
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
import io.contract_testing.contractcase.case_boundary.PrintableMatchError;
import io.contract_testing.contractcase.case_boundary.PrintableMessageError;
import io.contract_testing.contractcase.case_boundary.PrintableTestTitle;
import io.contract_testing.contractcase.grpc.ContractCaseGrpc;
import io.contract_testing.contractcase.grpc.ContractCaseGrpc.ContractCaseStub;
import io.contract_testing.contractcase.grpc.ContractCaseStream;
import io.contract_testing.contractcase.grpc.ContractCaseStream.BeginDefinitionRequest;
import io.contract_testing.contractcase.grpc.ContractCaseStream.ContractCaseConfig;
import io.contract_testing.contractcase.grpc.ContractCaseStream.ContractCaseConfig.UsernamePassword;
import io.contract_testing.contractcase.grpc.ContractCaseStream.DefinitionRequest;
import io.contract_testing.contractcase.grpc.ContractCaseStream.DefinitionRequest.Builder;
import io.contract_testing.contractcase.grpc.ContractCaseStream.DefinitionResponse;
import io.contract_testing.contractcase.grpc.ContractCaseStream.EndDefinitionRequest;
import io.contract_testing.contractcase.grpc.ContractCaseStream.LogPrinterResponse;
import io.contract_testing.contractcase.grpc.ContractCaseStream.ResultFailure;
import io.contract_testing.contractcase.grpc.ContractCaseStream.ResultPrinterResponse;
import io.contract_testing.contractcase.grpc.ContractCaseStream.ResultSuccess;
import io.contract_testing.contractcase.grpc.ContractCaseStream.ResultSuccessHasAnyPayload;
import io.contract_testing.contractcase.grpc.ContractCaseStream.ResultSuccessHasMapPayload;
import io.contract_testing.contractcase.grpc.ContractCaseStream.RunExampleRequest;
import io.contract_testing.contractcase.grpc.ContractCaseStream.StateHandlerHandle;
import io.contract_testing.contractcase.grpc.ContractCaseStream.StateHandlerHandle.Stage;
import io.contract_testing.contractcase.grpc.ContractCaseStream.StateHandlerResponse;
import io.contract_testing.contractcase.grpc.ContractCaseStream.TriggerFunctionHandle;
import io.contract_testing.contractcase.grpc.ContractCaseStream.TriggerFunctionResponse;
import io.contract_testing.contractcase.test_equivalence_matchers.base.AnyMatcher;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;

public class InternalDefinerClient {

  public static final String CONTRACT_CASE_JAVA_WRAPPER = "ContractCase Java DSL";
  private static final int DEFAULT_PORT = 50200;
  private final ILogPrinter logPrinter;
  private final IResultPrinter resultPrinter;
  private final List<String> parentVersions;
  private final ContractCaseConfig config;
  private final StreamObserver<DefinitionRequest> requestObserver;

  private final ConcurrentMap<String, CompletableFuture<ContractCaseStream.BoundaryResult>> responseFutures = new ConcurrentHashMap<>();

  private final AtomicInteger nextId = new AtomicInteger();

  private Status errorStatus;


  public InternalDefinerClient(final @NotNull ContractCaseBoundaryConfig config,
      final @NotNull ILogPrinter logPrinter,
      final @NotNull IResultPrinter resultPrinter,
      final @NotNull List<String> parentVersions) {
    ResultTypeConstantsCopy.validate();
    this.config = mapConfig(config);
    ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", DEFAULT_PORT)
        .usePlaintext()
        .build();
    ContractCaseStub asyncStub = ContractCaseGrpc.newStub(channel);
    requestObserver = createConnection(asyncStub);
    this.logPrinter = logPrinter;
    this.resultPrinter = resultPrinter;
    this.parentVersions = parentVersions;

    BoundaryResultMapper.map(begin());
  }

  private ContractCaseConfig mapConfig(final @NotNull ContractCaseBoundaryConfig config) {
    var builder = ContractCaseConfig.newBuilder();

    if (config.getBrokerBasicAuth() != null) {
      var auth = config.getBrokerBasicAuth();
      builder.setBrokerBasicAuth(UsernamePassword.newBuilder()
          .setPassword(auth.getPassword())
          .setUsername(auth.getUsername())
          .build());
    }

    if (config.getPrintResults() != null) {
      builder.setPrintResults(config.getPrintResults());
    }
    if (config.getThrowOnFail() != null) {
      builder.setThrowOnFail(config.getThrowOnFail());
    }
    if (config.getTriggerAndTests() != null) {
      config.getTriggerAndTests()
          .forEach((key, value) -> builder.putTriggerAndTests(key,
              TriggerFunctionHandle.newBuilder().setHandle(key).build()));
    }
    if (config.getTriggerAndTest() != null) {
      builder.setTriggerAndTest(TriggerFunctionHandle.newBuilder()
          .setHandle("ContractCase::TriggerAndTest")
          .build());
    }

    if (config.getStateHandlers() != null) {
      // TODO: TEARDOWN FUNCTION
      config.getStateHandlers().forEach((key, value) -> {
        builder.addStateHandlers(StateHandlerHandle.newBuilder()
            .setHandle(key)
            .setStage(Stage.STAGE_SETUP_UNSPECIFIED)
            .build());
      });
    }

    if (config.getBaseUrlUnderTest() != null) {
      builder.setBaseUrlUnderTest(config.getBaseUrlUnderTest());
    }

    if (config.getBrokerBaseUrl() != null) {
      builder.setBrokerBaseUrl(config.getBrokerBaseUrl());
    }
    if (config.getBrokerCiAccessToken() != null) {
      builder.setBrokerCiAccessToken(config.getBrokerCiAccessToken());
    }

    if (config.getConsumerName() != null) {
      builder.setConsumerName(config.getConsumerName());
    }
    if (config.getContractDir() != null) {
      builder.setContractDir(config.getContractDir());
    }
    if (config.getContractFilename() != null) {
      builder.setContractFilename(config.getContractFilename());
    }
    if (config.getLogLevel() != null) {
      builder.setLogLevel(config.getLogLevel());
    }
    if (config.getProviderName() != null) {
      builder.setProviderName(config.getProviderName());
    }

    if (config.getPublish() != null) {
      builder.setPublish(config.getPublish());
    }

    return builder.build();
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
    requestObserver.onNext(builder.setId(id).build());

    try {
      return mapBoundaryResult(future.get(3, TimeUnit.SECONDS));
    } catch (TimeoutException e) {
      if (errorStatus != null) {
        return new BoundaryFailure(BoundaryFailureKindConstants.CASE_CONFIGURATION_ERROR,
            "ContractCase's internal connection failed while waiting for a request: " + errorStatus,
            CONTRACT_CASE_JAVA_WRAPPER);
      }
      return new BoundaryFailure(BoundaryFailureKindConstants.CASE_CONFIGURATION_ERROR,
          "Timed out waiting for internal connection to ContractCase",
          CONTRACT_CASE_JAVA_WRAPPER);
    } catch (ExecutionException e) {
      return new BoundaryFailure(BoundaryFailureKindConstants.CASE_CORE_ERROR,
          "Failed waiting for a response: " + e.getMessage(),
          CONTRACT_CASE_JAVA_WRAPPER);
    } catch (InterruptedException e) {
      return new BoundaryFailure(BoundaryFailureKindConstants.CASE_CONFIGURATION_ERROR,
          "ContractCase was interrupted during its run. This isn't really a configuration error, it usually happens if a user killed the run.",
          CONTRACT_CASE_JAVA_WRAPPER);
    }
  }

  private void completeWait(String id, ContractCaseStream.BoundaryResult result) {
    var future = responseFutures.get(id);
    if (future == null) {
      throw new ContractCaseCoreError(
          "There was no future with id '" + id + "'. This is a bug in the wrapper or the boundary.",
          CONTRACT_CASE_JAVA_WRAPPER);
    }
    responseFutures.get(id).complete(result);
  }

  private void sendResponse(Builder builder, String id) {
    requestObserver.onNext(builder.setId(id).build());
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
        return new BoundaryFailure(wireFailure.getKind(),
            wireFailure.getMessage(),
            wireFailure.getLocation());
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
        return new BoundarySuccessWithMap(mapJson(wireWithAny.getPayload()));
      }
      case SUCCESS_HAS_MAP -> {
        var wireWithMap = wireBoundaryResult.getSuccessHasMap();
        if (wireWithMap == null) {
          throw new ContractCaseCoreError(
              "undefined wire with map in a boundary result. This is probably an error in the connector server library.",
              CONTRACT_CASE_JAVA_WRAPPER);
        }
        return new BoundarySuccessWithMap(mapJsonMap(wireWithMap.getMap()));
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

  private Map<String, Object> mapJson(Value payload) {
    // TODO
    throw new RuntimeException("Not implemented");
  }


  private Map<String, Object> mapJsonMap(Struct map) {
    // TODO
    throw new RuntimeException("Not implemented");
  }

  private BoundaryResult begin() {
    return executeCallAndWait(DefinitionRequest.newBuilder()
        .setBeginDefinition(BeginDefinitionRequest.newBuilder()
            .addAllCallerVersions(parentVersions)
            .setConfig(config)
            .build()));
  }


  public @NotNull BoundaryResult endRecord() {
    requestObserver.onNext(DefinitionRequest.newBuilder()
        .setEndDefinition(EndDefinitionRequest.newBuilder().build())
        .build());

    requestObserver.onCompleted();
    return new BoundaryFailure(BoundaryFailureKindConstants.CASE_CORE_ERROR,
        "endRecord not implemented",
        CONTRACT_CASE_JAVA_WRAPPER); // TODO
  }

  public @NotNull BoundaryResult runExample(JsonNode definition,
      @NotNull ContractCaseBoundaryConfig runConfig) {

    var structBuilder = Struct.newBuilder();

    try {
      JsonFormat.parser()
          .merge(new ObjectMapper().writeValueAsString(definition), structBuilder);
    } catch (JsonProcessingException | InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    }

    return executeCallAndWait(DefinitionRequest.newBuilder()
        .setRunExample(RunExampleRequest.newBuilder()
            .setConfig(mapConfig(runConfig)) // TODO handle additional state handlers or triggers
            .setExampleDefinition(structBuilder)
            .build()));
  }

  public @NotNull BoundaryResult runRejectingExample(@NotNull BoundaryMockDefinition definition,
      @NotNull ContractCaseBoundaryConfig runConfig) {
    return new BoundaryFailure(BoundaryFailureKindConstants.CASE_CORE_ERROR,
        "runRejectingExample not implemented",
        CONTRACT_CASE_JAVA_WRAPPER);
  }

  public @NotNull BoundaryResult stripMatchers(@NotNull AnyMatcher matcherOrData) {
    return new BoundaryFailure(BoundaryFailureKindConstants.CASE_CORE_ERROR,
        "stripMatchers not implemented",
        CONTRACT_CASE_JAVA_WRAPPER);
  }

  @NotNull
  private static ContractCaseStream.BoundaryResult mapResult(@NotNull BoundaryResult result) {
    return switch (result.getResultType()) {
      case ResultTypeConstantsCopy.RESULT_SUCCESS -> ContractCaseStream.BoundaryResult.newBuilder()
          .setSuccess(ResultSuccess.newBuilder().build())
          .build();
      case ResultTypeConstantsCopy.RESULT_FAILURE -> {
        var failure = ((BoundaryFailure) result);
        yield ContractCaseStream.BoundaryResult.newBuilder()
            .setFailure(ResultFailure.newBuilder()
                .setKind(failure.getKind())
                .setLocation(failure.getLocation())
                .setMessage(failure.getMessage())
                .build())
            .build();
      }
      case ResultTypeConstantsCopy.RESULT_SUCCESS_HAS_MAP_PAYLOAD ->
          ContractCaseStream.BoundaryResult.newBuilder()
              .setSuccessHasMap(ResultSuccessHasMapPayload.newBuilder()
                  .setMap(mapMapToStruct(((BoundarySuccessWithMap) result).getPayload()))
                  .build())
              .build();
      case ResultTypeConstantsCopy.RESULT_SUCCESS_HAS_ANY_PAYLOAD ->
          ContractCaseStream.BoundaryResult.newBuilder()
              .setSuccessHasAny(ResultSuccessHasAnyPayload.newBuilder()
                  .setPayload(mapMapToValue(((BoundarySuccessWithAny) result).getPayload()))
                  .build())
              .build();
      default -> ContractCaseStream.BoundaryResult.newBuilder()
          .setFailure(ResultFailure.newBuilder()
              .setKind(BoundaryFailureKindConstants.CASE_CORE_ERROR)
              .setLocation(CONTRACT_CASE_JAVA_WRAPPER)
              .setMessage("Tried to map an unknown result type: " + result.getResultType())
              .build())
          .build();
    };
  }

  private static Value mapMapToValue(Object payload) {
    // TODO
    throw new RuntimeException("Not implemented");
  }

  private static Struct mapMapToStruct(Map<String, Object> payload) {
    // TODO
    throw new RuntimeException("Not implemented");
  }

  private StreamObserver<DefinitionRequest> createConnection(ContractCaseStub asyncStub) {
    return asyncStub.contractDefinition(new StreamObserver<>() {

      @Override
      public void onNext(DefinitionResponse note) {
        /* For when we receive messages from the server */
        switch (note.getKindCase()) {
          case RUN_STATE_HANDLER -> {
            var stateHandlerRunRequest = note.getRunStateHandler();
            // TODO Implement this properly
            sendResponse(DefinitionRequest.newBuilder()
                .setStateHandlerResponse(StateHandlerResponse.newBuilder()
                    .setResult(ContractCaseStream.BoundaryResult.newBuilder()
                        .setSuccess(ResultSuccess.newBuilder().build())
                        .build())), note.getId());
          }
          case LOG_REQUEST -> {
            var logRequest = note.getLogRequest();
            sendResponse(DefinitionRequest.newBuilder()
                .setLogPrinterResponse(LogPrinterResponse.newBuilder()
                    .setResult(mapResult(logPrinter.log(logRequest.getLevel(),
                        logRequest.getTimestamp(),
                        logRequest.getVersion(),
                        logRequest.getTypeString(),
                        logRequest.getLocation(),
                        logRequest.getMessage(),
                        logRequest.getAdditional())))), note.getId());
          }
          case PRINT_MATCH_ERROR_REQUEST -> {
            var printMatchErrorRequest = note.getPrintMatchErrorRequest();
            sendResponse(DefinitionRequest.newBuilder()
                .setResultPrinterResponse(ResultPrinterResponse.newBuilder()
                    .setResult(mapResult(resultPrinter.printMatchError(PrintableMatchError.builder()
                        .kind(printMatchErrorRequest.getKind())
                        .expected(printMatchErrorRequest.getExpected())
                        .actual(printMatchErrorRequest.getActual())
                        .errorTypeTag(printMatchErrorRequest.getErrorTypeTag())
                        .location(printMatchErrorRequest.getLocation())
                        .message(printMatchErrorRequest.getMessage())
                        .build())))), note.getId());
          }
          case PRINT_MESSAGE_ERROR_REQUEST -> {
            var printMessageErrorRequest = note.getPrintMessageErrorRequest();
            sendResponse(DefinitionRequest.newBuilder()
                .setResultPrinterResponse(ResultPrinterResponse.newBuilder()
                    .setResult(mapResult(resultPrinter.printMessageError(PrintableMessageError.builder()
                        .errorTypeTag(printMessageErrorRequest.getErrorTypeTag())
                        .kind(printMessageErrorRequest.getKind())
                        .location(printMessageErrorRequest.getLocation())
                        .locationTag(printMessageErrorRequest.getLocationTag())
                        .message(printMessageErrorRequest.getMessage())
                        .build())))), note.getId());
          }
          case PRINT_TEST_TITLE_REQUEST -> {
            var printTestTitleRequest = note.getPrintTestTitleRequest();
            sendResponse(DefinitionRequest.newBuilder()
                .setResultPrinterResponse(ResultPrinterResponse.newBuilder()
                    .setResult(mapResult(resultPrinter.printTestTitle(PrintableTestTitle.builder()
                        .title(printTestTitleRequest.getTitle())
                        .kind(printTestTitleRequest.getKind())
                        .additionalText(printTestTitleRequest.getAdditionalText())
                        .icon(printTestTitleRequest.getIcon())
                        .build())))), note.getId());
          }
          case TRIGGER_FUNCTION_REQUEST -> {
            var triggerFunctionRequest = note.getTriggerFunctionRequest();
            // TODO Implement this properly
            sendResponse(DefinitionRequest.newBuilder()
                .setTriggerFunctionResponse(TriggerFunctionResponse.newBuilder()
                    .setResult(ContractCaseStream.BoundaryResult.newBuilder()
                        .setSuccess(ResultSuccess.newBuilder().build())
                        .build())), note.getId());
          }
          case BEGIN_DEFINITION_RESPONSE -> {
            var beginDefinitionResponse = note.getBeginDefinitionResponse();
            completeWait(note.getId(), beginDefinitionResponse.getResult());
          }
          case RUN_EXAMPLE_RESPONSE -> {
            var runExampleResponse = note.getRunExampleResponse();
            completeWait(note.getId(), runExampleResponse.getResult());
          }
          case RUN_REJECTING_EXAMPLE_RESPONSE -> {
            var runRejectingExampleResponse = note.getRunRejectingExampleResponse();
            completeWait(note.getId(), runRejectingExampleResponse.getResult());
          }
          case STRIP_MATCHERS_RESPONSE -> {
            var stripMatchersResponse = note.getStripMatchersResponse();
            completeWait(note.getId(), stripMatchersResponse.getResult());
          }
          case END_DEFINITION_RESPONSE -> {
            var endDefinitionResponse = note.getEndDefinitionResponse();
            completeWait(note.getId(), endDefinitionResponse.getResult());
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
        System.err.println("ContractCase failed: " + status);
        errorStatus = status;
      }

      @Override
      public void onCompleted() {
      }
    });
  }


}
