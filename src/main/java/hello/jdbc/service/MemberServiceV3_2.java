package hello.jdbc.service;

import hello.jdbc.Repository.MemberRepositoryV3;
import hello.jdbc.domain.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 트랜잭션 - 트랜잭션 탬플릿
 */
@Slf4j
//@RequiredArgsConstructor
public class MemberServiceV3_2 {
//    private final PlatformTransactionManager PlatformTransactionManager;
    //TransactionTemplate 을 사용하려면 transactionManager 가 필요하다. 생성자에서
    //transactionManager 를 주입 받으면서 TransactionTemplate 을 생성했다.
    private final TransactionTemplate txTemplate;
    private final MemberRepositoryV3 memberRepository;

    public MemberServiceV3_2(PlatformTransactionManager transactionManager, MemberRepositoryV3 memberRepository) {
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.memberRepository = memberRepository;
    }

    public void accountTransFer(String fromId, String toId, int money) throws SQLException {
        //트랜잭션 시작
        //비즈니스 로직이 정상 수행되면 커밋한다.
        //언체크 예외가 발생하면 롤백한다. 그 외의 경우 커밋한다
        //해당 람다에서 체크 예외를 밖으로 던질 수 없기 때문에 언체크
        //예외로 바꾸어 던지도록 예외를 전환했다.
        txTemplate.executeWithoutResult((status) -> {
            //비즈니스 로직
            //비즈니스 로직은 따로 빼서 보는 것이 가독성이 좋다.
            try {
                bizLogic(fromId, toId, money);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private void bizLogic(String fromId, String toId, int money) throws SQLException {
        //입금자
        Member fromMember = memberRepository.findById(fromId);
        //받는자
        Member toMember = memberRepository.findById(toId);

        memberRepository.update(fromId, fromMember.getMoney() - money);
        //검증
        validation(toMember);
        memberRepository.update(toId, toMember.getMoney() + money);
    }

    private static void release(Connection con) {
        if (con != null) {
            try {
                //connection pool로 반환 할 때는 default 값인 true로 변경해줘야 한다.
                con.setAutoCommit(true);
                con.close();
            } catch (Exception e) {
                log.info("error", e);
            }
        }
    }

    private static void validation(Member toMember) {
        if (toMember.getMemberId().equals("ex")) {
            throw new IllegalStateException("이체중 예외 발생");
        }
    }


}
