package io.contract_testing.contractcase.client;

import static io.contract_testing.contractcase.client.InternalDefinerClient.CONTRACT_CASE_JAVA_WRAPPER;
import static io.contract_testing.contractcase.client.InternalDefinerClient.CONTRACT_CASE_TRIGGER_AND_TEST;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.BoolValue;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import io.contract_testing.contractcase.case_boundary.BoundaryFailure;
import io.contract_testing.contractcase.case_boundary.BoundaryFailureKindConstants;
import io.contract_testing.contractcase.case_boundary.BoundaryMockDefinition;
import io.contract_testing.contractcase.case_boundary.BoundaryResult;
import io.contract_testing.contractcase.case_boundary.BoundarySuccessWithAny;
import io.contract_testing.contractcase.case_boundary.BoundarySuccessWithMap;
import io.contract_testing.contractcase.case_boundary.ContractCaseBoundaryConfig;
import io.contract_testing.contractcase.grpc.ContractCaseStream;
import io.contract_testing.contractcase.grpc.ContractCaseStream.ContractCaseConfig;
import io.contract_testing.contractcase.grpc.ContractCaseStream.ContractCaseConfig.UsernamePassword;
import io.contract_testing.contractcase.grpc.ContractCaseStream.DefinitionRequest;
import io.contract_testing.contractcase.grpc.ContractCaseStream.DefinitionRequest.Builder;
import io.contract_testing.contractcase.grpc.ContractCaseStream.ResultFailure;
import io.contract_testing.contractcase.grpc.ContractCaseStream.ResultResponse;
import io.contract_testing.contractcase.grpc.ContractCaseStream.ResultSuccess;
import io.contract_testing.contractcase.grpc.ContractCaseStream.ResultSuccessHasAnyPayload;
import io.contract_testing.contractcase.grpc.ContractCaseStream.ResultSuccessHasMapPayload;
import io.contract_testing.contractcase.grpc.ContractCaseStream.RunExampleRequest;
import io.contract_testing.contractcase.grpc.ContractCaseStream.StateHandlerHandle;
import io.contract_testing.contractcase.grpc.ContractCaseStream.StateHandlerHandle.Stage;
import io.contract_testing.contractcase.grpc.ContractCaseStream.TriggerFunctionHandle;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

class ConnectorOutgoingMapper {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  static StringValue map(String s) {
    if (s == null) {
      return null;
    }
    return StringValue.newBuilder().setValue(s).build();
  }

  static BoolValue map(Boolean s) {
    if (s == null) {
      return null;
    }
    return BoolValue.newBuilder().setValue(s).build();
  }

  static ContractCaseConfig mapConfig(final @NotNull ContractCaseBoundaryConfig config) {
    var builder = ContractCaseConfig.newBuilder();

    if (config.getBrokerBasicAuth() != null) {
      var auth = config.getBrokerBasicAuth();
      builder.setBrokerBasicAuth(UsernamePassword.newBuilder()
          .setPassword(ConnectorOutgoingMapper.map(auth.getPassword()))
          .setUsername(ConnectorOutgoingMapper.map(auth.getUsername()))
          .build());
    }

    if (config.getPrintResults() != null) {
      builder.setPrintResults(ConnectorOutgoingMapper.map(config.getPrintResults()));
    }
    if (config.getThrowOnFail() != null) {
      builder.setThrowOnFail(ConnectorOutgoingMapper.map(config.getThrowOnFail()));
    }
    if (config.getTriggerAndTests() != null) {
      config.getTriggerAndTests()
          .forEach((key, value) -> builder.putTriggerAndTests(key,
              TriggerFunctionHandle.newBuilder()
                  .setHandle(ConnectorOutgoingMapper.map(key))
                  .build()));
    }
    if (config.getTriggerAndTest() != null) {
      builder.setTriggerAndTest(TriggerFunctionHandle.newBuilder()
          .setHandle(ConnectorOutgoingMapper.map(CONTRACT_CASE_TRIGGER_AND_TEST))
          .build());
    }

    if (config.getStateHandlers() != null) {
      // TODO: TEARDOWN FUNCTION
      config.getStateHandlers().forEach((key, value) -> {
        builder.addStateHandlers(StateHandlerHandle.newBuilder()
            .setHandle(ConnectorOutgoingMapper.map(key))
            .setStage(Stage.STAGE_SETUP_UNSPECIFIED)
            .build());
      });
    }

    if (config.getBaseUrlUnderTest() != null) {
      builder.setBaseUrlUnderTest(ConnectorOutgoingMapper.map(config.getBaseUrlUnderTest()));
    }

    if (config.getBrokerBaseUrl() != null) {
      builder.setBrokerBaseUrl(ConnectorOutgoingMapper.map(config.getBrokerBaseUrl()));
    }
    if (config.getBrokerCiAccessToken() != null) {
      builder.setBrokerCiAccessToken(ConnectorOutgoingMapper.map(config.getBrokerCiAccessToken()));
    }

    if (config.getConsumerName() != null) {
      builder.setConsumerName(ConnectorOutgoingMapper.map(config.getConsumerName()));
    }
    if (config.getContractDir() != null) {
      builder.setContractDir(ConnectorOutgoingMapper.map(config.getContractDir()));
    }
    if (config.getContractFilename() != null) {
      builder.setContractFilename(ConnectorOutgoingMapper.map(config.getContractFilename()));
    }
    if (config.getLogLevel() != null) {
      builder.setLogLevel(ConnectorOutgoingMapper.map(config.getLogLevel()));
    }
    if (config.getProviderName() != null) {
      builder.setProviderName(ConnectorOutgoingMapper.map(config.getProviderName()));
    }

    if (config.getPublish() != null) {
      builder.setPublish(ConnectorOutgoingMapper.map(config.getPublish()));
    }

    return builder.build();
  }


