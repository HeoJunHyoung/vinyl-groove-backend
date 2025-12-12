package basic.service;

import basic.entity.CartItem;
import basic.entity.Item;
import basic.entity.Member;
import basic.repository.CartItemRepository;
import basic.repository.ItemRepository;
import basic.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final MemberRepository memberRepository;
    private final ItemRepository itemRepository;

    public CartService(CartItemRepository cartItemRepository, MemberRepository memberRepository, ItemRepository itemRepository) {
        this.cartItemRepository = cartItemRepository;
        this.memberRepository = memberRepository;
        this.itemRepository = itemRepository;
    }

    public void addToCart(Long memberId, Long itemId, int count) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        Item item = itemRepository.findById(itemId).orElseThrow();

        Optional<CartItem> existing = cartItemRepository.findByMemberIdAndItemId(memberId, itemId);

        if (existing.isPresent()) {
            existing.get().addCount(count);
        } else {
            CartItem cartItem = CartItem.of(member, item, count);
            cartItemRepository.save(cartItem);
        }
    }

    public List<CartItem> getCart(Long memberId) {
        return cartItemRepository.findByMemberId(memberId);
    }

    public void removeCartItem(Long cartItemId) {
        cartItemRepository.deleteById(cartItemId);
    }

    public void clearCart(Long memberId) {
        List<CartItem> items = cartItemRepository.findByMemberId(memberId);
        cartItemRepository.deleteAll(items);
    }
}
