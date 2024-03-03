package io.contract_testing.contractcase.client;

import static io.contract_testing.contractcase.client.ConnectorIncomingMapper.mapMatchErrorRequest;
import static io.contract_testing.contractcase.client.ConnectorIncomingMapper.mapMessageErrorRequest;
import static io.contract_testing.contractcase.client.ConnectorIncomingMapper.mapPrintableTestTitle;
import static io.contract_testing.contractcase.client.ConnectorOutgoingMapper.mapResult;
import static io.contract_testing.contractcase.client.ConnectorOutgoingMapper.mapResultResponse;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.GeneratedMessageV3.Builder;
import io.contract_testing.contractcase.ContractCaseCoreError;
import io.contract_testing.contractcase.case_boundary.BoundaryResult;
import io.contract_testing.contractcase.case_boundary.BoundaryStateHandler;
import io.contract_testing.contractcase.case_boundary.ILogPrinter;
import io.contract_testing.contractcase.case_boundary.IResultPrinter;
import io.contract_testing.contractcase.case_boundary.IRunTestCallback;
import io.contract_testing.contractcase.grpc.ContractCaseStream.ContractResponse;
import io.contract_testing.contractcase.grpc.ContractCaseStream.ResultResponse;
import io.contract_testing.contractcase.grpc.ContractCaseStream.StateHandlerHandle.Stage;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.jetbrains.annotations.NotNull;

class ContractResponseStreamObserver<T extends AbstractMessage, B extends Builder<B>> implements
    StreamObserver<ContractResponse> {

  private final AbstractRpcConnector<T, B> rpcConnector;
  private final ILogPrinter logPrinter;
  private final IResultPrinter resultPrinter;
  private final ConfigHandle configHandle;
  private final IRunTestCallback runTestCallback;


  public ContractResponseStreamObserver(
      final @NotNull AbstractRpcConnector<T, B> rpcConnector,
      final @NotNull ILogPrinter logPrinter,
      final @NotNull IResultPrinter resultPrinter,
      final @NotNull ConfigHandle configHandle,
      final @NotNull IRunTestCallback runTestCallback) {
    this.rpcConnector = rpcConnector;
    this.logPrinter = logPrinter;
    this.resultPrinter = resultPrinter;
    this.configHandle = configHandle;
    this.runTestCallback = runTestCallback;
  }

  @Override
  public void onNext(final ContractResponse note) {
    /* For when we receive messages from the server */
    final var requestId = ConnectorIncomingMapper.map(note.getId());
    switch (note.getKindCase()) {
      case RUN_STATE_HANDLER -> {
        final var stateHandlerRunRequest = note.getRunStateHandler();
        var stateName = stateHandlerRunRequest.getStateHandlerHandle()
            .getHandle()
            .getValue();
        var handle = configHandle.getStateHandler(
            stateName);

        rpcConnector.sendResponse(
            ResultResponse.newBuilder()
                .setResult(mapResult(runStateHandler(
                    stateHandlerRunRequest.getStateHandlerHandle()
                        .getStage(),
                    stateName,
                    handle
                ))).build(),
            requestId
        );
      }
      case LOG_REQUEST -> {
        final var logRequest = note.getLogRequest();
        rpcConnector.sendResponse(
            ResultResponse.newBuilder().setResult(
                mapResult(
                    logPrinter.log(
                        ConnectorIncomingMapper.map(logRequest.getLevel()),
                        ConnectorIncomingMapper.map(logRequest.getTimestamp()),
                        ConnectorIncomingMapper.map(logRequest.getVersion()),
                        ConnectorIncomingMapper.map(logRequest.getTypeString()),
                        ConnectorIncomingMapper.map(logRequest.getLocation()),
                        ConnectorIncomingMapper.map(logRequest.getMessage()),
                        ConnectorIncomingMapper.map(logRequest.getAdditional())
                    )
                )
            ).build(),
            requestId
        );
      }
      case PRINT_MATCH_ERROR_REQUEST -> {
        final var printMatchErrorRequest = note.getPrintMatchErrorRequest();
        rpcConnector.sendResponse(
            mapResultResponse(
                resultPrinter.printMatchError(
                    mapMatchErrorRequest(printMatchErrorRequest)
                )
            ),
            requestId
        );
      }
      case PRINT_MESSAGE_ERROR_REQUEST -> {
        rpcConnector.sendResponse(
            mapResultResponse(
                resultPrinter.printMessageError(
                    mapMessageErrorRequest(
                        note.getPrintMessageErrorRequest()
                    )
                )
            ),
            requestId
        );
      }
      case PRINT_TEST_TITLE_REQUEST -> {
        final var printTestTitleRequest = note.getPrintTestTitleRequest();
        rpcConnector.sendResponse(mapResultResponse(resultPrinter.printTestTitle(
            mapPrintableTestTitle(printTestTitleRequest))), requestId);
      }
      case TRIGGER_FUNCTION_REQUEST -> {
        var triggerFunctionRequest = note.getTriggerFunctionRequest();
        var handle = ConnectorIncomingMapper.map(triggerFunctionRequest.getTriggerFunction()
            .getHandle());
        if (handle == null) {
          throw new ContractCaseCoreError(
              "Received a trigger request message with a null trigger handle",
              "Java Internal Connector"
          );
        }

        rpcConnector.sendResponse(
            ResultResponse.newBuilder().setResult(
                mapResult(
                    configHandle.getTriggerFunction(handle).trigger(
                        ConnectorIncomingMapper.map(triggerFunctionRequest.getConfig()
                        )
                    )
                )
            ).build(),
            requestId
        );
      }
      case RESULT_RESPONSE -> {
        rpcConnector.completeWait(requestId, note.getResultResponse().getResult());
      }
      case START_TEST_EVENT -> {
        var startTestEvent = note.getStartTestEvent();
        rpcConnector.sendResponse(
            mapResultResponse(
                runTestCallback.runTest(
                    startTestEvent.getTestName().getValue(),
                    () -> rpcConnector.executeCallAndWait(rpcConnector.makeInvokeTest(
                        startTestEvent.getInvokerId()))
                )),
            requestId
        );
        // TODO: Implement this
        throw new ContractCaseCoreError(
            "Received start test event incorrectly during a define contract",
            "Java Internal Connector"
        );
      }
      case KIND_NOT_SET -> {
        throw new ContractCaseCoreError(
            "Received a message with no kind set",
            "Java Internal Connector"
        );
      }
    }
  }

  @Override
  public void onError(final Throwable t) {
    Status status = Status.fromThrowable(t);
    if (Status.Code.UNAVAILABLE.equals(status.getCode())) {
      System.err.println("""
          ContractCase was unable to contact its internal server.
             This is either a conflict while starting the server,
             a crash while the server was running, or a bug in
             ContractCase. Please see the rest of the log output
             for details.

             If you are unable to resolve this locally,
             please open an issue here:

             https://github.com/case-contract-testing/contract-case
          """);
    } else {
      System.err.println("ContractCase failed: " + status);
    }
    rpcConnector.setErrorStatus(status);
  }

  @Override
  public void onCompleted() {
  }

  @NotNull
  private static BoundaryResult runStateHandler(Stage stage,
      String stateName,
      BoundaryStateHandler handle) {
    return switch (stage) {
      case STAGE_SETUP_UNSPECIFIED -> handle.setup();
      case STAGE_TEARDOWN -> handle.teardown();
      case UNRECOGNIZED -> throw new ContractCaseCoreError(
          "Unrecognised state handler stage while trying to run '" + stateName +
              "'");
    };
  }
}
