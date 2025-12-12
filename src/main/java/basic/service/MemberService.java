package basic.service;

import basic.dto.MemberDto;
import basic.dto.UserSession;
import basic.entity.Member;
import basic.repository.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.util.Optional;


@Service
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final UserSessionService userSessionService;

    public MemberService(MemberRepository memberRepository, UserSessionService userSessionService) {
        this.memberRepository = memberRepository;
        this.userSessionService = userSessionService;
    }

    // 회원가입
    @Transactional
    public Long join(MemberDto memberDto) {
        validateDuplicateMember(memberDto);
        Member member = Member.of(memberDto.getUsername(), memberDto.getPassword());
        memberRepository.save(member);
        return member.getId();
    }

    // 로그인
    public String login(String username, String password, HttpServletRequest request, Model model) {

        Optional<Member> findMember = memberRepository.findByUsername(username);

        if (findMember.isEmpty()) {
            model.addAttribute("loginError", "존재하지 않는 사용자입니다.");
            return "index";
        }

        Member member = findMember.get();
        if (!member.getPassword().equals(password)) {
            model.addAttribute("loginError", "비밀번호가 일치하지 않습니다.");
            return "index";
        }

        /**
         * 추가 사항
         */
        HttpSession session = request.getSession();
        UserSession userSession = UserSession.builder()
                .userId(member.getId().toString())
                .username(member.getUsername())
                .loginTime(LocalDateTime.now())
                .ipAddress(request.getRemoteAddr())
                .userAgent(request.getHeader("User-Agent"))
                .build();

        userSessionService.login(session, userSession);

//        session.setAttribute("loginMember", member);
        return "redirect:/home";
    }

    private void validateDuplicateMember(MemberDto memberDto) {
        memberRepository.findByUsername(memberDto.getUsername())
                .ifPresent(m -> {
                    throw new IllegalStateException("이미 존재하는 회원입니다.");
                });
    }

}
