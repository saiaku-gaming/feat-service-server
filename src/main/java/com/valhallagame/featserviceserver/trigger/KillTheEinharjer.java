package com.valhallagame.featserviceserver.trigger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.valhallagame.featserviceclient.message.FeatName;
import com.valhallagame.featserviceserver.service.FeatService;
import com.valhallagame.statisticsserviceclient.message.StatisticsKey;

import lombok.EqualsAndHashCode;

@Component
@EqualsAndHashCode(callSuper=true)
public class KillTheEinharjer extends FeatTrigger implements IntCounterTriggerable {

	@Autowired
	private FeatService featService;
	
	public KillTheEinharjer() {
		super(FeatName.KILL_THE_EINHARJER);
	}
	
	@Override
	public void intCounterTrigger(String characterName, String key, int count) {
		
		if(!StatisticsKey.EINHARJER_KILLED.name().equals(key)){
			return;
		}
		
		if(count < 1) {
			return;
		}
		
		featService.createFeat(characterName, getName());
	}
}
