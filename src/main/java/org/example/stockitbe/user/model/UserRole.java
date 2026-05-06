package org.example.stockitbe.user.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {
    HQ("hq"),
    STORE("st"),
    WAREHOUSE("wa");

    private final String codePrefix;
}
