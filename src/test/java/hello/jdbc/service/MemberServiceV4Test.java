package hello.jdbc.service;

import hello.jdbc.repository.MemberRepository;
import hello.jdbc.repository.MemberRepositoryV4_1;
import hello.jdbc.domain.Member;
import hello.jdbc.repository.MemberRepositoryV4_2;
import hello.jdbc.repository.MemberRepositoryV5;
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

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 예외 누수 문제 해결
 * SQLException 제거
 *
 * MemberRepository interface 의존
 */
@Slf4j
//AOP 사용을 위해선 Spring container 사용을 해야한다.
@SpringBootTest
class MemberServiceV4Test {
    private static final String MEMBER_A = "memberA";
    private static final String MEMBER_B = "memberB";
    private static final String MEMBER_EX = "ex";
    //bean 주입
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private MemberService memberService;

    //Spring container 설정
    //필요한 bean 생성
    //Webconfig 같은 file
    @TestConfiguration
    static class TestConfig {

        private final DataSource dataSource;

        @Autowired
        public TestConfig(DataSource dataSource) {
            //생성자 주입으로 db info 값을 가져올 수 있다.
            //spring 에서 자동으로 bean으로 등록해서 반환해준다.
            //Transaction manager 같은 경우도 spring이 만들어준다.
            this.dataSource = dataSource;
        }

        @Bean
        MemberService memberService() {
            return new MemberServiceV4(memberRepository());
        }

        @Bean
        MemberRepository memberRepository() {
//            System.out.println("TestConfig.memberRepositoryV4_1");
//            return new MemberRepositoryV4_1(dataSource);
//            return new MemberRepositoryV4_2(dataSource);
            return new MemberRepositoryV5(dataSource);
        }

        @Bean
        MemberServiceV4 memberServiceV4() {
            System.out.println("TestConfig.memberServiceV4");
            return new MemberServiceV4(memberRepository());
        }
    }

    @AfterEach
    void after(){
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
    void accountTransFer(){
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
    void accountTransFerEx(){
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