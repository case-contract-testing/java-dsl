package io.contracttesting.contractcase;

import io.contract_testing.contractcase.case_boundary.ITriggerFunction;
import java.util.Map;

class BoundaryTriggerGroupMapper {

  static Map<String, ? extends ITriggerFunction> map(TriggerGroups triggers) {
    return triggers.toMap();
  }
}
