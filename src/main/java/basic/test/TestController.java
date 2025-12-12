package basic.test;

import basic.entity.Item;
import basic.entity.Member;
import basic.repository.ItemRepository;
import basic.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {
    private final ItemRepository itemRepository;
    private final MemberRepository memberRepository;

    // 재고 초기화 API (테스트 전용)
    @PostMapping("/init")
    public String initTestData(
            @RequestParam Long itemId,
            @RequestParam int stock,
            @RequestParam Long memberId
    ) {
        Item item = itemRepository.findById(itemId).orElseThrow();
        item.setStockQuantity(stock);
        itemRepository.save(item);

        if (!memberRepository.existsById(memberId)) {
            memberRepository.save(Member.of("test-user", "temp"));
        }

        return String.format("Item %d stock set to %d", itemId, stock);
    }
}
