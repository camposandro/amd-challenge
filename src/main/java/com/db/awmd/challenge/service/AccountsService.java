package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.NonexistentAccountException;
import com.db.awmd.challenge.exception.NotPositiveAmountException;
import com.db.awmd.challenge.exception.SameOriginAndTargetAccountsException;
import com.db.awmd.challenge.repository.AccountsRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;

@Service
@Slf4j
public class AccountsService {

  @Getter
  @Autowired
  private AccountsRepository accountsRepository;

  @Autowired
  private NotificationService notificationService;

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId).orElseThrow(() ->
      new NonexistentAccountException("Account (ID: " + accountId + ") does not exist")
    );
  }

  public void transfer(String fromId, String toId, BigDecimal amount) {
    validateTransaction(fromId, toId, amount);

    Account from = getAccount(fromId);
    Account to = getAccount(toId);

    Account firstLock = from;
    Account secondLock = to;

    if (from.getAccountId().compareTo(to.getAccountId()) < 0) {
      firstLock = to;
      secondLock = from;
    }

    synchronized (firstLock) {
      synchronized (secondLock) {
        from.withdraw(amount);
        to.deposit(amount);
      }
    }

    notifyAccountHolders(from, to, amount);
  }

  private void validateTransaction(String fromId, String toId, BigDecimal amount) {
    if (!(amount.compareTo(BigDecimal.ZERO) > 0)) {
      throw new NotPositiveAmountException();
    }
    if (Objects.equals(fromId, toId)) {
      throw new SameOriginAndTargetAccountsException();
    }
  }

  private void notifyAccountHolders(Account from, Account to, BigDecimal amount) {
    notificationService.notifyAboutTransfer(
      from, "Transferred " + amount + " to " + to.getAccountId()
    );
    notificationService.notifyAboutTransfer(
      to, "Received " + amount + " from " + from.getAccountId()
    );
  }
}
