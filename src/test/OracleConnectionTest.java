
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class OracleConnectionTest {

    private static final String URL = "jdbc:oracle:thin:@211.106.171.36:11527:POS";
    private static final String USER = "APPLICANT";
    private static final String PASSWORD = "inspien00";

    public static void main(String[] args) {

        System.out.println("1.Oracle DB 연결 테스트 시작");

        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");

            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
                System.out.println("2.DB 연결 성공");

                // 기본 연결 테스트
                PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM DUAL");
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    System.out.println("3.기본연결 확인 완료");
                }

                // ORDER_TB 존재 여부 확인
                String ORDER_TB_sql = "SELECT COUNT(1) FROM ORDER_TB";
                ps = conn.prepareStatement(ORDER_TB_sql);
                rs = ps.executeQuery();

                if (rs.next()) {
                    System.out.println("4.ORDER_TB 조회 성공 (COUNT = " + rs.getInt(1) + ")");
                }

                // SHIPMENT_TB 존재 여부 확인
                String SHIPMENT_TB_sql = "SELECT COUNT(1) FROM SHIPMENT_TB";
                ps = conn.prepareStatement(SHIPMENT_TB_sql);
                rs = ps.executeQuery();

                if (rs.next()) {
                    System.out.println("5.SHIPMENT_TB 조회 성공 (COUNT = " + rs.getInt(1) + ")");
                }

            }

        } catch (Exception e) {
            System.err.println("DB 연결 실패");
            e.printStackTrace();
        }
    }
}