# API Tìm Kiếm Real Estate

## Tổng quan
API tìm kiếm cung cấp khả năng tìm kiếm các listing bất động sản với nhiều tiêu chí khác nhau, bao gồm cả các thuộc tính cố định và thuộc tính động.

## Endpoints

### 1. Tìm kiếm cơ bản (GET)
```
GET /api/v1/listings/search
```

**Tham số Query:**
- `keyword` (string, optional): Từ khóa tìm kiếm trong tiêu đề và mô tả
- `location` (string, optional): Địa điểm tìm kiếm
- `listingType` (string, optional): Loại listing (RENT, SALE, etc.)
- `status` (string, optional): Trạng thái (ACTIVE, INACTIVE, etc.)
- `categoryId` (string, optional): ID danh mục
- `minPrice` (string, optional): Giá tối thiểu
- `maxPrice` (string, optional): Giá tối đa
- `minArea` (string, optional): Diện tích tối thiểu
- `maxArea` (string, optional): Diện tích tối đa
- `minBedrooms` (integer, optional): Số phòng ngủ tối thiểu
- `maxBedrooms` (integer, optional): Số phòng ngủ tối đa
- `minBathrooms` (integer, optional): Số phòng tắm tối thiểu
- `maxBathrooms` (integer, optional): Số phòng tắm tối đa
- `sortBy` (string, optional): Sắp xếp theo (price, area, created_at, etc.)
- `sortDirection` (string, optional): Hướng sắp xếp (ASC, DESC)
- `page` (integer, optional, default=0): Trang hiện tại
- `size` (integer, optional, default=20): Kích thước trang

**Ví dụ request:**
```
GET /api/v1/listings/search?keyword=căn hộ&location=Hà Nội&minPrice=2000000000&maxPrice=5000000000&minBedrooms=2&sortBy=price&sortDirection=ASC&page=0&size=10
```

### 2. Tìm kiếm nâng cao (POST)
```
POST /api/v1/listings/search
```

**Request Body:**
```json
{
  "keyword": "căn hộ cao cấp",
  "location": "Quận 1, TP.HCM",
  "listingType": "SALE",
  "status": "ACTIVE",
  "categoryId": "123e4567-e89b-12d3-a456-426614174000",
  "minPrice": 3000000000,
  "maxPrice": 8000000000,
  "minArea": 50.0,
  "maxArea": 120.0,
  "minBedrooms": 2,
  "maxBedrooms": 4,
  "minBathrooms": 2,
  "maxBathrooms": 3,
  "dynamicAttributes": {
    "furniture": "fully_furnished",
    "view": "city_view",
    "floor": "high_floor"
  },
  "sortBy": "price",
  "sortDirection": "DESC",
  "page": 0,
  "size": 20
}
```

## Response Format

```json
{
  "listings": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "listingCode": "RE001",
      "title": "Căn hộ cao cấp 3PN tại Vinhomes Central Park",
      "description": "Căn hộ đẹp, view sông, đầy đủ nội thất...",
      "price": "4.5 tỷ VNĐ",
      "area": "85.0 m²",
      "numBedrooms": 3,
      "numBathrooms": 2,
      "listingType": "SALE",
      "status": "ACTIVE",
      "fullAddress": "208 Nguyễn Hữu Cảnh, Quận Bình Thạnh, TP.HCM",
      "categoryName": "Căn hộ chung cư",
      "createdAt": "2024-01-15 10:30:00",
      "updatedAt": "2024-01-20 14:45:00",
      "thumbnailUrl": null,
      "pricePerSqm": "52.9 triệu VNĐ/m²"
    }
  ],
  "metadata": {
    "totalElements": 150,
    "totalPages": 8,
    "currentPage": 0,
    "pageSize": 20,
    "hasNext": true,
    "hasPrevious": false,
    "sortBy": "price",
    "sortDirection": "DESC",
    "searchTimeMs": 45
  }
}
```

## Các trường hợp sử dụng phổ biến

### 1. Tìm kiếm theo từ khóa
```
GET /api/v1/listings/search?keyword=căn hộ
```

### 2. Tìm kiếm theo khoảng giá
```
GET /api/v1/listings/search?minPrice=2000000000&maxPrice=5000000000
```

### 3. Tìm kiếm theo địa điểm
```
GET /api/v1/listings/search?location=Quận 1
```

### 4. Tìm kiếm căn hộ cho thuê
```
GET /api/v1/listings/search?listingType=RENT&minBedrooms=2&maxBedrooms=3
```

### 5. Sắp xếp theo giá tăng dần
```
GET /api/v1/listings/search?sortBy=price&sortDirection=ASC
```

### 6. Tìm kiếm với thuộc tính động
```json
POST /api/v1/listings/search
{
  "keyword": "căn hộ",
  "dynamicAttributes": {
    "furniture": "fully_furnished",
    "parking": "available",
    "pet_allowed": "yes"
  }
}
```

## Thông tin bổ sung

### Các trường sắp xếp hỗ trợ:
- `price`: Sắp xếp theo giá
- `area`: Sắp xếp theo diện tích
- `created_at`: Sắp xếp theo ngày tạo
- `updated_at`: Sắp xếp theo ngày cập nhật
- `title`: Sắp xếp theo tiêu đề
- `num_bedrooms`: Sắp xếp theo số phòng ngủ
- `num_bathrooms`: Sắp xếp theo số phòng tắm

### Giới hạn:
- Kích thước trang tối đa: 100
- Kích thước trang mặc định: 20
- Timeout: 30 giây

### Format giá:
- Đơn vị: VNĐ
- Tự động format hiển thị (triệu/tỷ)
- Tính toán giá/m² tự động

### Performance:
- Tìm kiếm cơ bản: < 100ms
- Tìm kiếm với thuộc tính động: < 300ms
- Index được tối ưu cho các trường thường tìm kiếm

### Error Handling:
```json
{
  "error": "INVALID_PARAMETER",
  "message": "Invalid price range",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Tips tối ưu hiệu suất:
1. Sử dụng pagination hợp lý
2. Giới hạn số thuộc tính động trong một query
3. Sử dụng cache cho các tìm kiếm phổ biến
4. Tránh sử dụng wildcards ở đầu từ khóa 