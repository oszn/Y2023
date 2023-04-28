package com.example.demo.component.mq;

import com.example.demo.pubdef.mo.MqMsgBodyMO;

/**
 * @param <T>
 */
public interface MqMsgProcessor<T extends MqMsgBodyMO> {
    MqMsgTypeEnum getMsgTypeEnum();
    MqSubMsgTypeEnum getSubMsgTypeEnum();
    Class<T> getBodyClazz();
    boolean process(T body, int version);
}
