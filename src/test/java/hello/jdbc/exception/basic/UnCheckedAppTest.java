package hello.jdbc.exception.basic;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
public class UnCheckedAppTest {

    @Test
    void unchecked() {
        Controller controller = new Controller();
        assertThatThrownBy(() -> controller.request())
                .isInstanceOf(Exception.class);
    }

    @Test
    void printEx() {
        Controller controller = new Controller();
        try {
            controller.request();
        } catch (Exception e) {
            //e.printStackTrace() 사용하게 되면 Sustem.out에 표출된다.
//            e.printStackTrace();
            log.info("ex", e);
        }

    }

    static class Controller {
        Service service = new Service();

        public void request(){
            service.logic();
        }
    }


    static class Service {
        Repository repository = new Repository();
        NetworkClient networkClient = new NetworkClient();

        public void logic(){
            repository.call();
            networkClient.call();
        }
    }

    static class NetworkClient {
        public void call()  {
            throw new RuntimeConnectException("ex");
        }
    }

    //리포지토리에서 체크 예외인 SQLException 이 발생하면 런타임 예외인 RuntimeSQLException 으로
    //전환해서 예외를 던진다.
    //참고로 이때 기존 예외를 포함해주어야 예외 출력시 스택 트레이스에서 기존 예외도 함께 확인할 수 있다.
    //기존 예외를 사용하려면 내가 지정한 RuntimeSQLException 생성자를 만들어줄 때
    //Throwable cause 를 파라미터로 받는 생성자를 만들어줘야 한다 !! 중요!
    static class Repository {
        public void call() {
            try {
                runSQL();
            } catch (SQLException e) {
                throw new RuntimeSQLException(e);
            }
        }

        public void runSQL() throws SQLException {
            throw new SQLException("ex");
        }
    }

    static class RuntimeConnectException extends RuntimeException {
        public RuntimeConnectException(String message) {
            super(message);
        }
    }

    static class RuntimeSQLException extends RuntimeException {
        public RuntimeSQLException(Throwable cause) {
            super(cause);
        }
    }
}
