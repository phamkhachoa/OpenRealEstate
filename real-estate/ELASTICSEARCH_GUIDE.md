# Hướng Dẫn Elasticsearch cho Real Estate System

## Tổng quan

Hệ thống đã được tích hợp Elasticsearch để cung cấp khả năng tìm kiếm nâng cao với:
- **Full-text Search**: Tìm kiếm văn bản tự nhiên, fuzzy matching
- **Geospatial Search**: Tìm kiếm theo tọa độ và bán kính
- **Faceted Search**: Lọc theo nhiều tiêu chí đồng thời
- **Autocomplete**: Gợi ý tìm kiếm thông minh
- **Similar Listings**: Tìm các listing tương tự

## Setup và Cài đặt

### 1. Khởi động Elasticsearch và Kibana
```bash
# Trong thư mục real-estate
docker-compose up -d

# Kiểm tra trạng thái
curl http://localhost:9200/_cluster/health
```

### 2. Verify Setup
- Elasticsearch: http://localhost:9200
- Kibana: http://localhost:5601

### 3. Kiểm tra Index Health
```bash
# API health check
curl http://localhost:8080/api/v1/enhanced-search/health

# Xem thống kê index
curl http://localhost:8080/api/v1/enhanced-search/admin/stats
```

## API Endpoints

### 1. Enhanced Search (POST)
**Endpoint**: `POST /api/v1/enhanced-search/listings`

**Request Body**:
```json
{
  "keyword": "căn hộ 2 phòng ngủ",
  "location": "Hà Nội",
  "listingType": "RENT",
  "minPrice": 5000000,
  "maxPrice": 15000000,
  "minArea": 50,
  "maxArea": 100,
  "minBedrooms": 2,
  "page": 0,
  "size": 20,
  "sortBy": "price",
  "sortDirection": "asc",
  "dynamicAttributes": {
    "furniture": "Đầy đủ",
    "view": "Thành phố"
  }
}
```

### 2. Enhanced Search (GET)
**Endpoint**: `GET /api/v1/enhanced-search/listings`

**Parameters**:
```
?keyword=căn hộ cao cấp
&location=Hà Nội
&listingType=RENT
&minPrice=10000000
&maxPrice=20000000
&page=0
&size=20
```

### 3. Geospatial Search
**Endpoint**: `GET /api/v1/enhanced-search/listings/nearby`

**Parameters**:
```
?latitude=21.0285
&longitude=105.8542
&radiusKm=5
&query=căn hộ
&page=0
&size=20
```

**Ví dụ**: Tìm tất cả listings trong bán kính 5km từ tọa độ Hà Nội.

### 4. Similar Listings
**Endpoint**: `GET /api/v1/enhanced-search/listings/{listingId}/similar`

**Parameters**:
```
?size=10
```

**Mô tả**: Tìm các listing tương tự dựa trên giá cả, diện tích và danh mục.

### 5. Autocomplete
**Endpoint**: `GET /api/v1/enhanced-search/autocomplete`

**Parameters**:
```
?query=căn hộ
```

**Response**:
```json
[
  "Căn hộ cao cấp Vinhomes",
  "Căn hộ 2 phòng ngủ Times City",
  "Căn hộ studio gần Big C"
]
```

## Tính năng đặc biệt

### 1. Full-text Search với Fuzzy Matching
- Tìm kiếm "can ho" sẽ match với "căn hộ"
- Tìm kiếm "Vincom" sẽ match với "Vinhomes"
- Hỗ trợ typo và các từ viết tắt

### 2. Geospatial Search
```bash
# Tìm trong bán kính 2km từ Cầu Giấy
GET /api/v1/enhanced-search/listings/nearby?latitude=21.0313&longitude=105.7829&radiusKm=2
```

### 3. Multi-field Search với Boost
- `title^2`: Tiêu đề có trọng số cao nhất
- `fullAddress^1.5`: Địa chỉ có trọng số cao
- `description`: Mô tả có trọng số thấp nhất

### 4. Dynamic Attributes Search
```json
{
  "keyword": "căn hộ",
  "dynamicAttributes": {
    "furniture": "Đầy đủ",
    "view": "Hồ",
    "balcony": "Có"
  }
}
```

## Admin Operations

### 1. Reindex All Listings
```bash
POST /api/v1/enhanced-search/admin/reindex
```

