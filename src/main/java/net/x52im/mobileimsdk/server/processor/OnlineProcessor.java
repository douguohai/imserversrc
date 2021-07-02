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

    private static final Logger logger = LoggerFactory.getLogger(OnlineProcessor.class);

    private static OnlineProcessor instance = null;

    /**
     * 线上用户集合
     */
    private final ConcurrentMap<String, Channel> onlineSessions = new ConcurrentHashMap<>();

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
     * @param userId
     * @param session
     */
    public void putUser(String userId, Channel session) {
        if (onlineSessions.containsKey(userId)) {
            logger.debug("[IMCORE-{}]【注意】用户id={}已经在在线列表中了，session也是同一个吗？{}", Gateway.$(session), userId, (onlineSessions.get(userId).hashCode() == session.hashCode()));
            // TODO 同一账号的重复登陆情况可在此展开处理逻辑
        }
        onlineSessions.put(userId, session);
        printOnline();// just for debug
    }

    /**
     * 输出单机下的用户列表
     */
    public void printOnline() {
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
     * @param userId
     * @return
     */
    public boolean removeUser(String userId) {
        synchronized (onlineSessions) {
            if (!onlineSessions.containsKey(userId)) {
                logger.warn("[IMCORE]！用户id={}不存在在线列表中，本次removeUser没有继续.", userId);
                printOnline();// just for debug
                return false;
            } else {
                return (onlineSessions.remove(userId) != null);
            }
        }
    }

    /**
     * 根据用户id，获取对应的netty链接
     *
     * @param userId
     * @return
     */
    public Channel getOnlineSession(String userId) {
        if (userId == null) {
            logger.warn("[IMCORE][CAUTION] getOnlineSession时，作为key的userId == null.");
            return null;
        }

        return onlineSessions.get(userId);
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
            if (attr != null) {
                return (String) attr;
            }
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
