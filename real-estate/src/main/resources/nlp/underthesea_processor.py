#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
UndertheSea NLP Processor for Real Estate Search
Processes Vietnamese natural language queries for property search
"""

import sys
import json
import re
from typing import Dict, List, Any, Optional, Tuple

try:
    import underthesea
except ImportError:
    print("ERROR: underthesea not installed. Please run: pip install underthesea")
    sys.exit(1)

class RealEstateNLPProcessor:
    """Vietnamese NLP processor specifically designed for real estate queries"""
    
    def __init__(self):
        """Initialize the processor with real estate specific dictionaries"""
        
        # Property types mapping
        self.property_types = {
            "căn hộ": "APARTMENT",
            "chung cư": "APARTMENT", 
            "condo": "APARTMENT",
            "condotel": "CONDOTEL",
            "nhà riêng": "HOUSE",
            "nhà phố": "TOWNHOUSE",
            "biệt thự": "VILLA",
            "villa": "VILLA",
            "shophouse": "SHOPHOUSE",
            "nhà mặt tiền": "SHOPHOUSE",
            "đất nền": "LAND",
            "đất": "LAND",
            "kho": "WAREHOUSE",
            "văn phòng": "OFFICE",
            "studio": "STUDIO"
        }
        
        # Location indicators
        self.location_indicators = [
            "ở", "tại", "khu vực", "quận", "huyện", "phường", "xã", 
            "thành phố", "tp", "q.", "p.", "kv", "gần", "cách"
        ]
        
        # Price unit conversion (to VND)
        self.price_units = {
            "tỷ": 1_000_000_000,
            "ty": 1_000_000_000,
            "billion": 1_000_000_000,
            "triệu": 1_000_000,
            "million": 1_000_000,
            "nghìn": 1_000,
            "nghin": 1_000,
            "thousand": 1_000,
            "k": 1_000
        }
        
        # Area units (to square meters)
        self.area_units = {
            "m2": 1,
            "m²": 1,
            "mét vuông": 1,
            "met vuong": 1,
            "sqm": 1,
            "ha": 10_000,
            "hecta": 10_000,
            "hectare": 10_000
        }
        
        # Common amenities and features
        self.amenities_keywords = {
            "gần trường học": "near_school",
            "gần trường": "near_school", 
            "gần bệnh viện": "near_hospital",
            "gần chợ": "near_market",
            "gần siêu thị": "near_supermarket",
            "gần công viên": "near_park",
            "view sông": "river_view",
            "view biển": "ocean_view", 
            "hồ bơi": "swimming_pool",
            "gym": "gym",
            "phòng tập": "gym",
            "bảo vệ": "security",
            "an ninh": "security",
            "thang máy": "elevator",
            "chỗ đậu xe": "parking",
            "gara": "garage",
            "sân vườn": "garden",
            "ban công": "balcony"
        }
        
        # Transaction types
        self.transaction_types = {
            "mua": "BUY",
            "bán": "SELL",
            "thuê": "RENT",
            "cho thuê": "RENT_OUT",
            "đầu tư": "INVEST",
            "investment": "INVEST"
        }
        
    def process_query(self, text: str) -> Dict[str, Any]:
        """
        Main processing function that extracts all relevant information from text
        
        Args:
            text (str): Vietnamese natural language query
            
        Returns:
            Dict[str, Any]: Extracted entities and intent
        """
        try:
            # Normalize text
            normalized_text = self._normalize_text(text)
            
            # Basic NLP processing
            tokens = underthesea.word_tokenize(normalized_text)
            ner_results = underthesea.ner(normalized_text)
            
            # Extract structured information
            result = {
                "original_query": text,
                "normalized_query": normalized_text,
                "tokens": tokens,
                "ner_results": ner_results,
                "extracted_entities": {
                    "property_type": self._extract_property_type(normalized_text),
                    "price_range": self._extract_price_range(normalized_text),
                    "area_range": self._extract_area_range(normalized_text),
                    "bedrooms": self._extract_bedrooms(normalized_text),
                    "bathrooms": self._extract_bathrooms(normalized_text),
                    "location": self._extract_location(normalized_text, ner_results),
                    "amenities": self._extract_amenities(normalized_text),
                    "transaction_type": self._extract_transaction_type(normalized_text)
                },
                "confidence_score": self._calculate_confidence_score(normalized_text)
            }
            
            return result
            
        except Exception as e:
            return {
                "error": str(e),
                "original_query": text
            }
    
    def _normalize_text(self, text: str) -> str:
        """Normalize Vietnamese text using underthesea"""
        try:
            normalized = underthesea.text_normalize(text)
            # Additional cleanup
            normalized = re.sub(r'\s+', ' ', normalized.strip().lower())
            return normalized
        except:
            return text.strip().lower()
    
    def _extract_property_type(self, text: str) -> Optional[str]:
        """Extract property type from text"""
        for keyword, prop_type in self.property_types.items():
            if keyword in text:
                return prop_type
        return None
    
    def _extract_price_range(self, text: str) -> Optional[Dict[str, Any]]:
        """Extract price range from text"""
        price_patterns = [
            # "5 tỷ", "2.5 tỷ", "từ 3 tỷ đến 5 tỷ"  
            r'(?:từ\s+)?(\d+(?:[,\.]\d+)*)\s*(?:đến|-)?\s*(\d+(?:[,\.]\d+)*)?\s*(tỷ|ty|billion|triệu|million|nghìn|nghin|thousand|k)',
            # "dưới 5 tỷ", "trên 3 tỷ"
            r'(dưới|under|below|trên|above|over)\s+(\d+(?:[,\.]\d+)*)\s*(tỷ|ty|billion|triệu|million|nghìn|nghin|thousand|k)',
            # "5-10 tỷ"
            r'(\d+(?:[,\.]\d+)*)\s*-\s*(\d+(?:[,\.]\d+)*)\s*(tỷ|ty|billion|triệu|million|nghìn|nghin|thousand|k)'
        ]
        
        for pattern in price_patterns:
            matches = re.findall(pattern, text, re.IGNORECASE)
            if matches:
                return self._parse_price_match(matches[0])
        
        return None
    
    def _parse_price_match(self, match: Tuple) -> Dict[str, Any]:
        """Parse price match tuple into structured data"""
        if len(match) == 3:
            # Handle range or single value
            if match[0] in ['dưới', 'under', 'below']:
                return {
                    'max_price': float(match[1].replace(',', '.')) * self.price_units.get(match[2], 1),
                    'operator': 'lt'
                }
            elif match[0] in ['trên', 'above', 'over']:
                return {
                    'min_price': float(match[1].replace(',', '.')) * self.price_units.get(match[2], 1),
                    'operator': 'gt'
                }
            else:
                # Range: từ X đến Y or X-Y
                min_val = float(match[0].replace(',', '.')) * self.price_units.get(match[2], 1)
                max_val = float(match[1].replace(',', '.')) * self.price_units.get(match[2], 1) if match[1] else None
                
                if max_val:
                    return {'min_price': min_val, 'max_price': max_val, 'operator': 'range'}
                else:
                    return {'exact_price': min_val, 'operator': 'eq'}
        
        return None
    
    def _extract_area_range(self, text: str) -> Optional[Dict[str, Any]]:
        """Extract area range from text"""
        area_patterns = [
            r'(\d+(?:[,\.]\d+)*)\s*(?:đến|-)?\s*(\d+(?:[,\.]\d+)*)?\s*(m2|m²|mét vuông|met vuong|sqm|ha|hecta|hectare)',
            r'(dưới|under|below|trên|above|over)\s+(\d+(?:[,\.]\d+)*)\s*(m2|m²|mét vuông|met vuong|sqm|ha|hecta|hectare)'
        ]
        
        for pattern in area_patterns:
            matches = re.findall(pattern, text, re.IGNORECASE)
            if matches:
                return self._parse_area_match(matches[0])
        
        return None
    
    def _parse_area_match(self, match: Tuple) -> Dict[str, Any]:
        """Parse area match tuple into structured data"""
        if len(match) == 3:
            if match[0] in ['dưới', 'under', 'below']:
                return {
                    'max_area': float(match[1].replace(',', '.')) * self.area_units.get(match[2], 1),
                    'operator': 'lt'
                }
            elif match[0] in ['trên', 'above', 'over']:
                return {
                    'min_area': float(match[1].replace(',', '.')) * self.area_units.get(match[2], 1),
                    'operator': 'gt'
                }
            else:
                min_val = float(match[0].replace(',', '.')) * self.area_units.get(match[2], 1)
                max_val = float(match[1].replace(',', '.')) * self.area_units.get(match[2], 1) if match[1] else None
                
                if max_val:
                    return {'min_area': min_val, 'max_area': max_val, 'operator': 'range'}
                else:
                    return {'exact_area': min_val, 'operator': 'eq'}
        
        return None
    
    def _extract_bedrooms(self, text: str) -> Optional[int]:
        """Extract number of bedrooms"""
        patterns = [
            r'(\d+)\s*(?:phòng ngủ|pn|phòng|bedrooms?|bed)',
            r'(\d+)\s*pn\b',
            r'(\d+)\s*br\b'
        ]
        
        for pattern in patterns:
            match = re.search(pattern, text, re.IGNORECASE)
            if match:
                return int(match.group(1))
        
        return None
    
    def _extract_bathrooms(self, text: str) -> Optional[int]:
        """Extract number of bathrooms"""
        patterns = [
            r'(\d+)\s*(?:phòng tắm|toilet|wc|bathrooms?|bath)',
            r'(\d+)\s*wc\b'
        ]
        
        for pattern in patterns:
            match = re.search(pattern, text, re.IGNORECASE)
            if match:
                return int(match.group(1))
        
        return None
    
    def _extract_location(self, text: str, ner_results: List) -> Optional[Dict[str, Any]]:
        """Extract location information"""
        location_info = {}
        
        # Extract from NER results
        current_location = []
        for token_info in ner_results:
            if len(token_info) >= 4 and token_info[3] in ['B-LOC', 'I-LOC']:
                current_location.append(token_info[0])
            elif current_location:
                location_info['ner_location'] = ' '.join(current_location)
                current_location = []
        
        if current_location:
            location_info['ner_location'] = ' '.join(current_location)
        
        # Extract specific administrative divisions
        district_match = re.search(r'quận\s+(\d+|[a-zA-ZÀ-ỹ\s]+)', text, re.IGNORECASE)
        if district_match:
            location_info['district'] = f"Quận {district_match.group(1).strip()}"
        
        ward_match = re.search(r'phường\s+([a-zA-ZÀ-ỹ\s\d]+)', text, re.IGNORECASE)
        if ward_match:
            location_info['ward'] = f"Phường {ward_match.group(1).strip()}"
        
        # Extract specific area names
        area_patterns = [
            r'(?:khu vực|kv)\s+([a-zA-ZÀ-ỹ\s]+)',
            r'(?:ở|tại)\s+([a-zA-ZÀ-ỹ\s]+)',
        ]
        
        for pattern in area_patterns:
            match = re.search(pattern, text, re.IGNORECASE)
            if match:
                location_info['area'] = match.group(1).strip()
                break
        
        return location_info if location_info else None
    
    def _extract_amenities(self, text: str) -> List[str]:
        """Extract amenities and features"""
        found_amenities = []
        
        for keyword, amenity_code in self.amenities_keywords.items():
            if keyword in text:
                found_amenities.append(amenity_code)
        
        return found_amenities
    
    def _extract_transaction_type(self, text: str) -> Optional[str]:
        """Extract transaction type (buy, rent, invest, etc.)"""
        for keyword, transaction_type in self.transaction_types.items():
            if keyword in text:
                return transaction_type
        
        return None
    
    def _calculate_confidence_score(self, text: str) -> float:
        """Calculate confidence score based on extracted entities"""
        score = 0.0
        factors = 0
        
        # Property type adds confidence
        if any(keyword in text for keyword in self.property_types.keys()):
            score += 0.3
        factors += 1
        
        # Price information adds confidence  
        if any(unit in text for unit in self.price_units.keys()):
            score += 0.25
        factors += 1
        
        # Location information adds confidence
        if any(indicator in text for indicator in self.location_indicators):
            score += 0.2
        factors += 1
        
        # Specific numbers (bedrooms, area) add confidence
        if re.search(r'\d+', text):
            score += 0.15
        factors += 1
        
        # Transaction type adds confidence
        if any(keyword in text for keyword in self.transaction_types.keys()):
            score += 0.1
        factors += 1
        
        return min(score, 1.0)

def main():
    """Main function for command-line usage"""
    if len(sys.argv) != 2:
        print("Usage: python underthesea_processor.py '<query_text>'")
        sys.exit(1)
    
    query_text = sys.argv[1]
    processor = RealEstateNLPProcessor()
    result = processor.process_query(query_text)
    
    # Output as JSON
    print(json.dumps(result, ensure_ascii=False, indent=2))

if __name__ == "__main__":
    main() 