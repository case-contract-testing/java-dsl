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

    BoundaryResultMapper.map(begin(mapConfig(boundaryConfig)));
  }

  private ContractCaseConfig mapConfig(final @NotNull ContractCaseBoundaryConfig config) {
    this.boundaryConfig = config;
    var builder = ContractCaseConfig.newBuilder();

    if (config.getBrokerBasicAuth() != null) {
      var auth = config.getBrokerBasicAuth();
      builder.setBrokerBasicAuth(UsernamePassword.newBuilder()
          .setPassword(ValueMapper.map(auth.getPassword()))
          .setUsername(ValueMapper.map(auth.getUsername()))
          .build());
    }

    if (config.getPrintResults() != null) {
      builder.setPrintResults(ValueMapper.map(config.getPrintResults()));
    }
    if (config.getThrowOnFail() != null) {
      builder.setThrowOnFail(ValueMapper.map(config.getThrowOnFail()));
    }
    if (config.getTriggerAndTests() != null) {
      config.getTriggerAndTests()
          .forEach((key, value) -> builder.putTriggerAndTests(key,
              TriggerFunctionHandle.newBuilder().setHandle(ValueMapper.map(key)).build()));
    }
    if (config.getTriggerAndTest() != null) {
      builder.setTriggerAndTest(TriggerFunctionHandle.newBuilder()
          .setHandle(ValueMapper.map(CONTRACT_CASE_TRIGGER_AND_TEST))
          .build());
    }

    if (config.getStateHandlers() != null) {
      // TODO: TEARDOWN FUNCTION
      config.getStateHandlers().forEach((key, value) -> {
        builder.addStateHandlers(StateHandlerHandle.newBuilder()
            .setHandle(ValueMapper.map(key))
            .setStage(Stage.STAGE_SETUP_UNSPECIFIED)
            .build());
      });
    }

    if (config.getBaseUrlUnderTest() != null) {
      builder.setBaseUrlUnderTest(ValueMapper.map(config.getBaseUrlUnderTest()));
    }

    if (config.getBrokerBaseUrl() != null) {
      builder.setBrokerBaseUrl(ValueMapper.map(config.getBrokerBaseUrl()));
    }
    if (config.getBrokerCiAccessToken() != null) {
      builder.setBrokerCiAccessToken(ValueMapper.map(config.getBrokerCiAccessToken()));
    }

    if (config.getConsumerName() != null) {
      builder.setConsumerName(ValueMapper.map(config.getConsumerName()));
    }
    if (config.getContractDir() != null) {
      builder.setContractDir(ValueMapper.map(config.getContractDir()));
    }
    if (config.getContractFilename() != null) {
      builder.setContractFilename(ValueMapper.map(config.getContractFilename()));
    }
    if (config.getLogLevel() != null) {
      builder.setLogLevel(ValueMapper.map(config.getLogLevel()));
    }
    if (config.getProviderName() != null) {
      builder.setProviderName(ValueMapper.map(config.getProviderName()));
    }

    if (config.getPublish() != null) {
      builder.setPublish(ValueMapper.map(config.getPublish()));
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
    requestObserver.onNext(builder.setId(ValueMapper.map(id)).build());

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
    requestObserver.onNext(builder.setId(ValueMapper.map(id)).build());
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
        return new BoundaryFailure(ValueMapper.map(wireFailure.getKind()),
            ValueMapper.map(wireFailure.getMessage()),
            ValueMapper.map(wireFailure.getLocation()));
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
        return new BoundarySuccessWithAny(ValueMapper.map(wireWithAny.getPayload()));
      }
      case SUCCESS_HAS_MAP -> {
        var wireWithMap = wireBoundaryResult.getSuccessHasMap();
        if (wireWithMap == null) {
          throw new ContractCaseCoreError(
              "undefined wire with map in a boundary result. This is probably an error in the connector server library.",
              CONTRACT_CASE_JAVA_WRAPPER);
        }
        return new BoundarySuccessWithMap(ValueMapper.map(wireWithMap.getMap()));
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
            .addAllCallerVersions(parentVersions.stream().map(ValueMapper::map).toList())
            .setConfig(wireConfig)
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
      var string = new ObjectMapper().writeValueAsString(definition);
      JsonFormat.parser()
          .merge(string, structBuilder);
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
        "runRejectingExample not implemented", // TODO
        CONTRACT_CASE_JAVA_WRAPPER);
  }

  public @NotNull BoundaryResult stripMatchers(@NotNull AnyMatcher matcherOrData) {
    return new BoundaryFailure(BoundaryFailureKindConstants.CASE_CORE_ERROR,
        "stripMatchers not implemented", // TODO
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
                .setKind(ValueMapper.map(failure.getKind()))
                .setLocation(ValueMapper.map(failure.getLocation()))
                .setMessage(ValueMapper.map(failure.getMessage()))
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
              .setKind(ValueMapper.map(BoundaryFailureKindConstants.CASE_CORE_ERROR))
              .setLocation(ValueMapper.map(CONTRACT_CASE_JAVA_WRAPPER))
              .setMessage(ValueMapper.map(
                  "Tried to map an unknown result type: " + result.getResultType()))
              .build())
          .build();
    };
  }

  private static Value mapMapToValue(Object payload) {
    // TODO
    throw new RuntimeException("Not implemented Client Side");
  }

  private static Struct mapMapToStruct(Map<String, Object> payload) {
    // TODO
    throw new RuntimeException("Not implemented Server Side");
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
                        .build())), ValueMapper.map(note.getId()));
          }
          case LOG_REQUEST -> {
            var logRequest = note.getLogRequest();
            sendResponse(DefinitionRequest.newBuilder()
                    .setLogPrinterResponse(LogPrinterResponse.newBuilder()
                        .setResult(mapResult(logPrinter.log(ValueMapper.map(logRequest.getLevel()),
                            ValueMapper.map(logRequest.getTimestamp()),
                            ValueMapper.map(logRequest.getVersion()),
                            ValueMapper.map(logRequest.getTypeString()),
                            ValueMapper.map(logRequest.getLocation()),
                            ValueMapper.map(logRequest.getMessage()),
                            ValueMapper.map(logRequest.getAdditional()))))),
                ValueMapper.map(note.getId()));
          }
          case PRINT_MATCH_ERROR_REQUEST -> {
            var printMatchErrorRequest = note.getPrintMatchErrorRequest();
            sendResponse(DefinitionRequest.newBuilder()
                .setResultPrinterResponse(ResultPrinterResponse.newBuilder()
                    .setResult(mapResult(resultPrinter.printMatchError(PrintableMatchError.builder()
                        .kind(ValueMapper.map(printMatchErrorRequest.getKind()))
                        .expected(ValueMapper.map(printMatchErrorRequest.getExpected()))
                        .actual(ValueMapper.map(printMatchErrorRequest.getActual()))
                        .errorTypeTag(ValueMapper.map(printMatchErrorRequest.getErrorTypeTag()))
                        .location(ValueMapper.map(printMatchErrorRequest.getLocation()))
                        .message(ValueMapper.map(printMatchErrorRequest.getMessage()))
                        .build())))), ValueMapper.map(note.getId()));
          }
          case PRINT_MESSAGE_ERROR_REQUEST -> {
            var printMessageErrorRequest = note.getPrintMessageErrorRequest();
            sendResponse(DefinitionRequest.newBuilder()
                .setResultPrinterResponse(ResultPrinterResponse.newBuilder()
                    .setResult(mapResult(resultPrinter.printMessageError(PrintableMessageError.builder()
                        .errorTypeTag(ValueMapper.map(printMessageErrorRequest.getErrorTypeTag()))
                        .kind(ValueMapper.map(printMessageErrorRequest.getKind()))
                        .location(ValueMapper.map(printMessageErrorRequest.getLocation()))
                        .locationTag(ValueMapper.map(printMessageErrorRequest.getLocationTag()))
                        .message(ValueMapper.map(printMessageErrorRequest.getMessage()))
                        .build())))), ValueMapper.map(note.getId()));
          }
          case PRINT_TEST_TITLE_REQUEST -> {
            var printTestTitleRequest = note.getPrintTestTitleRequest();
            sendResponse(DefinitionRequest.newBuilder()
                .setResultPrinterResponse(ResultPrinterResponse.newBuilder()
                    .setResult(mapResult(resultPrinter.printTestTitle(PrintableTestTitle.builder()
                        .title(ValueMapper.map(printTestTitleRequest.getTitle()))
                        .kind(ValueMapper.map(printTestTitleRequest.getKind()))
                        .additionalText(ValueMapper.map(printTestTitleRequest.getAdditionalText()))
                        .icon(ValueMapper.map(printTestTitleRequest.getIcon()))
                        .build())))), ValueMapper.map(note.getId()));
          }
          case TRIGGER_FUNCTION_REQUEST -> {
            var triggerFunctionRequest = note.getTriggerFunctionRequest();
            var handle = ValueMapper.map(triggerFunctionRequest.getTriggerFunction().getHandle());
            if (handle == null) {
              throw new ContractCaseCoreError(
                  "Received a trigger request message with a null trigger handle",
                  "Java Internal Connector");
            }

            if (handle.equals(CONTRACT_CASE_TRIGGER_AND_TEST)) {
              sendResponse(DefinitionRequest.newBuilder()
                  .setTriggerFunctionResponse(TriggerFunctionResponse.newBuilder()
                      // TODO fix null case
                      .setResult(mapResult(boundaryConfig.getTriggerAndTest()
                          .trigger(ValueMapper.map(triggerFunctionRequest.getConfig()))
                      ))), ValueMapper.map(note.getId()));
            } else {
              sendResponse(DefinitionRequest.newBuilder()
                  .setTriggerFunctionResponse(TriggerFunctionResponse.newBuilder()
                      .setResult(mapResult(boundaryConfig.getTriggerAndTests()
                          .get(handle)
                          .trigger(ValueMapper.map(triggerFunctionRequest.getConfig()))
                      ))), ValueMapper.map(note.getId()));
            }
          }
          case BEGIN_DEFINITION_RESPONSE -> {
            var beginDefinitionResponse = note.getBeginDefinitionResponse();
            completeWait(ValueMapper.map(note.getId()), beginDefinitionResponse.getResult());
          }
          case RUN_EXAMPLE_RESPONSE -> {
            var runExampleResponse = note.getRunExampleResponse();
            completeWait(ValueMapper.map(note.getId()), runExampleResponse.getResult());
          }
          case RUN_REJECTING_EXAMPLE_RESPONSE -> {
            var runRejectingExampleResponse = note.getRunRejectingExampleResponse();
            completeWait(ValueMapper.map(note.getId()), runRejectingExampleResponse.getResult());
          }
          case STRIP_MATCHERS_RESPONSE -> {
            var stripMatchersResponse = note.getStripMatchersResponse();
            completeWait(ValueMapper.map(note.getId()), stripMatchersResponse.getResult());
          }
          case END_DEFINITION_RESPONSE -> {
            var endDefinitionResponse = note.getEndDefinitionResponse();
            completeWait(ValueMapper.map(note.getId()), endDefinitionResponse.getResult());
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
