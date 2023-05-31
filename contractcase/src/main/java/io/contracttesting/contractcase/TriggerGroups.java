package io.contracttesting.contractcase;

import io.contract_testing.contractcase.case_boundary.ITriggerFunction;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class TriggerGroups {

  private final Map<String, TriggerGroup<?>> triggerGroups = new HashMap();


  public <T> void addTriggerGroup(
      final @NotNull TriggerGroup<T> triggerGroup) {
    triggerGroups.put(triggerGroup.getName(), triggerGroup);
  }

  Map<String, ITriggerFunction> toMap() {
    var ret = new HashMap<String, ITriggerFunction>();

    triggerGroups.forEach((key, value) -> ret.putAll(value.toMap()));
    return ret;
  }
}
