package io.contract_testing.contractcase.client;

import com.google.protobuf.BoolValue;
import com.google.protobuf.StringValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.contract_testing.contractcase.ContractCaseCoreError;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

class ValueMapper {

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

  static String map(StringValue s) {
    if (s == null) {
      return null;
    }
    return s.getValue();
  }

  static Map<String, Object> map(Struct config) {
    return config.getFieldsMap().entrySet().stream()
        .map(entry -> Map.entry(entry.getKey(), ValueMapper.map(entry.getValue())))
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
          value.getListValue().getValuesList().stream().map(ValueMapper::map).toList();
      case KIND_NOT_SET -> throw new ContractCaseCoreError(
          "Attempted to map a value that doesn't have a set kind. This is probably a bug in the core library",
          "Java ValueMapper");
    };
  }
}
