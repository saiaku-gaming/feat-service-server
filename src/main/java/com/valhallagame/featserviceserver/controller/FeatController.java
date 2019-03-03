package com.valhallagame.featserviceserver.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.valhallagame.characterserviceclient.CharacterServiceClient;
import com.valhallagame.characterserviceclient.model.CharacterData;
import com.valhallagame.common.JS;
import com.valhallagame.common.RestResponse;
import com.valhallagame.common.rabbitmq.NotificationMessage;
import com.valhallagame.common.rabbitmq.RabbitMQRouting;
import com.valhallagame.common.rabbitmq.RabbitSender;
import com.valhallagame.featserviceclient.message.AddFeatParameter;
import com.valhallagame.featserviceclient.message.GetFeatsParameter;
import com.valhallagame.featserviceclient.message.RemoveFeatParameter;
import com.valhallagame.featserviceserver.model.Feat;
import com.valhallagame.featserviceserver.service.FeatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping(path = "/v1/feat")
public class FeatController {
	private static final Logger logger = LoggerFactory.getLogger(FeatController.class);

	@Autowired
	private RabbitSender rabbitSender;

	@Autowired
	private FeatService featService;
	
	@Autowired
	private CharacterServiceClient characterServiceClient;

	@RequestMapping(path = "/get-feats", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> getFeats(@Valid @RequestBody GetFeatsParameter input) {
		logger.info("Get Feats called with {}", input);
		List<Feat> feats = featService.getFeats(input.getCharacterName().toLowerCase());
		List<String> items = feats.stream().map(Feat::getName).collect(Collectors.toList());
		return JS.message(HttpStatus.OK, items);
	}

	@RequestMapping(path = "/add-feat", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> addFeat(@Valid @RequestBody AddFeatParameter input) {
		logger.info("Add Feat called with {}", input);
		String characterName = input.getCharacterName().toLowerCase();

		// Duplicate protection
		List<Feat> feats = featService.getFeats(characterName);
		List<String> items = feats.stream().map(Feat::getName).collect(Collectors.toList());
		if (items.contains(input.getName().name())) {
			return JS.message(HttpStatus.ALREADY_REPORTED, "Already in store");
		}

		featService.createFeat(characterName, input.getName());
		return JS.message(HttpStatus.OK, "Feat item added");
	}

	@RequestMapping(path = "/remove-feat", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> removeFeat(@Valid @RequestBody RemoveFeatParameter input) throws IOException {
		logger.info("Remove Feat called with {}", input);
		RestResponse<CharacterData> characterResp = characterServiceClient.getCharacter(input.getCharacterName());
		Optional<CharacterData> characterOpt = characterResp.get();
		if(!characterOpt.isPresent()) {
			return JS.message(characterResp);
		}
		CharacterData character = characterOpt.get();
		
		List<Feat> feats = featService.getFeats(input.getCharacterName());
		Optional<Feat> featOpt = feats.stream().filter(f -> f.getName().equals(input.getName().name())).findAny();
		if (featOpt.isPresent()) {
			featService.removeFeat(featOpt.get());
			rabbitSender.sendMessage(RabbitMQRouting.Exchange.FEAT, RabbitMQRouting.Feat.REMOVE.name(),
					new NotificationMessage(character.getOwnerUsername(), "feat item removed"));
			return JS.message(HttpStatus.OK, "Feat item removed");
		} else {

			return JS.message(HttpStatus.NOT_FOUND, "No feat found");
		}
	}
}
