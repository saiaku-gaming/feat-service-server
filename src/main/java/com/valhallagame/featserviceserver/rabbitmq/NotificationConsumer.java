package com.valhallagame.featserviceserver.rabbitmq;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.valhallagame.common.rabbitmq.NotificationMessage;
import com.valhallagame.featserviceserver.model.Feat;
import com.valhallagame.featserviceserver.service.FeatService;

@Component
public class NotificationConsumer {

	Logger logger = LoggerFactory.getLogger(NotificationConsumer.class);

	@Autowired
	FeatService featService;

	@RabbitListener(queues = { "#{featCharacterDeleteQueue.name}" })
	public void receiveCharacterDelete(NotificationMessage message) {
		String characterName = (String) message.getData().get("characterName");
		List<Feat> feats = featService.getFeats(characterName);
		for (Feat feat : feats) {
			featService.deleteFeat(feat);
		}
	}

	@RabbitListener(queues = { "#{featStatisticsIntCounterQueue.name}" })
	public void receiveStatisticsIntCounter(NotificationMessage message) {
		logger.info("Gout int counter");
		Map<String, Object> data = message.getData();
		String characterName = (String) data.get("characterName");
		String key = (String) data.get("key");
		int count = (Integer) data.get("count");
		featService.parseIntCounterData(characterName, key, count);
	}

	@RabbitListener(queues = { "#{featStatisticsLowTimerQueue.name}" })
	public void receiveStatisticsLowTimer(NotificationMessage message) {
		logger.info("Gout low timer");
		Map<String, Object> data = message.getData();
		String characterName = (String) data.get("characterName");
		String key = (String) data.get("key");
		double timer = (Double) data.get("timer");
		featService.parseLowTimerData(characterName, key, timer);
	}

	@RabbitListener(queues = { "#{featStatisticsHighTimerQueue.name}" })
	public void receiveStatisticsHighTimer(NotificationMessage message) {
		logger.info("Gout high timer");
		Map<String, Object> data = message.getData();
		String characterName = (String) data.get("characterName");
		String key = (String) data.get("key");
		double timer = (Double) data.get("timer");
		featService.parseHighTimerData(characterName, key, timer);
	}
}
