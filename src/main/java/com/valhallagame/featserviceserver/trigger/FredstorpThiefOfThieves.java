package com.valhallagame.featserviceserver.trigger;

import org.springframework.beans.factory.annotation.Autowired;

import com.valhallagame.featserviceclient.message.FeatName;
import com.valhallagame.featserviceserver.service.FeatService;
import com.valhallagame.statisticsserviceclient.message.StatisticsKey;

public class FredstorpThiefOfThieves extends FeatTrigger implements IntCounterTriggerable{

	@Autowired
	private FeatService featService;
	
	public FredstorpThiefOfThieves() {
		super(FeatName.FREDSTORP_THIEF_OF_THIEVES);
	}

	@Override
	public void intCounterTrigger(String characterName, String key, int count) {
		if (!StatisticsKey.FREDSTORP_CLEARED.name().equals(key)) {
			return;
		}

		if (count < 1) {
			return;
		}

		featService.createFeat(characterName, getName());
	}
}
