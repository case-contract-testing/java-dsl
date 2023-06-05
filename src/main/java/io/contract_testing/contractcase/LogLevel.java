package io.contract_testing.contractcase;

import io.contract_testing.contractcase.case_boundary.ConfigLogLevelConstants;

/**
 * Enum for the log level configuration option
 */
public enum LogLevel {

  /**
   * Print no logs (note, results may still be printed - see the configuration options for
   * printResults
   */
  NONE(ConfigLogLevelConstants.NONE),

  /**
   * Logs when something has gone wrong during the execution of the test framework
   */
  ERROR(ConfigLogLevelConstants.ERROR),

  /**
   * Logs when it seems likely that there is a misconfiguration
   */
  WARN(ConfigLogLevelConstants.WARN),

  /**
   * Logs information to help users find out what is happening during their tests
   */
  DEBUG(ConfigLogLevelConstants.DEBUG),

  /**
   * Logs debugging information for ContractCase maintainers. Take care publishing this publicly, as
   * this level may print your secrets.
   */
  MAINTAINER_DEBUG(ConfigLogLevelConstants.MAINTAINER_DEBUG),

  /**
   * Logs very detailled debugging information for ContractCase maintainers. Take care publishing
   * this publicly, as this level may print your secrets.
   */
  DEEP_MAINTAINER_DEBUG(
      ConfigLogLevelConstants.DEEP_MAINTAINER_DEBUG);

  private final String level;

  LogLevel(String level) {
    this.level = level;
  }

  public String toString() {
    return this.level;
  }
}
