package io.contracttesting.contractcase;

public class LogLevel {
  // TODO: Get these strings from the boundary

  /**
   * Print no logs (note, results may still be printed - see the configuration options for
   * printResults
   */
  public static final LogLevel NONE = new LogLevel("none");

  /**
   * Logs when something has gone wrong during the execution of the test framework
   */
  public static final LogLevel ERROR = new LogLevel("error");

  /**
   * Logs when it seems likely that there is a misconfiguration
   */
  public static final LogLevel WARN = new LogLevel("warn");

  /**
   * Logs information to help users find out what is happening during their tests
   */
  public static final LogLevel DEBUG = new LogLevel("debug");

  /**
   * Logs debugging information for ContractCase maintainers. Take care publishing this publicly, as
   * this level may print your secrets.
   */
  public static final LogLevel MAINTAINER_DEBUG = new LogLevel("maintainerDebug");

  /**
   * Logs very detailled debugging information for ContractCase maintainers. Take care publishing
   * this publicly, as this level may print your secrets.
   */
  public static final LogLevel DEEP_MAINTAINER_DEBUG = new LogLevel("deepMaintainerDebug");

  private final String level;

  private LogLevel(String level) {
    this.level = level;
  }

  public String toString() {
    return this.level;
  }
}
