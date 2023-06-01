package io.contract_testing.contractcase;

import io.contract_testing.contractcase.case_boundary.BoundaryFailure;
import io.contract_testing.contractcase.case_boundary.BoundaryFailureKindConstants;
import io.contract_testing.contractcase.case_boundary.BoundaryResult;
import java.util.Arrays;
import java.util.stream.Collectors;

class BoundaryExceptionMapper {

  private static String stackTraceToString(Exception e) {
    return Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).collect(
        Collectors.joining());
  }

  static BoundaryFailure map(Exception e) {
    return new BoundaryFailure(e.getClass().getName(), e.getMessage(), stackTraceToString(e));
  }

  static BoundaryFailure mapAsTriggerFailure(Exception e) {
    return new BoundaryFailure(BoundaryFailureKindConstants.CASE_TRIGGER_ERROR, e.getMessage(),
        stackTraceToString(e));
  }

  static BoundaryFailure mapAsVerifyFailure(Exception e) {
    return new BoundaryFailure(BoundaryFailureKindConstants.CASE_VERIFY_RETURN_ERROR,
        e.getMessage(),
        stackTraceToString(e));
  }

  public static BoundaryResult mapAsStateFailure(Exception e) {
    return new BoundaryFailure(BoundaryFailureKindConstants.CASE_CONFIGURATION_ERROR,
        "State handler failed with" + e.getMessage(),
        stackTraceToString(e));
  }
}
