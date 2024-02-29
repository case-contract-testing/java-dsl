package io.contract_testing.contractcase;

import io.contract_testing.contractcase.case_boundary.BoundaryCrashMessage;

class BoundaryCrashReporter {

  static void handleAndRethrow(Throwable e) {
    // This method should not call BoundaryResultMapper
    if (e instanceof ContractCaseConfigurationError) {
      throw (ContractCaseConfigurationError) e;
    } else if (e instanceof ContractCaseExpectationsNotMet) {
      throw (ContractCaseExpectationsNotMet) e;
    }
    if (e instanceof ContractCaseCoreError) {
      System.err.println(
          BoundaryCrashMessage.CRASH_MESSAGE_START
              + "\n\n"
              + e
              + "\n"
              + ((ContractCaseCoreError) e).getLocation()
              + "\n\n"
              + BoundaryCrashMessage.CRASH_MESSAGE_END);
    } else {
      System.err.println(
          BoundaryCrashMessage.CRASH_MESSAGE_START
              + "\n\n"
              + e.toString()
              + "\n"
              + BoundaryExceptionMapper.stackTraceToString(e)
              + (e.getCause() != null ? "Caused by: " + e.getCause().toString() + "\n"
              + BoundaryExceptionMapper.stackTraceToString(e.getCause()) : "")
              + "\n\n"
              + BoundaryCrashMessage.CRASH_MESSAGE_END);
    }

    throw new ContractCaseCoreError(e);
  }

}
