package github.jlyyxd.registry;

import github.jlyyxd.extension.SPI;
import java.net.InetSocketAddress;

@SPI
public interface ServiceRegistry {
    /**
     *
     * @param serviceName   服务名称
     * @param inetSocketAddress 服务地址
     */
    void registerService(String serviceName, InetSocketAddress inetSocketAddress);
}
