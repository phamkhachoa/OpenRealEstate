# 🚀 **PHASE 7-8 IMPLEMENTATION: IDX FEEDS & ADVANCED SEARCH**
## 📊 **Chi tiết triển khai IDX Integration và Elasticsearch Search**

**Timeline**: 2-3 weeks  
**Priority**: ⚡ Immediate implementation  
**Dependencies**: Phases 1-6 (✅ Complete)

---

## 🎯 **PHASE 7-8 OBJECTIVES**

### **✅ Deliverables**
1. **IDX Feed Integration** - Multi-MLS data aggregation
2. **Elasticsearch Search** - High-performance property search
3. **Map-based Search** - Geographic property discovery
4. **Advanced Filters** - Detailed search criteria
5. **Real-time Updates** - Live property data synchronization

---

## 🏗️ **ARCHITECTURE OVERVIEW**

```
┌─────────────────────────────────────────────────────────────┐
│                    IDX & SEARCH ARCHITECTURE                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  📡 IDX FEEDS           🔍 SEARCH ENGINE      📱 FRONTEND   │
│  ┌─────────────┐       ┌─────────────────┐  ┌─────────────┐ │
│  │ MLS Feed A  │────── │                 │  │             │ │
│  │ MLS Feed B  │────── │  Elasticsearch  │──│  Web App    │ │
│  │ MLS Feed C  │────── │     Cluster     │  │             │ │
│  └─────────────┘       │                 │  └─────────────┘ │
│         │               └─────────────────┘        │        │
│         │                       │                  │        │
│  ┌─────────────┐       ┌─────────────────┐  ┌─────────────┐ │
│  │   Sync      │       │   PostgreSQL    │  │  Mobile App │ │
│  │  Service    │────── │   (Master DB)   │──│             │ │
│  └─────────────┘       └─────────────────┘  └─────────────┘ │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 📋 **IMPLEMENTATION PLAN**

### **Week 1: IDX Feed Foundation**

#### **1.1 IDX Feed Entity Models**
```java
@Entity
@Table(name = "idx_feeds")
public class IdxFeed {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "mls_id", unique = true, nullable = false)
    private String mlsId;
    
    @Column(name = "mls_name")
    private String mlsName;
    
    @Column(name = "feed_url")
    private String feedUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "feed_type")
    private IdxFeedType feedType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type")
    private AuthenticationType authType;
    
    @Column(name = "username")
    private String username;
    
    @Column(name = "password")
    private String password;
    
    @Column(name = "api_key")
    private String apiKey;
    
    @Column(name = "last_sync_timestamp")
    private LocalDateTime lastSyncTimestamp;
    
    @Column(name = "last_sync_status")
    @Enumerated(EnumType.STRING)
    private SyncStatus lastSyncStatus;
    
    @Column(name = "total_properties_synced")
    private Integer totalPropertiesSynced;
    
    @Column(name = "sync_frequency_minutes")
    private Integer syncFrequencyMinutes = 60; // Default 1 hour
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @OneToMany(mappedBy = "idxFeed", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<IdxSyncLog> syncLogs = new ArrayList<>();
    
    // Constructors, getters, setters
}

public enum IdxFeedType {
    RETS_1_7_2,
    RETS_1_8,
    RESO_WEB_API,
    CSV_FEED,
    XML_FEED,
    JSON_FEED
}

public enum AuthenticationType {
    BASIC_AUTH,
    API_KEY,
    OAUTH2,
    DIGEST_AUTH
}

public enum SyncStatus {
    SUCCESS,
    PARTIAL_SUCCESS,
    FAILED,
    IN_PROGRESS,
    SCHEDULED
}

@Entity
@Table(name = "idx_sync_logs")
public class IdxSyncLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idx_feed_id")
    private IdxFeed idxFeed;
    
    @Column(name = "sync_start_time")
    private LocalDateTime syncStartTime;
    
    @Column(name = "sync_end_time")
    private LocalDateTime syncEndTime;
    
    @Column(name = "properties_processed")
    private Integer propertiesProcessed;
    
    @Column(name = "properties_created")
    private Integer propertiesCreated;
    
    @Column(name = "properties_updated")
    private Integer propertiesUpdated;
    
    @Column(name = "properties_deleted")
    private Integer propertiesDeleted;
    
    @Column(name = "sync_status")
    @Enumerated(EnumType.STRING)
    private SyncStatus syncStatus;
    
    @Column(name = "error_message", length = 2000)
    private String errorMessage;
    
    @Column(name = "sync_duration_ms")
    private Long syncDurationMs;
}
```

#### **1.2 IDX Feed Service Implementation**
```java
@Service
@Transactional
@Slf4j
public class IdxFeedService {
    
