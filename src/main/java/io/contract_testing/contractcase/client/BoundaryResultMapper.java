package io.contract_testing.contractcase.client;

import io.contract_testing.contractcase.ContractCaseConfigurationError;
import io.contract_testing.contractcase.ContractCaseCoreError;
import io.contract_testing.contractcase.ContractCaseExpectationsNotMet;
import io.contract_testing.contractcase.case_boundary.BoundaryFailure;
import io.contract_testing.contractcase.case_boundary.BoundaryFailureKindConstants;
import io.contract_testing.contractcase.case_boundary.BoundaryResult;
import io.contract_testing.contractcase.case_boundary.BoundaryResultTypeConstants;

class BoundaryResultMapper {

  static void map(BoundaryResult result) {
    final var resultType = result.getResultType();

    if (resultType.equals(BoundaryResultTypeConstants.RESULT_SUCCESS)) {
      return;
    }
    if (resultType.equals(BoundaryResultTypeConstants.RESULT_FAILURE)) {
      mapFailure((BoundaryFailure) result);
    }
  }

  private static void mapFailure(BoundaryFailure result) {
    String kind = result.getKind();

    if (kind.equals(BoundaryFailureKindConstants.CASE_BROKER_ERROR)
        || kind.equals(BoundaryFailureKindConstants.CASE_CONFIGURATION_ERROR)
        || kind.equals(BoundaryFailureKindConstants.CASE_TRIGGER_ERROR)) {
      throw new ContractCaseConfigurationError(result.getMessage(), result.getLocation());
    } else if (kind.equals(BoundaryFailureKindConstants.CASE_CORE_ERROR)) {
      throw new ContractCaseCoreError(result.getMessage(), result.getLocation());
    } else if (kind.equals(BoundaryFailureKindConstants.CASE_FAILED_ASSERTION_ERROR)
        || kind.equals(BoundaryFailureKindConstants.CASE_VERIFY_RETURN_ERROR)) {
      throw new ContractCaseExpectationsNotMet(result.getMessage(), result.getLocation());
    }

    throw new ContractCaseCoreError(
        "Unhandled error kind (" + kind + "): " + result.getMessage(),
        result.getLocation());
  }


}
