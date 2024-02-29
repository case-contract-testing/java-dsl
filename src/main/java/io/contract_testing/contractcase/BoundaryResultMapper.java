package io.contract_testing.contractcase;

import io.contract_testing.contractcase.case_boundary.BoundaryFailure;
import io.contract_testing.contractcase.case_boundary.BoundaryFailureKindConstants;
import io.contract_testing.contractcase.case_boundary.BoundaryResult;
import io.contract_testing.contractcase.case_boundary.BoundaryResultTypeConstants;
import io.contract_testing.contractcase.case_boundary.BoundarySuccessWithAny;
import java.util.List;

class BoundaryResultMapper {

  static void mapVoid(BoundaryResult result) {
    final var resultType = result.getResultType();

    if (resultType.equals(BoundaryResultTypeConstants.RESULT_SUCCESS)) {
      return;
    }
    if (resultType.equals(BoundaryResultTypeConstants.RESULT_FAILURE)) {
      mapFailure((BoundaryFailure) result);
    }

    throw new ContractCaseCoreError(
        "Unexpected non-void BoundaryResult typa '" + resultType + "'",
        "BoundaryResultMapper.mapVoid"
    );
  }

  private static void mapFailure(BoundaryFailure result) {
    final var kind = result.getKind();

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
        result.getLocation()
    );
  }

  public static List<ContractDescription> mapListAvailableContracts(BoundaryResult result) {
    final var resultType = result.getResultType();
    if (resultType.equals(BoundaryResultTypeConstants.RESULT_SUCCESS_HAS_ANY_PAYLOAD)) {
      // TODO implement this
      System.out.println(((BoundarySuccessWithAny) result).getPayload());
      throw new ContractCaseCoreError("The parsing of this object hasn't yet been implemented"
          + ((BoundarySuccessWithAny) result).getPayload());
    }
    if (resultType.equals(BoundaryResultTypeConstants.RESULT_FAILURE)) {
      mapFailure((BoundaryFailure) result);
    }
    throw new ContractCaseCoreError(
        "Unexpected non-void BoundaryResult typa '" + resultType + "'",
        "BoundaryResultMapper.mapListAvailableContracts"
    );
  }
}
