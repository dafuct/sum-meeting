package com.zoomtranscriber.core.storage;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "installation_states")
public class InstallationState {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Component component;
    
    @Column(length = 50)
    private String version;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InstallStatus status;
    
    @Column(name = "install_path")
    private String installPath;
    
    @Column(name = "last_checked")
    private LocalDateTime lastChecked;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum Component {
        Java,
        Ollama,
        Model
    }
    
    public enum InstallStatus {
        NOT_INSTALLED,
        INSTALLING,
        INSTALLED,
        ERROR
    }
    
    public InstallationState() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = InstallStatus.NOT_INSTALLED;
    }
    
    public InstallationState(Component component) {
        this();
        this.component = component;
    }
    
    public InstallationState(Component component, InstallStatus status) {
        this(component);
        this.status = status;
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.lastChecked = LocalDateTime.now();
    }
    
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public Component getComponent() {
        return component;
    }
    
    public void setComponent(Component component) {
        this.component = component;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public InstallStatus getStatus() {
        return status;
    }
    
    public void setStatus(InstallStatus status) {
        this.status = status;
    }
    
    public String getInstallPath() {
        return installPath;
    }
    
    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }
    
    public LocalDateTime getLastChecked() {
        return lastChecked;
    }
    
    public void setLastChecked(LocalDateTime lastChecked) {
        this.lastChecked = lastChecked;
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
        
        var that = (InstallationState) o;
        return component != null && component.equals(that.component);
    }
    
    @Override
    public int hashCode() {
        return component != null ? component.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "InstallationState{" +
                "id=" + id +
                ", component=" + component +
                ", version='" + version + '\'' +
                ", status=" + status +
                ", installPath='" + installPath + '\'' +
                ", lastChecked=" + lastChecked +
                '}';
    }
}