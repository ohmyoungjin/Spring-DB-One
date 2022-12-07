package hello.jdbc.exception.translator;

import hello.jdbc.domain.Member;
import hello.jdbc.repository.ex.MyDbException;
import hello.jdbc.repository.ex.MyDuplicateKeyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;

import static hello.jdbc.connection.ConnectionConst.*;

@Slf4j
public class ExTranslatorV2Test {

    Repository repository;
    Service service;

    @BeforeEach
    void init() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);
        repository = new Repository(dataSource);
        service = new Service(repository);
    }

    @Test
    void duplicateKeySave() {
        service.create("myId");
        service.create("myId");
    }

    @Slf4j
    @RequiredArgsConstructor
    static class Service {
        private final Repository repository;
        //V2로 새로 만들어서 SQLErrorCodeSQLExceptionTranslator 사용
        //Exception 처리하는게 조금 더 좋은게 있을 것 같은데 .. 아직은 잘 모르겠다 우선 중복나면 해당하는 Exception 처리하도록 변경.
        //이외의 catch도 class별로 따로 설정해줄수 있다 !
        public void create(String memberId) {
            try {
                //유저 등록
                repository.save(new Member(memberId, 0));
                log.info("saveId={}", memberId);
            } catch (DuplicateKeyException e) {
                    log.info("키 중복, 복구 시도!");
                    log.info("class={}", e.getClass());
                    String retryId = generateNewId(memberId);
                    log.info("retryId={}", retryId);
                    repository.save(new Member(retryId, 0));
            } catch (RuntimeException e) {
                log.info("e", e);
            }
        }

        private String generateNewId(String memberId) {
            return memberId + new Random().nextInt(10000);
        }
    }

//    @RequiredArgsConstructor
    static class Repository {
        private final DataSource dataSource;

        private final SQLExceptionTranslator exTranslator;

        public Repository(DataSource dataSource) {
            this.dataSource = dataSource;
            this.exTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
        }

    public Member save(Member member) {
            String sql = "insert into member(member_id, money) values(?,?)";
            Connection con = null;
            PreparedStatement pstmt = null;

            try {
                con = dataSource.getConnection();
                pstmt = con.prepareStatement(sql);
                pstmt.setString(1, member.getMemberId());
                pstmt.setInt(2, member.getMoney());
                pstmt.executeUpdate();
                return member;
            } catch (SQLException e) {
                //h2 db
                throw exTranslator.translate("save", sql, e);
            } finally {
                JdbcUtils.closeStatement(pstmt);
                JdbcUtils.closeConnection(con);
            }
        }

    }

}
