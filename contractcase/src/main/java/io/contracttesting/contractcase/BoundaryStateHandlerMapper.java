package io.contracttesting.contractcase;

import io.contract_testing.contractcase.case_boundary.BoundaryResult;
import io.contract_testing.contractcase.case_boundary.BoundaryStateHandler;
import io.contract_testing.contractcase.case_boundary.BoundaryStateHandlerWithTeardown;
import io.contract_testing.contractcase.case_boundary.BoundarySuccess;
import io.contract_testing.contractcase.case_boundary.BoundarySuccessWithMap;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

class BoundaryStateHandlerMapper {

  public static Map<String, ? extends BoundaryStateHandler> map(
      Map<String, StateHandler> stateHandlers) {
    var ret = new HashMap<String, BoundaryStateHandler>();

    stateHandlers.forEach((key, value) -> ret.put(key, map(value)));

    return ret;
  }

  private static BoundaryStateHandlerWithTeardown map(StateHandler handler) {

    return new BoundaryStateHandlerWithTeardown() {

      @Override
      public @NotNull BoundaryResult setup() {
        try {
          var config = handler.setup();
          if (config == null) {
            return new BoundarySuccess();
          }
          return new BoundarySuccessWithMap(config);
        } catch (Exception e) {
          return BoundaryExceptionMapper.mapAsStateFailure(e);
        }
      }

      @Override
      public @NotNull BoundaryResult teardown() {
        try {
          handler.teardown();

          return new BoundarySuccess();
        } catch (Exception e) {
          return BoundaryExceptionMapper.mapAsStateFailure(e);
        }
      }
    };
  }
}
