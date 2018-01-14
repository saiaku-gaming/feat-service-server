package com.valhallagame.featserviceserver.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.valhallagame.common.rabbitmq.RabbitMQRouting;

@Configuration
public class RabbitMQConfig {

	@Bean
	public DirectExchange statisticsExchange() {
		return new DirectExchange(RabbitMQRouting.Exchange.STATISTICS.name());
	}
	
	@Bean
	public DirectExchange characterExchange() {
		return new DirectExchange(RabbitMQRouting.Exchange.CHARACTER.name());
	}
	
	@Bean
	public Queue featCharacterDeleteQueue() {
		return new Queue("featCharacterDeleteQueue");
	}

	@Bean
	public Queue featStatisticsIntCounterQueue() {
		return new Queue("featStatisticsIntCounterQueue");
	}

	
	@Bean
	public Binding bindingCharacterDeleted(DirectExchange characterExchange, Queue featCharacterDeleteQueue) {
		return BindingBuilder.bind(featCharacterDeleteQueue).to(characterExchange)
				.with(RabbitMQRouting.Character.DELETE);
	}
	
	@Bean
	public Binding bindingStatisticsIntCounter(DirectExchange statisticsExchange, Queue featStatisticsIntCounterQueue) {
		return BindingBuilder.bind(featStatisticsIntCounterQueue).to(statisticsExchange)
				.with(RabbitMQRouting.Statistics.INT_COUNTER);
	}

	@Bean
	public Jackson2JsonMessageConverter jacksonConverter() {
		return new Jackson2JsonMessageConverter();
	}

	@Bean
	public SimpleRabbitListenerContainerFactory containerFactory() {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		factory.setMessageConverter(jacksonConverter());
		return factory;
	}

}
