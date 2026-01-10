package com.github.kusoroadeolu.annote;

public enum Type {
    NUMBER("num"),
    BOOLEAN("bool"),
    STRING("string");

    private final String value;

    Type(String value) {
        this.value = value;
    }

    public boolean isNum(){
        return this == NUMBER;
    }

    public boolean isBool(){
        return this == BOOLEAN;
    }

    public boolean isString(){
        return this == STRING;
    }

     public static Type fromString(String type){
        return switch (type){
            case "num" -> Type.NUMBER;
            case "bool" -> Type.BOOLEAN;
            case "string" -> Type.STRING;
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    public String toString(){
        return value;
    }


}
