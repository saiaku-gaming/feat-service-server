package com.valhallagame.featserviceserver.trigger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.valhallagame.featserviceclient.message.FeatName;
import com.valhallagame.featserviceserver.service.FeatService;
import com.valhallagame.statisticsserviceclient.message.StatisticsKey;

import lombok.EqualsAndHashCode;

@Component
@EqualsAndHashCode(callSuper = true)
public class TrainingEfficency extends FeatTrigger implements LowTimerTriggerable {

	@Autowired
	private FeatService featService;

	public TrainingEfficency() {
		super(FeatName.TRAINING_EFFICIENCY);
	}

	@Override
	public void lowTimerTrigger(String characterName, String key, double timer) {
		if (!StatisticsKey.TRAINING_GROUNDS_CLEAR_TIME.name().equals(key)) {
			return;
		}

		if (timer > 60.0f) {
			return;
		}

		featService.createFeat(characterName, getName());
	}

}
