-- =====================================================
-- MLS System Database Migration V1.0
-- Creates core MLS tables for multi-tenant real estate platform
-- =====================================================

-- MLS Regions Table
-- Supports multi-tenancy for different MLS territories
CREATE TABLE mls_regions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    region_code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    
    -- Geographic boundaries
    north_boundary DOUBLE PRECISION,
    south_boundary DOUBLE PRECISION,
    east_boundary DOUBLE PRECISION,
    west_boundary DOUBLE PRECISION,
    
    -- MLS Configuration
    listing_prefix VARCHAR(10),
    max_listing_days INTEGER DEFAULT 180,
    requires_approval BOOLEAN DEFAULT false,
    allows_private_remarks BOOLEAN DEFAULT true,
    mandatory_showing_time_hours INTEGER DEFAULT 24,
    
    -- Contact Information
    admin_email VARCHAR(255),
    admin_phone VARCHAR(20),
    website_url VARCHAR(500),
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Offices/Brokerages Table
CREATE TABLE offices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mls_region_id UUID REFERENCES mls_regions(id),
    
    -- Office Information
    office_name VARCHAR(255) NOT NULL,
    license_number VARCHAR(100),
    office_type VARCHAR(50) NOT NULL, -- BROKERAGE, FRANCHISE, INDEPENDENT
    
    -- Contact Information
    phone VARCHAR(20),
    email VARCHAR(255),
    website VARCHAR(500),
    
    -- Address
    street_address VARCHAR(500),
    city VARCHAR(100),
    state VARCHAR(2),
    zip_code VARCHAR(20),
    country VARCHAR(2) DEFAULT 'US',
    
    -- Geographic coordinates
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    
    -- Business Details
    established_date DATE,
    total_agents INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    franchise_name VARCHAR(255),
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- MLS Memberships Table
CREATE TABLE mls_memberships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL, -- References users table
    mls_region_id UUID NOT NULL REFERENCES mls_regions(id),
    office_id UUID REFERENCES offices(id),
    
    -- Membership Details
    membership_type VARCHAR(20) NOT NULL, -- SUPER_ADMIN, MLS_ADMIN, BROKER_ADMIN, BROKER, AGENT, ASSISTANT, READONLY_MEMBER, IDX_CONSUMER
    membership_status VARCHAR(20) NOT NULL, -- PENDING, ACTIVE, SUSPENDED, EXPIRED, TERMINATED, GRACE_PERIOD
    membership_number VARCHAR(50) UNIQUE,
    
    start_date DATE NOT NULL,
    end_date DATE,
    annual_fee DECIMAL(10,2),
    
    -- License Information
    license_number VARCHAR(50),
    license_state VARCHAR(2),
    license_expiry_date DATE,
    
    -- Permissions
    can_list_properties BOOLEAN DEFAULT false,
    can_view_all_listings BOOLEAN DEFAULT false,
    can_access_idx_feeds BOOLEAN DEFAULT false,
    can_manage_office BOOLEAN DEFAULT false,
    can_view_showing_info BOOLEAN DEFAULT false,
    can_view_agent_contact BOOLEAN DEFAULT false,
    can_export_data BOOLEAN DEFAULT false,
    can_bulk_import BOOLEAN DEFAULT false,
    
    -- Administrative permissions
    can_approve_listings BOOLEAN DEFAULT false,
    can_manage_members BOOLEAN DEFAULT false,
    can_configure_rules BOOLEAN DEFAULT false,
    can_access_analytics BOOLEAN DEFAULT false,
    can_manage_commissions BOOLEAN DEFAULT false,
    
    -- Compliance tracking
    training_completed BOOLEAN DEFAULT false,
    training_completion_date DATE,
    last_login TIMESTAMP WITH TIME ZONE,
    agreement_accepted BOOLEAN DEFAULT false,
    agreement_accepted_date TIMESTAMP WITH TIME ZONE,
    
    -- Billing
    billing_address TEXT,
    payment_method VARCHAR(50),
    last_payment_date DATE,
    next_payment_due DATE,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- MLS Rules Table
