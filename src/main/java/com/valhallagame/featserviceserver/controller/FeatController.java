package com.valhallagame.featserviceserver.controller;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.JsonNode;
import com.valhallagame.characterserviceclient.CharacterServiceClient;
import com.valhallagame.characterserviceclient.model.CharacterData;
import com.valhallagame.common.JS;
import com.valhallagame.common.RestResponse;
import com.valhallagame.common.rabbitmq.NotificationMessage;
import com.valhallagame.common.rabbitmq.RabbitMQRouting;
import com.valhallagame.featserviceclient.message.AddFeatParameter;
import com.valhallagame.featserviceclient.message.DebugAddFeatParameter;
import com.valhallagame.featserviceclient.message.GetFeatsParameter;
import com.valhallagame.featserviceserver.model.Feat;
import com.valhallagame.featserviceserver.service.FeatService;

@Controller
@RequestMapping(path = "/v1/feat")
public class FeatController {

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private FeatService featService;

	@Autowired
	private CharacterServiceClient characterServiceClient;

	@RequestMapping(path = "/get-feats", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> getFeats(@Valid @RequestBody GetFeatsParameter input) {
		List<Feat> Feats = featService.getFeats(input.getCharacterName());
		List<String> items = Feats.stream().map(Feat::getName).collect(Collectors.toList());
		return JS.message(HttpStatus.OK, items);
	}

	@RequestMapping(path = "/debug-add-feat", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> addFeat(@Valid @RequestBody DebugAddFeatParameter input) throws IOException {
		RestResponse<CharacterData> characterResp = characterServiceClient
				.getCharacterWithoutOwnerValidation(input.getUsername().toLowerCase());
		Optional<CharacterData> characterOpt = characterResp.get();
		if (characterOpt.isPresent()) {
			AddFeatParameter newItemParam = new AddFeatParameter(characterOpt.get().getCharacterName(),
					input.getItemName());
			return addFeat(newItemParam);
		} else {
			return JS.message(characterResp);
		}
	}

	@RequestMapping(path = "/add-feat", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> addFeat(@Valid @RequestBody AddFeatParameter input) {

		// Duplicate protection
		List<Feat> Feats = featService.getFeats(input.getCharacterName());
		List<String> items = Feats.stream().map(Feat::getName).collect(Collectors.toList());
		if (items.contains(input.getFeatName())) {
			return JS.message(HttpStatus.ALREADY_REPORTED, "Already in store");
		}

		featService.saveFeat(new Feat(input.getFeatName(), input.getCharacterName()));
		rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FEAT.name(), RabbitMQRouting.Feat.ADD_FEAT.name(),
				new NotificationMessage(input.getCharacterName(), "feat item added"));

		return JS.message(HttpStatus.OK, "Feat item added");
	}
}
