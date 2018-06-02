package com.valhallagame.featserviceserver.trigger;

import org.springframework.beans.factory.annotation.Autowired;

import com.valhallagame.featserviceclient.message.FeatName;
import com.valhallagame.featserviceserver.service.FeatService;
import com.valhallagame.statisticsserviceclient.message.StatisticsKey;

public class FredstorpSpeedRunner extends FeatTrigger implements LowTimerTriggerable {

	@Autowired
	private FeatService featService;
	
	public FredstorpSpeedRunner() {
		super(FeatName.FREDSTORP_SPEEDRUNNER);
	}

	@Override
	public void lowTimerTrigger(String characterName, String key, double timer) {
		if (!StatisticsKey.FREDSTORP_CLEAR_TIME.name().equals(key)) {
			return;
		}

		if (timer > 300.0f) {
			return;
		}

		featService.createFeat(characterName, getName());
	}
}
