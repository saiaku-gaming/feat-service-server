package com.valhallagame.featserviceserver.trigger;

import com.valhallagame.featserviceclient.message.FeatName;
import com.valhallagame.featserviceserver.service.FeatService;
import com.valhallagame.statisticsserviceclient.message.StatisticsKey;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@EqualsAndHashCode(callSuper = true)
public class Explorer extends FeatTrigger implements IntCounterTriggerable {
    @Autowired
    private FeatService featService;

    public Explorer() {
        super(FeatName.HJUO_EXPLORER);
    }

    @Override
    public void intCounterTrigger(String characterName, String key, int count) {
        if (!StatisticsKey.HJUO_CLEARED.name().equals(key)) {
            return;
        }

        if (count < 1) {
            return;
        }

        featService.createFeat(characterName, getName());
    }
}
