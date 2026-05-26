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

    /**
     * 시퀀스 값을 명시적으로 설정 — 안전망 보정 전용.
     * (AccountService.generateEmployeeCode 에서 user 테이블 max 와 동기화 시 사용)
     * 비관적 락 + @Transactional 안에서만 호출되어야 함.
     */
    public void setLastNumber(int lastNumber) {
        this.lastNumber = lastNumber;
    }

    public static EmployeeCodeSequence of(String roleCode, int initialNumber) {
        EmployeeCodeSequence seq = new EmployeeCodeSequence();
        seq.roleCode = roleCode;
        seq.lastNumber = initialNumber;
        return seq;
    }
}
