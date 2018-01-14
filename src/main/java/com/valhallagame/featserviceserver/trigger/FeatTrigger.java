package com.valhallagame.featserviceserver.trigger;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public abstract class FeatTrigger {
	protected final String name;
}
