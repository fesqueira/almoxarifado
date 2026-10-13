package com.almoxarifado.model;

public enum MovementType {
    ENTRY("Entrada"),
    EXIT("Saída");

    private final String label;

    MovementType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
