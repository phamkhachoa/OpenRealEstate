package com.proptech.realestate.controller;

import com.proptech.realestate.model.entity.Media;
import com.proptech.realestate.service.MediaService;
import jakarta.servlet.ServletContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/listings/{listingId}/media")
@RequiredArgsConstructor
public class MediaController {
    private final MediaService mediaService;

    @Value("${media.upload.dir:uploads/media}")
    private String uploadDir;

    // Lấy danh sách media cho listing
    @GetMapping
    public List<Media> getMedia(@PathVariable UUID listingId) {
        return mediaService.getMediaByListingId(listingId);
    }

    // Thêm media (metadata hoặc upload file)
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<Media> uploadMedia(
            @PathVariable UUID listingId,
            @RequestParam("mediaType") String mediaType,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "order", required = false) Integer order,
            @RequestParam(value = "isPrimary", required = false) Boolean isPrimary,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        // Lưu file vào thư mục local
        String filename = UUID.randomUUID() + "_" + StringUtils.cleanPath(file.getOriginalFilename());
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        Path filePath = uploadPath.resolve(filename);
        file.transferTo(filePath);
        String fileUrl = "/" + uploadDir + "/" + filename;

        Media media = new Media();
        media.setMediaType(mediaType);
        media.setMediaUrl(fileUrl);
        media.setDescription(description);
        media.setOrder(order);
        media.setIsPrimary(isPrimary != null ? isPrimary : false);
        Media saved = mediaService.addMedia(listingId, media);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // Xóa media
    @DeleteMapping("/{mediaId}")
    public ResponseEntity<Void> deleteMedia(@PathVariable UUID listingId, @PathVariable UUID mediaId) {
        mediaService.deleteMedia(mediaId);
        return ResponseEntity.noContent().build();
    }

    // Sửa thông tin media (metadata)
    @PutMapping("/{mediaId}")
    public ResponseEntity<Media> updateMedia(
            @PathVariable UUID listingId,
            @PathVariable UUID mediaId,
            @RequestBody Media update
    ) {
        Media saved = mediaService.updateMedia(mediaId, update);
        return ResponseEntity.ok(saved);
    }

    // Đặt media làm primary
    @PostMapping("/{mediaId}/set-primary")
    public ResponseEntity<Void> setPrimary(@PathVariable UUID listingId, @PathVariable UUID mediaId) {
        mediaService.setPrimary(mediaId);
        return ResponseEntity.ok().build();
    }
} 