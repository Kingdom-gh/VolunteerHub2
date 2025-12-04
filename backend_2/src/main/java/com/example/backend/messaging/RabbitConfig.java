package com.example.backend.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;
import org.springframework.retry.interceptor.MethodInvocationRecoverer;
import com.example.backend.messaging.VolunteerRequestMessage;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "volunteer.request.exchange";
    public static final String ROUTING_KEY = "volunteer.request.created";
    public static final String QUEUE = "volunteer.request.queue";
    // Retry / DLQ configuration
    public static final String RETRY_EXCHANGE = "volunteer.request.retry.exchange";
    public static final String RETRY_ROUTING_KEY_1 = "volunteer.request.retry.1";
    public static final String RETRY_ROUTING_KEY_2 = "volunteer.request.retry.2";
    public static final String RETRY_QUEUE_1 = "volunteer.request.retry.queue.1"; // 5s
    public static final String RETRY_QUEUE_2 = "volunteer.request.retry.queue.2"; // 30s
    public static final String DLQ = "volunteer.request.dlq";
    public static final String DLX = "volunteer.request.dlx";

    @Bean
    public TopicExchange volunteerRequestExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue volunteerRequestQueue() {
        // Main queue: dead-letter to retry exchange -> retry queue 1
        var args = new java.util.HashMap<String, Object>();
        args.put("x-dead-letter-exchange", RETRY_EXCHANGE);
        args.put("x-dead-letter-routing-key", RETRY_ROUTING_KEY_1);
        return new Queue(QUEUE, true, false, false, args);
    }

    @Bean
    public TopicExchange retryExchange() {
        return new TopicExchange(RETRY_EXCHANGE, true, false);
    }

    @Bean
    public Queue retryQueue1() {
        // TTL 5s, then dead-letter to retry.exchange with routing key RETRY_ROUTING_KEY_2
        var args = new java.util.HashMap<String, Object>();
        args.put("x-message-ttl", 5000); // 5 seconds
        args.put("x-dead-letter-exchange", RETRY_EXCHANGE);
        args.put("x-dead-letter-routing-key", RETRY_ROUTING_KEY_2);
        return new Queue(RETRY_QUEUE_1, true, false, false, args);
    }

    @Bean
    public Binding retry1Binding(Queue retryQueue1, TopicExchange retryExchange) {
        return BindingBuilder.bind(retryQueue1).to(retryExchange).with(RETRY_ROUTING_KEY_1);
    }

    @Bean
    public Queue retryQueue2() {
        // TTL 30s, then dead-letter back to main exchange for final retry
        var args = new java.util.HashMap<String, Object>();
        args.put("x-message-ttl", 30000); // 30 seconds
        args.put("x-dead-letter-exchange", EXCHANGE);
        args.put("x-dead-letter-routing-key", ROUTING_KEY);
        return new Queue(RETRY_QUEUE_2, true, false, false, args);
    }

    @Bean
    public Binding retry2Binding(Queue retryQueue2, TopicExchange retryExchange) {
        return BindingBuilder.bind(retryQueue2).to(retryExchange).with(RETRY_ROUTING_KEY_2);
    }

    @Bean
    public TopicExchange dlxExchange() {
        return new TopicExchange(DLX, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return new Queue(DLQ, true);
    }

    @Bean
    public Binding dlqBinding(Queue deadLetterQueue, TopicExchange dlxExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(dlxExchange).with("#");
    }

    @Bean
    public Binding volunteerRequestBinding(Queue volunteerRequestQueue, TopicExchange volunteerRequestExchange) {
        return BindingBuilder.bind(volunteerRequestQueue).to(volunteerRequestExchange).with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            RabbitTemplate rabbitTemplate,
            MessageConverter jsonMessageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);

        // Retry interceptor: maxAttempts = 3, exponential backoff (initial 500ms, multiplier 2.0, max 5s)
        RetryOperationsInterceptor retryInterceptor = RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(500, 2.0, 5000)
                .recoverer(new MethodInvocationRecoverer<Object>() {
                    @Override
                    public Object recover(Object[] args, Throwable cause) {
                        // try to find VolunteerRequestMessage argument and republish to DLX
                        if (args != null) {
                            for (Object a : args) {
                                if (a instanceof VolunteerRequestMessage) {
                                    try {
                                        rabbitTemplate.convertAndSend(DLX, ROUTING_KEY, a);
                                    } catch (Exception e) {
                                        // swallow - nothing we can do here
                                    }
                                    break;
                                }
                            }
                        }
                        return null;
                    }
                })
                .build();

        factory.setAdviceChain(retryInterceptor);
        return factory;
    }
}
