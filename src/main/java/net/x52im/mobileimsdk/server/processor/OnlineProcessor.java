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
 * OnlineProcessor.java at 2020-8-22 16:00:59, code by Jack Jiang.
 */
package net.x52im.mobileimsdk.server.processor;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import net.x52im.mobileimsdk.server.network.Gateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 在线用户处理
 */
public class OnlineProcessor {

    public final static String USER_ID_IN_SESSION_ATTRIBUTE = "__user_id__";

    /**
     * netty 中channel 的属性对象
     */
    public static final AttributeKey<String> USER_ID_IN_SESSION_ATTRIBUTE_ATTR = AttributeKey.newInstance(USER_ID_IN_SESSION_ATTRIBUTE);

    public static boolean DEBUG = false;

    private static Logger logger = LoggerFactory.getLogger(OnlineProcessor.class);

    private static OnlineProcessor instance = null;

    /**
     * 线上用户集合
     */
    private ConcurrentMap<String, Channel> onlineSessions = new ConcurrentHashMap<String, Channel>();

    /**
     * 单例
     *
     * @return
     */
    public static OnlineProcessor getInstance() {
        if (instance == null) {
            instance = new OnlineProcessor();
        }
        return instance;
    }

    private OnlineProcessor() {
    }

    /**
     * 新增用户
     *
     * @param user_id
     * @param session
     */
    public void putUser(String user_id, Channel session) {
        if (onlineSessions.containsKey(user_id)) {
            logger.debug("[IMCORE-{}]【注意】用户id={}已经在在线列表中了，session也是同一个吗？{}", Gateway.$(session), user_id, (onlineSessions.get(user_id).hashCode() == session.hashCode()));
            // TODO 同一账号的重复登陆情况可在此展开处理逻辑
        }
        onlineSessions.put(user_id, session);
        __printOnline();// just for debug
    }

    /**
     * 输出单机下的用户列表
     */
    public void __printOnline() {
        logger.debug("【@】当前在线用户共(" + onlineSessions.size() + ")人------------------->");
        if (DEBUG) {
            for (String key : onlineSessions.keySet()) {
                logger.debug("      > user_id=" + key + ",session=" + onlineSessions.get(key).remoteAddress());
            }
        }
    }

    /**
     * 根据用户id删除用户
     *
     * @param user_id
     * @return
     */
    public boolean removeUser(String user_id) {
        synchronized (onlineSessions) {
            if (!onlineSessions.containsKey(user_id)) {
                logger.warn("[IMCORE]！用户id={}不存在在线列表中，本次removeUser没有继续.", user_id);
                __printOnline();// just for debug
                return false;
            } else {
                return (onlineSessions.remove(user_id) != null);
            }
        }
    }

    /**
     * 根据用户id，获取对应的netty链接
     *
     * @param user_id
     * @return
     */
    public Channel getOnlineSession(String user_id) {
        if (user_id == null) {
            logger.warn("[IMCORE][CAUTION] getOnlineSession时，作为key的user_id== null.");
            return null;
        }

        return onlineSessions.get(user_id);
    }


    public ConcurrentMap<String, Channel> getOnlineSessions() {
        return onlineSessions;
    }

    /**
     * 判断当前用户是否登陆
     *
     * @param session
     * @return
     */
    public static boolean isLogined(Channel session) {
        return session != null && getUserIdFromSession(session) != null;
    }

    /**
     * 从session中获取用户id
     *
     * @param session
     * @return
     */
    public static String getUserIdFromSession(Channel session) {
        Object attr = null;
        if (session != null) {
            attr = session.attr(USER_ID_IN_SESSION_ATTRIBUTE_ATTR).get();
            if (attr != null)
                return (String) attr;
        }
        return null;
    }

    /**
     * 判断当前用户是否在线
     *
     * @param userId
     * @return
     */
    public static boolean isOnline(String userId) {
        return OnlineProcessor.getInstance().getOnlineSession(userId) != null;
    }
}
