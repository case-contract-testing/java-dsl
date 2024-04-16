package io.contract_testing.contractcase.edge;

import io.contract_testing.contractcase.ContractCaseCoreError;
import io.contract_testing.contractcase.case_boundary.BoundaryFailureKindConstants;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ConnectorExceptionMapper {

  public static String stackTraceToString(Throwable e) {
    return Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).collect(
        Collectors.joining("\n"));
  }

  public static ConnectorFailure map(Throwable e) {
    var failure = new ConnectorFailure(
        e.getClass().getName(),
        e.getMessage(),
        stackTraceToString(e)
    );
    if (failure.getResultType() == null) {
      throw new ContractCaseCoreError(
          "Missing a result type - this is almost certainly a threading issue in the Java DSL (both grpc and jsii are not thread safe)");
    }
    return failure;
  }

  public static ConnectorFailure mapAsTriggerFailure(Throwable e) {
    return new ConnectorFailure(
        BoundaryFailureKindConstants.CASE_TRIGGER_ERROR,
        "Trigger function failed: " + e.getMessage(),
        stackTraceToString(e)
    );
  }

  static ConnectorFailure mapAsVerifyFailure(Throwable e) {
    return new ConnectorFailure(
        BoundaryFailureKindConstants.CASE_VERIFY_RETURN_ERROR,
        "Verification failed: " + e.getMessage(),
        stackTraceToString(e)
    );
  }

  public static ConnectorResult mapAsStateFailure(Throwable e) {
    return new ConnectorFailure(
        BoundaryFailureKindConstants.CASE_CONFIGURATION_ERROR,
        "State handler failed: " + e.getMessage(),
        stackTraceToString(e)
    );
  }
}
