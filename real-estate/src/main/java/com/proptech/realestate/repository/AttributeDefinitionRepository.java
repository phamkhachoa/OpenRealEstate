package com.proptech.realestate.repository;

import com.proptech.realestate.model.entity.AttributeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AttributeDefinitionRepository extends JpaRepository<AttributeDefinition, Integer> {
    Optional<AttributeDefinition> findByCode(String code);
} 