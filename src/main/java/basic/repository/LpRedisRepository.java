package basic.repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class LpRedisRepository {
	private final StringRedisTemplate redisTemplate;
	private static final String LP_SALES_KEY = "lp:sales";

	public void increaseSales(String key, Long lpId, int count) {

		redisTemplate.opsForZSet().incrementScore(key, String.valueOf(lpId), count);
	}

	// 상위 N개 조회
	public Set<TypedTuple<String>> getTopSales(int topN) {
		return redisTemplate.opsForZSet().reverseRangeWithScores(LP_SALES_KEY, 0, topN - 1);
	}
}