package hello.jdbc.connection;

//abstract으로 객체 생성을 막아두었다.
public abstract class ConnectionConst {
    public static final String URL = "jdbc:h2:tcp://localhost/~/test";
    public static final String USERNAME ="sa";
    public static final String PASSWORD ="";
}
