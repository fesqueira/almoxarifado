package com.almoxarifado.ui;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Períodos oferecidos nos filtros de movimentações e de vendas.
public enum Period {
    THIS_MONTH("Este mês"),
    LAST_30_DAYS("Últimos 30 dias"),
    THIS_YEAR("Este ano"),
    ALL("Todo o período");

    private final String label;

    Period(String label) {
        this.label = label;
    }

    // Início do período; null quando não há limite.
    public LocalDateTime from() {
        LocalDate today = LocalDate.now();
        return switch (this) {
            case THIS_MONTH -> today.withDayOfMonth(1).atStartOfDay();
            case LAST_30_DAYS -> today.minusDays(30).atStartOfDay();
            case THIS_YEAR -> today.withDayOfYear(1).atStartOfDay();
            case ALL -> null;
        };
    }

    public boolean contains(LocalDateTime dateTime) {
        LocalDateTime from = from();
        return from == null || !dateTime.isBefore(from);
    }

    @Override
    public String toString() {
        return label;
    }
}
