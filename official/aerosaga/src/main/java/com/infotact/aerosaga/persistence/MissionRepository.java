package com.infotact.aerosaga.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionRepository extends JpaRepository<MissionRecord, String> {

    List<MissionRecord> findTop50ByOrderByCreatedAtDesc();
}
