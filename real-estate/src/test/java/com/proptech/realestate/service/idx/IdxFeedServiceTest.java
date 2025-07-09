package com.proptech.realestate.service.idx;

import com.proptech.realestate.dto.idx.IdxFeedResult;
import com.proptech.realestate.model.entity.*;
import com.proptech.realestate.repository.IdxFeedRepository;
import com.proptech.realestate.repository.IdxSyncLogRepository;
import com.proptech.realestate.service.PropertyService;
import com.proptech.realestate.service.search.PropertySearchIndexService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class for IDX Feed Service
 * Tests Phase 7 Implementation
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class IdxFeedServiceTest {

    @Mock
    private IdxFeedRepository idxFeedRepository;

    @Mock
    private IdxSyncLogRepository syncLogRepository;

    @Mock
    private PropertyService propertyService;

    @Mock
    private PropertySearchIndexService searchIndexService;

    @Mock
    private List<IdxFeedProcessor> feedProcessors;

    @Mock
    private IdxDataAggregationService aggregationService;

    private IdxFeedService idxFeedService;

    @BeforeEach
    void setUp() {
        idxFeedService = new IdxFeedService();
        // Use reflection to inject mocked dependencies
        try {
            java.lang.reflect.Field idxFeedRepoField = IdxFeedService.class.getDeclaredField("idxFeedRepository");
            idxFeedRepoField.setAccessible(true);
            idxFeedRepoField.set(idxFeedService, idxFeedRepository);

            java.lang.reflect.Field syncLogRepoField = IdxFeedService.class.getDeclaredField("syncLogRepository");
            syncLogRepoField.setAccessible(true);
            syncLogRepoField.set(idxFeedService, syncLogRepository);

            java.lang.reflect.Field propertyServiceField = IdxFeedService.class.getDeclaredField("propertyService");
            propertyServiceField.setAccessible(true);
            propertyServiceField.set(idxFeedService, propertyService);

            java.lang.reflect.Field searchIndexServiceField = IdxFeedService.class.getDeclaredField("searchIndexService");
            searchIndexServiceField.setAccessible(true);
            searchIndexServiceField.set(idxFeedService, searchIndexService);

            java.lang.reflect.Field feedProcessorsField = IdxFeedService.class.getDeclaredField("feedProcessors");
            feedProcessorsField.setAccessible(true);
            feedProcessorsField.set(idxFeedService, feedProcessors);

            java.lang.reflect.Field aggregationServiceField = IdxFeedService.class.getDeclaredField("aggregationService");
            aggregationServiceField.setAccessible(true);
            aggregationServiceField.set(idxFeedService, aggregationService);

        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mocked dependencies", e);
        }
    }

    @Test
    void testCreateFeed() {
        // Given
        IdxFeed feed = createTestFeed();
        when(idxFeedRepository.save(any(IdxFeed.class))).thenReturn(feed);

        // When
        IdxFeed createdFeed = idxFeedService.createFeed(feed);

        // Then
        assertNotNull(createdFeed);
        assertEquals(feed.getMlsName(), createdFeed.getMlsName());
        assertNotNull(createdFeed.getCreatedAt());
        assertNotNull(createdFeed.getUpdatedAt());
        verify(idxFeedRepository).save(feed);
    }

    @Test
    void testGetFeedById() {
        // Given
        String feedId = "test-feed-id";
        IdxFeed feed = createTestFeed();
        when(idxFeedRepository.findById(feedId)).thenReturn(Optional.of(feed));

        // When
        IdxFeed foundFeed = idxFeedService.getFeedById(feedId);

        // Then
        assertNotNull(foundFeed);
        assertEquals(feed.getMlsName(), foundFeed.getMlsName());
        verify(idxFeedRepository).findById(feedId);
    }

    @Test
    void testGetFeedById_NotFound() {
        // Given
        String feedId = "non-existent-feed";
        when(idxFeedRepository.findById(feedId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> idxFeedService.getFeedById(feedId));
        verify(idxFeedRepository).findById(feedId);
    }

    @Test
    void testUpdateFeed() {
        // Given
        String feedId = "test-feed-id";
        IdxFeed existingFeed = createTestFeed();
        IdxFeed feedUpdate = createTestFeed();
        feedUpdate.setMlsName("Updated MLS Name");

        when(idxFeedRepository.findById(feedId)).thenReturn(Optional.of(existingFeed));
        when(idxFeedRepository.save(any(IdxFeed.class))).thenReturn(existingFeed);

        // When
        IdxFeed updatedFeed = idxFeedService.updateFeed(feedId, feedUpdate);

        // Then
        assertNotNull(updatedFeed);
        assertEquals("Updated MLS Name", existingFeed.getMlsName());
        assertNotNull(existingFeed.getUpdatedAt());
        verify(idxFeedRepository).findById(feedId);
        verify(idxFeedRepository).save(existingFeed);
    }

    @Test
    void testUpdateFeedStatus() {
        // Given
        String feedId = "test-feed-id";
        IdxFeed feed = createTestFeed();
        feed.setIsActive(false);

        when(idxFeedRepository.findById(feedId)).thenReturn(Optional.of(feed));
        when(idxFeedRepository.save(any(IdxFeed.class))).thenReturn(feed);

        // When
        IdxFeed updatedFeed = idxFeedService.updateFeedStatus(feedId, true);

        // Then
        assertNotNull(updatedFeed);
        assertTrue(feed.getIsActive());
        assertNotNull(feed.getUpdatedAt());
        verify(idxFeedRepository).findById(feedId);
        verify(idxFeedRepository).save(feed);
    }

    @Test
    void testGetSyncStatus() {
        // Given
        String feedId = "test-feed-id";
        IdxFeed feed = createTestFeed();
        IdxSyncLog latestLog = createTestSyncLog();

        when(idxFeedRepository.findById(feedId)).thenReturn(Optional.of(feed));
        when(syncLogRepository.findTop1ByFeedIdOrderBySyncStartTimeDesc(feedId))
                .thenReturn(Arrays.asList(latestLog));

        // When
        Map<String, Object> status = idxFeedService.getSyncStatus(feedId);

        // Then
        assertNotNull(status);
        assertEquals(feedId, status.get("feedId"));
        assertEquals(feed.getMlsName(), status.get("feedName"));
        assertEquals(feed.getIsActive(), status.get("isActive"));
        assertEquals("IDLE", status.get("status"));
        assertEquals(latestLog.getSyncStatus(), status.get("lastSyncStatus"));
        verify(idxFeedRepository).findById(feedId);
        verify(syncLogRepository).findTop1ByFeedIdOrderBySyncStartTimeDesc(feedId);
    }

    @Test
    void testGetSystemHealth() {
        // Given
        when(idxFeedRepository.count()).thenReturn(5L);
        when(idxFeedRepository.countByIsActiveTrue()).thenReturn(3L);
        when(syncLogRepository.countBySyncStartTimeAfter(any(LocalDateTime.class))).thenReturn(10L);

        // When
        Map<String, Object> health = idxFeedService.getSystemHealth();

        // Then
        assertNotNull(health);
        assertEquals("HEALTHY", health.get("status"));
        assertEquals(5L, health.get("totalFeeds"));
        assertEquals(3L, health.get("activeFeeds"));
        assertEquals(0, health.get("runningJobs")); // No running jobs in test
        assertEquals(10L, health.get("recentSyncs24h"));
        assertNotNull(health.get("checkTime"));
        
        verify(idxFeedRepository).count();
        verify(idxFeedRepository).countByIsActiveTrue();
        verify(syncLogRepository).countBySyncStartTimeAfter(any(LocalDateTime.class));
    }

    @Test
    void testGetMlsCoverage() {
        // Given
        List<IdxFeed> allFeeds = Arrays.asList(
                createTestFeed("MLS001", "Test MLS 1"),
                createTestFeed("MLS001", "Test MLS 1 Secondary"),
                createTestFeed("MLS002", "Test MLS 2")
        );
        List<IdxFeed> activeFeeds = Arrays.asList(allFeeds.get(0), allFeeds.get(2));

        when(idxFeedRepository.findAll()).thenReturn(allFeeds);
        when(idxFeedRepository.findByIsActiveTrue()).thenReturn(activeFeeds);
        when(propertyService.countByMlsId("MLS001")).thenReturn(150L);
        when(propertyService.countByMlsId("MLS002")).thenReturn(200L);

        // When
        Map<String, Object> coverage = idxFeedService.getMlsCoverage();

        // Then
        assertNotNull(coverage);
        assertEquals(3, coverage.get("totalFeeds"));
        assertEquals(2, coverage.get("activeFeeds"));
        assertEquals(2, coverage.get("totalMlsProviders"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mlsDetails = (List<Map<String, Object>>) coverage.get("mlsDetails");
        assertNotNull(mlsDetails);
        assertEquals(2, mlsDetails.size());

        verify(idxFeedRepository).findAll();
        verify(idxFeedRepository).findByIsActiveTrue();
        verify(propertyService).countByMlsId("MLS001");
        verify(propertyService).countByMlsId("MLS002");
    }

    @Test
    void testGetSupportedFeedTypes() {
        // When
        List<Map<String, Object>> feedTypes = idxFeedService.getSupportedFeedTypes();

        // Then
        assertNotNull(feedTypes);
        assertFalse(feedTypes.isEmpty());
        
        // Verify all feed types are included
        Set<String> typeNames = new HashSet<>();
        for (Map<String, Object> feedType : feedTypes) {
            typeNames.add((String) feedType.get("type"));
            assertNotNull(feedType.get("displayName"));
            assertNotNull(feedType.get("description"));
            assertNotNull(feedType.get("supported"));
        }
        
        assertTrue(typeNames.contains("RESO_WEB_API"));
        assertTrue(typeNames.contains("RETS"));
        assertTrue(typeNames.contains("CSV"));
        assertTrue(typeNames.contains("XML"));
    }

    @Test
    void testDeleteFeed() {
        // Given
        String feedId = "test-feed-id";
        IdxFeed feed = createTestFeed();
        
        when(idxFeedRepository.findById(feedId)).thenReturn(Optional.of(feed));

        // When
        idxFeedService.deleteFeed(feedId);

        // Then
        verify(idxFeedRepository).findById(feedId);
        verify(idxFeedRepository).delete(feed);
    }

    // Helper methods to create test objects

    private IdxFeed createTestFeed() {
        return createTestFeed("TEST_MLS", "Test MLS");
    }

    private IdxFeed createTestFeed(String mlsId, String mlsName) {
        IdxFeed feed = new IdxFeed();
        feed.setId(UUID.randomUUID().toString());
        feed.setMlsId(mlsId);
        feed.setMlsName(mlsName);
        feed.setFeedUrl("https://api.testmls.com/reso");
        feed.setFeedType(IdxFeedType.RESO_WEB_API);
        feed.setAuthenticationType(AuthenticationType.OAUTH2);
        feed.setUsername("testuser");
        feed.setPassword("testpass");
        feed.setSyncFrequencyMinutes(60);
        feed.setIsActive(true);
        feed.setCreatedAt(LocalDateTime.now());
        feed.setUpdatedAt(LocalDateTime.now());
        
        Map<String, Object> config = new HashMap<>();
        config.put("batchSize", 1000);
        config.put("propertyTypes", Arrays.asList("Residential", "Commercial"));
        feed.setFeedConfiguration(config);
        
        return feed;
    }

    private IdxSyncLog createTestSyncLog() {
        IdxSyncLog log = new IdxSyncLog();
        log.setId(UUID.randomUUID().toString());
        log.setSyncStatus(SyncStatus.SUCCESS);
        log.setSyncStartTime(LocalDateTime.now().minusMinutes(30));
        log.setSyncEndTime(LocalDateTime.now().minusMinutes(25));
        log.setSyncDurationMs(300000L); // 5 minutes
        log.setPropertiesProcessed(1000);
        log.setPropertiesCreated(50);
        log.setPropertiesUpdated(950);
        log.setPropertiesDeleted(0);
        log.setPropertiesSkipped(0);
        log.setRecordsPerSecond(3.33);
        log.setCreatedAt(LocalDateTime.now().minusMinutes(25));
        return log;
    }
}
