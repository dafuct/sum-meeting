package com.zoomtranscriber.core.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfigurationRepository extends JpaRepository<Configuration, java.util.UUID> {
    
    Optional<Configuration> findByKey(String key);
    
    List<Configuration> findByCategory(Configuration.ConfigCategory category);
    
    List<Configuration> findByIsEncrypted(Boolean isEncrypted);
    
    List<Configuration> findByCategoryAndIsEncrypted(Configuration.ConfigCategory category, Boolean isEncrypted);
    
    @Query("SELECT c FROM Configuration c WHERE c.key LIKE %:keyword% OR c.description LIKE %:keyword%")
    List<Configuration> findByKeyOrDescriptionContaining(@Param("keyword") String keyword);
    
    @Query("SELECT c FROM Configuration c WHERE c.category = :category AND (c.key LIKE %:keyword% OR c.description LIKE %:keyword%)")
    List<Configuration> findByCategoryAndKeyOrDescriptionContaining(@Param("category") Configuration.ConfigCategory category, 
                                                                @Param("keyword") String keyword);
    
    @Query("SELECT DISTINCT c.category FROM Configuration c")
    List<Configuration.ConfigCategory> findAllCategories();
    
    @Query("SELECT COUNT(c) FROM Configuration c WHERE c.category = :category")
    long countByCategory(@Param("category") Configuration.ConfigCategory category);
    
    @Query("SELECT COUNT(c) FROM Configuration c WHERE c.isEncrypted = true")
    long countEncryptedConfigurations();
    
    boolean existsByKey(String key);
    
    @Query("SELECT c FROM Configuration c WHERE c.key IN :keys")
    List<Configuration> findByKeysIn(@Param("keys") List<String> keys);
    
    @Query("SELECT c FROM Configuration c WHERE c.category IN :categories")
    List<Configuration> findByCategoriesIn(@Param("categories") List<Configuration.ConfigCategory> categories);
    
    @Query("SELECT c.key, c.value FROM Configuration c WHERE c.isEncrypted = false")
    List<Object[]> findAllUnencryptedKeyValuePairs();
    
    @Query("SELECT c.key, c.value FROM Configuration c WHERE c.category = :category AND c.isEncrypted = false")
    List<Object[]> findUnencryptedKeyValuePairsByCategory(@Param("category") Configuration.ConfigCategory category);
    
    void deleteByKey(String key);
    
    void deleteByCategory(Configuration.ConfigCategory category);
}