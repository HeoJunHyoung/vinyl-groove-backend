package basic.test;

import basic.exception.NotEnoughStockException;
import basic.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/load-test")
@RequiredArgsConstructor
public class LoadTestController {
    private final OrderService orderService;

    // 단순화된 주문 API (부하 테스트 전용)
    @PostMapping("/order")
    public ResponseEntity<String> createOrder(
            @RequestParam Long memberId,
            @RequestParam Long itemId,
            @RequestParam int count
    ) {
        try {
            orderService.order(memberId, itemId, count);
            return ResponseEntity.ok("Order successful");
        } catch (NotEnoughStockException e) {
            return ResponseEntity.status(409).body("Out of stock");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}