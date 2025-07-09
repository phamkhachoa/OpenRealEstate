package com.proptech.realestate.service;

import com.proptech.realestate.model.entity.Listing;
import com.proptech.realestate.model.entity.Media;
import com.proptech.realestate.repository.ListingRepository;
import com.proptech.realestate.repository.MediaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaService {
    private final MediaRepository mediaRepository;
    private final ListingRepository listingRepository;

    public List<Media> getMediaByListingId(UUID listingId) {
        return mediaRepository.findByListingId(listingId);
    }

    @Transactional
    public Media addMedia(UUID listingId, Media media) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new EntityNotFoundException("Listing not found: " + listingId));
        media.setListing(listing);
        Media saved = mediaRepository.save(media);
        updateMediaCounts(listing);
        return saved;
    }

    @Transactional
    public void deleteMedia(UUID mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new EntityNotFoundException("Media not found: " + mediaId));
        Listing listing = media.getListing();
        mediaRepository.delete(media);
        updateMediaCounts(listing);
    }

    @Transactional
    public Media updateMedia(UUID mediaId, Media update) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new EntityNotFoundException("Media not found: " + mediaId));
        media.setMediaType(update.getMediaType());
        media.setMediaUrl(update.getMediaUrl());
        media.setOrder(update.getOrder());
        media.setDescription(update.getDescription());
        media.setIsPrimary(update.getIsPrimary());
        Media saved = mediaRepository.save(media);
        updateMediaCounts(media.getListing());
        return saved;
    }

    @Transactional
    public void setPrimary(UUID mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new EntityNotFoundException("Media not found: " + mediaId));
        List<Media> all = mediaRepository.findByListing(media.getListing());
        for (Media m : all) {
            m.setIsPrimary(m.getId().equals(mediaId));
        }
        mediaRepository.saveAll(all);
    }

    private void updateMediaCounts(Listing listing) {
        List<Media> all = mediaRepository.findByListing(listing);
        long photoCount = all.stream().filter(m -> "PHOTO".equalsIgnoreCase(m.getMediaType())).count();
        long videoCount = all.stream().filter(m -> "VIDEO".equalsIgnoreCase(m.getMediaType())).count();
        listing.setPhotosCount((int) photoCount);
        listing.setVideosCount((int) videoCount);
        listingRepository.save(listing);
    }
} 