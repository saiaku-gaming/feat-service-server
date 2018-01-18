package com.valhallagame.featserviceserver.controller;

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
import com.valhallagame.common.JS;
import com.valhallagame.common.rabbitmq.NotificationMessage;
import com.valhallagame.common.rabbitmq.RabbitMQRouting;
import com.valhallagame.featserviceclient.message.AddFeatParameter;
import com.valhallagame.featserviceclient.message.GetFeatsParameter;
import com.valhallagame.featserviceclient.message.RemoveFeatParameter;
import com.valhallagame.featserviceserver.model.Feat;
import com.valhallagame.featserviceserver.service.FeatService;

@Controller
@RequestMapping(path = "/v1/feat")
public class FeatController {

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private FeatService featService;

	@RequestMapping(path = "/get-feats", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> getFeats(@Valid @RequestBody GetFeatsParameter input) {
		List<Feat> feats = featService.getFeats(input.getCharacterName().toLowerCase());
		List<String> items = feats.stream().map(Feat::getName).collect(Collectors.toList());
		return JS.message(HttpStatus.OK, items);
	}

	@RequestMapping(path = "/add-feat", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> addFeat(@Valid @RequestBody AddFeatParameter input) {

		// Duplicate protection
		List<Feat> feats = featService.getFeats(input.getCharacterName());
		List<String> items = feats.stream().map(Feat::getName).collect(Collectors.toList());
		if (items.contains(input.getName().name())) {
			return JS.message(HttpStatus.ALREADY_REPORTED, "Already in store");
		}

		featService.createFeat(input.getCharacterName(), input.getName());
		return JS.message(HttpStatus.OK, "Feat item added");
	}
	
	@RequestMapping(path = "/remove-feat", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<JsonNode> removeFeat(@Valid @RequestBody RemoveFeatParameter input) {
		List<Feat> feats = featService.getFeats(input.getCharacterName());
		Optional<Feat> featOpt = feats.stream().filter(f-> f.getName().equals(input.getName().name())).findAny();
		if(featOpt.isPresent()) {
			featService.removeFeat(featOpt.get());
			rabbitTemplate.convertAndSend(RabbitMQRouting.Exchange.FEAT.name(), RabbitMQRouting.Feat.REMOVE.name(),
					new NotificationMessage(input.getCharacterName(), "feat item removed"));
			return JS.message(HttpStatus.OK, "Feat item removed");
		} else {
			return JS.message(HttpStatus.NOT_FOUND, "No feat found");
		}
	}
}

