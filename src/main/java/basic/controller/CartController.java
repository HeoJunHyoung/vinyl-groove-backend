package basic.controller;

import basic.dto.UserSession;
import basic.entity.CartItem;
import basic.service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/add")
    public String addToCart(HttpSession session, @RequestParam Long itemId, @RequestParam int count) {
        UserSession userSession = getUserSession(session);
        cartService.addToCart(Long.valueOf(userSession.getUserId()), itemId, count);
        return "redirect:/items/list";
    }

    @GetMapping("/view")
    public String viewCart(HttpSession session, Model model) {
        UserSession userSession = getUserSession(session);
        List<CartItem> cartItems = cartService.getCart(Long.valueOf(userSession.getUserId()));
        model.addAttribute("cartItems", cartItems);
        return "cart/view";
    }

    @PostMapping("/delete")
    public String deleteCartItem(@RequestParam Long cartItemId) {
        cartService.removeCartItem(cartItemId);
        return "redirect:/home";
    }

    private UserSession getUserSession(HttpSession session) {
        UserSession userSession = (UserSession) session.getAttribute("userSession");
        if (userSession == null) {
            throw new IllegalStateException("로그인 정보가 없습니다.");
        }
        return userSession;
    }
}
