package io.contract_testing.contractcase;


import io.contract_testing.contractcase.edge.ConnectorExceptionMapper;
import io.contract_testing.contractcase.edge.ConnectorSuccess;
import io.contract_testing.contractcase.edge.ITriggerFunction;

class BoundaryTriggerMapper {

  static <T> ITriggerFunction map(
      Trigger<T> trigger,
      TestResponseFunction<T> testResponseFunction) {

    return config -> {
      T ret;
      try {
        ret = trigger.call(config);
      } catch (Throwable e) {
        return ConnectorExceptionMapper.mapAsTriggerFailure(e);
      }

      try {
        testResponseFunction.call(ret);
      } catch (Throwable e) {
        return ConnectorExceptionMapper.mapAsVerifyFailure(e);
      }

      return new ConnectorSuccess();
    };
  }

  static <T> ITriggerFunction map(Trigger<T> trigger,
      TestErrorResponseFunction testErrorResponseFunction) {
    return config -> {
      try {
        trigger.call(config);
        return ConnectorExceptionMapper.mapAsTriggerFailure(
            new RuntimeException("Expected the trigger to fail, but it did not"));
      } catch (Throwable triggerException) {
        try {
          testErrorResponseFunction.call(triggerException);
        } catch (Throwable verifyException) {
          return ConnectorExceptionMapper.mapAsVerifyFailure(verifyException);
        }
      }

      return new ConnectorSuccess();
    };
  }
}
