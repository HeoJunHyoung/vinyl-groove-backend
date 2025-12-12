package basic.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CounterService {

    private final StringRedisTemplate stringRedisTemplate;
    //private final RedisTemplate<String, Object> redisTemplate;
    
    //증가
    public long increment(String key) {
    	//문자열 작업 opsForValue 사용
    	return stringRedisTemplate.opsForValue().increment(key);
    }
    
    //조회수 카운터
	
    public Long incrementViewCount(Long contentId) {
    	//key
    	String key = "view:count:" + contentId;
    	Long count = increment(key);
    	
    	if(count == 1) {
    		stringRedisTemplate.expire(key, 1, TimeUnit.DAYS); //TTL 1일
    	}
    	
    	return count;
    }
    
	/*
	 * 일일 방문자 
	 * 1. 키 설계 -> visit:<today> 
	 * 2. 중복 방지 -> set 사용
	 * 3. 일일 설정 -> 
	 */    
    
    public Long incrementDailyVisitor(String userId) {
    	String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
    	String key = "visitor:" + today;
    	
    	stringRedisTemplate.opsForSet().add(key, userId);
    	
    	LocalDateTime now = LocalDateTime.now();
    	LocalDateTime tommorow =LocalDate.now().plusDays(1).atStartOfDay();
    	//자정에 초기화 . 현재시간
    	Long ttl = ChronoUnit.SECONDS.between(now, tommorow); 
    	stringRedisTemplate.expire(key, ttl, TimeUnit.SECONDS);
    	
    	return stringRedisTemplate.opsForSet().size(key);
    	
    }
    
    
    
}
