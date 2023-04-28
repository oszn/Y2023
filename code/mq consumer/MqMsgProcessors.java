package com.example.demo.component.mq;

import com.alibaba.fastjson.JSONObject;
import com.example.demo.component.mq.faillistener.MqMsgConsumeFailedListeners;
import com.example.demo.pubdef.dto.ResultEnum;
import com.example.demo.pubdef.mo.MqMsgBodyMO;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class MqMsgProcessors {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private Map<String, MqMsgProcessor> processorMap = new HashMap<>();
    private Map<String,MqMsgProcessor> batchProcessMap=new HashMap<>();
    @Autowired
    private void init(Set<MqMsgProcessor> processorSet){
        for (MqMsgProcessor processor: processorSet){
            String key = getProcessorKey(processor.getMsgTypeEnum().type(), processor.getSubMsgTypeEnum().type());
            if (processorMap.containsKey(key)){
                throw new BusinessException(ResultEnum.MQ_MSG_LISTENER_CONFLICT);
            }
            processorMap.put(key, processor);
            if(processor.getMsgTypeEnum().priority()==4){
                batchProcessMap.put(key,processor);
            }
        }
    }
    @Autowired
    private MqMsgConsumeFailedListeners consumeFailedListeners;

    public boolean process(MqMsgMO msgMO, boolean ifBroadcast){
        log.info("received mq msg:{}", msgMO);
        if (msgMO.getMsgType() == null || msgMO.getVersion() == null
                || msgMO.getSubMsgType() == null || msgMO.getSubVersion() == null){
            log.error("received mq invalid msg:{}", msgMO);
            return true;
        }
        MqMsgTypeEnum msgTypeEnum = MqMsgTypeEnum.findEnum(msgMO.getMsgType());
        if (msgTypeEnum == null){
            return ifBroadcast;
        }
        MqSubMsgTypeEnum subMsgTypeEnum = MqSubMsgTypeEnum.findEnum(msgMO.getSubMsgType());
        if (subMsgTypeEnum == null){
            return ifBroadcast;
        }
        // 非广播消息要做版本兼容性校验
        if (!ifBroadcast && !isVersionValid(msgMO, msgTypeEnum, subMsgTypeEnum)){
            return false;
        }

        MqMsgProcessor processor = getProcessor(msgMO.getMsgType(), msgMO.getSubMsgType());
        int tryTimes = 0;
        do {
            try {
                ++tryTimes;
                return processor.process((MqMsgBodyMO) JSONObject.parseObject(msgMO.getBody(),
                        processor.getBodyClazz()), msgMO.getSubVersion());
            }catch (Exception e){
                logger.error("process mq msg:{}, tryTimes:{}, e:{}", msgMO, tryTimes, e);
            }
        }while (tryTimes < subMsgTypeEnum.maxTryTimes());

        // 走到这里, 说明一直没有消费成功
        consumeFailedListeners.onMsgConsumeFailed(msgMO);
        logger.error("process mq msg:{}, tryTimes:{} failed", msgMO, tryTimes);
        throw new BusinessException(ResultEnum.MQ_MSG_CONSUME_FAILED);
    }

    private MqMsgProcessor getProcessor(int msgType, int subMsgType){
        MqMsgProcessor processor = processorMap.get(getProcessorKey(msgType, subMsgType));
        if (processor == null){
            throw new BusinessException(ResultEnum.MQ_MSG_TYPE_NOT_EXIST);
        }
        int priority=MqMsgTypeEnum.findEnum(msgType).priority();
        if(priority==4){
            processor=batchProcessMap.get(getProcessorKey(msgType,subMsgType));
            if (processor == null){
                throw new BusinessException(ResultEnum.MQ_MSG_TYPE_NOT_EXIST);
            }
        }
        return processor;
    }

    private String getProcessorKey(int msgType, int subMsgType){
        return msgType + "/" + subMsgType;
    }

    public boolean isVersionValid(MqMsgMO msgMO, MqMsgTypeEnum msgTypeEnum, MqSubMsgTypeEnum subMsgTypeEnum) {
        return msgMO.getVersion() <= msgTypeEnum.version()
                && msgMO.getSubVersion() <= subMsgTypeEnum.version();
    }
}
