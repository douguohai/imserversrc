package net.x52im.mobileimsdk.server.service;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import net.x52im.mobileimsdk.server.event.ServerEventListener;
import net.x52im.mobileimsdk.server.protocal.Protocal;

/**
 * <p>
 * java类作用描述
 * </p>
 *
 * @author : tianwen
 * @version : 1.0
 * @date : 2023/5/31 11:42
 **/
@Slf4j
public class ServerEventListenerImpl implements ServerEventListener {

    /**
     * 用户身份验证回调方法定义
     * 服务端的应用层可在本方法中实现用户登陆验证。详细请参见API文档说明。
     *
     * @param userId
     * @param token
     * @param extra
     * @param session
     * @return
     */
    @Override
    public int onUserLoginVerify(String userId, String token, String extra, Channel session) {
        log.info("正在调用回调方法：OnVerifyUserCallBack...");
        return 0;
    }

    // 用户登录验证成功后的回调方法定义
    // 服务端的应用层通常可在本方法中实现用户上线通知等。详细请参见API文档说明。
    @Override
    public void onUserLoginSucess(String userId, String extra, Channel session) {
        log.info("正在调用回调方法：OnUserLoginAction_CallBack...");
    }

    // 用户退出登录回调方法定义。
    // 服务端的应用层通常可在本方法中实现用户下线通知等。详细请参见API文档说明。
    @Override
    public void onUserLogout(String userId, Object obj, Channel session) {
        log.info("正在调用回调方法：OnUserLogoutAction_CallBack...");
    }

    // 通用数据回调方法定义（客户端发给服务端的（即接收user_id=0））
    // 上层通常可在本方法中实现如：添加好友请求等业务实现。详细请参见API文档说明。
    @Override
    public boolean onTransferMessage4C2S(Protocal p, Channel session) {
        log.info("收到了客户端" + p.getFrom() + "发给服务端的消息：str=" + p.getDataContent());
        return true;
    }

    // 通道数据回调函数定义（客户端发给客户端的（即接收user_id>0））。详细请参见API文档说明。
    // 上层通常可在本方法中实现用户聊天信息的收集，以便后期监控分析用户的行为等^_^。
    @Override
    public void onTransferMessage4C2C(Protocal p) {
        log.info("收到了客户端" + p.getFrom() + "发给客户端" + p.getTo() + "的消息：str=" + p.getDataContent());
    }

    // 通用数据实时发送失败后的回调函数定义（客户端发给客户端的（即接收user_id>0））
    // 开发者可在此方法中处理离线消息的保存等。详细请参见API文档说明。
    @Override
    public boolean onTransferMessage_RealTimeSendFaild(Protocal p) {
        log.info("客户端" + p.getFrom() + "发给客户端" + p.getTo() + "的消息：str=" + p.getDataContent()
                + "因实时发送没有成功，需要上层应用作离线处理哦，否则此消息将被丢弃.");
        return false;
    }
}

