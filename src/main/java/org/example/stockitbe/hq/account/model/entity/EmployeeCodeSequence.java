package org.example.stockitbe.hq.account.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "employee_code_sequence")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmployeeCodeSequence {

    @Id
    @Column(name = "role_code", length = 2)
    private String roleCode;   // 'hq' | 'st' | 'wh'

    @Column(name = "last_number", nullable = false)
    private int lastNumber;

    /** 다음 번호로 증가 후 반환 — 트랜잭션 + 비관적 락 내부에서만 호출 */
    public int incrementAndGet() {
        return ++this.lastNumber;
    }

    public static EmployeeCodeSequence of(String roleCode, int initialNumber) {
        EmployeeCodeSequence seq = new EmployeeCodeSequence();
        seq.roleCode = roleCode;
        seq.lastNumber = initialNumber;
        return seq;
    }
}
