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
 * Protocal.java at 2020-8-22 16:00:59, code by Jack Jiang.
 */
package net.x52im.mobileimsdk.server.protocal;

import com.google.gson.Gson;

import java.util.UUID;

public class Protocal {

    /**
     * 是否来自跨服务器的消息，true表示是、否则不是。
     */
    protected boolean bridge = false;

    /**
     * 协议类型。
     */
    protected int type = 0;

    /**
     * 传输的数据详情
     */
    protected String dataContent = null;

    /**
     * 消息发出方的id
     * 说明：为“-1”表示未设定、为“0”表示来自Server。
     */
    protected String from = "-1";

    /**
     * 消息接收方的id（当用户退出时，此值可不设置）
     * 说明：为“-1”表示未设定、为“0”表示发给Server。
     */
    protected String to = "-1";

    /**
     * 用于QoS消息包的质量保证时作为消息的指纹特征码,这里用uuid来实现的
     */
    protected String fp = null;

    /**
     * true表示本包需要进行QoS质量保证，否则不需要.
     */
    protected boolean QoS = false;

    /**
     * 应用层专用字段——用于应用层存放聊天、推送等场景下的消息类型。
     */
    protected int typeu = -1;

    /**
     * 本字段仅用于客户端QoS时：表示丢包重试次数
     */
    protected transient int retryCount = 0;

    public Protocal(int type, String dataContent, String from, String to) {
        this(type, dataContent, from, to, -1);
    }

    public Protocal(int type, String dataContent, String from, String to, int typeu) {
        this(type, dataContent, from, to, false, null, typeu);
    }

    public Protocal(int type, String dataContent, String from, String to, boolean QoS, String fingerPrint) {
        this(type, dataContent, from, to, QoS, fingerPrint, -1);
    }

    public Protocal(int type, String dataContent, String from, String to, boolean QoS, String fingerPrint, int typeu) {
        this.type = type;
        this.dataContent = dataContent;
        this.from = from;
        this.to = to;
        this.QoS = QoS;
        this.typeu = typeu;

        if (QoS && fingerPrint == null) {
            fp = Protocal.genFingerPrint();
        } else {
            fp = fingerPrint;
        }
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getDataContent() {
        return this.dataContent;
    }

    public void setDataContent(String dataContent) {
        this.dataContent = dataContent;
    }

    public String getFrom() {
        return this.from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return this.to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getFp() {
        return this.fp;
    }

    public int getRetryCount() {
        return this.retryCount;
    }

    public void increaseRetryCount() {
        this.retryCount += 1;
    }

    public boolean isQoS() {
        return QoS;
    }

    public void setQoS(boolean qoS) {
        this.QoS = qoS;
    }

    public boolean isBridge() {
        return bridge;
    }

    public void setBridge(boolean bridge) {
        this.bridge = bridge;
    }

    public int getTypeu() {
        return typeu;
    }

    public void setTypeu(int typeu) {
        this.typeu = typeu;
    }

    public String toGsonString() {
        return new Gson().toJson(this);
    }

    public byte[] toBytes() {
        return CharsetHelper.getBytes(toGsonString());
    }

    @Override
    public Object clone() {
        Protocal cloneP = new Protocal(this.getType()
                , this.getDataContent(), this.getFrom(), this.getTo(), this.isQoS(), this.getFp());
        // since 3.0
        cloneP.setBridge(this.bridge);
        // since 3.0
        cloneP.setTypeu(this.typeu);
        return cloneP;
    }

    public static String genFingerPrint() {
        return UUID.randomUUID().toString();
    }
}