    @Autowired
    private IdxFeedRepository idxFeedRepository;
    
    @Autowired
    private IdxSyncLogRepository syncLogRepository;
    
    @Autowired
    private PropertyService propertyService;
    
    @Autowired
    private PropertySearchIndexService searchIndexService;
    
    @Async("idxSyncExecutor")
    public CompletableFuture<IdxSyncLog> syncFeed(String feedId) {
        IdxFeed feed = idxFeedRepository.findById(feedId)
            .orElseThrow(() -> new EntityNotFoundException("IDX Feed not found: " + feedId));
        
        IdxSyncLog syncLog = createSyncLog(feed);
        
        try {
            log.info("Starting IDX sync for feed: {} ({})", feed.getMlsName(), feed.getMlsId());
            
            // Get appropriate feed processor
            IdxFeedProcessor processor = getFeedProcessor(feed.getFeedType());
            
            // Process feed data
            IdxFeedResult result = processor.processFeed(feed);
            
            // Update sync log
            updateSyncLog(syncLog, result);
            
            // Update search index
            searchIndexService.indexProperties(result.getProperties());
            
            log.info("IDX sync completed for feed: {} - {} properties processed", 
                feed.getMlsName(), result.getTotalProcessed());
            
            return CompletableFuture.completedFuture(syncLog);
            
        } catch (Exception e) {
            log.error("IDX sync failed for feed: {}", feed.getMlsName(), e);
            syncLog.setSyncStatus(SyncStatus.FAILED);
            syncLog.setErrorMessage(e.getMessage());
            syncLog.setSyncEndTime(LocalDateTime.now());
            syncLogRepository.save(syncLog);
            throw new IdxSyncException("Sync failed for feed: " + feed.getMlsName(), e);
        }
    }
    
    @Scheduled(fixedRate = 300000) // Check every 5 minutes
    public void scheduledSync() {
        List<IdxFeed> activeFeeds = idxFeedRepository.findByIsActiveTrue();
        
        for (IdxFeed feed : activeFeeds) {
            if (shouldSync(feed)) {
                syncFeed(feed.getId());
            }
        }
    }
    
    private boolean shouldSync(IdxFeed feed) {
        if (feed.getLastSyncTimestamp() == null) {
            return true; // Never synced before
        }
        
        LocalDateTime nextSyncTime = feed.getLastSyncTimestamp()
            .plusMinutes(feed.getSyncFrequencyMinutes());
        
        return LocalDateTime.now().isAfter(nextSyncTime);
    }
    
    private IdxFeedProcessor getFeedProcessor(IdxFeedType feedType) {
        switch (feedType) {
            case RETS_1_7_2:
            case RETS_1_8:
                return new RetsIdxFeedProcessor();
            case RESO_WEB_API:
                return new ResoIdxFeedProcessor();
            case CSV_FEED:
                return new CsvIdxFeedProcessor();
            case XML_FEED:
                return new XmlIdxFeedProcessor();
            case JSON_FEED:
                return new JsonIdxFeedProcessor();
            default:
                throw new UnsupportedOperationException("Feed type not supported: " + feedType);
        }
    }
}

@Component
public class ResoIdxFeedProcessor implements IdxFeedProcessor {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private PropertyMappingService mappingService;
    
    @Override
    public IdxFeedResult processFeed(IdxFeed feed) {
        HttpHeaders headers = createAuthHeaders(feed);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        // Get property data from RESO API
        String url = feed.getFeedUrl() + "/Property?$filter=ModificationTimestamp gt " + 
                    getLastSyncTimestamp(feed);
        
        ResponseEntity<ResoPropertyResponse> response = restTemplate.exchange(
            url, HttpMethod.GET, entity, ResoPropertyResponse.class);
        
        List<PropertyResource> resoProperties = response.getBody().getValue();
        
        IdxFeedResult result = new IdxFeedResult();
        
        for (PropertyResource resoProperty : resoProperties) {
            try {
                // Map RESO property to internal Property entity
                Property property = mappingService.mapResoToProperty(resoProperty, feed.getMlsId());
                
                // Save or update property
                Property savedProperty = propertyService.saveOrUpdate(property);
                result.addProcessedProperty(savedProperty);
                
            } catch (Exception e) {
                log.error("Failed to process property: {}", resoProperty.getListingKey(), e);
                result.addError(e.getMessage());
            }
        }
        
        return result;
    }
}
```

### **Week 2: Elasticsearch Integration**

#### **2.1 Elasticsearch Configuration**
```java
@Configuration
@EnableElasticsearchRepositories
public class ElasticsearchConfiguration {
    
