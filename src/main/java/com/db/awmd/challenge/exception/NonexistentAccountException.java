package com.db.awmd.challenge.exception;

public class NonexistentAccountException extends RuntimeException {

  public NonexistentAccountException(String message) {
    super(message);
  }
}
