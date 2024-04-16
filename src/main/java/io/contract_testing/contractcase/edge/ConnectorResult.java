package io.contract_testing.contractcase.edge;

import io.contract_testing.contractcase.ContractCaseCoreError;
import io.contract_testing.contractcase.case_boundary.BoundaryFailure;
import io.contract_testing.contractcase.case_boundary.BoundaryResult;
import io.contract_testing.contractcase.case_boundary.BoundarySuccess;
import io.contract_testing.contractcase.case_boundary.BoundarySuccessWithAny;
import io.contract_testing.contractcase.case_boundary.BoundarySuccessWithMap;
import java.util.concurrent.Semaphore;
import org.jetbrains.annotations.NotNull;

public abstract class ConnectorResult {

  private static final Semaphore jsiiMutex = new Semaphore(1);

  public static BoundaryResult toBoundaryResult(ConnectorResult result) {
    try {
      jsiiMutex.acquire();
      return switch (result.getResultType()) {
        case ConnectorResultTypeConstants.RESULT_FAILURE -> {
          var failure = (ConnectorFailure) result;
          yield new BoundaryFailure(failure.getKind(), failure.getMessage(), failure.getLocation());
        }
        case ConnectorResultTypeConstants.RESULT_SUCCESS -> new BoundarySuccess();
        case ConnectorResultTypeConstants.RESULT_SUCCESS_HAS_ANY_PAYLOAD ->
            new BoundarySuccessWithAny(((ConnectorSuccessWithAny) result).getPayload());
        case ConnectorResultTypeConstants.RESULT_SUCCESS_HAS_MAP_PAYLOAD ->
            new BoundarySuccessWithMap(((ConnectorSuccessWithMap) result).getPayload());
        default -> throw new ContractCaseCoreError(
            "Unexpected type of connector result: " + result.getResultType());
      };
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      jsiiMutex.release();
    }
  }

  public static ConnectorResult fromBoundaryResult(@NotNull BoundaryResult result) {
    try {
      jsiiMutex.acquire();
      return switch (result.getResultType()) {
        case ConnectorResultTypeConstants.RESULT_FAILURE -> {
          var failure = (BoundaryFailure) result;
          yield new ConnectorFailure(
              failure.getKind(),
              failure.getMessage(),
              failure.getLocation()
          );
        }
        case ConnectorResultTypeConstants.RESULT_SUCCESS -> new ConnectorSuccess();
        case ConnectorResultTypeConstants.RESULT_SUCCESS_HAS_ANY_PAYLOAD ->
            new ConnectorSuccessWithAny(((BoundarySuccessWithAny) result).getPayload());
        case ConnectorResultTypeConstants.RESULT_SUCCESS_HAS_MAP_PAYLOAD ->
            new ConnectorSuccessWithMap(((BoundarySuccessWithMap) result).getPayload());
        default -> throw new ContractCaseCoreError(
            "Unexpected type of connector result: " + result.getResultType());
      };
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      jsiiMutex.release();
    }
  }

  public abstract String getResultType();

}
