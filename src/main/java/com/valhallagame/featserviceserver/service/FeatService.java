package com.valhallagame.featserviceserver.service;

import com.valhallagame.characterserviceclient.CharacterServiceClient;
import com.valhallagame.characterserviceclient.model.CharacterData;
import com.valhallagame.common.RestResponse;
import com.valhallagame.common.rabbitmq.NotificationMessage;
import com.valhallagame.common.rabbitmq.RabbitMQRouting;
import com.valhallagame.common.rabbitmq.RabbitSender;
import com.valhallagame.featserviceclient.message.FeatName;
import com.valhallagame.featserviceserver.model.Feat;
import com.valhallagame.featserviceserver.repository.FeatRepository;
import com.valhallagame.featserviceserver.trigger.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;

@Service
public class FeatService {

	private static final Logger logger = LoggerFactory.getLogger(FeatService.class);

	@Autowired
	private CharacterServiceClient characterServiceClient;

	@Autowired
	private RabbitSender rabbitSender;

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
		logger.info("Saving feat {}", feat);
		return featRepository.save(feat);
	}

	public void deleteFeat(Feat feat) {
		logger.info("Deleting feat {}", feat);
		featRepository.delete(feat);
	}

	public List<Feat> getFeats(String characterName) {
		logger.info("Getting feats for {}", characterName);
		return featRepository.findByCharacterName(characterName.toLowerCase());
	}

	public void parseIntCounterData(String characterName, String key, int count) {
		logger.info("Parsing int count data for {} with key {} and count {}", characterName, key, count);
		List<Feat> ownedFeats = featRepository.findByCharacterName(characterName);
		List<IntCounterTriggerable> notOwnedFeats = filterNotOwned(intCounterTriggerable, ownedFeats);
		notOwnedFeats.forEach(feat -> feat.intCounterTrigger(characterName, key, count));
	}

	public void parseLowTimerData(String characterName, String key, double timer) {
		logger.info("Parsing low timer data for {} with key {} and timer {}", characterName, key, timer);
		List<Feat> ownedFeats = featRepository.findByCharacterName(characterName);
		List<LowTimerTriggerable> notOwnedFeats = filterNotOwned(lowTimerTriggerable, ownedFeats);
		notOwnedFeats.forEach(feat -> feat.lowTimerTrigger(characterName, key, timer));
	}

	public void parseHighTimerData(String characterName, String key, double timer) {
		logger.info("Parsing high timer data for {} with key {} and timer {}", characterName, key, timer);
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
		logger.info("Creating feat for {} with name {}", characterName, featName);
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
				rabbitSender.sendMessage(RabbitMQRouting.Exchange.FEAT, RabbitMQRouting.Feat.ADD.name(),
						message);
				logger.info("Created feat " + feat + " and sent a message to " + characterName + " about it.");
			} else {
				logger.warn("Feat service tried to create a feat for character {}, but no such character exists",
						characterName);
			}
		} catch (IOException e) {
			logger.error("Character service error", e);
		}

	}

	public void removeFeat(Feat feat) {
		logger.info("Removing feat {}", feat);
		try {
			RestResponse<CharacterData> characterResp = characterServiceClient
					.getCharacter(feat.getCharacterName());
			Optional<CharacterData> characterOpt = characterResp.get();
			if (characterOpt.isPresent()) {
				NotificationMessage message = new NotificationMessage(characterOpt.get().getOwnerUsername(),
						feat.getCharacterName() + " lost " + feat.getName());
				message.addData("feat", feat.getName());
				rabbitSender.sendMessage(RabbitMQRouting.Exchange.FEAT, RabbitMQRouting.Feat.REMOVE.name(),
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
