package com.valhallagame.featserviceserver.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.valhallagame.featserviceserver.model.Feat;

public interface FeatRepository extends JpaRepository<Feat, Integer> {
	public List<Feat> findByCharacterName(String characterName);
}
