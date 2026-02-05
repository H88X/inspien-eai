
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import com.inspien.eai.config.MyBatisConfig;

public class MyBatisConnectionTest {

    public static void main(String[] args) {

        SqlSessionFactory factory = MyBatisConfig.getFactory();

        try (SqlSession session = factory.openSession()) {
            session.getConnection();
            System.out.println("MyBatis + Oracle DB 연결 성공");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