-- Region-specific business rules and compliance requirements
CREATE TABLE mls_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mls_region_id UUID NOT NULL REFERENCES mls_regions(id),
    
    rule_name VARCHAR(255) NOT NULL,
    rule_type VARCHAR(50) NOT NULL, -- LISTING_VALIDATION, COMMISSION_POLICY, SHOWING_REQUIREMENT, DATA_SHARING
    rule_category VARCHAR(50), -- MANDATORY, OPTIONAL, RECOMMENDED
    
    description TEXT,
    rule_value JSONB, -- Store rule parameters as JSON
    
    is_active BOOLEAN DEFAULT true,
    effective_date DATE,
    expiry_date DATE,
    
    created_by UUID, -- User who created the rule
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Commission Structures Table
CREATE TABLE commission_structures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mls_region_id UUID NOT NULL REFERENCES mls_regions(id),
    
    structure_name VARCHAR(255) NOT NULL,
    structure_type VARCHAR(50) NOT NULL, -- PERCENTAGE, FLAT_FEE, TIERED, CUSTOM
    
    -- Commission rates
    listing_side_percentage DECIMAL(5,2), -- e.g., 3.00 for 3%
    selling_side_percentage DECIMAL(5,2), -- e.g., 3.00 for 3%
    
    flat_fee_amount DECIMAL(10,2),
    minimum_commission DECIMAL(10,2),
    maximum_commission DECIMAL(10,2),
    
    -- Tiered structure (stored as JSON)
    tier_structure JSONB,
    
    -- Policies
    allows_variable_commission BOOLEAN DEFAULT true,
    requires_approval BOOLEAN DEFAULT false,
    
    is_default BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Enhanced Listings Table (extends existing listings)
