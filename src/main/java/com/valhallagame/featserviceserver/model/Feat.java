package com.valhallagame.featserviceserver.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "feat")
public class Feat {
	@Id
	@SequenceGenerator(name = "feat_feat_id_seq", sequenceName = "feat_feat_id_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "feat_feat_id_seq")
	@Column(name = "feat_id", updatable = false)
	private Integer id;

	@Column(name = "name")
	private String name;

	@Column(name = "characterName")
	private String characterName;

	public Feat(String name, String characterName) {
		this.name = name;
		this.characterName = characterName;
	}
}
