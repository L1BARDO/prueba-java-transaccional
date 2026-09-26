package com.switchtx.infrastructure.adapter.out.persistence.repository;

import com.switchtx.application.port.in.account.AccountFilter;
import com.switchtx.application.port.in.transaction.TransactionFilter;
import com.switchtx.domain.model.customer.CustomerStatus;
import com.switchtx.infrastructure.adapter.out.persistence.entity.AccountEntity;
import com.switchtx.infrastructure.adapter.out.persistence.entity.CustomerEntity;
import com.switchtx.infrastructure.adapter.out.persistence.entity.TransactionEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/** Filtros dinámicos (Criteria API): solo se agregan los criterios informados. */
public final class Specifications {

    private Specifications() {
    }

    public static Specification<CustomerEntity> customers(CustomerStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<AccountEntity> accounts(AccountFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.customerId() != null) {
                predicates.add(cb.equal(root.get("customerId"), filter.customerId()));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<TransactionEntity> transactions(TransactionFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.accountId() != null) {
                predicates.add(cb.or(
                        cb.equal(root.get("sourceAccountId"), filter.accountId()),
                        cb.equal(root.get("destinationAccountId"), filter.accountId())));
            }
            if (filter.type() != null) {
                predicates.add(cb.equal(root.get("type"), filter.type()));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.from()));
            }
            if (filter.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.to()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
