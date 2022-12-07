package hello.jdbc.service;

import hello.jdbc.repository.MemberRepositoryV3;
import hello.jdbc.domain.Member;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.SQLException;

import static hello.jdbc.connection.ConnectionConst.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 트랜잭션 - @Transactional AOP
 */
@Slf4j
//AOP 사용을 위해선 Spring container 사용을 해야한다.
@SpringBootTest
class MemberServiceV3_3Test {
    private static final String MEMBER_A = "memberA";
    private static final String MEMBER_B = "memberB";
    private static final String MEMBER_EX = "ex";
    @Autowired
    private MemberRepositoryV3 memberRepository;
    @Autowired
    private MemberServiceV3_3 memberService;

    //Spring container 설정
    //필요한 bean 주입
    @TestConfiguration
    static class TestConfig {
        @Bean
        DataSource datasource() {
            System.out.println("TestConfig.datasource");
            return new DriverManagerDataSource(URL, USERNAME, PASSWORD);
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            System.out.println("TestConfig.transactionManager");
            return new DataSourceTransactionManager(datasource());
        }

        @Bean
        MemberRepositoryV3 memberRepositoryV3() {
            System.out.println("TestConfig.memberRepositoryV3");
            return new MemberRepositoryV3(datasource());
        }

        @Bean
        MemberServiceV3_3 memberServiceV3_3() {
            System.out.println("TestConfig.memberServiceV3_3");
            return new MemberServiceV3_3(memberRepositoryV3());
        }
    }

    @AfterEach
    void after() throws SQLException {
        memberRepository.delete(MEMBER_A);
        memberRepository.delete(MEMBER_B);
        memberRepository.delete(MEMBER_EX);

    }

    @Test
    void AopCheck() {
        log.info("memberService class={}", memberService.getClass());
        // memberService class=class hello.jdbc.service.MemberServiceV3_3$$EnhancerBySpringCGLIB$$2765123b
        //Service 에는 @Transactional 적용돼있어서 proxy class로 나오게 된다.
        log.info("memberRepository class={}", memberRepository.getClass());
        // memberRepository class=class hello.jdbc.Repository.MemberRepositoryV3
        Assertions.assertThat(AopUtils.isAopProxy(memberService)).isTrue();
        Assertions.assertThat(AopUtils.isAopProxy(memberRepository)).isFalse();
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