**Khi nào cần reindex**:
- Sau khi thay đổi mapping
- Sau khi import dữ liệu lớn
- Khi có lỗi về consistency

### 2. Index Statistics
```bash
GET /api/v1/enhanced-search/admin/stats
```

**Response**:
```json
{
  "indexedListings": 1250,
  "indexName": "real_estate_listings"
}
```

## Data Synchronization

### Automatic Sync
Hệ thống tự động đồng bộ dữ liệu khi:
- Tạo listing mới
- Cập nhật listing
- Xóa listing
- Thay đổi status

### Manual Sync
```java
// Trong service layer
@Autowired
private ElasticsearchService elasticsearchService;

// Sync một listing cụ thể
elasticsearchService.syncListingAfterChange(listingId);

// Reindex toàn bộ
elasticsearchService.reindexAllListings();
```

## Performance Optimization

### 1. Search Performance
- Sử dụng pagination để giới hạn kết quả
- Cache kết quả search với Redis
- Sử dụng index templates cho mapping

### 2. Indexing Performance
- Bulk indexing cho datasets lớn
- Async indexing để không block main thread
- Optimize mapping cho từng field

### 3. Monitoring với Kibana
- **Dev Tools**: http://localhost:5601/app/dev_tools
- **Index Management**: http://localhost:5601/app/management/data/index_management
- **Search Profiler**: Để debug slow queries

## Elasticsearch Queries Examples

### 1. Basic Full-text Search
```json
GET /real_estate_listings/_search
{
  "query": {
    "multi_match": {
      "query": "căn hộ cao cấp",
      "fields": ["searchText", "title^2", "description"],
      "fuzziness": "AUTO"
    }
  }
}
```

### 2. Geospatial Search
```json
GET /real_estate_listings/_search
{
  "query": {
    "geo_distance": {
      "distance": "5km",
      "location": {
        "lat": 21.0285,
        "lon": 105.8542
      }
    }
  }
}
```

### 3. Range + Text Search
```json
GET /real_estate_listings/_search
{
  "query": {
    "bool": {
      "must": [
        {
          "multi_match": {
            "query": "căn hộ",
            "fields": ["title^2", "description"]
          }
        }
      ],
      "filter": [
        {
          "range": {
            "price": {
              "gte": 5000000,
              "lte": 15000000
            }
          }
        },
        {
          "term": {
            "status": "ACTIVE"
          }
        }
      ]
    }
  }
}
```

## Troubleshooting

### Common Issues

**1. Connection Refused**
```bash
# Kiểm tra Elasticsearch có chạy không
curl http://localhost:9200

# Restart containers
docker-compose restart elasticsearch
```

**2. Index Not Found**
```bash
# Reindex data
POST /api/v1/enhanced-search/admin/reindex
```

**3. Slow Queries**
- Kiểm tra mapping
- Sử dụng filters thay vì queries khi có thể
- Optimize sort operations

**4. Memory Issues**
```bash
# Trong docker-compose.yml
environment:
  - "ES_JAVA_OPTS=-Xms1g -Xmx1g"
```

## Best Practices

### 1. Search Strategy
- Bắt đầu với filters (exact match)
- Sau đó mới đến queries (scoring)
- Sử dụng bool query để combine conditions

### 2. Index Design
- Mapping phù hợp cho từng field type
- Disable indexing cho fields không search
- Sử dụng keyword fields cho exact match

### 3. Performance
- Monitor query performance
- Cache frequent searches
- Use pagination appropriately
- Optimize refresh intervals

## Integration với Frontend

### React/Vue.js Example
```javascript
// Autocomplete search
const autocomplete = async (query) => {
  const response = await fetch(`/api/v1/enhanced-search/autocomplete?query=${query}`);
  return await response.json();
};

// Enhanced search
const search = async (criteria) => {
  const response = await fetch('/api/v1/enhanced-search/listings', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(criteria)
  });
  return await response.json();
};

// Geospatial search
const searchNearby = async (lat, lon, radius) => {
  const response = await fetch(
    `/api/v1/enhanced-search/listings/nearby?latitude=${lat}&longitude=${lon}&radiusKm=${radius}`
  );
  return await response.json();
};
```

Với việc tích hợp Elasticsearch, hệ thống real estate giờ đây có khả năng tìm kiếm mạnh mẽ và linh hoạt, phù hợp với các yêu cầu phức tạp của người dùng cuối! 