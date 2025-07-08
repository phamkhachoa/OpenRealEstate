package com.proptech.realestate.repository;

import com.proptech.realestate.model.entity.ListingCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ListingCategoryRepository extends JpaRepository<ListingCategory, Integer> {
} 