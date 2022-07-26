package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.InsufficientFundsException;
import com.db.awmd.challenge.exception.NonexistentAccountException;
import com.db.awmd.challenge.exception.NotPositiveAmountException;
import com.db.awmd.challenge.exception.SameOriginAndTargetAccountsException;
import com.db.awmd.challenge.repository.AccountsRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
    Account from = getAccount(fromId);
    Account to = getAccount(toId);

    validateTransaction(from, to, amount);

    Account firstLock = from;
    Account secondLock = to;

    if (from.getAccountId().compareTo(to.getAccountId()) < 0) {
      firstLock = to;
      secondLock = from;
    }

    synchronized (firstLock) {
      synchronized (secondLock) {
        updateBalancesAndNotifyAccountHolders(from, to, amount);
      }
    }
  }

  private void validateTransaction(Account from, Account to, BigDecimal amount) {
    if (Objects.equals(from.getAccountId(), to.getAccountId())) {
      throw new SameOriginAndTargetAccountsException();
    }
    if (!(amount.compareTo(BigDecimal.ZERO) > 0)) {
      throw new NotPositiveAmountException();
    }
    if (from.getBalance().subtract(amount).compareTo(BigDecimal.ZERO) < 0) {
      throw new InsufficientFundsException(
        "Account (ID: " + from.getAccountId() + ") has insufficient funds!");
    }
  }

  private void updateBalancesAndNotifyAccountHolders(Account from, Account to, BigDecimal amount) {
    from.setBalance(from.getBalance().subtract(amount));
    to.setBalance(to.getBalance().add(amount));

    accountsRepository.updateAccounts(new ArrayList<>(List.of(from, to)));

    notificationService.notifyAboutTransfer(
      from, "Transferred " + amount + " to " + to.getAccountId()
    );
    notificationService.notifyAboutTransfer(
      to, "Received " + amount + " from " + from.getAccountId()
    );
  }
}
