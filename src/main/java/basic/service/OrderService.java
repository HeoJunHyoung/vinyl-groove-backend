package basic.service;

import basic.entity.*;
import basic.cachedto.OrderCacheDto;
import basic.entity.Item;
import basic.entity.Member;
import basic.entity.Order;
import basic.entity.OrderItem;
import basic.exception.NotEnoughStockException;
import basic.repository.ItemRepository;
import basic.repository.LpRedisRepository;
import basic.repository.MemberRepository;
import basic.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {

	private final MemberRepository memberRepository;
	private final OrderRepository orderRepository;
	private final ItemRepository itemRepository;
	private final LpRedisRepository lpRedisRepository;
	private final RedisTemplate<String, Object> redisTemplate;
	private final RedissonClient redissonClient;


//	/** 주문 */
//	@Transactional
//	public Long order(Long memberId, Long itemId, int count) {
//		// 엔티티 조회
//		Member findMember = memberRepository.findById(memberId)
//				.orElseThrow(() -> new IllegalStateException("존재하지 않는 회원입니다."));
//		Item findItem = itemRepository.findById(itemId).orElseThrow(() -> new IllegalStateException("존재하지 않는 상품입니다."));
//		// 주문상품 생성
//		OrderItem orderItem = OrderItem.createOrderItem(findItem, findItem.getPrice(), count);
//		// 주문 생성
//		Order order = Order.createOrder(findMember, orderItem);
//		// 주문 저장
//		Order savedOrder = orderRepository.save(order);
//		// 캐시 저장
//		String key = "order:list:member:" + memberId;
//		// 기존 주문 목록 가져오기 (없으면 새로)
//		List<OrderCacheDto> cachedList = (List<OrderCacheDto>) redisTemplate.opsForValue().get(key);
//		if (cachedList == null) {
//			cachedList = new ArrayList<>();
//		}
//		OrderCacheDto cacheDto = new OrderCacheDto(savedOrder.getId(), savedOrder.getMember().getId(),
//				orderItem.getItem().getId(), orderItem.getCount(), orderItem.getTotalPrice());
//
//		if (cachedList.size() >= 10) { // 최근 주문 10개 유지
//			cachedList.remove(0);
//		}
//
//		cachedList.add(cacheDto);
//
//		// String key = ODER_CACHE_KEY + savedOrder.getId();
//		redisTemplate.opsForValue().set(key, cachedList, Duration.ofDays(1));
//
//				// LP 판매량 증가
//		lpRedisRepository.increaseSales("lp:sales", itemId, count);
//		return order.getId();
//	}

	/**
	 * order
	 * ㄴ Redisson 분산 락 버전
	 * ㄴ redis에서 monitor했을 때, 락 흭득과 해제 시퀀스 로그 찍히고 pub/sub 통신으로 락 해제 시에 다른 대기 중인 스레드에 통신 수행
	 */
	@Transactional
	public Long order(Long memberId, Long itemId, int count) {
		// 락 키 생성 (아이템별로 락을 걸기 위해 아이템 ID를 키로 사용)
		String lockKey = "order:item:" + itemId;
		RLock lock = redissonClient.getLock(lockKey);

		try {
			// 락 획득 시도 (최대 5초 대기, 락 획득 후 10초 유지) - 일단 이렇게 해두고 나중에 수정해야 할 듯
			boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);

			if (!isLocked) {
				throw new IllegalStateException("잠시 후 다시 시도해주세요.");
			}

			try {
				// 엔티티 조회
				Member findMember = memberRepository.findById(memberId)
						.orElseThrow(() -> new IllegalStateException("존재하지 않는 회원입니다."));
				Item findItem = itemRepository.findById(itemId)
						.orElseThrow(() -> new IllegalStateException("존재하지 않는 상품입니다."));

				// 재고 확인
				if (findItem.getStockQuantity() < count) {
					throw new NotEnoughStockException("재고가 부족합니다.");
				}

				// 주문상품 생성
				OrderItem orderItem = OrderItem.createOrderItem(findItem, findItem.getPrice(), count);

				// 주문 생성
				Order order = Order.createOrder(findMember, orderItem);

				// 주문 저장
				Order savedOrder = orderRepository.save(order);

				// 캐시 업데이트
				updateOrderCache(memberId, savedOrder, orderItem);

				// LP 판매량 증가
				lpRedisRepository.increaseSales("lp:sales", itemId, count);

				return savedOrder.getId();
			} finally {
				// 락 해제
				if (lock.isHeldByCurrentThread()) {
					lock.unlock();
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("주문 처리 중 오류가 발생했습니다.", e);
		}
	}

	private void updateOrderCache(Long memberId, Order savedOrder, OrderItem orderItem) {
		String key = "order:list:member:" + memberId;
		List<OrderCacheDto> cachedList = (List<OrderCacheDto>) redisTemplate.opsForValue().get(key);

		if (cachedList == null) {
			cachedList = new ArrayList<>();
		}

		OrderCacheDto cacheDto = new OrderCacheDto(
				savedOrder.getId(),
				savedOrder.getMember().getId(),
				orderItem.getItem().getId(),
				orderItem.getCount(),
				orderItem.getTotalPrice()
		);

		if (cachedList.size() >= 10) {
			cachedList.remove(0);
		}

		cachedList.add(cacheDto);
		redisTemplate.opsForValue().set(key, cachedList, Duration.ofDays(1));
	}


    /** 장바구니 주문 */
    @Transactional
    public void orderFromCart(Long memberId, List<CartItem> cartItems) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 회원입니다."));

        List<OrderItem> orderItems = cartItems.stream()
                .map(cartItem -> {
                    Item item = itemRepository.findById(cartItem.getItem().getId())
                            .orElseThrow(() -> new IllegalStateException("존재하지 않는 상품입니다."));
                    return OrderItem.createOrderItem(item, item.getPrice(), cartItem.getCount());
                })
                .collect(Collectors.toList());


        Order order = Order.createOrder(member, orderItems.toArray(new OrderItem[0]));

        orderRepository.save(order);

    }


	/** 주문 취소 */
	@Transactional
	public void cancelOrder(Long orderId) {
		// 주문 엔티티 조회
		Order order = orderRepository.findById(orderId).orElseThrow(() -> new IllegalStateException("존재하지 않는 주문입니다."));
		order.cancel();
	}

	public List<Order> findOrdersByMemberId(Long memberId) {

		// 캐시에서 먼저 조회 후 없으면 db조회
//		 String key = "order:list:member:" + memberId;
//		 List<OrderCacheDto> cachedList = (List<OrderCacheDto>) redisTemplate.opsForValue().get(key);
//		 if (cachedList == null) { return Collections.emptyList(); }

		return orderRepository.findByMemberId(memberId);
	}

}