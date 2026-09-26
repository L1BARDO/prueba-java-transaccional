package com.switchtx.infrastructure.adapter.in.rest.transaction;

/** Restricciones compartidas por los requests monetarios. */
final class MoneyFields {

    static final String MIN_AMOUNT = "0.01";
    static final int INTEGER_DIGITS = 17;
    static final int FRACTION_DIGITS = 2;
    static final String CURRENCY_REGEX = "^[A-Z]{3}$";
    static final String CURRENCY_MESSAGE = "debe ser un código ISO-4217 de 3 letras mayúsculas";

    private MoneyFields() {
    }
}
