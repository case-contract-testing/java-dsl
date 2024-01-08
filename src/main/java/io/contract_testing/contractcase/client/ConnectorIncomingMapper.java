package io.contract_testing.contractcase.client;

import com.google.protobuf.BoolValue;
import com.google.protobuf.StringValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.contract_testing.contractcase.ContractCaseCoreError;
import io.contract_testing.contractcase.case_boundary.PrintableMatchError;
import io.contract_testing.contractcase.case_boundary.PrintableMessageError;
import io.contract_testing.contractcase.case_boundary.PrintableTestTitle;
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


}
