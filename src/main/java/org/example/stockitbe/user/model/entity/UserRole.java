package org.example.stockitbe.user.model.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {
    HQ("hq"),
    STORE("st"),
    WAREHOUSE("wh");

    private final String codePrefix;
}
