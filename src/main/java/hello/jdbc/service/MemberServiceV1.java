package hello.jdbc.service;

import hello.jdbc.Repository.MemberRepositoryV1;
import hello.jdbc.domain.Member;
import lombok.RequiredArgsConstructor;

import java.sql.SQLException;

@RequiredArgsConstructor
public class MemberServiceV1 {

    private final MemberRepositoryV1 memberRepository;

    public void accountTransFer(String fromId, String toId, int money) throws SQLException {
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
