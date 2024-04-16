package io.contract_testing.contractcase;


import io.contract_testing.contractcase.edge.ConnectorFailure;
import io.contract_testing.contractcase.edge.ConnectorFailureKindConstants;
import io.contract_testing.contractcase.edge.ConnectorResult;
import io.contract_testing.contractcase.edge.ConnectorResultTypeConstants;
import io.contract_testing.contractcase.edge.ConnectorSuccessWithAny;
import java.util.List;

class ConnectorResultMapper {

  static void mapVoid(ConnectorResult result) {
    final var resultType = result.getResultType();

    if (resultType.equals(ConnectorResultTypeConstants.RESULT_SUCCESS)) {
      return;
    }
    if (resultType.equals(ConnectorResultTypeConstants.RESULT_FAILURE)) {
      mapFailure((ConnectorFailure) result);
    }

    throw new ContractCaseCoreError(
        "Unexpected non-void ConnectorResult typa '" + resultType + "'",
        "ConnectorResultMapper.mapVoid"
    );
  }

  private static void mapFailure(ConnectorFailure result) {
    final var kind = result.getKind();

    if (kind.equals(ConnectorFailureKindConstants.CASE_BROKER_ERROR)
        || kind.equals(ConnectorFailureKindConstants.CASE_CONFIGURATION_ERROR)
        || kind.equals(ConnectorFailureKindConstants.CASE_TRIGGER_ERROR)) {
      throw new ContractCaseConfigurationError(result.getMessage(), result.getLocation());
    } else if (kind.equals(ConnectorFailureKindConstants.CASE_CORE_ERROR)) {
      throw new ContractCaseCoreError(result.getMessage(), result.getLocation());
    } else if (kind.equals(ConnectorFailureKindConstants.CASE_FAILED_ASSERTION_ERROR)
        || kind.equals(ConnectorFailureKindConstants.CASE_VERIFY_RETURN_ERROR)) {
      throw new ContractCaseExpectationsNotMet(result.getMessage(), result.getLocation());
    }

    throw new ContractCaseCoreError(
        "Unhandled error kind (" + kind + "): " + result.getMessage(),
        result.getLocation()
    );
  }

  public static List<ContractDescription> mapListAvailableContracts(ConnectorResult result) {
    final var resultType = result.getResultType();
    if (resultType.equals(ConnectorResultTypeConstants.RESULT_SUCCESS_HAS_ANY_PAYLOAD)) {
      // TODO implement this
      System.out.println(((ConnectorSuccessWithAny) result).getPayload());
      throw new ContractCaseCoreError("The parsing of this object hasn't yet been implemented"
          + ((ConnectorSuccessWithAny) result).getPayload());
    }
    if (resultType.equals(ConnectorResultTypeConstants.RESULT_FAILURE)) {
      mapFailure((ConnectorFailure) result);
    }
    throw new ContractCaseCoreError(
        "Unexpected non-void ConnectorResult typa '" + resultType + "'",
        "ConnectorResultMapper.mapListAvailableContracts"
    );
  }
}
