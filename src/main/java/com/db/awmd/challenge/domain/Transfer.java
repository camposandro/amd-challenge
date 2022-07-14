package com.db.awmd.challenge.domain;

import lombok.Getter;

import javax.validation.constraints.NotEmpty;
import java.math.BigDecimal;

@Getter
public class Transfer {

  @NotEmpty
  private final String fromId;

  @NotEmpty
  private final String toId;

  @NotEmpty
  private final String amount;

  public Transfer(String fromId, String toId, BigDecimal amount) {
    this.fromId = fromId;
    this.toId = toId;
    this.amount = String.valueOf(amount);
  }

  public BigDecimal getBigDecimalAmount() {
    return BigDecimal.valueOf(Double.parseDouble(amount));
  }

  public String toString() {
    return "{\"fromId\":\"" + getFromId() + "\",\"toId\":\"" + getToId() + "\",\"amount\": "
      + getAmount() + "}";
  }
}
