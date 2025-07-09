package com.proptech.realestate.repository;

import com.proptech.realestate.model.entity.Media;
import com.proptech.realestate.model.entity.Listing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MediaRepository extends JpaRepository<Media, UUID> {
    List<Media> findByListing(Listing listing);
    List<Media> findByListingId(UUID listingId);
    List<Media> findByListingIdAndMediaType(UUID listingId, String mediaType);
} 