import io.netty.util.NettyRuntime;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class TestProgramme {
    public static void main(String[] args) throws UnknownHostException {
        System.out.println(NettyRuntime.availableProcessors());
        final String hostAddress = InetAddress.getLocalHost().getHostAddress();
        System.out.println(hostAddress);
        for (int i = 0; i < 1000; i++) {
            final String hostAddress1 = InetAddress.getLocalHost().getHostAddress();
            if (!hostAddress1.equals(hostAddress)) {
                System.out.println(i);
                System.out.println("hostAddress1 = " + hostAddress1);
                System.out.println("hostAddress = " + hostAddress);
                break;
            }
        }
    }
}
