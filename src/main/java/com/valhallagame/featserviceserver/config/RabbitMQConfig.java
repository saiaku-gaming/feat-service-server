package com.valhallagame.featserviceserver.config;

import com.valhallagame.common.rabbitmq.RabbitMQRouting;
import com.valhallagame.common.rabbitmq.RabbitSender;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Bean
	public DirectExchange statisticsExchange() {
		return new DirectExchange(RabbitMQRouting.Exchange.STATISTICS.name());
	}

	@Bean
	public DirectExchange characterExchange() {
		return new DirectExchange(RabbitMQRouting.Exchange.CHARACTER.name());
	}

	@Bean
	public DirectExchange featExchange() {
		return new DirectExchange(RabbitMQRouting.Exchange.FEAT.name());
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
	public Queue featStatisticsLowTimerQueue() {
		return new Queue("featStatisticsLowTimerQueue");
	}

	@Bean
	public Queue featStatisticsHighTimerQueue() {
		return new Queue("featStatisticsHighTimerQueue");
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
	public Binding bindingStatisticsLowTimer(DirectExchange statisticsExchange, Queue featStatisticsLowTimerQueue) {
		return BindingBuilder.bind(featStatisticsLowTimerQueue).to(statisticsExchange)
				.with(RabbitMQRouting.Statistics.LOW_TIMER);
	}

	@Bean
	public Binding bindingStatisticsHighTimer(DirectExchange statisticsExchange, Queue featStatisticsHighTimerQueue) {
		return BindingBuilder.bind(featStatisticsHighTimerQueue).to(statisticsExchange)
				.with(RabbitMQRouting.Statistics.HIGH_TIMER);
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

	@Bean
	public RabbitSender rabbitSender() {
		return new RabbitSender(rabbitTemplate);
	}
}
