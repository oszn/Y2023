package com.example.demo.service.order.sharing.action;


import com.example.demo.component.mq.MqClient;
import com.example.demo.component.mq.MqMsgTypeEnum;
import com.example.demo.dao.entry.order.CommodityOrder;
import com.example.demo.pubdef.bo.order.CommodityOrderBriefBO;
import com.example.demo.pubdef.bo.order.CommodityOrderStatisticsChangedBO;
import com.example.demo.pubdef.bo.order.CommodityOrderUserStatisticsBO;
import com.example.demo.pubdef.mo.CommodityOrderMo;
import com.example.demo.pubdef.mo.ShopOrderMO;
import com.example.demo.service.order.sharing.msg.CommoditySysMsgMetadata;
import com.example.demo.service.order.sharing.userstatistics.CommodityOrderUserStatistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

public abstract class AbsCommodityOrderAction implements CommodityOrderAction {
    @Autowired
    MqClient mqClient;
    @Autowired
    CommodityOrderUserStatistics userStatistics;
    public abstract List<CommoditySysMsgMetadata> genSystemMqMsg(CommodityOrderBriefBO orderBriefBO);
//    public abstract void Update
    public abstract CommodityOrderStatisticsChangedBO getOrderStatisticsChangedBO(CommodityOrderBriefBO orderBriefBO);
    protected abstract void notifyOrderChanged(CommodityOrderBriefBO orderBriefBO);
    protected String getBroadCastMsg(CommodityOrderBriefBO orderBriefBO) {
        return null;
    }

    @Override
    public void onAction(CommodityOrderBriefBO orderBriefBO) {
        sendMqMsg(orderBriefBO);
        sendSysMsg(orderBriefBO);
        notifyOrderChanged(orderBriefBO);
        updateOrderStatistics(orderBriefBO);
        sendBroadCastMsg(orderBriefBO,getBroadCastMsg(orderBriefBO));
    }
    public void sendMqMsg(CommodityOrderBriefBO orderBriefBO){
        mqClient.sendTaskMsg(MqMsgTypeEnum.COMMODITY_ORDER_CHANGED,new ShopOrderMO(getActionEnum().getAction(),orderBriefBO.getOrderNo()));
    }
    public void sendSysMsg(CommodityOrderBriefBO orderBriefBO){
        List<CommoditySysMsgMetadata>metadata=genSystemMqMsg(orderBriefBO);
        if(metadata!=null&&!metadata.isEmpty()){

        }
    }

    private void sendBroadCastMsg(CommodityOrderBriefBO orderBriefBO, String msg) {
        if (StringUtils.isEmpty(msg)) {
            return;
        }

//        mqClient.sendBroadcastMsg(MqMsgTypeEnum.BROADCAST_MSG, new BroadcastMsgMO(msgTypeEnum, showTypeEnum, msg, null));
    }

    private void updateOrderStatistics(CommodityOrderBriefBO orderBriefBO) {
        return;
//        CommodityOrderStatisticsChangedBO changedBO = getOrderStatisticsChangedBO(orderBriefBO);
//        if (changedBO == null) {
//            return;
//        }
//        changedBO.setUid(orderBriefBO.getUid());
//        int changeNum = 0;
//        do {
//            changeNum = userStatistics.updateUserOrderStatistics(changedBO);
//        } while (changeNum == 0);
    }

}
