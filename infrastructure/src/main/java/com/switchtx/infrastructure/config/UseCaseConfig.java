package com.switchtx.infrastructure.config;

import com.switchtx.application.port.out.AccountRepositoryPort;
import com.switchtx.application.port.out.CustomerRepositoryPort;
import com.switchtx.application.port.out.MovementRepositoryPort;
import com.switchtx.application.port.out.TransactionReferenceGenerator;
import com.switchtx.application.port.out.TransactionRepositoryPort;
import com.switchtx.application.port.out.UnitOfWork;
import com.switchtx.application.service.AccountService;
import com.switchtx.application.service.CustomerService;
import com.switchtx.application.service.TransactionQueryService;
import com.switchtx.application.service.TransactionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Ensambla los casos de uso (Java puro) con sus adaptadores. La capa de aplicación no tiene
 * anotaciones de Spring: la inyección se declara aquí, en el borde de la arquitectura.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public CustomerService customerService(CustomerRepositoryPort customers, AccountRepositoryPort accounts,
                                           UnitOfWork unitOfWork, Clock clock) {
        return new CustomerService(customers, accounts, unitOfWork, clock);
    }

    @Bean
    public AccountService accountService(AccountRepositoryPort accounts, CustomerRepositoryPort customers,
                                         MovementRepositoryPort movements, UnitOfWork unitOfWork, Clock clock) {
        return new AccountService(accounts, customers, movements, unitOfWork, clock);
    }

    @Bean
    public TransactionService transactionService(AccountRepositoryPort accounts,
                                                 TransactionRepositoryPort transactions,
                                                 MovementRepositoryPort movements,
                                                 TransactionReferenceGenerator referenceGenerator,
                                                 UnitOfWork unitOfWork, Clock clock) {
        return new TransactionService(accounts, transactions, movements, referenceGenerator, unitOfWork, clock);
    }

    @Bean
    public TransactionQueryService transactionQueryService(TransactionRepositoryPort transactions,
                                                           UnitOfWork unitOfWork) {
        return new TransactionQueryService(transactions, unitOfWork);
    }

    @Bean
    public com.switchtx.application.service.AuthService authService(
            com.switchtx.application.port.out.UserRepositoryPort userRepository,
            com.switchtx.application.port.out.PasswordEncoderPort passwordEncoder,
            UnitOfWork unitOfWork, Clock clock) {
        return new com.switchtx.application.service.AuthService(userRepository, passwordEncoder, unitOfWork, clock);
    }
}
