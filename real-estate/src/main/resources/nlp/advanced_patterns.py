#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Advanced Real Estate NLP Patterns
Extended patterns and rules for better entity extraction
"""

import re
import json
from typing import Dict, List, Any, Optional, Tuple

class AdvancedRealEstatePatterns:
    """Advanced patterns for real estate entity extraction"""
    
    def __init__(self):
        """Initialize advanced patterns and rules"""
        
        # Advanced property type patterns with context
        self.advanced_property_patterns = {
            # Căn hộ variations
            r'(?:căn\s+hộ|chung\s+cư|condo|apartment)(?:\s+(?:cao\s+cấp|sang\s+trọng|luxury))?': 'APARTMENT',
            r'(?:căn\s+hộ|chung\s+cư)\s+(?:dịch\s+vụ|serviced)': 'SERVICED_APARTMENT',
            r'(?:căn\s+hộ|chung\s+cư)\s+(?:penthouse|duplex)': 'PENTHOUSE',
            r'condotel|khách\s+sạn\s+căn\s+hộ': 'CONDOTEL',
            
            # Nhà riêng variations  
            r'(?:nhà\s+riêng|nhà\s+phố|town\s*house)(?:\s+(?:\d+\s*tầng|\d+\s*lầu))?': 'TOWNHOUSE',
            r'nhà\s+(?:mặt\s+tiền|mặt\s+phố|MT)': 'SHOPHOUSE',
            r'biệt\s+thự|villa|bungalow': 'VILLA',
            
            # Commercial
            r'(?:shop\s*house|nhà\s+mặt\s+tiền)': 'SHOPHOUSE',
            r'văn\s+phòng|office|tòa\s+nhà\s+văn\s+phòng': 'OFFICE',
            r'kho\s+(?:bãi|xưởng)|warehouse|factory': 'WAREHOUSE',
            r'đất\s+(?:nền|thổ\s+cư|ở)|land|plot': 'LAND',
            
            # Special types
            r'studio|căn\s+hộ\s+studio': 'STUDIO',
            r'loft|căn\s+hộ\s+loft': 'LOFT',
            r'mini\s+(?:house|home)|nhà\s+mini': 'MINI_HOUSE'
        }
        
        # Advanced location patterns with districts/wards
        self.location_patterns = [
            # Quận patterns
            r'(?:quận|q\.?|district)\s+(\d+|[a-zA-ZÀ-ỹ\s]+(?:quận)?)',
            r'(?:huyện|h\.?)\s+([a-zA-ZÀ-ỹ\s]+)',
            
            # Phường/Xã patterns  
            r'(?:phường|p\.?|ward)\s+(\d+|[a-zA-ZÀ-ỹ\s]+)',
            r'(?:xã|x\.?)\s+([a-zA-ZÀ-ỹ\s]+)',
            
            # Thành phố patterns
            r'(?:thành\s+phố|tp\.?|city)\s+([a-zA-ZÀ-ỹ\s]+)',
            
            # Special area patterns
            r'(?:khu\s+vực|kv\.?|area)\s+([a-zA-ZÀ-ỹ\s]+)',
            r'(?:gần|near|close\s+to)\s+([a-zA-ZÀ-ỹ\s\d]+)',
            r'(?:ở|tại|at|in)\s+([a-zA-ZÀ-ỹ\s\d]+(?:quận|phường|huyện|xã)?)',
            
            # Landmark patterns
            r'(?:gần|cách)\s+(?:sân\s+bay|airport)\s+([a-zA-ZÀ-ỹ\s]+)',
            r'(?:gần|cách)\s+(?:bệnh\s+viện|hospital)\s+([a-zA-ZÀ-ỹ\s]+)',
            r'(?:gần|cách)\s+(?:trường|school)\s+([a-zA-ZÀ-ỹ\s]+)',
            r'(?:gần|cách)\s+(?:chợ|market)\s+([a-zA-ZÀ-ỹ\s]+)',
            r'(?:gần|cách)\s+(?:trung\s+tâm|center|downtown)',
        ]
        
        # Advanced price patterns with flexible formats
        self.price_patterns = [
            # Standard format: "5 tỷ", "2.5 tỷ", "500 triệu"
            r'(?:giá\s+)?(?:từ\s+)?(\d+(?:[,\.]\d+)*)\s*(?:đến\s+(\d+(?:[,\.]\d+)*)\s*)?(tỷ|ty|billion|triệu|million|nghìn|thousand|k)(?:\s+(?:đồng|vnd|dong))?',
            
            # Range format: "từ 3 đến 5 tỷ", "3-5 tỷ"  
            r'(?:từ\s+)?(\d+(?:[,\.]\d+)*)\s*(?:đến|-)\s*(\d+(?:[,\.]\d+)*)\s*(tỷ|ty|billion|triệu|million|nghìn|thousand|k)',
            
            # Comparison format: "dưới 5 tỷ", "trên 3 tỷ", "không quá 10 tỷ"
            r'(dưới|under|below|không\s+quá|max|tối\s+đa)\s+(\d+(?:[,\.]\d+)*)\s*(tỷ|ty|billion|triệu|million|nghìn|thousand|k)',
            r'(trên|above|over|từ|min|tối\s+thiểu)\s+(\d+(?:[,\.]\d+)*)\s*(tỷ|ty|billion|triệu|million|nghìn|thousand|k)',
            
            # Negotiable: "giá thỏa thuận", "TT"
            r'(giá\s+(?:thỏa\s+thuận|thoả\s+thuận|tt)|negotiable|thương\s+lượng)',
            
            # Monthly rental: "5 triệu/tháng", "10tr/th"
            r'(\d+(?:[,\.]\d+)*)\s*(triệu|tr|million|k)\s*(?:\/|\s*per\s*)(?:tháng|th|month)',
        ]
        
        # Advanced area patterns
        self.area_patterns = [
            # Standard: "80 m2", "100 mét vuông"
            r'(?:diện\s+tích\s+)?(\d+(?:[,\.]\d+)*)\s*(?:đến\s+(\d+(?:[,\.]\d+)*)\s*)?(m2|m²|mét\s+vuông|met\s+vuong|sqm)',
            
            # Large areas: "1 hecta", "5000 m2"
            r'(\d+(?:[,\.]\d+)*)\s*(ha|hecta|hectare|acre)',
            
            # Flexible formats: "diện tích 80m2", "dt: 100m2"
            r'(?:diện\s+tích|dt)[:\s]*(\d+(?:[,\.]\d+)*)\s*(m2|m²|mét\s+vuông)',
            
            # Comparison: "trên 100m2", "dưới 50m2"
            r'(trên|over|above|dưới|under|below)\s+(\d+(?:[,\.]\d+)*)\s*(m2|m²|mét\s+vuông|sqm)',
        ]
        
        # Room count patterns (bedrooms, bathrooms)
        self.room_patterns = {
            'bedrooms': [
                r'(\d+)\s*(?:phòng\s+ngủ|pn|phòng|bedroom|bed|br)(?:\s|$)',
                r'(\d+)\s*pn(?:\s|$)',
                r'(\d+)\s*br(?:\s|$)',
                r'(\d+)\s*(?:phòng|room)(?:\s+ngủ)?(?:\s|$)',
            ],
            'bathrooms': [
                r'(\d+)\s*(?:phòng\s+tắm|toilet|wc|bathroom|bath)(?:\s|$)',
                r'(\d+)\s*wc(?:\s|$)',
                r'(\d+)\s*toilet(?:\s|$)',
            ]
        }
        
        # Advanced amenities with context
        self.amenities_patterns = {
            # Location-based amenities
            'education': [
                r'gần\s+(?:trường\s+)?(?:học|school|university|đại\s+học|cao\s+đẳng)',
                r'khu\s+vực\s+(?:giáo\s+dục|educational)',
                r'(?:quốc\s+tế|international)\s+school',
            ],
            'healthcare': [
                r'gần\s+(?:bệnh\s+viện|hospital|phòng\s+khám|clinic)',
                r'khu\s+y\s+tế',
            ],
            'shopping': [
                r'gần\s+(?:chợ|market|siêu\s+thị|supermarket|shopping\s+(?:mall|center)|trung\s+tâm\s+thương\s+mại)',
                r'khu\s+mua\s+sắm',
            ],
            'transportation': [
                r'gần\s+(?:bến\s+xe|bus\s+station|metro|mrt|tàu\s+điện)',
                r'thuận\s+tiện\s+(?:giao\s+thông|đi\s+lại)',
                r'gần\s+sân\s+bay',
            ],
            'recreation': [
                r'gần\s+(?:công\s+viên|park|hồ|lake|biển|beach)',
                r'khu\s+vui\s+chơi\s+giải\s+trí',
            ],
            
            # Property amenities
            'luxury': [
                r'(?:hồ\s+bơi|swimming\s+pool|pool)',
                r'(?:gym|phòng\s+tập|fitness)',
                r'(?:spa|sauna)',
                r'(?:sân\s+tennis|tennis\s+court)',
                r'(?:sân\s+golf|golf\s+course)',
            ],
            'convenience': [
                r'(?:thang\s+máy|elevator|lift)',
                r'(?:máy\s+lạnh|air\s+conditioning|ac)',
                r'(?:tủ\s+lạnh|refrigerator|fridge)',
                r'(?:máy\s+giặt|washing\s+machine)',
                r'(?:tivi|tv|television)',
                r'(?:wifi|internet)',
            ],
            'security': [
                r'(?:bảo\s+vệ|security|guard)\s*(?:24/7|24\s*giờ)?',
                r'(?:an\s+ninh|camera|cctv)',
                r'(?:cổng\s+từ|card\s+access)',
            ],
            'parking': [
                r'(?:chỗ\s+đậu\s+xe|parking|bãi\s+đỗ\s+xe)',
                r'(?:garage|gara|nhà\s+để\s+xe)',
                r'(?:hầm\s+xe|underground\s+parking)',
            ],
            'view': [
                r'(?:view\s+)?(?:sông|river|hồ|lake)',
                r'(?:view\s+)?(?:biển|ocean|sea)',
                r'(?:view\s+)?(?:thành\s+phố|city|cityview)',
                r'(?:view\s+)?(?:công\s+viên|park)',
                r'(?:tầng\s+cao|high\s+floor)',
                r'(?:penthouse|tầng\s+thượng)',
            ],
            'outdoor': [
                r'(?:ban\s+công|balcony)',
                r'(?:sân\s+vườn|garden|yard)',
                r'(?:sân\s+thượng|rooftop|terrace)',
                r'(?:sân\s+riêng|private\s+yard)',
            ]
        }
        
        # Transaction intent patterns
        self.intent_patterns = {
            'buy': [
                r'(?:mua|buy|purchase|sở\s+hữu|acquire)',
                r'(?:cần\s+mua|tìm\s+mua|muốn\s+mua)',
                r'(?:đầu\s+tư|investment|invest)',
            ],
            'rent': [
                r'(?:thuê|rent|rental|cho\s+thuê)',
                r'(?:cần\s+thuê|tìm\s+thuê|muốn\s+thuê)',
                r'(?:lease|leasing)',
            ],
            'sell': [
                r'(?:bán|sell|selling)',
                r'(?:cần\s+bán|muốn\s+bán)',
                r'(?:chuyển\s+nhượng|transfer)',
            ],
            'invest': [
                r'(?:đầu\s+tư|investment|invest)',
                r'(?:sinh\s+lời|profitable|return)',
                r'(?:cho\s+thuê\s+lại|sublease)',
            ]
        }
        
        # Urgency and timeline patterns
        self.urgency_patterns = {
            'urgent': [
                r'(?:gấp|urgent|asap|ngay|immediately)',
                r'(?:cần\s+gấp|very\s+urgent)',
            ],
            'timeline': [
                r'(?:trong\s+)?(\d+)\s*(?:ngày|day|tháng|month|tuần|week)',
                r'(?:cuối\s+tháng|end\s+of\s+month)',
                r'(?:đầu\s+tháng|beginning\s+of\s+month)',
            ]
        }
        
    def extract_advanced_entities(self, text: str) -> Dict[str, Any]:
        """Extract entities using advanced patterns"""
        
        entities = {}
        
        # Extract with advanced property type patterns
        entities['property_type'] = self._extract_advanced_property_type(text)
        
        # Extract with advanced location patterns
        entities['location'] = self._extract_advanced_location(text)
        
        # Extract with advanced price patterns
        entities['price_info'] = self._extract_advanced_price(text)
        
        # Extract with advanced area patterns
        entities['area_info'] = self._extract_advanced_area(text)
        
        # Extract room counts
        entities['rooms'] = self._extract_room_counts(text)
        
        # Extract amenities by category
        entities['amenities'] = self._extract_categorized_amenities(text)
        
        # Extract transaction intent
        entities['intent'] = self._extract_transaction_intent(text)
        
        # Extract urgency and timeline
        entities['urgency'] = self._extract_urgency(text)
        
        return entities
    
    def _extract_advanced_property_type(self, text: str) -> Optional[str]:
        """Extract property type using advanced patterns"""
        for pattern, prop_type in self.advanced_property_patterns.items():
            if re.search(pattern, text, re.IGNORECASE):
                return prop_type
        return None
        
    def _extract_advanced_location(self, text: str) -> Dict[str, Any]:
        """Extract location with hierarchical structure"""
        location_info = {
            'districts': [],
            'wards': [],
            'cities': [],
            'areas': [],
            'landmarks': []
        }
        
        for pattern in self.location_patterns:
            matches = re.findall(pattern, text, re.IGNORECASE)
            for match in matches:
                if 'quận' in pattern or 'district' in pattern:
                    location_info['districts'].append(match.strip())
                elif 'phường' in pattern or 'ward' in pattern:
                    location_info['wards'].append(match.strip())
                elif 'thành phố' in pattern or 'city' in pattern:
                    location_info['cities'].append(match.strip())
                elif 'khu vực' in pattern or 'area' in pattern:
                    location_info['areas'].append(match.strip())
                else:
                    location_info['landmarks'].append(match.strip())
        
        # Remove empty lists
        return {k: v for k, v in location_info.items() if v}
    
    def _extract_advanced_price(self, text: str) -> Dict[str, Any]:
        """Extract price with comprehensive pattern matching"""
        price_info = {}
        
        for pattern in self.price_patterns:
            matches = re.findall(pattern, text, re.IGNORECASE)
            for match in matches:
                if isinstance(match, tuple) and len(match) >= 3:
                    # Handle structured price patterns
                    if match[0] in ['giá thỏa thuận', 'tt', 'negotiable']:
                        price_info['negotiable'] = True
                    elif match[0] in ['dưới', 'under', 'below', 'không quá', 'max']:
                        price_info['max_price'] = self._convert_price_to_vnd(match[1], match[2])
                        price_info['operator'] = 'lte'
                    elif match[0] in ['trên', 'above', 'over', 'từ', 'min']:
                        price_info['min_price'] = self._convert_price_to_vnd(match[1], match[2])
                        price_info['operator'] = 'gte'
                    else:
                        # Range or exact price
                        min_val = self._convert_price_to_vnd(match[0], match[2])
                        if match[1]:  # Range
                            max_val = self._convert_price_to_vnd(match[1], match[2])
                            price_info['min_price'] = min_val
                            price_info['max_price'] = max_val
                            price_info['operator'] = 'range'
                        else:  # Exact
                            price_info['exact_price'] = min_val
                            price_info['operator'] = 'eq'
        
        return price_info
    
    def _extract_advanced_area(self, text: str) -> Dict[str, Any]:
        """Extract area with comprehensive pattern matching"""
        area_info = {}
        
        for pattern in self.area_patterns:
            matches = re.findall(pattern, text, re.IGNORECASE)
            for match in matches:
                if isinstance(match, tuple) and len(match) >= 2:
                    if match[0] in ['trên', 'over', 'above']:
                        area_info['min_area'] = self._convert_area_to_sqm(match[1], match[2])
                        area_info['operator'] = 'gte'
                    elif match[0] in ['dưới', 'under', 'below']:
                        area_info['max_area'] = self._convert_area_to_sqm(match[1], match[2])
                        area_info['operator'] = 'lte'
                    else:
                        # Range or exact area
                        min_val = self._convert_area_to_sqm(match[0], match[1])
                        if len(match) > 2 and match[1]:  # Range
                            max_val = self._convert_area_to_sqm(match[1], match[2])
                            area_info['min_area'] = min_val
                            area_info['max_area'] = max_val
                            area_info['operator'] = 'range'
                        else:  # Exact
                            area_info['exact_area'] = min_val
                            area_info['operator'] = 'eq'
        
        return area_info
    
    def _extract_room_counts(self, text: str) -> Dict[str, int]:
        """Extract bedroom and bathroom counts"""
        rooms = {}
        
        for room_type, patterns in self.room_patterns.items():
            for pattern in patterns:
                match = re.search(pattern, text, re.IGNORECASE)
                if match:
                    try:
                        rooms[room_type] = int(match.group(1))
                        break  # Take first match for each room type
                    except (ValueError, IndexError):
                        continue
        
        return rooms
    
    def _extract_categorized_amenities(self, text: str) -> Dict[str, List[str]]:
        """Extract amenities grouped by categories"""
        amenities = {}
        
        for category, patterns in self.amenities_patterns.items():
            found_amenities = []
            for pattern in patterns:
                if re.search(pattern, text, re.IGNORECASE):
                    # Extract the specific amenity mentioned
                    match = re.search(pattern, text, re.IGNORECASE)
                    if match:
                        found_amenities.append(match.group(0))
            
            if found_amenities:
                amenities[category] = found_amenities
        
        return amenities
    
    def _extract_transaction_intent(self, text: str) -> Optional[str]:
        """Extract transaction intent"""
        for intent, patterns in self.intent_patterns.items():
            for pattern in patterns:
                if re.search(pattern, text, re.IGNORECASE):
                    return intent
        return None
    
    def _extract_urgency(self, text: str) -> Dict[str, Any]:
        """Extract urgency and timeline information"""
        urgency_info = {}
        
        # Check for urgency indicators
        for urgency_type, patterns in self.urgency_patterns.items():
            for pattern in patterns:
                match = re.search(pattern, text, re.IGNORECASE)
                if match:
                    if urgency_type == 'urgent':
                        urgency_info['is_urgent'] = True
                    elif urgency_type == 'timeline':
                        urgency_info['timeline'] = match.group(0)
                        if '\\d+' in pattern:
                            urgency_info['timeline_value'] = int(match.group(1))
        
        return urgency_info
    
    def _convert_price_to_vnd(self, amount_str: str, unit: str) -> int:
        """Convert price string to VND amount"""
        try:
            amount = float(amount_str.replace(',', '.'))
            unit_multipliers = {
                'tỷ': 1_000_000_000, 'ty': 1_000_000_000, 'billion': 1_000_000_000,
                'triệu': 1_000_000, 'million': 1_000_000,
                'nghìn': 1_000, 'nghin': 1_000, 'thousand': 1_000, 'k': 1_000
            }
            multiplier = unit_multipliers.get(unit.lower(), 1)
            return int(amount * multiplier)
        except:
            return 0
    
    def _convert_area_to_sqm(self, amount_str: str, unit: str) -> float:
        """Convert area string to square meters"""
        try:
            amount = float(amount_str.replace(',', '.'))
            unit_multipliers = {
                'm2': 1, 'm²': 1, 'mét vuông': 1, 'met vuong': 1, 'sqm': 1,
                'ha': 10_000, 'hecta': 10_000, 'hectare': 10_000, 'acre': 4_047
            }
            multiplier = unit_multipliers.get(unit.lower().replace(' ', ''), 1)
            return amount * multiplier
        except:
            return 0.0

def main():
    """Test the advanced patterns"""
    if len(sys.argv) != 2:
        print("Usage: python advanced_patterns.py '<query_text>'")
        return
    
    query_text = sys.argv[1]
    patterns = AdvancedRealEstatePatterns()
    result = patterns.extract_advanced_entities(query_text)
    
    print(json.dumps(result, ensure_ascii=False, indent=2))

if __name__ == "__main__":
    import sys
    main() 