package com.db.awmd.challenge.domain;

import com.db.awmd.challenge.exception.InsufficientFundsException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class Account {

  @NotNull
  @NotEmpty
  private final String accountId;

  @NotNull
  @Min(value = 0, message = "Initial balance must be positive.")
  private BigDecimal balance;

  public Account(String accountId) {
    this.accountId = accountId;
    this.balance = BigDecimal.ZERO;
  }

  @JsonCreator
  public Account(@JsonProperty("accountId") String accountId,
    @JsonProperty("balance") BigDecimal balance) {
    this.accountId = accountId;
    this.balance = balance;
  }

  public void deposit(BigDecimal amount) {
    this.balance = this.balance.add(amount);
  }

  public void withdraw(BigDecimal amount) {
    BigDecimal finalAmount = this.balance.subtract(amount);
    if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
      throw new InsufficientFundsException(
        "Account (ID: " + accountId + ") has insufficient funds!");
    }
    this.balance = finalAmount;
  }
}
