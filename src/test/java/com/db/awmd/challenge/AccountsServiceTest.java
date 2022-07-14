package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.InsufficientFundsException;
import com.db.awmd.challenge.exception.NonexistentAccountException;
import com.db.awmd.challenge.exception.NotPositiveAmountException;
import com.db.awmd.challenge.exception.SameOriginAndTargetAccountsException;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.NotificationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountsServiceTest {

  private static Account acc1;
  private static Account acc2;

  @Autowired
  private AccountsService accountsService;

  @MockBean
  private NotificationService notificationService;

  @Before
  public void setup() {
    acc1 = new Account("Id-1", new BigDecimal(1000));
    acc2 = new Account("Id-2", new BigDecimal(0));
    accountsService.createAccount(acc1);
    accountsService.createAccount(acc2);
  }

  @After
  public void tearDown() {
    accountsService.getAccountsRepository().clearAccounts();
  }

  @Test
  public void addAccount() {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }

  @Test
  public void addAccount_failsOnDuplicateId() {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }
  }

  @Test
  public void transfer() {
    acc1.setBalance(new BigDecimal(1000));
    acc2.setBalance(new BigDecimal(0));

    accountsService.transfer(
      acc1.getAccountId(),
      acc2.getAccountId(),
      BigDecimal.valueOf(300)
    );

    assertEquals(BigDecimal.valueOf(700), acc1.getBalance());
    assertEquals(BigDecimal.valueOf(300), acc2.getBalance());
    verify(notificationService, times(1))
      .notifyAboutTransfer(eq(acc1), anyString());
    verify(notificationService, times(1))
      .notifyAboutTransfer(eq(acc2), anyString());
  }

  @Test
  public void transfer_concurrentThreads() throws InterruptedException {
    acc1.setBalance(new BigDecimal(1000));
    acc2.setBalance(new BigDecimal(0));

    // Action: 50 concurrent transfers
    // Account 1 transfers 25 times the amount of 10e to Account 2
    // Account 2 transfers 25 times the amount of 1e to Account 1
    callConcurrentTransfers(50);

    assertEquals(new BigDecimal(775), acc1.getBalance());
    assertEquals(new BigDecimal(225), acc2.getBalance());
    verify(notificationService, times(50))
      .notifyAboutTransfer(eq(acc1), anyString());
    verify(notificationService, times(50))
      .notifyAboutTransfer(eq(acc2), anyString());
  }

  private void callConcurrentTransfers(int numTransfers) throws InterruptedException {
    ExecutorService service = Executors.newFixedThreadPool(10);
    CountDownLatch latch = new CountDownLatch(numTransfers);

    for (int i = 0; i < numTransfers; i++) {
      int finalI = i;
      service.execute(() -> {
        if (finalI % 2 == 0) {
          accountsService.transfer(
            acc1.getAccountId(),
            acc2.getAccountId(),
            new BigDecimal(10)
          );
        } else {
          accountsService.transfer(
            acc2.getAccountId(),
            acc1.getAccountId(),
            new BigDecimal(1)
          );
        }
        latch.countDown();
      });
    }

    latch.await();
  }

  @Test(expected = NonexistentAccountException.class)
  public void transfer_NonexistentAccount() {
    accountsService.transfer(acc1.getAccountId(), "Id-0", BigDecimal.valueOf(300));
  }

  @Test(expected = InsufficientFundsException.class)
  public void transfer_InsufficientFunds() {
    acc1.setBalance(new BigDecimal(200));
    acc2.setBalance(new BigDecimal(1000));

    accountsService.transfer(
      acc1.getAccountId(),
      acc2.getAccountId(),
      BigDecimal.valueOf(300)
    );
  }

  @Test(expected = NotPositiveAmountException.class)
  public void transfer_AmountIsNegative() {
    acc1.setBalance(new BigDecimal(200));
    acc2.setBalance(new BigDecimal(1000));

    accountsService.transfer(
      acc1.getAccountId(),
      acc2.getAccountId(),
      BigDecimal.valueOf(-200)
    );
  }

  @Test(expected = NotPositiveAmountException.class)
  public void transfer_AmountIsZero() {
    acc1.setBalance(new BigDecimal(200));
    acc2.setBalance(new BigDecimal(1000));

    accountsService.transfer(
      acc1.getAccountId(),
      acc2.getAccountId(),
      BigDecimal.ZERO
    );
  }

  @Test(expected = SameOriginAndTargetAccountsException.class)
  public void transfer_SameOriginAndTargetAccounts() {
    acc1.setBalance(new BigDecimal(200));

    accountsService.transfer(
      acc1.getAccountId(),
      acc1.getAccountId(),
      BigDecimal.valueOf(200)
    );
  }
}
