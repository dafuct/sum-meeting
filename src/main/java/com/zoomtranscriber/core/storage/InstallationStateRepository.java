package com.zoomtranscriber.core.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InstallationStateRepository extends JpaRepository<InstallationState, java.util.UUID> {
    
    Optional<InstallationState> findByComponent(InstallationState.Component component);
    
    List<InstallationState> findByStatus(InstallationState.InstallStatus status);
    
    List<InstallationState> findByVersion(String version);
    
    List<InstallationState> findByInstallPath(String installPath);
    
    @Query("SELECT i FROM InstallationState i WHERE i.lastChecked >= :since")
    List<InstallationState> findByLastCheckedAfter(@Param("since") LocalDateTime since);
    
    @Query("SELECT i FROM InstallationState i WHERE i.lastChecked <= :before")
    List<InstallationState> findByLastCheckedBefore(@Param("before") LocalDateTime before);
    
    @Query("SELECT COUNT(i) FROM InstallationState i WHERE i.status = :status")
    long countByStatus(@Param("status") InstallationState.InstallStatus status);
    
    @Query("SELECT i.component, i.status, COUNT(i) FROM InstallationState i GROUP BY i.component, i.status")
    List<Object[]> countByComponentAndStatusGrouped();
    
    @Query("SELECT i.component, COUNT(i) FROM InstallationState i WHERE i.status = 'INSTALLED' GROUP BY i.component")
    List<Object[]> countInstalledByComponentGrouped();
    
    @Query("SELECT i FROM InstallationState i WHERE i.status IN :statuses")
    List<InstallationState> findByStatusIn(@Param("statuses") List<InstallationState.InstallStatus> statuses);
    
    @Query("SELECT i FROM InstallationState i WHERE i.component IN :components")
    List<InstallationState> findByComponentsIn(@Param("components") List<InstallationState.Component> components);
    
    @Query("SELECT DISTINCT i.component FROM InstallationState i")
    List<InstallationState.Component> findAllComponents();
    
    @Query("SELECT DISTINCT i.status FROM InstallationState i")
    List<InstallationState.InstallStatus> findAllStatuses();
    
    @Query("SELECT i FROM InstallationState i WHERE i.component = :component AND i.status = :status")
    Optional<InstallationState> findByComponentAndStatus(@Param("component") InstallationState.Component component, 
                                                      @Param("status") InstallationState.InstallStatus status);
    
    boolean existsByComponent(InstallationState.Component component);
    
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM InstallationState i WHERE i.component = :component AND i.status = 'INSTALLED'")
    boolean isComponentInstalled(@Param("component") InstallationState.Component component);
    
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM InstallationState i WHERE i.status = 'ERROR'")
    boolean hasAnyInstallationErrors();
    
    @Query("SELECT i FROM InstallationState i WHERE i.status = 'ERROR' ORDER BY i.updatedAt DESC")
    List<InstallationState> findInstallationErrors();
    
    void deleteByComponent(InstallationState.Component component);
}