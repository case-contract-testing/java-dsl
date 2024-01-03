package io.contract_testing.contractcase.client;

import io.contract_testing.contractcase.ContractCaseCoreError;
import io.contract_testing.contractcase.case_boundary.BoundaryResultTypeConstants;
import org.jetbrains.annotations.NotNull;

class ResultTypeConstantsCopy {

  /**
   * This is a success type with no payload
   */
  static final String RESULT_SUCCESS = "Success";
  /**
   * This is a success type with a map payload
   */
  static final String RESULT_SUCCESS_HAS_MAP_PAYLOAD = "SuccessMap";
  /**
   * This is a success type with an arbitrary object payload
   */
  static final String RESULT_SUCCESS_HAS_ANY_PAYLOAD = "SuccessAny";
  /**
   * This is a failure
   */
  static final String RESULT_FAILURE = "Failure";


  private static void checkEqual(@NotNull String wrapper, @NotNull String boundary) {
    if (!wrapper.equals(boundary)) {
      throw new ContractCaseCoreError(
          "Mismatched result type constants - the wrapper has '" + wrapper
              + "', but the boundary expects '" + boundary
              + "'. This is a bug in the ContractCase wrapper.",
          "Java Wrapper");
    }
  }

  static void validate() {
    checkEqual(RESULT_SUCCESS, BoundaryResultTypeConstants.RESULT_SUCCESS);
    checkEqual(RESULT_FAILURE, BoundaryResultTypeConstants.RESULT_FAILURE);
    checkEqual(RESULT_SUCCESS_HAS_ANY_PAYLOAD,
        BoundaryResultTypeConstants.RESULT_SUCCESS_HAS_ANY_PAYLOAD);
    checkEqual(RESULT_SUCCESS_HAS_MAP_PAYLOAD,
        BoundaryResultTypeConstants.RESULT_SUCCESS_HAS_MAP_PAYLOAD);
  }
}
