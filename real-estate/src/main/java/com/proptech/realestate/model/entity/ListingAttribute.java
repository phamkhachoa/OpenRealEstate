package com.proptech.realestate.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "listing_attributes")
@Data
public class ListingAttribute {

    @EmbeddedId
    private ListingAttributeId id = new ListingAttributeId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("listingId")
    @JoinColumn(name = "listing_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("attributeId")
    @JoinColumn(name = "attribute_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private AttributeDefinition definition;

    @Column(name = "attribute_value", nullable = false)
    private String value;

    // --- EmbeddedId Class ---
    @Embeddable
    @Data
    public static class ListingAttributeId implements Serializable {
        private static final long serialVersionUID = 1L;

        @Column(name = "listing_id")
        private UUID listingId;

        @Column(name = "attribute_id")
        private Integer attributeId;
    }
} 