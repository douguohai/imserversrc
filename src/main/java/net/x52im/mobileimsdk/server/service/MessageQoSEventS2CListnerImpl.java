package net.x52im.mobileimsdk.server.service;

import net.x52im.mobileimsdk.server.event.MessageQoSEventListenerS2C;
import net.x52im.mobileimsdk.server.protocal.Protocal;

import java.util.ArrayList;

/**
 * <p>
 * java类作用描述
 * </p>
 *
 * @author : tianwen
 * @version : 1.0
 * @date : 2023/5/31 11:49
 **/
public class MessageQoSEventS2CListnerImpl implements MessageQoSEventListenerS2C {

    /**
     * 消息无法完成实时送达的通知
     * @param lostMessages
     */
    @Override
    public void messagesLost(ArrayList<Protocal> lostMessages) {
        System.out.println("【QoS_S2C事件】收到系统的未实时送达事件通知，当前共有"
                + lostMessages.size() + "个包QoS保证机制结束，判定为【无法实时送达】！");
    }

    /**
     * 接收方已成功收到消息的通知
     * @param theFingerPrint
     */
    @Override
    public void messagesBeReceived(String theFingerPrint) {
        if (theFingerPrint != null) {
            System.out.println("【QoS_S2C事件】收到对方已收到消息事件的通知，fp=" + theFingerPrint);
        }
    }
}
