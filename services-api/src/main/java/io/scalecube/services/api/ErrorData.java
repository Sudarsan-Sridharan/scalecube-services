package io.scalecube.services.api;

public final class ErrorData {

  private int errorCode;
  private String errorMessage;

  /**
   * Constructor for SerDe.
   *
   * @deprecated exposed only for de/serialization purpose.
   */
  public ErrorData() {}

  /**
   * Create an error data.
   *
   * @param errorCode the business error code
   * @param errorMessage the error message
   */
  public ErrorData(int errorCode, String errorMessage) {
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
  }

  public int getErrorCode() {
    return errorCode;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  @Override
  public String toString() {
    return "ErrorData{" + "errorCode=" + errorCode + ", errorMessage='" + errorMessage + '\'' + '}';
  }
}
