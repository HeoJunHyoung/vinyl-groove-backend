package basic.service;

import basic.dto.UserSession;
import jakarta.servlet.http.HttpSession;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSessionService {

    private final RedisTemplate<String, Object> redisTemplate;


    // 로그인 처리
    public void login(HttpSession session, UserSession userSession) {
        session.setAttribute("userSession", userSession);
        session.setAttribute("userId", userSession.getUserId());


        Enumeration<String> names = session.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            log.info("세션에 저장된 key: {}", name);
        }
        log.info("userSession id = {}", userSession.getUserId());
        log.info("userSession username = {}", userSession.getUsername());


        // 추가 정보를 Redis에 직접 저장
        String sessionKey = "spring:session:sessions:" + session.getId();
        redisTemplate.opsForHash().put(sessionKey, "loginTime", userSession.getLoginTime());
        redisTemplate.opsForHash().put(sessionKey, "ipAddress", userSession.getIpAddress());

        String customKey = "user:session:meta:" + session.getId();
        Map<String, Object> meta = Map.of(
                "loginTime", userSession.getLoginTime().toString(),
                "ipAddress", userSession.getIpAddress()
        );
        redisTemplate.opsForHash().putAll(customKey, meta);

        // TTL 세션과 동일 (SessionConfig)
        // @EnableRedisHttpSession(maxInactiveIntervalInSeconds = 3600)과 일치 시켜야 함.
        redisTemplate.expire(customKey, Duration.ofSeconds(3600));

        // 동시 로그인 제한
        limitConcurrentSessions(userSession.getUserId(), session.getId());
    }

    // 동시 로그인 제한 (최대 2개 세션)
    private void limitConcurrentSessions(String userId, String currentSessionId) {
        String userSessionsKey = "user:sessions:" + userId;

        // 현재 세션 추가
        redisTemplate.opsForZSet().add(
                userSessionsKey,
                currentSessionId,
                System.currentTimeMillis()
        );

        // 오래된 세션 제거
        Set<Object> sessions = redisTemplate.opsForZSet().range(userSessionsKey, 0, -1);
        if (sessions.size() > 2) {
            Set<Object> toRemove = redisTemplate.opsForZSet().range(userSessionsKey, 0, sessions.size() - 3);
            for (Object sessionId : toRemove) {
                // 세션 무효화
                redisTemplate.delete("spring:session:sessions:" + sessionId);
                redisTemplate.opsForZSet().remove(userSessionsKey, sessionId);
                log.info("Removed old session for user {}: {}", userId, sessionId);
            }
        }
    }

    // 활성 사용자 수 조회
    public long getActiveUserCount() {
        Set<String> sessionKeys = redisTemplate.keys("spring:session:sessions:*");
        return sessionKeys != null ? sessionKeys.size() : 0;
    }

    // 사용자별 세션 조회
    public List<Map<String, Object>> getUserSessions(String userId) {
        String userSessionsKey = "user:sessions:" + userId;
        Set<ZSetOperations.TypedTuple<Object>> sessions =
                redisTemplate.opsForZSet().rangeWithScores(userSessionsKey, 0, -1);

        List<Map<String, Object>> result = new ArrayList<>();
        for (ZSetOperations.TypedTuple<Object> session : sessions) {
            String sessionId = session.getValue().toString();
            Map<String, Object> sessionInfo = new HashMap<>();
            sessionInfo.put("sessionId", sessionId);
            sessionInfo.put("loginTime", new Date(session.getScore().longValue()));

            // 세션 상세 정보 조회
            String sessionKey = "spring:session:sessions:" + sessionId;
            Map<Object, Object> sessionData = redisTemplate.opsForHash().entries(sessionKey);
            sessionInfo.put("lastAccess", sessionData.get("lastAccessedTime"));
            sessionInfo.put("ipAddress", sessionData.get("ipAddress"));

            result.add(sessionInfo);
        }

        return result;
    }
}