CREATE TABLE mls_listings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    base_listing_id UUID, -- References existing listings table if extending
    mls_region_id UUID NOT NULL REFERENCES mls_regions(id),
    
    -- MLS-specific identifiers
    mls_number VARCHAR(50) UNIQUE NOT NULL,
    original_list_price DECIMAL(12,2),
    
    -- Agent and Office Information
    listing_agent_id UUID, -- References users table
    listing_office_id UUID REFERENCES offices(id),
    selling_agent_id UUID, -- References users table  
    selling_office_id UUID REFERENCES offices(id),
    
    -- Commission Information
    commission_structure_id UUID REFERENCES commission_structures(id),
    total_commission_percentage DECIMAL(5,2),
    listing_commission_percentage DECIMAL(5,2),
    selling_commission_percentage DECIMAL(5,2),
    
    -- MLS Compliance and Workflow
    mls_submission_date TIMESTAMP WITH TIME ZONE,
    last_mls_update TIMESTAMP WITH TIME ZONE,
    approval_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED, EXPIRED
    approved_by UUID, -- User who approved
    approved_at TIMESTAMP WITH TIME ZONE,
    
    -- Sharing and Cooperation
    is_shared_listing BOOLEAN DEFAULT true,
    sharing_restrictions TEXT,
    cooperation_compensation TEXT,
    
    -- Showing Information
    showing_instructions TEXT,
    lockbox_type VARCHAR(50),
    lockbox_location VARCHAR(255),
    showing_contact_name VARCHAR(255),
    showing_contact_phone VARCHAR(20),
    showing_requirements TEXT,
    
    -- Private Remarks (MLS members only)
    private_remarks TEXT,
    broker_remarks TEXT,
    
    -- Market Information
    days_on_market INTEGER DEFAULT 0,
    cumulative_days_on_market INTEGER DEFAULT 0,
    price_change_timestamp TIMESTAMP WITH TIME ZONE,
    
    -- Transaction Information
    under_contract_date DATE,
    closing_date DATE,
    sold_price DECIMAL(12,2),
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Showing Instructions Table
CREATE TABLE showing_instructions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES mls_listings(id),
    
    instruction_type VARCHAR(50) NOT NULL, -- APPOINTMENT_REQUIRED, CALL_FIRST, LOCKBOX, KEY_AT_OFFICE
    instruction_text TEXT,
    
    -- Contact Information
    contact_name VARCHAR(255),
    contact_phone VARCHAR(20),
    contact_email VARCHAR(255),
    
    -- Timing
    available_days VARCHAR(100), -- JSON array of available days
    available_hours VARCHAR(100), -- JSON object with hours per day
    advance_notice_hours INTEGER DEFAULT 24,
    
    -- Special Requirements
    requires_appointment BOOLEAN DEFAULT false,
    requires_confirmation BOOLEAN DEFAULT false,
    pet_restrictions TEXT,
    other_restrictions TEXT,
    
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Listing Analytics Table
CREATE TABLE listing_analytics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES mls_listings(id),
    
    -- View Analytics
    total_views INTEGER DEFAULT 0,
    agent_views INTEGER DEFAULT 0,
    public_views INTEGER DEFAULT 0,
    
    -- Interest Metrics
    total_inquiries INTEGER DEFAULT 0,
    showing_requests INTEGER DEFAULT 0,
    completed_showings INTEGER DEFAULT 0,
    
    -- Performance Metrics
    price_reductions INTEGER DEFAULT 0,
    last_price_change DECIMAL(12,2),
    average_days_on_market DECIMAL(10,2),
    
    -- Geographic Performance
    search_radius_views JSONB, -- Views by distance radius
    
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Market Statistics Table
CREATE TABLE market_statistics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mls_region_id UUID NOT NULL REFERENCES mls_regions(id),
    
    -- Time Period
    period_type VARCHAR(20) NOT NULL, -- DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    
    -- Geographic Area
    area_type VARCHAR(50), -- CITY, COUNTY, ZIP_CODE, NEIGHBORHOOD
    area_identifier VARCHAR(100),
    
    -- Listing Statistics
    new_listings INTEGER DEFAULT 0,
    closed_sales INTEGER DEFAULT 0,
    pending_sales INTEGER DEFAULT 0,
    expired_listings INTEGER DEFAULT 0,
    
    -- Price Statistics
    median_sale_price DECIMAL(12,2),
    average_sale_price DECIMAL(12,2),
    median_list_price DECIMAL(12,2),
    average_list_price DECIMAL(12,2),
    
    -- Market Metrics
    average_days_on_market DECIMAL(10,2),
    inventory_months DECIMAL(5,2),
    absorption_rate DECIMAL(5,2),
    
    -- Price Trends
    price_per_sqft_median DECIMAL(10,2),
    price_per_sqft_average DECIMAL(10,2),
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Agent Performance Table
CREATE TABLE agent_performance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_id UUID NOT NULL, -- References users table
    mls_region_id UUID NOT NULL REFERENCES mls_regions(id),
    office_id UUID REFERENCES offices(id),
    
    -- Time Period
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    
    -- Listing Performance
    listings_taken INTEGER DEFAULT 0,
    listings_sold INTEGER DEFAULT 0,
    listings_expired INTEGER DEFAULT 0,
    
    -- Sales Performance
    total_sales INTEGER DEFAULT 0,
    total_sales_volume DECIMAL(15,2) DEFAULT 0,
    average_sale_price DECIMAL(12,2),
    
    -- Buyer Performance
    buyer_transactions INTEGER DEFAULT 0,
    buyer_sales_volume DECIMAL(15,2) DEFAULT 0,
    
    -- Performance Metrics
    average_days_to_sell DECIMAL(10,2),
    list_to_sale_ratio DECIMAL(5,2),
    
    -- Commission Earned
    total_commission_earned DECIMAL(12,2),
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- INDEXES for Performance
-- =====================================================

-- MLS Regions
CREATE INDEX idx_mls_regions_region_code ON mls_regions(region_code);
CREATE INDEX idx_mls_regions_active ON mls_regions(is_active);

