package com.almoxarifado.model;

import java.util.Arrays;
import java.util.List;

public enum MovementReason {
    PURCHASE(MovementType.ENTRY, "Compra"),
    INITIAL_STOCK(MovementType.ENTRY, "Estoque inicial"),
    ADJUSTMENT_IN(MovementType.ENTRY, "Ajuste de inventário"),
    SALE(MovementType.EXIT, "Venda"),
    INTERNAL_USE(MovementType.EXIT, "Uso interno"),
    LOSS(MovementType.EXIT, "Perda / avaria"),
    ADJUSTMENT_OUT(MovementType.EXIT, "Ajuste de inventário");

    private final MovementType type;
    private final String label;

    MovementReason(MovementType type, String label) {
        this.type = type;
        this.label = label;
    }

    public MovementType getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    // Motivos que o usuário pode escolher na tela (estoque inicial só nasce no cadastro).
    public static List<MovementReason> selectableFor(MovementType type) {
        return Arrays.stream(values())
                .filter(reason -> reason.type == type && reason != INITIAL_STOCK)
                .toList();
    }

    @Override
    public String toString() {
        return label;
    }
}
