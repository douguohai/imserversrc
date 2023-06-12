package net.x52im.mobileimsdk.server.service;

import net.x52im.mobileimsdk.server.ServerLauncher;
import net.x52im.mobileimsdk.server.network.Gateway;
import net.x52im.mobileimsdk.server.network.GatewayTCP;
import net.x52im.mobileimsdk.server.network.GatewayUDP;
import net.x52im.mobileimsdk.server.qos.QoS4ReciveDaemonC2S;
import net.x52im.mobileimsdk.server.qos.QoS4SendDaemonS2C;
import net.x52im.mobileimsdk.server.utils.ServerToolKits;

import java.io.IOException;

/**
 * <p>
 * java类作用描述
 * </p>
 *
 * @author : tianwen
 * @version : 1.0
 * @date : 2023/5/31 11:50
 **/
public class ServerLauncherImpl extends ServerLauncher {
    /**
     * 静态类方法：进行一些全局配置设置。
     */
    static {
        // 设置MobileIMSDK服务端的UDP网络监听端口
        GatewayUDP.PORT = 7901;
        // 设置MobileIMSDK服务端的TCP网络监听端口
        GatewayTCP.PORT = 8901;

        // 设置MobileIMSDK服务端仅支持UDP协议
//         ServerLauncher.supportedGateways = Gateway.SUPPORT_UDP;
        // 设置MobileIMSDK服务端仅支持TCP协议
//        ServerLauncher.supportedGateways = Gateway.SUPPORT_TCP;
        // 设置MobileIMSDK服务端同时支持UDP、TCP两种协议

        // 开/关Demog日志的输出
        QoS4SendDaemonS2C.getInstance().setDebugable(true);
        QoS4ReciveDaemonC2S.getInstance().setDebugable(true);
        ServerLauncher.debug = true;

        // 与客户端协商一致的心跳频率模式设置
        ServerToolKits.setSenseModeUDP(ServerToolKits.SenseModeUDP.MODE_10S);
        ServerToolKits.setSenseModeTCP(ServerToolKits.SenseModeTCP.MODE_15S);

        // 关闭与Web端的消息互通桥接器（其实SDK中默认就是false）
        ServerLauncher.bridgeEnabled = false;
        // TODO 跨服桥接器MQ的URI（本参数只在ServerLauncher.bridgeEnabled为true时有意义）
//        BridgeProcessor.IMMQ_URI = "amqp://js:19844713@192.168.0.190";

        // 设置最大TCP帧内容长度（不设置则默认最大是 6 * 1024字节）
        GatewayTCP.TCP_FRAME_MAX_BODY_LENGTH = 60 * 1024;
    }

    /**
     * 实例构造方法。
     *
     * @throws IOException
     */
    public ServerLauncherImpl() throws IOException {
        super();
    }

    /**
     * 初始化消息处理事件监听者.
     */
    @Override
    protected void initListeners() {
        // ** 设置各种回调事件处理实现类
        this.setServerEventListener(new ServerEventListenerImpl());
        this.setServerMessageQoSEventListener(new MessageQoSEventS2CListnerImpl());
    }

    public static void main(String[] args) throws Exception {
        // 实例化后记得startup哦，单独startup()的目的是让调用者可以延迟决定何时真正启动IM服务
        final ServerLauncherImpl sli = new ServerLauncherImpl();

        // 启动MobileIMSDK服务端的Demo
        sli.startup();

        // 加一个钩子，确保在JVM退出时释放netty的资源
        Runtime.getRuntime().addShutdownHook(new Thread(() -> sli.shutdown()));
    }
}
