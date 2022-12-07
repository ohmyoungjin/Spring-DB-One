package hello.jdbc.service;

import hello.jdbc.repository.MemberRepositoryV2;
import hello.jdbc.domain.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 트랜잭션 - 파라미터 연동, 풀을 고려한 종료
 */
@Slf4j
@RequiredArgsConstructor
public class MemberServiceV2 {
    private final DataSource dataSource;
    private final MemberRepositoryV2 memberRepository;

    public void accountTransFer(String fromId, String toId, int money) throws SQLException {
        //트랜잭션 처리할 때는 같은 connection을 써야한다.
        Connection con = dataSource.getConnection();
        try {
            //트랜젝션 시작 AutoCommit 끈다.
            con.setAutoCommit(false);
            //비즈니스 로직
            //비즈니스 로직은 따로 빼서 보는 것이 가독성이 좋다.
            bizLogic(con, fromId, toId, money);
            //정보 저장 commit
            con.commit();
        } catch (Exception e) {
            //실패 시 롤백
            con.rollback();
            log.info("roll back !");
            throw new IllegalStateException(e);
        } finally {
            //트랜잭션 시작 하는 곳에서 connection 맺고 끊어줘야 한다.
            //connection 끊는 부분
            release(con);
        }

    }

    private void bizLogic(Connection con, String fromId, String toId, int money) throws SQLException {
        //입금자
        Member fromMember = memberRepository.findById(con, fromId);
        //받는자
        Member toMember = memberRepository.findById(con, toId);

        memberRepository.update(con, fromId, fromMember.getMoney() - money);
        //검증
        validation(toMember);
        memberRepository.update(con, toId, toMember.getMoney() + money);
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
