package com.valhallagame.featserviceserver.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
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
import com.valhallagame.featserviceclient.message.FeatName;
import com.valhallagame.featserviceserver.model.Feat;
import com.valhallagame.featserviceserver.repository.FeatRepository;
import com.valhallagame.featserviceserver.trigger.EinharjerSlayer;
import com.valhallagame.featserviceserver.trigger.FeatTrigger;
import com.valhallagame.featserviceserver.trigger.FredstorpSpeedRunner;
import com.valhallagame.featserviceserver.trigger.FredstorpThiefOfThieves;
import com.valhallagame.featserviceserver.trigger.HighTimerTriggerable;
import com.valhallagame.featserviceserver.trigger.IntCounterTriggerable;
import com.valhallagame.featserviceserver.trigger.LowTimerTriggerable;
import com.valhallagame.featserviceserver.trigger.TrainingEfficency;

@Service
public class FeatService {

	@Autowired
	CharacterServiceClient characterServiceClient;

	Logger logger = LoggerFactory.getLogger(FeatService.class);

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private EinharjerSlayer killTheEinharjer;

	@Autowired
	private TrainingEfficency trainingEfficency;
	
	@Autowired
	private FredstorpSpeedRunner fredstorpSpeedRunner;
	
	@Autowired
	private FredstorpThiefOfThieves fredstorpThiefOfThieves;

	private Map<FeatName, IntCounterTriggerable> intCounterTriggerable = new EnumMap<>(FeatName.class);
	private Map<FeatName, LowTimerTriggerable> lowTimerTriggerable = new EnumMap<>(FeatName.class);
	private Map<FeatName, HighTimerTriggerable> highTimerTriggerable = new EnumMap<>(FeatName.class);

	@PostConstruct
	private void init() {
		List<FeatTrigger> allFeatTriggers = new ArrayList<>();
		allFeatTriggers.add(killTheEinharjer);
		allFeatTriggers.add(trainingEfficency);
		allFeatTriggers.add(fredstorpSpeedRunner);
		allFeatTriggers.add(fredstorpThiefOfThieves);

		for (FeatTrigger ft : allFeatTriggers) {
			if (ft instanceof IntCounterTriggerable) {
				intCounterTriggerable.put(ft.getName(), (IntCounterTriggerable) ft);
			}
			if (ft instanceof LowTimerTriggerable) {
				lowTimerTriggerable.put(ft.getName(), (LowTimerTriggerable) ft);
			}
			if (ft instanceof HighTimerTriggerable) {
				highTimerTriggerable.put(ft.getName(), (HighTimerTriggerable) ft);
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
		return featRepository.findByCharacterName(characterName.toLowerCase());
	}

	public void parseIntCounterData(String characterName, String key, int count) {
		List<Feat> ownedFeats = featRepository.findByCharacterName(characterName);
		List<IntCounterTriggerable> notOwnedFeats = filterNotOwned(intCounterTriggerable, ownedFeats);
		notOwnedFeats.forEach(feat -> feat.intCounterTrigger(characterName, key, count));
	}

	public void parseLowTimerData(String characterName, String key, double timer) {
		List<Feat> ownedFeats = featRepository.findByCharacterName(characterName);
		List<LowTimerTriggerable> notOwnedFeats = filterNotOwned(lowTimerTriggerable, ownedFeats);
		notOwnedFeats.forEach(feat -> feat.lowTimerTrigger(characterName, key, timer));
	}

	public void parseHighTimerData(String characterName, String key, double timer) {
		List<Feat> ownedFeats = featRepository.findByCharacterName(characterName);
		List<HighTimerTriggerable> notOwnedFeats = filterNotOwned(highTimerTriggerable, ownedFeats);
		notOwnedFeats.forEach(feat -> feat.highTimerTrigger(characterName, key, timer));
	}

	private <T> List<T> filterNotOwned(Map<FeatName, T> allFeats, List<Feat> ownedFeats) {

		Map<FeatName, T> out = new EnumMap<>(allFeats);
		for (Feat owned : ownedFeats) {
			try {
				FeatName featName = FeatName.valueOf(owned.getName());
				out.remove(featName);
			} catch (IllegalArgumentException e) {
				// Feat that does not have a correct enum value.
				featRepository.delete(owned);
			}
		}
		return new ArrayList<>(out.values());
	}

	public void createFeat(String characterName, FeatName featName) {
		Feat feat = new Feat();
		feat.setCharacterName(characterName.toLowerCase());
		feat.setName(featName.name());
		featRepository.save(feat);

		try {
			RestResponse<CharacterData> characterResp = characterServiceClient
					.getCharacter(characterName);
			Optional<CharacterData> characterOpt = characterResp.get();
			if (characterOpt.isPresent()) {
				CharacterData character = characterOpt.get();
				NotificationMessage message = new NotificationMessage(character.getOwnerUsername(),
						characterName + " got " + feat.getName());
				message.addData("feat", feat.getName());
				message.addData("characterName", characterName);
				rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FEAT.name(), RabbitMQRouting.Feat.ADD.name(),
						message);
			} else {
				logger.warn("Feat service tried to create a feat for character {}, but no such character exists",
						characterName);
			}
		} catch (IOException e) {
			logger.error("Character service error", e);
		}

	}

	public void removeFeat(Feat feat) {
		try {
			RestResponse<CharacterData> characterResp = characterServiceClient
					.getCharacter(feat.getCharacterName());
			Optional<CharacterData> characterOpt = characterResp.get();
			if (characterOpt.isPresent()) {
				NotificationMessage message = new NotificationMessage(characterOpt.get().getOwnerUsername(),
						feat.getCharacterName() + " lost " + feat.getName());
				message.addData("feat", feat.getName());
				rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FEAT.name(), RabbitMQRouting.Feat.REMOVE.name(),
						message);
			} else {
				logger.warn("Feat service tried to remove a feat for character {}, but no such character exists",
						feat.getCharacterName());
			}
		} catch (IOException e) {
			logger.error("Character service error", e);
		}
		featRepository.delete(feat);
	}
}
