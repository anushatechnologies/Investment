package com.anushabazaar.backend.repository;

import com.anushabazaar.backend.domain.PlatformSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformSettingRepository extends JpaRepository<PlatformSetting, String> {
}
