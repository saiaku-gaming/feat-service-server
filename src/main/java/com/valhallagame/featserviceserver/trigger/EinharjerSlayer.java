package com.valhallagame.featserviceserver.trigger;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.valhallagame.common.RestResponse;
import com.valhallagame.featserviceclient.message.FeatName;
import com.valhallagame.featserviceserver.service.FeatService;
import com.valhallagame.statisticsserviceclient.message.StatisticsKey;
import com.valhallagame.wardrobeserviceclient.WardrobeServiceClient;
import com.valhallagame.wardrobeserviceclient.message.AddWardrobeItemParameter;
import com.valhallagame.wardrobeserviceclient.message.WardrobeItem;

import lombok.EqualsAndHashCode;

@Component
@EqualsAndHashCode(callSuper = true)
public class EinharjerSlayer extends FeatTrigger implements IntCounterTriggerable {

	private static final Logger logger = LoggerFactory.getLogger(EinharjerSlayer.class);

	@Autowired
	private FeatService featService;

	@Autowired
	private WardrobeServiceClient wardrobeServiceClient;

	public EinharjerSlayer() {
		super(FeatName.EINHARJER_SLAYER);
	}

	@Override
	public void intCounterTrigger(String characterName, String key, int count) {

		if (!StatisticsKey.EINHARJER_KILLED.name().equals(key)) {
			return;
		}

		if (count < 1) {
			return;
		}

		try {
			RestResponse<String> addWardrobeItem = wardrobeServiceClient
					.addWardrobeItem(new AddWardrobeItemParameter(characterName, WardrobeItem.MAIL_ARMOR));
			if (!addWardrobeItem.isOk()) {
				logger.error(addWardrobeItem.getErrorMessage());
			} else {
				String mess = addWardrobeItem.getResponse().orElse("");
				logger.debug(mess);
				featService.createFeat(characterName, getName());
			}
		} catch (IOException e) {
			logger.error("Wardrobe problem!", e);
		}
	}
}
