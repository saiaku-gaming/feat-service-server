package com.valhallagame.featserviceserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.valhallagame.characterserviceclient.CharacterServiceClient;
import com.valhallagame.personserviceclient.PersonServiceClient;
import com.valhallagame.wardrobeserviceclient.WardrobeServiceClient;

@Configuration
@Profile("default")
public class DevConfig {
	@Bean
	public CharacterServiceClient characterServiceClient() {
		return CharacterServiceClient.get();
	}

	@Bean
	public PersonServiceClient personServiceClient() {
		return PersonServiceClient.get();
	}
	
	@Bean
	public WardrobeServiceClient wardrobeServiceClient() {
		return WardrobeServiceClient.get();
	}
}
