package com.db.awmd.challenge.exception;

public class SameOriginAndTargetAccountsException extends RuntimeException {

  public SameOriginAndTargetAccountsException() {
    super("Origin and target accounts must not be the same");
  }
}
