package com.proptech.realestate.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "media")
public class Media {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @Column(nullable = false)
    private String mediaType; // PHOTO, VIDEO, DOCUMENT, ...

    @Column(nullable = false)
    private String mediaUrl;

    @Column(name = "media_order")
    private Integer order;

    private String description;

    @Column(name = "is_primary")
    private Boolean isPrimary = false;

    private LocalDateTime modificationTimestamp;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.modificationTimestamp = LocalDateTime.now();
    }
} 