package io.contract_testing.contractcase.client;

import static io.contract_testing.contractcase.client.ConnectorOutgoingMapper.CONTRACT_CASE_TRIGGER_AND_TEST;

import io.contract_testing.contractcase.ContractCaseConfigurationError;
import io.contract_testing.contractcase.case_boundary.BoundaryStateHandler;
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

  BoundaryStateHandler getStateHandler(String stateName) {
    if (boundaryConfig.getStateHandlers() == null) {
      throw new ContractCaseConfigurationError(
          "No state handlers provided, you must provide a state handler that can run '" + stateName
              + "'");
    }
    var stateHandler = boundaryConfig.getStateHandlers().get(stateName);

    if (stateHandler == null) {
      throw new ContractCaseConfigurationError(
          "The state handler named '" + stateName
              + "' was not provided in the configuration");
    }
    return stateHandler;
  }

  @NotNull
  ITriggerFunction getTriggerFunction(String handle) {
    ITriggerFunction trigger = getTriggerInternal(handle);
    if (trigger == null) {
      throw new ContractCaseConfigurationError(
          "The trigger function named '" + handle
              + "' wasn't provided in the configuration, but is required by this test run");
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
      throw new ContractCaseConfigurationError(
          "No trigger functions were provided, unable to run the trigger function '" + handle
              + "'");
    }
    return triggerMap.get(handle);
  }


}
