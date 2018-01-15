package com.valhallagame.featserviceserver.trigger;

import com.valhallagame.featserviceclient.message.FeatName;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public abstract class FeatTrigger {
	protected final FeatName name;
}
