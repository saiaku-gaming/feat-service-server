package com.valhallagame.featserviceserver.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.valhallagame.characterserviceclient.CharacterServiceClient;
import com.valhallagame.characterserviceclient.model.CharacterData;
import com.valhallagame.common.RestResponse;
import com.valhallagame.common.rabbitmq.NotificationMessage;
import com.valhallagame.common.rabbitmq.RabbitMQRouting;
import com.valhallagame.featserviceserver.model.Feat;
import com.valhallagame.featserviceserver.repository.FeatRepository;
import com.valhallagame.featserviceserver.trigger.FeatTrigger;
import com.valhallagame.featserviceserver.trigger.IntCounterTriggerable;
import com.valhallagame.featserviceserver.trigger.KillTheEinharjer;

@Service
public class FeatService {
	
	@Autowired
	CharacterServiceClient characterServiceClient;
	
	Logger logger = LoggerFactory.getLogger(FeatService.class);
	
	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private KillTheEinharjer killTheEinharjer;
	
	private Map<String, IntCounterTriggerable> intCounterTriggerable = new HashMap<>();
	
	@PostConstruct
	private void init() {
		List<FeatTrigger> allFeatTriggers = new ArrayList<>();
 		allFeatTriggers.add(killTheEinharjer);
		
 		for(FeatTrigger ft : allFeatTriggers) {
			if (ft instanceof IntCounterTriggerable) {
				intCounterTriggerable.put(ft.getName(), (IntCounterTriggerable) ft);
			}
		}
	}
	
	@Autowired
	private FeatRepository featRepository;

	public Feat saveFeat(Feat feat) {
		return featRepository.save(feat);
	}

	public void deleteFeat(Feat feat) {
		featRepository.delete(feat);
	}

	public List<Feat> getFeats(String characterName) {
		return featRepository.findByCharacterName(characterName);
	}

	public void parseIntCounterData(String characterName, String key, int count) {
		List<Feat> ownedFeats = featRepository.findByCharacterName(characterName);
		List<IntCounterTriggerable> notOwnedFeats = filterNotOwned(intCounterTriggerable, ownedFeats);
		notOwnedFeats.forEach(feat -> feat.intCounterTrigger(characterName, key, count));
	}

	private <T> List<T> filterNotOwned(Map<String, T> allFeats,
			List<Feat> ownedFeats) {

		Map<String, T> out = new HashMap<>(allFeats);
		for(Feat owned : ownedFeats) {
			out.remove(owned.getName());
		}
		return new ArrayList<>(out.values());
	}

	public void createFeat(String characterName, String name) {
		Feat feat = new Feat();
		feat.setCharacterName(characterName);
		feat.setName(name);
		featRepository.save(feat);
		
		try {
			RestResponse<CharacterData> characterResp = characterServiceClient.getCharacterWithoutOwnerValidation(characterName);
			Optional<CharacterData> characterOpt = characterResp.get();
			if(characterOpt.isPresent()) {
				NotificationMessage message = new NotificationMessage(characterOpt.get().getOwnerUsername(), characterName + " got " + feat.getName());
				message.addData("feat", feat.getName());
				rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FEAT.name(), RabbitMQRouting.Feat.ADD.name(), message);
			} else {
				logger.warn("Feat service tried to create a feat for character {}, but no such character exists", characterName);
			}
		} catch (IOException e) {
			logger.error("Character service error", e);
		}

	}
}