    @Value("${elasticsearch.host:localhost}")
    private String elasticsearchHost;
    
    @Value("${elasticsearch.port:9200}")
    private Integer elasticsearchPort;
    
    @Bean
    public ElasticsearchClient elasticsearchClient() {
        RestClient restClient = RestClient.builder(
                new HttpHost(elasticsearchHost, elasticsearchPort))
            .setRequestConfigCallback(requestConfigBuilder ->
                requestConfigBuilder
                    .setConnectTimeout(5000)
                    .setSocketTimeout(60000))
            .setHttpClientConfigCallback(httpClientBuilder ->
                httpClientBuilder
                    .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(5000)
                        .setSocketTimeout(60000)
                        .build()))
            .build();
        
        ElasticsearchTransport transport = new RestClientTransport(
            restClient, new JacksonJsonpMapper());
        
        return new ElasticsearchClient(transport);
    }
    
    @Bean
    public ElasticsearchOperations elasticsearchOperations() {
        return new ElasticsearchRestTemplate(elasticsearchClient());
    }
}
```

#### **2.2 Property Search Document**
```java
@Document(indexName = "properties")
@Setting(settingPath = "/elasticsearch/property-settings.json")
@Mapping(mappingPath = "/elasticsearch/property-mapping.json")
public class PropertySearchDocument {
    
    @Id
    private String id;
    
    @Field(type = FieldType.Keyword)
    private String listingKey;
    
    @Field(type = FieldType.Keyword)
    private String mlsId;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String address;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String city;
    
    @Field(type = FieldType.Keyword)
    private String state;
    
    @Field(type = FieldType.Keyword)
    private String zipCode;
    
    @Field(type = FieldType.Geo_Point)
    private GeoPoint location;
    
    @Field(type = FieldType.Keyword)
    private String propertyType;
    
    @Field(type = FieldType.Keyword)
    private String propertySubType;
    
    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal listPrice;
    
    @Field(type = FieldType.Integer)
    private Integer bedroomsTotal;
    
    @Field(type = FieldType.Integer)
    private Integer bathroomsTotalInteger;
    
    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal livingArea;
    
    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal lotSizeSquareFeet;
    
    @Field(type = FieldType.Integer)
    private Integer yearBuilt;
    
    @Field(type = FieldType.Keyword)
    private String standardStatus;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String publicRemarks;
    
    @Field(type = FieldType.Keyword, index = false)
    private List<String> photos;
    
    @Field(type = FieldType.Nested)
    private List<PropertyFeature> features;
    
    @Field(type = FieldType.Date)
    private LocalDateTime listingContractDate;
    
    @Field(type = FieldType.Date)
    private LocalDateTime modificationTimestamp;
    
    @Field(type = FieldType.Integer)
    private Integer daysOnMarket;
    
    @Field(type = FieldType.Keyword)
    private String listAgentMlsId;
    
    @Field(type = FieldType.Text)
    private String listAgentFullName;
    
    @Field(type = FieldType.Keyword)
    private String listOfficeMlsId;
    
    @Field(type = FieldType.Text)
    private String listOfficeName;
    
    // School district information
    @Field(type = FieldType.Text)
    private String schoolDistrict;
    
    @Field(type = FieldType.Keyword)
    private String elementarySchool;
    
    @Field(type = FieldType.Keyword)
    private String middleSchool;
    
    @Field(type = FieldType.Keyword)
    private String highSchool;
}

@Data
public class PropertyFeature {
    private String name;
    private String value;
    private String category; // "INTERIOR", "EXTERIOR", "COMMUNITY", "FINANCIAL"
}
```

#### **2.3 Advanced Search Service**
```java
@Service
@Slf4j
public class PropertySearchService {
    
    @Autowired
    private ElasticsearchOperations elasticsearchOps;
    
