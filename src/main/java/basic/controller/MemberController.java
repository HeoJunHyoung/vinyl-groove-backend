package basic.controller;

import basic.dto.MemberDto;
import basic.dto.UserSession;
import basic.service.MemberService;
import basic.service.UserSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@Slf4j
public class MemberController {

	private final MemberService memberService;
	private final UserSessionService userSessionService;

	public MemberController(MemberService memberService, UserSessionService userSessionService) {
		this.memberService = memberService;
		this.userSessionService = userSessionService;
	}

	@GetMapping("/")
	public String mainPage() {
		return "index";
	}

	// 회원가입 처리
	@GetMapping("/member/signup")
	public String signUp(Model model) {
		model.addAttribute("memberDto", new MemberDto());
		return "createMemberDto";
	}

	@PostMapping("/member/signup")
	public String signUp(@ModelAttribute MemberDto memberDto, Model model) {
		try {
			memberService.join(memberDto);
			return "redirect:/";
		} catch (IllegalStateException e) {
			model.addAttribute("signupError", e.getMessage());
			return "index";
		}
	}

	// 로그인 처리
	@PostMapping("/login")
	public String login(@RequestParam String username, @RequestParam String password, Model model,
			HttpServletRequest request) {
		return memberService.login(username, password, request, model);
	}

	@GetMapping("/home")
	public String homePage(HttpSession session, Model model) {

		UserSession userSession = (UserSession) session.getAttribute("userSession");

		if (userSession == null) {
			log.info("세션이 존재하지 않습니다.");
			return "redirect:/";
		}
		model.addAttribute("username", userSession.getUsername());
		return "home";
	}

	@GetMapping("/logout")
	public String logout(HttpServletRequest request, HttpSession session) {
		UserSession userSession = (UserSession) session.getAttribute("userSession");
		if (userSession != null) {
			log.info("로그아웃: 사용자 [{}] 세션 종료", userSession.getUsername());
		} else {
			log.info("로그아웃 요청, 하지만 세션이 없습니다.");
		}
		session.invalidate();
		return "redirect:/";
	}
}
