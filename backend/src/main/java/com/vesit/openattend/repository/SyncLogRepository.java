package com.vesit.openattend.repository;

import com.vesit.openattend.entity.SyncLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SyncLogRepository extends JpaRepository<SyncLog, String> {
    List<SyncLog> findBySourceIdentifierOrderByStartedAtDesc(String sourceIdentifier, Pageable pageable);
    List<SyncLog> findAllByOrderByStartedAtDesc(Pageable pageable);
    Optional<SyncLog> findFirstBySourceIdentifierOrderByStartedAtDesc(String sourceIdentifier);
}
