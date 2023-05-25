package io.contracttesting.contractcase.connectors;

import io.contract_testing.contractcase.case_boundary.BoundaryFailure;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ExceptionMapper {

  public static BoundaryFailure map(Exception e) {
    return new BoundaryFailure(e.getClass().getName(), e.getMessage(),
        Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).collect(
            Collectors.joining()));
  }

}
