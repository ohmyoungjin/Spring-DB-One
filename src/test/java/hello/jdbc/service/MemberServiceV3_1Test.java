package hello.jdbc.service;

import hello.jdbc.repository.MemberRepositoryV3;
import hello.jdbc.domain.Member;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.SQLException;

import static hello.jdbc.connection.ConnectionConst.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 트랜잭션 - 커넥션 파라미터 전달 방식 동기화
 * 단점 : 트랜잭션 처리를 위해 connection 파라미터로 넘겨서 동기화 해줘야 함.
 * jdbc exception 처리가 서비스단 까지 넘어오게 됨.
 * try catch 와 같은 구문이 반복해서 사용되며 소스가 지저분해진다.
 */
@Slf4j
class MemberServiceV3_1Test {
    private static final String MEMBER_A = "memberA";
    private static final String MEMBER_B = "memberB";
    private static final String MEMBER_EX = "ex";

    private MemberRepositoryV3 memberRepository;
    private MemberServiceV3_1 memberService;


    @BeforeEach
    void before() throws SQLException {
        //DB setting
        //DI setting
        DriverManagerDataSource dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);
        memberRepository= new MemberRepositoryV3(dataSource);
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
        memberService = new MemberServiceV3_1(transactionManager, memberRepository);
    }

    @AfterEach
    void after() throws SQLException {
        memberRepository.delete(MEMBER_A);
        memberRepository.delete(MEMBER_B);
        memberRepository.delete(MEMBER_EX);

    }
    @Test
    @DisplayName("정상 이체")
    void accountTransFer() throws SQLException {
        //given
        Member memberA = new Member(MEMBER_A, 10000);
        Member memberB = new Member(MEMBER_B, 10000);
        memberRepository.save(memberA);
        memberRepository.save(memberB);

        //when
        //memberA = > memberB 2000 입금
        log.info("START TX");
        memberService.accountTransFer(memberA.getMemberId(), memberB.getMemberId(), 2000);
        log.info("END TX");
        //than
        Member findMemberA = memberRepository.findById(memberA.getMemberId());
        Member findMemberB = memberRepository.findById(memberB.getMemberId());

        //검증
        assertThat(findMemberA.getMoney()).isEqualTo(8000);
        assertThat(findMemberB.getMoney()).isEqualTo(12000);
    }

    @Test
    @DisplayName("이체중 예외 발생")
    void accountTransFerEx() throws SQLException {
        //given
        Member memberA = new Member(MEMBER_A, 10000);
        Member memberEX = new Member(MEMBER_EX, 10000);
        memberRepository.save(memberA);
        memberRepository.save(memberEX);

        //when
        //memberA = > memberB 2000 입금
        assertThatThrownBy(() -> memberService.accountTransFer(memberA.getMemberId(), memberEX.getMemberId(), 2000))
                .isInstanceOf(IllegalStateException.class);

        //than
        Member findMemberA = memberRepository.findById(memberA.getMemberId());
        Member findMemberB = memberRepository.findById(memberEX.getMemberId());

        //검증
        assertThat(findMemberA.getMoney()).isEqualTo(10000);
        assertThat(findMemberB.getMoney()).isEqualTo(10000);
    }




}