  @NotNull
  static ContractCaseStream.DefinitionRequest.Builder mapRunExampleRequest(JsonNode definition,
      @NotNull ContractCaseBoundaryConfig runConfig) {
    final var structBuilder = getStructBuilder(definition);
    return DefinitionRequest.newBuilder()
        .setRunExample(RunExampleRequest.newBuilder()
            .setConfig(ConnectorOutgoingMapper.mapConfig(runConfig)) // TODO handle additional state handlers or triggers
            .setExampleDefinition(structBuilder)
            .build());
  }

  static ContractCaseStream.DefinitionRequest.Builder mapRunRejectingExampleRequest(JsonNode definition, ContractCaseBoundaryConfig runConfig) {
    final var structBuilder = getStructBuilder(definition);
    return DefinitionRequest.newBuilder()
        .setRunExample(RunExampleRequest.newBuilder()
            .setConfig(ConnectorOutgoingMapper.mapConfig(runConfig)) // TODO handle additional state handlers or triggers
            .setExampleDefinition(structBuilder)
            .build());
  }


  @NotNull
  static ContractCaseStream.BoundaryResult mapResult(@NotNull BoundaryResult result) {
    return switch (result.getResultType()) {
      case ResultTypeConstantsCopy.RESULT_SUCCESS -> ContractCaseStream.BoundaryResult.newBuilder()
          .setSuccess(ResultSuccess.newBuilder().build())
          .build();
      case ResultTypeConstantsCopy.RESULT_FAILURE -> {
        var failure = ((BoundaryFailure) result);
        yield ContractCaseStream.BoundaryResult.newBuilder()
            .setFailure(ResultFailure.newBuilder()
                .setKind(ConnectorOutgoingMapper.map(failure.getKind()))
                .setLocation(ConnectorOutgoingMapper.map(failure.getLocation()))
                .setMessage(ConnectorOutgoingMapper.map(failure.getMessage()))
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
              .setKind(ConnectorOutgoingMapper.map(BoundaryFailureKindConstants.CASE_CORE_ERROR))
              .setLocation(ConnectorOutgoingMapper.map(CONTRACT_CASE_JAVA_WRAPPER))
              .setMessage(ConnectorOutgoingMapper.map(
                  "Tried to map an unknown result type: " + result.getResultType()))
              .build())
          .build();
    };
  }

  @NotNull
  static ContractCaseStream.DefinitionRequest.Builder mapResultResponse(BoundaryResult result) {
    return DefinitionRequest.newBuilder()
        .setResultResponse(ResultResponse.newBuilder()
            .setResult(mapResult(result)));
  }

  private static Value mapMapToValue(Object payload) {
    return Value.newBuilder()
        .setStructValue(getStructBuilder(objectMapper.valueToTree(payload)).build())
        .build();
  }

  private static Struct mapMapToStruct(Map<String, Object> payload) {
    return getStructBuilder(objectMapper.valueToTree(payload)).build();
  }

  @NotNull
  private static Struct.Builder getStructBuilder(JsonNode definition) {
    final var structBuilder = Struct.newBuilder();

    try {
      JsonFormat.parser()
          .merge(objectMapper.writeValueAsString(definition), structBuilder);
    } catch (JsonProcessingException | InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    }
    return structBuilder;
  }
}
