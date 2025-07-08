-- =================================================================
-- Solid Core Tables
-- =================================================================

-- Users Table (Simplified for this context)
CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(20),
    hashed_password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Location Tables (Normalized for Vietnamese administrative units)
CREATE TABLE cities (
    id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);
CREATE TABLE districts (
    id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    city_id INT NOT NULL REFERENCES cities(id)
);
CREATE TABLE wards (
    id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    district_id INT NOT NULL REFERENCES districts(id)
);

-- Addresses Table
CREATE TABLE addresses (
    id UUID PRIMARY KEY,
    street_number VARCHAR(50),
    street VARCHAR(255),
    ward_id INT NOT NULL REFERENCES wards(id),
    district_id INT NOT NULL REFERENCES districts(id),
    city_id INT NOT NULL REFERENCES cities(id),
    full_address TEXT, -- To store the complete address for quick display
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8)
);

-- Projects Table
CREATE TABLE projects (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address_id UUID REFERENCES addresses(id),
    developer VARCHAR(255),  -- Developer's name
    description TEXT,
    launch_date DATE,
    status VARCHAR(50)      -- Enum in Java: 'PRE_SALE', 'SELLING', 'SOLD_OUT', 'COMPLETED'
);

-- Listing Categories Table
CREATE TABLE listing_categories (
    id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL, -- E.g., "Apartment", "Townhouse", "Villa", "Land Plot"
    parent_id INT REFERENCES listing_categories(id) -- Supports multi-level categories
);

-- Central Listings Table
CREATE TABLE listings (
    id UUID PRIMARY KEY,
    listing_code VARCHAR(20) UNIQUE NOT NULL,          -- User-friendly unique ID (e.g., OHS-112233)

    -- Foreign Keys to other core tables
    user_id UUID NOT NULL REFERENCES users(id),
    project_id UUID REFERENCES projects(id),           -- Can be NULL for individual properties
    category_id INT NOT NULL REFERENCES listing_categories(id),
    address_id UUID NOT NULL REFERENCES addresses(id),

    -- Core Listing Details
    title VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(18, 2) NOT NULL,                     -- Use DECIMAL for currency to avoid floating-point errors
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    area DECIMAL(10, 2) NOT NULL,                      -- Area in square meters

    -- Common numerical attributes
    num_bedrooms INT DEFAULT 0,
    num_bathrooms INT DEFAULT 0,
    floor INT,
    total_floors INT,

    -- Status and Type
    listing_type VARCHAR(50) NOT NULL,                 -- Enum in Java: 'SALE', 'RENT'
    status VARCHAR(50) NOT NULL,                       -- Enum in Java: 'DRAFT', 'PENDING_APPROVAL', 'ACTIVE', 'ARCHIVED', 'SOLD', 'RENTED'

    -- Timestamps
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Media Table (for images, videos, 360 tours)
CREATE TABLE media (
    id UUID PRIMARY KEY,
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    url VARCHAR(512) NOT NULL,
    media_type VARCHAR(50) NOT NULL, -- Enum in Java: 'IMAGE', 'VIDEO_URL', 'VIRTUAL_TOUR_360'
    caption TEXT,
    sort_order INT DEFAULT 0,        -- To control display order
    is_thumbnail BOOLEAN DEFAULT FALSE
);


-- =================================================================
-- Flexible Shell Tables (EAV-like model for custom attributes)
-- =================================================================

-- Attribute Definitions Table
-- This is where you define custom fields without altering the schema.
CREATE TABLE attribute_definitions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(100) UNIQUE NOT NULL, -- Identifier, e.g., 'legal_status', 'balcony_direction', 'view_type'
    name VARCHAR(255) NOT NULL,        -- Display name, e.g., "Legal Status", "Balcony Direction", "View Type"
    data_type VARCHAR(50) NOT NULL,    -- Enum in Java: 'TEXT', 'NUMBER', 'BOOLEAN', 'SELECT_SINGLE', 'SELECT_MULTI'
    is_filterable BOOLEAN DEFAULT FALSE, -- Can this attribute be used in search filters?
    is_required BOOLEAN DEFAULT FALSE
);

-- Attribute Options Table
-- This stores the available options for 'SELECT' type attributes.
CREATE TABLE attribute_options (
    id INT PRIMARY KEY AUTO_INCREMENT,
    attribute_id INT NOT NULL REFERENCES attribute_definitions(id) ON DELETE CASCADE,
    value VARCHAR(255) NOT NULL, -- E.g., "Freehold (Sổ hồng)", "Sale-Purchase Agreement", "Southeast", "Pool View"
    sort_order INT DEFAULT 0
);

-- Listing Attributes Table
-- This is the join table that stores the actual values of custom attributes for each listing.
CREATE TABLE listing_attributes (
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    attribute_id INT NOT NULL REFERENCES attribute_definitions(id) ON DELETE CASCADE,
    -- The value is stored here. It could be free text or the ID from the attribute_options table.
    value TEXT NOT NULL,
    PRIMARY KEY (listing_id, attribute_id)
);


-- =================================================================
-- Indexes for Performance
-- =================================================================

CREATE INDEX idx_listings_user_id ON listings(user_id);
CREATE INDEX idx_listings_project_id ON listings(project_id);
CREATE INDEX idx_listings_status ON listings(status);
CREATE INDEX idx_listings_price ON listings(price);
CREATE INDEX idx_listings_area ON listings(area);

CREATE INDEX idx_addresses_city_id ON addresses(city_id);
CREATE INDEX idx_addresses_district_id ON addresses(district_id);

CREATE INDEX idx_media_listing_id ON media(listing_id);

CREATE INDEX idx_listing_attributes_listing_id ON listing_attributes(listing_id);
CREATE INDEX idx_listing_attributes_attribute_id ON listing_attributes(attribute_id); 