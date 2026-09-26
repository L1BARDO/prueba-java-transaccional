package com.switchtx.application.service;

import com.switchtx.application.port.out.UnitOfWork;

import java.util.function.Supplier;

class FakeUnitOfWork implements UnitOfWork {

    @Override
    public <T> T execute(Supplier<T> operation) {
        return operation.get();
    }

    @Override
    public <T> T executeReadOnly(Supplier<T> operation) {
        return operation.get();
    }
}
