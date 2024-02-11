package io.contract_testing.contractcase.client;

import static io.contract_testing.contractcase.client.ConnectorOutgoingMapper.CONTRACT_CASE_TRIGGER_AND_TEST;

import io.contract_testing.contractcase.ContractCaseCoreError;
import io.contract_testing.contractcase.case_boundary.ContractCaseBoundaryConfig;
import io.contract_testing.contractcase.case_boundary.ITriggerFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class ConfigHandle {

  public void setBoundaryConfig(ContractCaseBoundaryConfig boundaryConfig) {
    this.boundaryConfig = boundaryConfig;
  }

  private ContractCaseBoundaryConfig boundaryConfig;

  ConfigHandle(ContractCaseBoundaryConfig boundaryConfig) {
    this.boundaryConfig = boundaryConfig;
  }

  @NotNull
  ITriggerFunction getTriggerFunction(String handle) {
    ITriggerFunction trigger = getTriggerInternal(handle);
    if (trigger == null) {
      throw new ContractCaseCoreError(
          "Unable to trigger the function with handle '" + handle + "', as it is null");
    }
    return trigger;
  }

  @Nullable
  private ITriggerFunction getTriggerInternal(String handle) {
    if (handle.equals(CONTRACT_CASE_TRIGGER_AND_TEST)) {
      return boundaryConfig.getTriggerAndTest();
    }

    var triggerMap = boundaryConfig.getTriggerAndTests();
    if (triggerMap == null) {
      throw new ContractCaseCoreError(
          "Unable to trigger the function with handle '" + handle
              + "', as the entire trigger map is null");
    }
    return triggerMap.get(handle);
  }
}
