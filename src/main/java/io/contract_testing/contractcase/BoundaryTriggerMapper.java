package io.contract_testing.contractcase;

import io.contract_testing.contractcase.case_boundary.BoundarySuccess;
import io.contract_testing.contractcase.case_boundary.ITriggerFunction;

class BoundaryTriggerMapper {

  static <T> ITriggerFunction map(
      Trigger<T> trigger,
      TestResponseFunction<T> testResponseFunction) {

    return config -> {
      T ret;
      try {
        ret = trigger.call(config);
      } catch (Throwable e) {
        return BoundaryExceptionMapper.mapAsTriggerFailure(e);
      }

      try {
        testResponseFunction.call(ret);
      } catch (Throwable e) {
        return BoundaryExceptionMapper.mapAsVerifyFailure(e);
      }

      return new BoundarySuccess();
    };
  }

  static <T> ITriggerFunction map(Trigger<T> trigger,
      TestErrorResponseFunction testErrorResponseFunction) {
    return config -> {
      try {
        trigger.call(config);
        return BoundaryExceptionMapper.mapAsTriggerFailure(
            new RuntimeException("Expected the trigger to fail, but it did not"));
      } catch (Throwable triggerException) {
        try {
          testErrorResponseFunction.call(triggerException);
        } catch (Throwable verifyException) {
          return BoundaryExceptionMapper.mapAsVerifyFailure(verifyException);
        }
      }

      return new BoundarySuccess();
    };
  }
}
