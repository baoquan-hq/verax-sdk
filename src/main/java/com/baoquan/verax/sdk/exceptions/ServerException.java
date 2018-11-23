package com.baoquan.verax.sdk.exceptions;


public class ServerException extends Exception {

  private Long timestamp;

  public Long getTimestamp() {
    return timestamp;
  }

  public ServerException(String message, Long timestamp) {
    super(message);
    this.timestamp = timestamp;
  }
}