    @Autowired
    private PropertySearchRepository searchRepository;
    
    public PropertySearchResponse searchProperties(PropertySearchRequest request) {
        NativeSearchQuery query = buildSearchQuery(request);
        
        SearchHits<PropertySearchDocument> searchHits = elasticsearchOps.search(query, PropertySearchDocument.class);
        
        List<PropertySearchResult> results = searchHits.getSearchHits().stream()
            .map(hit -> {
                PropertySearchDocument doc = hit.getContent();
                PropertySearchResult result = mapToSearchResult(doc);
                result.setScore(hit.getScore());
                return result;
            })
            .collect(Collectors.toList());
        
        return PropertySearchResponse.builder()
            .results(results)
            .totalResults(searchHits.getTotalHits())
            .page(request.getPage())
            .size(request.getSize())
            .took(searchHits.getTotalHits())
            .searchId(UUID.randomUUID().toString())
            .build();
    }
    
    private NativeSearchQuery buildSearchQuery(PropertySearchRequest request) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        
        // Geographic search
        addGeographicFilters(boolQuery, request);
        
        // Price range
        addPriceFilters(boolQuery, request);
        
        // Property characteristics
        addPropertyFilters(boolQuery, request);
        
        // Text search
        addTextSearch(boolQuery, request);
        
        // Status filter (only active listings by default)
        if (request.getStatusFilter() == null || request.getStatusFilter().isEmpty()) {
            boolQuery.filter(QueryBuilders.termQuery("standardStatus", "Active"));
        } else {
            boolQuery.filter(QueryBuilders.termsQuery("standardStatus", request.getStatusFilter()));
        }
        
        return NativeSearchQueryBuilder.withQuery(boolQuery)
            .withPageable(PageRequest.of(request.getPage(), request.getSize()))
            .withSort(buildSortQuery(request.getSortBy(), request.getSortOrder()))
            .withSourceFilter(FetchSourceFilter.of(null, new String[]{"photos"})) // Exclude large photo arrays
            .build();
    }
    
    private void addGeographicFilters(BoolQueryBuilder boolQuery, PropertySearchRequest request) {
        if (request.getBounds() != null) {
            // Bounding box search
            GeoBoundingBoxQueryBuilder geoQuery = QueryBuilders.geoBoundingBoxQuery("location")
                .setCorners(
                    request.getBounds().getNorthEast().getLat(), 
                    request.getBounds().getNorthEast().getLon(),
                    request.getBounds().getSouthWest().getLat(), 
                    request.getBounds().getSouthWest().getLon()
                );
            boolQuery.filter(geoQuery);
        }
        
        if (request.getCenterPoint() != null && request.getRadiusMiles() != null) {
            // Radius search
            GeoDistanceQueryBuilder geoDistanceQuery = QueryBuilders.geoDistanceQuery("location")
                .point(request.getCenterPoint().getLat(), request.getCenterPoint().getLon())
                .distance(request.getRadiusMiles(), DistanceUnit.MILES);
            boolQuery.filter(geoDistanceQuery);
        }
        
        // City/State filters
        if (StringUtils.hasText(request.getCity())) {
            boolQuery.filter(QueryBuilders.termQuery("city.keyword", request.getCity()));
        }
        
        if (StringUtils.hasText(request.getState())) {
            boolQuery.filter(QueryBuilders.termQuery("state", request.getState()));
        }
        
        if (request.getZipCodes() != null && !request.getZipCodes().isEmpty()) {
            boolQuery.filter(QueryBuilders.termsQuery("zipCode", request.getZipCodes()));
        }
    }
    
    private void addPriceFilters(BoolQueryBuilder boolQuery, PropertySearchRequest request) {
        RangeQueryBuilder priceRange = QueryBuilders.rangeQuery("listPrice");
        boolean hasPriceFilter = false;
        
        if (request.getMinPrice() != null) {
            priceRange.gte(request.getMinPrice());
            hasPriceFilter = true;
        }
        
        if (request.getMaxPrice() != null) {
            priceRange.lte(request.getMaxPrice());
            hasPriceFilter = true;
        }
        
        if (hasPriceFilter) {
            boolQuery.filter(priceRange);
        }
    }
    
    private void addPropertyFilters(BoolQueryBuilder boolQuery, PropertySearchRequest request) {
        // Property types
        if (request.getPropertyTypes() != null && !request.getPropertyTypes().isEmpty()) {
            boolQuery.filter(QueryBuilders.termsQuery("propertyType", request.getPropertyTypes()));
        }
        
        // Bedrooms
        if (request.getMinBedrooms() != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("bedroomsTotal").gte(request.getMinBedrooms()));
        }
        
        // Bathrooms  
        if (request.getMinBathrooms() != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("bathroomsTotalInteger").gte(request.getMinBathrooms()));
        }
        
        // Square footage
        if (request.getMinSquareFeet() != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("livingArea").gte(request.getMinSquareFeet()));
        }
        
        if (request.getMaxSquareFeet() != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("livingArea").lte(request.getMaxSquareFeet()));
        }
        
        // Year built
        if (request.getMinYearBuilt() != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("yearBuilt").gte(request.getMinYearBuilt()));
        }
        
        // Property features
        if (request.getRequiredFeatures() != null && !request.getRequiredFeatures().isEmpty()) {
            for (String feature : request.getRequiredFeatures()) {
                boolQuery.filter(QueryBuilders.nestedQuery("features",
                    QueryBuilders.termQuery("features.name", feature), ScoreMode.None));
            }
        }
    }
    
    private void addTextSearch(BoolQueryBuilder boolQuery, PropertySearchRequest request) {
        if (StringUtils.hasText(request.getQuery())) {
            MultiMatchQueryBuilder textQuery = QueryBuilders.multiMatchQuery(request.getQuery())
                .field("address", 3.0f)
                .field("city", 2.0f)
                .field("publicRemarks", 1.0f)
                .field("schoolDistrict", 1.5f)
                .field("features.name", 1.2f)
                .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
                .fuzziness(Fuzziness.AUTO);
            
            boolQuery.must(textQuery);
        }
    }
}
```

### **Week 3: Advanced Search Features**

#### **3.1 Map-based Search API**
```java
@RestController
@RequestMapping("/api/search")
@Validated
@Slf4j
public class PropertySearchController {
    