-- Offices
CREATE INDEX idx_offices_mls_region ON offices(mls_region_id);
CREATE INDEX idx_offices_active ON offices(is_active);
CREATE INDEX idx_offices_location ON offices(latitude, longitude);

-- MLS Memberships
CREATE INDEX idx_mls_memberships_user ON mls_memberships(user_id);
CREATE INDEX idx_mls_memberships_region ON mls_memberships(mls_region_id);
CREATE INDEX idx_mls_memberships_office ON mls_memberships(office_id);
CREATE INDEX idx_mls_memberships_status ON mls_memberships(membership_status);
CREATE INDEX idx_mls_memberships_type ON mls_memberships(membership_type);
CREATE INDEX idx_mls_memberships_number ON mls_memberships(membership_number);

-- MLS Rules
CREATE INDEX idx_mls_rules_region ON mls_rules(mls_region_id);
CREATE INDEX idx_mls_rules_type ON mls_rules(rule_type);
CREATE INDEX idx_mls_rules_active ON mls_rules(is_active);

-- Commission Structures
CREATE INDEX idx_commission_structures_region ON commission_structures(mls_region_id);
CREATE INDEX idx_commission_structures_active ON commission_structures(is_active);
CREATE INDEX idx_commission_structures_default ON commission_structures(is_default);

-- MLS Listings
CREATE INDEX idx_mls_listings_mls_number ON mls_listings(mls_number);
CREATE INDEX idx_mls_listings_region ON mls_listings(mls_region_id);
CREATE INDEX idx_mls_listings_listing_agent ON mls_listings(listing_agent_id);
CREATE INDEX idx_mls_listings_selling_agent ON mls_listings(selling_agent_id);
CREATE INDEX idx_mls_listings_listing_office ON mls_listings(listing_office_id);
CREATE INDEX idx_mls_listings_selling_office ON mls_listings(selling_office_id);
CREATE INDEX idx_mls_listings_status ON mls_listings(approval_status);
CREATE INDEX idx_mls_listings_submission_date ON mls_listings(mls_submission_date);
CREATE INDEX idx_mls_listings_price ON mls_listings(original_list_price);

-- Showing Instructions
CREATE INDEX idx_showing_instructions_listing ON showing_instructions(listing_id);
CREATE INDEX idx_showing_instructions_active ON showing_instructions(is_active);

-- Analytics
CREATE INDEX idx_listing_analytics_listing ON listing_analytics(listing_id);
CREATE INDEX idx_market_statistics_region_period ON market_statistics(mls_region_id, period_start, period_end);
CREATE INDEX idx_agent_performance_agent_period ON agent_performance(agent_id, period_start, period_end);

-- =====================================================
-- CONSTRAINTS
-- =====================================================

-- Ensure membership number is unique within region
ALTER TABLE mls_memberships 
ADD CONSTRAINT uk_membership_number_region UNIQUE (membership_number, mls_region_id);

-- Ensure MLS number is unique within region
ALTER TABLE mls_listings 
ADD CONSTRAINT uk_mls_number_region UNIQUE (mls_number, mls_region_id);

-- Ensure date consistency
ALTER TABLE mls_memberships 
ADD CONSTRAINT chk_membership_dates CHECK (start_date <= COALESCE(end_date, start_date));

ALTER TABLE market_statistics 
ADD CONSTRAINT chk_market_stats_dates CHECK (period_start <= period_end);

ALTER TABLE agent_performance 
ADD CONSTRAINT chk_agent_performance_dates CHECK (period_start <= period_end);

