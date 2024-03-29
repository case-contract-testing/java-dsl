package io.contract_testing.contractcase;

import io.contract_testing.contractcase.case_boundary.BoundaryFailure;
import io.contract_testing.contractcase.case_boundary.BoundaryFailureKindConstants;
import io.contract_testing.contractcase.case_boundary.BoundaryResult;
import java.util.Arrays;
import java.util.stream.Collectors;

class BoundaryExceptionMapper {

  static String stackTraceToString(Throwable e) {
    return Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).collect(
        Collectors.joining("\n"));
  }

  static BoundaryFailure map(Throwable e) {
    return new BoundaryFailure(e.getClass().getName(), e.getMessage(), stackTraceToString(e));
  }

  static BoundaryFailure mapAsTriggerFailure(Throwable e) {
    return new BoundaryFailure(
        BoundaryFailureKindConstants.CASE_TRIGGER_ERROR,
        "Trigger function failed: " + e.getMessage(),
        stackTraceToString(e)
    );
  }

  static BoundaryFailure mapAsVerifyFailure(Throwable e) {
    return new BoundaryFailure(
        BoundaryFailureKindConstants.CASE_VERIFY_RETURN_ERROR,
        "Verification failed: " + e.getMessage(),
        stackTraceToString(e)
    );
  }

  static BoundaryResult mapAsStateFailure(Throwable e) {
    return new BoundaryFailure(
        BoundaryFailureKindConstants.CASE_CONFIGURATION_ERROR,
        "State handler failed: " + e.getMessage(),
        stackTraceToString(e)
    );
  }
}