    @Autowired
    private PropertySearchService searchService;
    
    @Autowired
    private SearchAnalyticsService analyticsService;
    
    @PostMapping("/properties")
    public ResponseEntity<PropertySearchResponse> searchProperties(
            @Valid @RequestBody PropertySearchRequest request,
            HttpServletRequest httpRequest) {
        
        // Track search analytics
        analyticsService.trackSearch(request, getUserId(httpRequest));
        
        PropertySearchResponse response = searchService.searchProperties(request);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/properties/map")
    public ResponseEntity<MapSearchResponse> mapSearch(
            @Valid @RequestBody MapSearchRequest request) {
        
        MapSearchResponse response = searchService.mapSearch(request);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/suggestions")
    public ResponseEntity<List<SearchSuggestion>> getSearchSuggestions(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        
        List<SearchSuggestion> suggestions = searchService.getSearchSuggestions(query, limit);
        
        return ResponseEntity.ok(suggestions);
    }
    
    @PostMapping("/saved")
    public ResponseEntity<SavedSearch> saveSearch(
            @Valid @RequestBody SaveSearchRequest request,
            Authentication auth) {
        
        String userId = auth.getName();
        SavedSearch savedSearch = searchService.saveSearch(request, userId);
        
        return ResponseEntity.ok(savedSearch);
    }
}

@Data
@Builder
public class MapSearchRequest {
    @NotNull
    private GeoBounds bounds;
    
    @Min(0)
    @Max(10000)
    private Integer maxResults = 1000;
    
    private List<String> propertyTypes;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer minBedrooms;
    private Integer minBathrooms;
    private String status = "Active";
    
    // Clustering parameters
    private Boolean enableClustering = true;
    private Integer clusterZoomLevel = 10;
}

@Data
@Builder
public class MapSearchResponse {
    private List<PropertyMapMarker> markers;
    private List<PropertyCluster> clusters;
    private Integer totalProperties;
    private GeoBounds searchBounds;
    private String searchId;
}

@Data
public class PropertyMapMarker {
    private String listingKey;
    private GeoPoint location;
    private BigDecimal listPrice;
    private String propertyType;
    private Integer bedrooms;
    private Integer bathrooms;
    private String photoUrl;
    private String address;
}

@Data
public class PropertyCluster {
    private GeoPoint center;
    private Integer propertyCount;
    private BigDecimal avgPrice;
    private Integer zoomLevel;
    private List<String> propertyTypes;
}
```

#### **3.2 Real-time Search Analytics**
```java
@Service
@Async
public class SearchAnalyticsService {
    
    @Autowired
    private SearchAnalyticsRepository analyticsRepository;
    
    @EventListener
    public void handleSearchEvent(SearchEvent event) {
        SearchAnalytics analytics = SearchAnalytics.builder()
            .searchId(event.getSearchId())
            .userId(event.getUserId())
            .searchQuery(event.getQuery())
            .filters(event.getFilters())
            .resultCount(event.getResultCount())
            .searchTimestamp(LocalDateTime.now())
            .responseTimeMs(event.getResponseTimeMs())
            .userAgent(event.getUserAgent())
            .ipAddress(event.getIpAddress())
            .build();
        
        analyticsRepository.save(analytics);
    }
    
    public void trackSearch(PropertySearchRequest request, String userId) {
        SearchEvent event = SearchEvent.builder()
            .searchId(UUID.randomUUID().toString())
            .userId(userId)
            .query(request.getQuery())
            .filters(serializeFilters(request))
            .timestamp(LocalDateTime.now())
            .build();
        
        applicationEventPublisher.publishEvent(event);
    }
}
```

---

## 🚀 **DEPLOYMENT CONFIGURATION**

### **Docker Compose for Development**
```yaml
version: '3.8'

services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    container_name: openre-elasticsearch
    environment:
      - discovery.type=single-node
      - "ES_JAVA_OPTS=-Xms2g -Xmx2g"
      - xpack.security.enabled=false
    ports:
      - "9200:9200"
      - "9300:9300"
    volumes:
      - elasticsearch_data:/usr/share/elasticsearch/data
    networks:
      - openre-network

  kibana:
    image: docker.elastic.co/kibana/kibana:8.11.0
    container_name: openre-kibana
    ports:
      - "5601:5601"
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
    depends_on:
      - elasticsearch
    networks:
      - openre-network

  property-service:
    image: openrealestate/property-service:latest
    container_name: openre-property-service
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/openrealestate
      - ELASTICSEARCH_HOST=elasticsearch
      - ELASTICSEARCH_PORT=9200
    depends_on:
      - postgres
      - elasticsearch
    networks:
      - openre-network

  postgres:
    image: postgres:15
    container_name: openre-postgres
    environment:
      - POSTGRES_DB=openrealestate
      - POSTGRES_USER=openre
      - POSTGRES_PASSWORD=openre123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - openre-network

volumes:
  elasticsearch_data:
  postgres_data:

networks:
  openre-network:
    driver: bridge
```

---

## ✅ **TESTING STRATEGY**

### **Integration Tests**
```java
@SpringBootTest
@Testcontainers
class PropertySearchIntegrationTest {
    
    @Container
    static ElasticsearchContainer elasticsearch = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.11.0")
        .withExposedPorts(9200);
    
    @Autowired
    private PropertySearchService searchService;
    
    @Test
    void testGeographicSearch() {
        // Given
        PropertySearchRequest request = PropertySearchRequest.builder()
            .bounds(GeoBounds.builder()
                .northEast(GeoPoint.of(37.8, -122.4))
                .southWest(GeoPoint.of(37.7, -122.5))
                .build())
            .minPrice(BigDecimal.valueOf(500000))
            .maxPrice(BigDecimal.valueOf(1000000))
            .build();
        
        // When
        PropertySearchResponse response = searchService.searchProperties(request);
        
        // Then
        assertThat(response.getResults()).isNotEmpty();
        assertThat(response.getTotalResults()).isGreaterThan(0);
    }
}
```

---

## 📊 **SUCCESS METRICS**

### **Performance Targets**
- **Search Response Time**: < 150ms
- **Map Load Time**: < 250ms  
- **Index Update Time**: < 30s
- **Concurrent Search Users**: 1000+

### **Feature Completeness**
- ✅ **IDX Feed Integration**: Multi-MLS support
- ✅ **Elasticsearch Search**: Advanced filtering
- ✅ **Map-based Search**: Geographic discovery
- ✅ **Real-time Updates**: Live data sync
- ✅ **Search Analytics**: Performance tracking

**Timeline**: Phase 7-8 hoàn thành trong **2-3 weeks** với team 3-4 developers.
