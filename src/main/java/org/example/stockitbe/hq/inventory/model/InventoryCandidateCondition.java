package org.example.stockitbe.hq.inventory.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.common.model.BaseEntity;

import java.util.Date;

@Entity
@Table(name = "inventory_candidate_condition", uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_candidate_condition", columnNames = {"inventory_id", "condition_code"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryCandidateCondition extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_id", nullable = false)
    private Long inventoryId;

    @Column(name = "condition_code", nullable = false)
    private Integer conditionCode;

    @Column(name = "condition_label", nullable = false, length = 128)
    private String conditionLabel;

    @Column(name = "matched_at", nullable = false)
    private Date matchedAt;

    @Builder
    private InventoryCandidateCondition(Long inventoryId, Integer conditionCode, String conditionLabel, Date matchedAt) {
        this.inventoryId = inventoryId;
        this.conditionCode = conditionCode;
        this.conditionLabel = conditionLabel;
        this.matchedAt = matchedAt == null ? new Date() : matchedAt;
    }
}
