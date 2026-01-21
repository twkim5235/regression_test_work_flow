package com.example.ddd_start.common.domain.exception;

public class InvalidUsernameLengthException extends RuntimeException {

  public InvalidUsernameLengthException() {
    super("Username must be 10 characters or less");
  }

  public InvalidUsernameLengthException(String message) {
    super(message);
  }
}
