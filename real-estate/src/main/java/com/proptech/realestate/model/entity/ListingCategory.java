package com.proptech.realestate.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "listing_categories")
@Data
public class ListingCategory {

    @Id
    private Integer id;

    private String name;
} 