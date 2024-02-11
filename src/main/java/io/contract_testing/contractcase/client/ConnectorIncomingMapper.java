package io.contract_testing.contractcase.client;

import static io.contract_testing.contractcase.client.MaintainerLog.CONTRACT_CASE_JAVA_WRAPPER;

import com.google.protobuf.StringValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.contract_testing.contractcase.ContractCaseCoreError;
import io.contract_testing.contractcase.case_boundary.BoundaryFailure;
import io.contract_testing.contractcase.case_boundary.BoundaryResult;
import io.contract_testing.contractcase.case_boundary.BoundarySuccess;
import io.contract_testing.contractcase.case_boundary.BoundarySuccessWithAny;
import io.contract_testing.contractcase.case_boundary.BoundarySuccessWithMap;
import io.contract_testing.contractcase.case_boundary.PrintableMatchError;
import io.contract_testing.contractcase.case_boundary.PrintableMessageError;
import io.contract_testing.contractcase.case_boundary.PrintableTestTitle;
import io.contract_testing.contractcase.grpc.ContractCaseStream;
import io.contract_testing.contractcase.grpc.ContractCaseStream.PrintMatchErrorRequest;
import io.contract_testing.contractcase.grpc.ContractCaseStream.PrintMessageErrorRequest;
import io.contract_testing.contractcase.grpc.ContractCaseStream.PrintTestTitleRequest;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

class ConnectorIncomingMapper {

  static String map(StringValue s) {
    if (s == null) {
      return null;
    }
    return s.getValue();
  }

  static Map<String, Object> map(Struct config) {
    return config.getFieldsMap().entrySet().stream()
        .map(entry -> Map.entry(entry.getKey(), ConnectorIncomingMapper.map(entry.getValue())))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  static Object map(Value value) {
    return switch (value.getKindCase()) {
      case NULL_VALUE -> null;
      case NUMBER_VALUE -> value.getNumberValue();
      case STRING_VALUE -> value.getStringValue();
      case BOOL_VALUE -> value.getBoolValue();
      case STRUCT_VALUE -> map(value.getStructValue());
      case LIST_VALUE ->
          value.getListValue().getValuesList().stream().map(ConnectorIncomingMapper::map).toList();
      case KIND_NOT_SET -> throw new ContractCaseCoreError(
          "Attempted to map a value that doesn't have a set kind. This is probably a bug in the core library",
          "Java ValueMapper");
    };
  }

  @NotNull
  static PrintableMatchError mapMatchErrorRequest(PrintMatchErrorRequest printMatchErrorRequest) {
    return PrintableMatchError.builder()
        .kind(ConnectorIncomingMapper.map(printMatchErrorRequest.getKind()))
        .expected(ConnectorIncomingMapper.map(printMatchErrorRequest.getExpected()))
        .actual(ConnectorIncomingMapper.map(printMatchErrorRequest.getActual()))
        .errorTypeTag(ConnectorIncomingMapper.map(printMatchErrorRequest.getErrorTypeTag()))
        .location(ConnectorIncomingMapper.map(printMatchErrorRequest.getLocation()))
        .locationTag(ConnectorIncomingMapper.map(printMatchErrorRequest.getLocationTag()))
        .message(ConnectorIncomingMapper.map(printMatchErrorRequest.getMessage()))
        .build();
  }

  @NotNull
  static PrintableMessageError mapMessageErrorRequest(PrintMessageErrorRequest printMessageErrorRequest) {
    return PrintableMessageError.builder()
        .errorTypeTag(ConnectorIncomingMapper.map(printMessageErrorRequest.getErrorTypeTag()))
        .kind(ConnectorIncomingMapper.map(printMessageErrorRequest.getKind()))
        .location(ConnectorIncomingMapper.map(printMessageErrorRequest.getLocation()))
        .locationTag(ConnectorIncomingMapper.map(printMessageErrorRequest.getLocationTag()))
        .message(ConnectorIncomingMapper.map(printMessageErrorRequest.getMessage()))
        .build();
  }


  @NotNull
  static PrintableTestTitle mapPrintableTestTitle(PrintTestTitleRequest printTestTitleRequest) {
    return PrintableTestTitle.builder()
        .title(ConnectorIncomingMapper.map(printTestTitleRequest.getTitle()))
        .kind(ConnectorIncomingMapper.map(printTestTitleRequest.getKind()))
        .additionalText(ConnectorIncomingMapper.map(printTestTitleRequest.getAdditionalText()))
        .icon(ConnectorIncomingMapper.map(printTestTitleRequest.getIcon()))
        .build();
  }


  static BoundaryResult mapBoundaryResult(ContractCaseStream.BoundaryResult wireBoundaryResult) {
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

}
