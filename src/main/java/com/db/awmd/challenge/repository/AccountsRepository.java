package com.db.awmd.challenge.repository;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;

import java.util.Optional;

public interface AccountsRepository {

  void createAccount(Account account) throws DuplicateAccountIdException;

  Optional<Account> getAccount(String accountId);

  void clearAccounts();
}
