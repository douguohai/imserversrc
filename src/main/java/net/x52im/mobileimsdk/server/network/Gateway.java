/*
 * Copyright (C) 2020  即时通讯网(52im.net) & Jack Jiang.
 * The MobileIMSDK v5.x Project.
 * All rights reserved.
 *
 * > Github地址：https://github.com/JackJiang2011/MobileIMSDK
 * > 文档地址：  http://www.52im.net/forum-89-1.html
 * > 技术社区：  http://www.52im.net/
 * > 技术交流群：320837163 (http://www.52im.net/topic-qqgroup.html)
 * > 作者公众号：“【即时通讯技术圈】”，欢迎关注！
 * > 联系作者：  http://www.52im.net/thread-2792-1-1.html
 *
 * "即时通讯网(52im.net) - 即时通讯开发者社区!" 推荐开源工程。
 *
 * Gateway.java at 2020-8-22 16:00:59, code by Jack Jiang.
 */
package net.x52im.mobileimsdk.server.network;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import net.x52im.mobileimsdk.server.ServerCoreHandler;

/**
 * @author tianwen
 */
@Slf4j
public abstract class Gateway {

    /**
     * 设置socket 属性关键字
     */
    public final static String SOCKET_TYPE_IN_CHANNEL_ATTRIBUTE = "__socket_type__";

    /**
     * 设置属性
     */
    public static final AttributeKey<Integer> SOCKET_TYPE_IN_CHANNEL_ATTRIBUTE_ATTR = AttributeKey.newInstance(SOCKET_TYPE_IN_CHANNEL_ATTRIBUTE);

    /**
     * udp socket 类型
     */
    public static int SOCKET_TYPE_UDP = 0;
    /**
     * tcp socket 类型
     */
    public static int SOCKET_TYPE_TCP = 1;

    /**
     * websocket  socket 类型
     */
    public static int SOCKET_TYPE_WEBSOCKET = 2;


    /**
     * 是否支持udp
     */
    public static boolean SUPPORT_SOCKET_TYPE_UDP = true;

    /**
     * 是否支持tcp
     */
    public static boolean SUPPORT_SOCKET_TYPE_TCP = true;

    /**
     * 是否支持udp
     */
    public static boolean SUPPORT_SOCKET_TYPE_WEBSOCKET = true;


    /**
     * 服务初始化
     *
     * @param serverCoreHandler
     */
    public abstract void init(ServerCoreHandler serverCoreHandler);

    /**
     * 端口绑定
     *
     * @throws Exception
     */
    public abstract void bind() throws Exception;

    /**
     * 服务关闭
     */
    public abstract void shutdown();

    /**
     * 设置socket 属性
     *
     * @param c          socket
     * @param socketType socket 类型
     */
    public static void setSocketType(Channel c, int socketType) {
        c.attr(SOCKET_TYPE_IN_CHANNEL_ATTRIBUTE_ATTR).set(socketType);
    }

    /**
     * 删除socket属性
     *
     * @param c socket
     */
    public static void removeSocketType(Channel c) {
        c.attr(SOCKET_TYPE_IN_CHANNEL_ATTRIBUTE_ATTR).set(null);
    }

    /**
     * 获取socket属性
     *
     * @param c socket
     * @return 关键字
     */
    public static int getSocketType(Channel c) {
        Integer socketType = c.attr(SOCKET_TYPE_IN_CHANNEL_ATTRIBUTE_ATTR).get();
        if (socketType != null) {
            return socketType;
        }
        return -1;
    }

    public static boolean isTCPChannel(Channel c) {
        return (c != null && getSocketType(c) == SOCKET_TYPE_TCP);
    }

    public static boolean isUDPChannel(Channel c) {
        return (c != null && getSocketType(c) == SOCKET_TYPE_UDP);
    }

    public static boolean isWebSocketChannel(Channel c) {
        return (c != null && getSocketType(c) == SOCKET_TYPE_WEBSOCKET);
    }

    public static String $(Channel c) {
        return getGatewayFlag(c);
    }

    public static String getGatewayFlag(Channel c) {
        log.info(">>>>>> c.class=" + c.getClass().getName());
        if (isUDPChannel(c)) {
            return "udp";
        } else if (isTCPChannel(c)) {
            return "tcp";
        } else if (isWebSocketChannel(c)) {
            return "websocket";
        } else {
            return "unknown";
        }
    }
}
