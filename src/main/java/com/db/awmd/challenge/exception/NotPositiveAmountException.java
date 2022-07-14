package com.db.awmd.challenge.exception;

public class NotPositiveAmountException extends RuntimeException {

  public NotPositiveAmountException() {
    super("Amount must be a positive value");
  }
}
