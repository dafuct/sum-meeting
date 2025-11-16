package com.zoomtranscriber.core.storage;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "configurations")
public class Configuration {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    private String key;
    
    @Column(nullable = false, length = 1000)
    private String value;
    
    @Column(length = 255)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConfigCategory category;
    
    @Column(name = "is_encrypted", nullable = false)
    private Boolean isEncrypted;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum ConfigCategory {
        AUDIO,
        TRANSCRIPTION,
        UI,
        PRIVACY,
        SYSTEM
    }
    
    public Configuration() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isEncrypted = false;
    }
    
    public Configuration(String key, String value, ConfigCategory category) {
        this();
        this.key = key;
        this.value = value;
        this.category = category;
    }
    
    public Configuration(String key, String value, ConfigCategory category, String description) {
        this(key, value, category);
        this.description = description;
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public ConfigCategory getCategory() {
        return category;
    }
    
    public void setCategory(ConfigCategory category) {
        this.category = category;
    }
    
    public Boolean getIsEncrypted() {
        return isEncrypted;
    }
    
    public void setIsEncrypted(Boolean isEncrypted) {
        this.isEncrypted = isEncrypted;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        var that = (Configuration) o;
        return key != null && key.equals(that.key);
    }
    
    @Override
    public int hashCode() {
        return key != null ? key.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "Configuration{" +
                "id=" + id +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", category=" + category +
                ", isEncrypted=" + isEncrypted +
                ", description='" + description + '\'' +
                '}';
    }
}