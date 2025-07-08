package com.proptech.realestate.model.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "attribute_definitions")
@Data
public class AttributeDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String code; // e.g., "balcony_direction", "legal_status"

    @Column(nullable = false)
    private String name; // e.g., "Hướng ban công", "Tình trạng pháp lý"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DataType dataType;

    public enum DataType {
        TEXT,
        NUMBER,
        BOOLEAN,
        SELECT_SINGLE,
        SELECT_MULTI
    }
} 