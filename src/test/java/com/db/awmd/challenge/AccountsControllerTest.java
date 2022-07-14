package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.service.AccountsService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class AccountsControllerTest {

  private MockMvc mockMvc;

  @Autowired
  private AccountsService accountsService;

  @Autowired
  private WebApplicationContext webApplicationContext;

  private Account acc1;
  private Account acc2;

  @Before
  public void prepareMockMvc() {
    this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

    // Reset the existing accounts before each test.
    accountsService.getAccountsRepository().clearAccounts();

    // Insert two accounts for transaction testing
    acc1 = new Account("Id-1");
    acc2 = new Account("Id-2");
    accountsService.createAccount(acc1);
    accountsService.createAccount(acc2);
  }

  @After
  public void tearDown() {
    accountsService.getAccountsRepository().clearAccounts();
  }

  @Test
  public void createAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    Account account = accountsService.getAccount("Id-123");
    assertThat(account.getAccountId()).isEqualTo("Id-123");
    assertThat(account.getBalance()).isEqualByComparingTo("1000");
  }

  @Test
  public void createDuplicateAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoBody() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNegativeBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountEmptyAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void getAccount() throws Exception {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
    this.accountsService.createAccount(account);
    this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
      .andExpect(status().isOk())
      .andExpect(
        content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
  }

  @Test
  public void transfer() throws Exception {
    acc1.setBalance(new BigDecimal(1000));
    acc2.setBalance(new BigDecimal(500));

    Transfer dto = new Transfer(acc1.getAccountId(), acc2.getAccountId(),
      new BigDecimal(100));

    this.mockMvc.perform(post("/v1/accounts/transfer")
        .contentType(MediaType.APPLICATION_JSON)
        .content(dto.toString()))
      .andExpect(status().isOk());
  }

  @Test
  public void transfer_NonexistentAccount() throws Exception {
    acc1.setBalance(new BigDecimal(200));

    Transfer dto = new Transfer(acc1.getAccountId(), "Id-0", new BigDecimal(100));

    this.mockMvc.perform(post("/v1/accounts/transfer")
        .contentType(MediaType.APPLICATION_JSON)
        .content(dto.toString()))
      .andExpect(status().isNotFound());
  }

  @Test
  public void transfer_InsufficientFunds() throws Exception {
    acc1.setBalance(new BigDecimal(10));
    acc2.setBalance(new BigDecimal(200));

    Transfer dto = new Transfer(acc1.getAccountId(), acc2.getAccountId(),
      new BigDecimal(500));

    this.mockMvc.perform(post("/v1/accounts/transfer")
        .contentType(MediaType.APPLICATION_JSON)
        .content(dto.toString()))
      .andExpect(status().isBadRequest());
  }

  @Test
  public void transfer_AmountIsNegative() throws Exception {
    acc1.setBalance(new BigDecimal(200));
    acc2.setBalance(new BigDecimal(200));

    Transfer dto = new Transfer(acc1.getAccountId(), acc2.getAccountId(),
      new BigDecimal(-20));

    this.mockMvc.perform(post("/v1/accounts/transfer")
        .contentType(MediaType.APPLICATION_JSON)
        .content(dto.toString()))
      .andExpect(status().isBadRequest());
  }

  @Test
  public void transfer_AmountIsZero() throws Exception {
    acc1.setBalance(new BigDecimal(200));
    acc2.setBalance(new BigDecimal(200));

    Transfer dto = new Transfer(acc1.getAccountId(), acc2.getAccountId(),
      new BigDecimal(0));

    this.mockMvc.perform(post("/v1/accounts/transfer")
        .contentType(MediaType.APPLICATION_JSON)
        .content(dto.toString()))
      .andExpect(status().isBadRequest());
  }

  @Test
  public void transfer_SameOriginAndTargetAccounts() throws Exception {
    acc1.setBalance(new BigDecimal(200));

    Transfer dto = new Transfer(acc1.getAccountId(), acc1.getAccountId(),
      new BigDecimal(50));

    this.mockMvc.perform(post("/v1/accounts/transfer")
        .contentType(MediaType.APPLICATION_JSON)
        .content(dto.toString()))
      .andExpect(status().isBadRequest());
  }
}
