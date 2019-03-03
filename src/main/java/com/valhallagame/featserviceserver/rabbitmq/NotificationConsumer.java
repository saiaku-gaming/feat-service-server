package com.valhallagame.featserviceserver.rabbitmq;

import com.valhallagame.common.rabbitmq.NotificationMessage;
import com.valhallagame.featserviceserver.model.Feat;
import com.valhallagame.featserviceserver.service.FeatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class NotificationConsumer {

	private static final Logger logger = LoggerFactory.getLogger(NotificationConsumer.class);

	@Autowired
	private FeatService featService;

	@Value("${spring.application.name}")
	private String appName;

	@RabbitListener(queues = { "#{featCharacterDeleteQueue.name}" })
	public void receiveCharacterDelete(NotificationMessage message) {
		MDC.put("service_name", appName);
		MDC.put("request_id", UUID.randomUUID().toString());

		logger.info("Received character delete notification with message: {}", message);

		try {
			String characterName = (String) message.getData().get("characterName");
			List<Feat> feats = featService.getFeats(characterName);
			for (Feat feat : feats) {
				featService.deleteFeat(feat);
			}
		} finally {
			MDC.clear();
		}
	}

	@RabbitListener(queues = { "#{featStatisticsIntCounterQueue.name}" })
	public void receiveStatisticsIntCounter(NotificationMessage message) {
		MDC.put("service_name", appName);
		MDC.put("request_id", UUID.randomUUID().toString());

		logger.info("Got int counter " + message);

		try {
			Map<String, Object> data = message.getData();
			String characterName = (String) data.get("characterName");
			String key = (String) data.get("key");
			int count = (Integer) data.get("count");
			featService.parseIntCounterData(characterName, key, count);
		} finally {
			MDC.clear();
		}
	}

	@RabbitListener(queues = { "#{featStatisticsLowTimerQueue.name}" })
	public void receiveStatisticsLowTimer(NotificationMessage message) {
		MDC.put("service_name", appName);
		MDC.put("request_id", UUID.randomUUID().toString());

		logger.info("got low timer " + message);

		try {
			Map<String, Object> data = message.getData();
			String characterName = (String) data.get("characterName");
			String key = (String) data.get("key");
			double timer = (Double) data.get("timer");
			featService.parseLowTimerData(characterName, key, timer);
		} finally {
			MDC.clear();
		}
	}

	@RabbitListener(queues = { "#{featStatisticsHighTimerQueue.name}" })
	public void receiveStatisticsHighTimer(NotificationMessage message) {
		MDC.put("service_name", appName);
		MDC.put("request_id", UUID.randomUUID().toString());

		logger.info("Got high timer " + message);

		try {
			Map<String, Object> data = message.getData();
			String characterName = (String) data.get("characterName");
			String key = (String) data.get("key");
			double timer = (Double) data.get("timer");
			featService.parseHighTimerData(characterName, key, timer);
		} finally {
			MDC.clear();
		}
	}
}
