package app.unattach.utils;

public record Logger(org.apache.logging.log4j.Logger logger) {
  /**
   * @return A logger with the name of the class that called this method.
   */
  public static Logger get() {
    StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();
    String callerClassName = stackTrace[1].getClassName();
    return get(callerClassName);
  }

  public static Logger get(String name) {
    return new Logger(org.apache.logging.log4j.LogManager.getLogger(name));
  }

  public void debug(Object message) {
    logger.debug(message);
  }

  public void debug(String format, Object... args) {
    logger.debug(String.format(format, args));
  }

  public void info(Object message) {
    logger.info(message);
  }

  public void info(String format, Object... args) {
    logger.info(String.format(format, args));
  }

  public void warn(String format, Object... args) {
    logger.warn(String.format(format, args));
  }

  public void error(Object message) {
    logger.error(message);
  }

  public void error(String format, Object... args) {
    logger.error(String.format(format, args));
  }

  public void error(Object message, Throwable t) {
    logger.error(message, t);
  }
}