-- =====================================================
-- TRIGGERS for Updated_At
-- =====================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for all tables
CREATE TRIGGER update_mls_regions_updated_at BEFORE UPDATE ON mls_regions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_offices_updated_at BEFORE UPDATE ON offices FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_mls_memberships_updated_at BEFORE UPDATE ON mls_memberships FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_mls_rules_updated_at BEFORE UPDATE ON mls_rules FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_commission_structures_updated_at BEFORE UPDATE ON commission_structures FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_mls_listings_updated_at BEFORE UPDATE ON mls_listings FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_showing_instructions_updated_at BEFORE UPDATE ON showing_instructions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_listing_analytics_updated_at BEFORE UPDATE ON listing_analytics FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_market_statistics_updated_at BEFORE UPDATE ON market_statistics FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_agent_performance_updated_at BEFORE UPDATE ON agent_performance FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- SAMPLE DATA for Testing
-- =====================================================

-- Insert sample MLS regions
INSERT INTO mls_regions (region_code, name, description, listing_prefix, admin_email, admin_phone) VALUES
('seattle_mls', 'Seattle Multiple Listing Service', 'Covers King County and surrounding areas in Washington State', 'SEA', 'admin@seattlemls.com', '+1-206-555-0100'),
('bay_area_mls', 'Bay Area Multiple Listing Service', 'Covers San Francisco, Oakland, and surrounding Bay Area', 'SF', 'admin@bayareamls.com', '+1-415-555-0200'),
('chicago_mls', 'Chicago Multiple Listing Service', 'Covers Chicago metropolitan area and Cook County', 'CHI', 'admin@chicagomls.com', '+1-312-555-0300');

-- Insert sample offices
INSERT INTO offices (mls_region_id, office_name, license_number, office_type, phone, email, street_address, city, state, zip_code) VALUES
((SELECT id FROM mls_regions WHERE region_code = 'seattle_mls'), 'Windermere Real Estate', 'WA-12345', 'BROKERAGE', '+1-206-555-1000', 'info@windermere.com', '5424 Sand Point Way NE', 'Seattle', 'WA', '98105'),
((SELECT id FROM mls_regions WHERE region_code = 'bay_area_mls'), 'Coldwell Banker Bay Area', 'CA-67890', 'FRANCHISE', '+1-415-555-2000', 'info@coldwellbanker.com', '2000 Powell St', 'San Francisco', 'CA', '94133'),
((SELECT id FROM mls_regions WHERE region_code = 'chicago_mls'), 'RE/MAX Chicago', 'IL-54321', 'FRANCHISE', '+1-312-555-3000', 'info@remax.com', '123 N Wacker Dr', 'Chicago', 'IL', '60606');

-- Insert sample commission structures
INSERT INTO commission_structures (mls_region_id, structure_name, structure_type, listing_side_percentage, selling_side_percentage, is_default) VALUES
((SELECT id FROM mls_regions WHERE region_code = 'seattle_mls'), 'Standard 6% Split', 'PERCENTAGE', 3.00, 3.00, true),
((SELECT id FROM mls_regions WHERE region_code = 'bay_area_mls'), 'Premium 5% Split', 'PERCENTAGE', 2.50, 2.50, true),
((SELECT id FROM mls_regions WHERE region_code = 'chicago_mls'), 'Competitive 5.5% Split', 'PERCENTAGE', 2.75, 2.75, true);

-- =====================================================
-- COMMENTS for Documentation
-- =====================================================

COMMENT ON TABLE mls_regions IS 'Geographic MLS territories with regional rules and boundaries';
COMMENT ON TABLE offices IS 'Real estate offices/brokerages within MLS regions';
COMMENT ON TABLE mls_memberships IS 'Agent and broker memberships with permissions and status';
COMMENT ON TABLE mls_rules IS 'Region-specific business rules and compliance requirements';
COMMENT ON TABLE commission_structures IS 'Commission rate structures for different MLS regions';
COMMENT ON TABLE mls_listings IS 'Enhanced listings with MLS-specific features and compliance';
COMMENT ON TABLE showing_instructions IS 'Property showing requirements and contact information';
COMMENT ON TABLE listing_analytics IS 'Performance analytics for individual listings';
COMMENT ON TABLE market_statistics IS 'Aggregated market data by region and time period';
COMMENT ON TABLE agent_performance IS 'Individual agent performance metrics and KPIs'; 