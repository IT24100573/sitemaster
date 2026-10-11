package com.sitemaster.hr;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Data access for training records. */
public interface TrainingRecordRepository
        extends JpaRepository<TrainingRecord, Long>, JpaSpecificationExecutor<TrainingRecord> {
}
