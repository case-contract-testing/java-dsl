package io.contract_testing.contractcase;

import java.util.Map;
import org.jetbrains.annotations.NotNull;

public interface Trigger<T> {

  T call(final @NotNull Map<String, Object> config);
}
