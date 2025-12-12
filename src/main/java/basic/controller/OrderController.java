package basic.controller;

import basic.dto.ItemResponse;
import basic.dto.UserSession;
import basic.entity.CartItem;
import basic.entity.Item;
import basic.entity.Member;
import basic.entity.Order;
import basic.exception.NotEnoughStockException;
import basic.service.CartService;
import basic.service.ItemService;
import basic.service.MemberService;
import basic.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;


@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final ItemService itemService;
    private final CartService cartService;

    public OrderController(OrderService orderService, ItemService itemService, CartService cartService) {
        this.orderService = orderService;
        this.itemService = itemService;
        this.cartService = cartService;
    }

    @GetMapping("/new")
    public String createDto(Model model) {
        List<ItemResponse> items = itemService.findItems();
        model.addAttribute("items", items);
        return "orders/createOrderDto";
    }

    @PostMapping("/new")
    public String order(
            HttpSession session,  // 또는 @AuthenticationPrincipal 사용 가능
            @RequestParam Long itemId,
            @RequestParam int count, Model model, RedirectAttributes redirectAttributes) {

        UserSession userSession = getUserSession(session);

//        orderService.order(Long.valueOf(userSession.getUserId()), itemId, count);
//        return "redirect:/orders";
        try {
            orderService.order(Long.valueOf(userSession.getUserId()), itemId, count);
            return "redirect:/orders";
        } catch (NotEnoughStockException e) {
            // ❗ 에러 발생 시 다시 폼으로 이동하며 에러 메시지 전달
            ItemResponse item = itemService.findOne(itemId); // 상품 다시 불러옴
            model.addAttribute("item", item);
            model.addAttribute("errorMessage", "재고가 부족합니다. 현재 재고: " + item.getStockQuantity());
            return "items/detail"; // ← 상세 페이지 뷰 이름
        }
    }

    @PostMapping("/cart")
    public String orderFromCart(HttpSession session) {
        UserSession userSession = getUserSession(session);
        Long memberId = Long.valueOf(userSession.getUserId());

        List<CartItem> cartItems = cartService.getCart(memberId);

        orderService.orderFromCart(memberId, cartItems);

        cartService.clearCart(memberId);
        return "redirect:/orders";
    }


    @GetMapping
    public String orderList(HttpSession session, Model model) {
        UserSession userSession = getUserSession(session);

        List<Order> orders = orderService.findOrdersByMemberId(Long.valueOf(userSession.getUserId()));
        model.addAttribute("orders", orders);
        return "orders/orderList";
    }

    /**
     * 주문 취소:
     */
    @PostMapping("/{orderId}/cancel")
    public String cancelOrder(@PathVariable Long orderId) {
        orderService.cancelOrder(orderId);
        return "redirect:/orders";
    }

    private static UserSession getUserSession(HttpSession session) {
        UserSession userSession = (UserSession) session.getAttribute("userSession");
        if (userSession == null) {
            throw new IllegalStateException("로그인 정보가 없습니다.");
        }
        return userSession;
    }
}