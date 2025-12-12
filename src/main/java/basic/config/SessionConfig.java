package basic.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
//import org.redisson.Redisson;
//import org.redisson.api.RedissonClient;
//import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.events.SessionCreatedEvent;
import org.springframework.session.events.SessionDeletedEvent;
import org.springframework.session.events.SessionExpiredEvent;
import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.web.http.HeaderHttpSessionIdResolver;
import org.springframework.session.web.http.HttpSessionIdResolver;
import org.springframework.stereotype.Component;

@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 3600) // 1시간
@Slf4j
public class SessionConfig {


	@Value("${spring.data.redis.host}")
	private String redisHost;

	@Value("${spring.data.redis.port}")
	private int redisPort;

	@Value("${spring.data.redis.password}")
	private String redisPassword;

	@Bean
	public HttpSessionIdResolver httpSessionIdResolver() {
		// return HeaderHttpSessionIdResolver.xAuthToken(); // 헤더 기반 세션
		return new CookieHttpSessionIdResolver();
	}

	// 세션 이벤트 리스너
	@Component
	public static class SessionEventListener {

		@EventListener
		public void handleSessionCreated(SessionCreatedEvent event) {
			log.info("Session created: {}", event.getSessionId());
		}

		@EventListener
		public void handleSessionDeleted(SessionDeletedEvent event) {
			log.info("Session deleted: {}", event.getSessionId());
		}

		@EventListener
		public void handleSessionExpired(SessionExpiredEvent event) {
			log.info("Session expired: {}", event.getSessionId());
		}
	}

	// Redisson을 사용한 더 안전한 분산 락
	@Bean
	public RedissonClient redissonClient() {
		Config config = new Config();
		String redisUrl = String.format("redis://%s:%d", redisHost, redisPort);
//		config.useSingleServer().setAddress(redisUrl).setPassword(redisPassword.isEmpty() ? null : redisPassword).setDatabase(1); // 세션은 0, redisson은 1로 분리
		config.useSingleServer().setAddress(redisUrl).setDatabase(1); // 세션은 0, redisson은 1로 분리
		return Redisson.create(config);
	}

//    @Bean
//    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.registerModule(new JavaTimeModule());
//        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//        return new GenericJackson2JsonRedisSerializer(mapper);
//    }

	@Bean
	public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL,
				JsonTypeInfo.As.PROPERTY);

		return new GenericJackson2JsonRedisSerializer(mapper);
	}

}