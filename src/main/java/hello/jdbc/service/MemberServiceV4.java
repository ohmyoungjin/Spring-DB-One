package hello.jdbc.service;

import hello.jdbc.repository.MemberRepository;
import hello.jdbc.domain.Member;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예외 누수 문제 해결
 * SQLException 제거
 *
 * MemberRepository interface 의존
 */
@Slf4j
public class MemberServiceV4 implements MemberService{

    private final MemberRepository memberRepository;

    public MemberServiceV4(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional
    public void accountTransFer(String fromId, String toId, int money){
        bizLogic(fromId, toId, money);
    }

    @Override
    public void bizLogic(String fromId, String toId, int money){
        //입금자
        Member fromMember = memberRepository.findById(fromId);
        //받는자
        Member toMember = memberRepository.findById(toId);

        memberRepository.update(fromId, fromMember.getMoney() - money);
        //검증
        validation(toMember);
        memberRepository.update(toId, toMember.getMoney() + money);
    }


    private static void validation(Member toMember) {
        if (toMember.getMemberId().equals("ex")) {
            throw new IllegalStateException("이체중 예외 발생");
        }
    }


}
