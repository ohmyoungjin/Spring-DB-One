package hello.jdbc.service;

public interface MemberService {

    void accountTransFer(String fromId, String toId, int money);
    void bizLogic(String fromId, String toId, int money);

}
