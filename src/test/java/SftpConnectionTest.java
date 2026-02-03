import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class SftpConnectionTest {

    private static final String HOST = "211.106.171.36";
    private static final int PORT = 20423;
    private static final String USER = "inspien";
    private static final String PASSWORD = "inspien";

    public static void main(String[] args) {

        System.out.println("SFTP 연결 테스트 시작");

        Session session = null;
        ChannelSftp channel = null;

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(USER, HOST, PORT);
            session.setPassword(PASSWORD);

            session.setConfig("StrictHostKeyChecking", "no");

            session.connect();
            System.out.println("1.SFTP 세션 연결 성공");

            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();
            System.out.println("2.SFTP 채널 연결 성공");

        } catch (Exception e) {
            System.err.println("3.SFTP 연결 실패");
            e.printStackTrace();
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
            System.out.println("4.SFTP 연결 테스트 종료");
        }
    }
}
