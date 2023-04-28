package com.example.demo.service.order.sharing.action;

import com.example.demo.component.mq.MqClient;
import com.example.demo.component.mq.MqMsgTypeEnum;
import com.example.demo.component.mq.MqSubMsgTypeEnum;
import com.example.demo.component.mq.amqp.AmqpConstant;
import com.example.demo.component.util.IdWorker;
import com.example.demo.dao.entry.commodity.CommodityInventoryDetails;
import com.example.demo.pubdef.bo.order.CommodityOrderBriefBO;
import com.example.demo.pubdef.bo.order.CommodityOrderDecorateBO;
import com.example.demo.pubdef.bo.order.CommodityOrderStatisticsChangedBO;
import com.example.demo.pubdef.mo.CommodityDecorateListMO;
import com.example.demo.pubdef.mo.CommodityDecorateMO;
import com.example.demo.pubdef.mo.CommodityOrderCompensateMO;
import com.example.demo.service.commodity.CommodityService;
import com.example.demo.service.order.sharing.constant.CommodityOrderActionEnum;
import com.example.demo.service.order.sharing.msg.CommoditySysMsgMetadata;
import org.aspectj.lang.annotation.Around;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CommodityOrderNewAction extends AbsCommodityOrderAction {
    @Autowired
    MqClient mqClient;
    @Autowired
    CommodityService commodityService;

    @Override
    public List<CommoditySysMsgMetadata> genSystemMqMsg(CommodityOrderBriefBO orderBriefBO) {
        return null;
    }

    @Override
    public CommodityOrderStatisticsChangedBO getOrderStatisticsChangedBO(CommodityOrderBriefBO orderBriefBO) {
        return null;
    }

    @Override
    protected void notifyOrderChanged(CommodityOrderBriefBO orderBriefBO) {
        orderBriefBO = (CommodityOrderDecorateBO) orderBriefBO;
        List<CommodityDecorateMO> list = ((CommodityOrderDecorateBO) orderBriefBO).getMos();
        IdWorker idWorker=new IdWorker(orderBriefBO.getUid(),orderBriefBO.getActId(),System.currentTimeMillis());
        for (CommodityDecorateMO mo : list) {
//            mqClient.sendTaskMsgRouteByKey(MqMsgTypeEnum.COMMODITY_INVENTORY_SINGLE, mo, AmqpConstant.INVENTORY_QUEUE);
//            mo.setOrderNo();
            mo.setOrderNo(orderBriefBO.getOrderNo());
            mo.setUuid(idWorker.nextId());
            mqClient.sendKafkaTaskMsgTopic1(MqMsgTypeEnum.COMMODITY_INVENTORY_STATION, mo, String.valueOf(mo.getId()));
        }
//        CommodityDecorateListMO mo=new CommodityDecorateListMO();
//        mo.setDecorateMOS(list);
//        mo.setOrderNo(orderBriefBO.getOrderNo());
//        mqClient.sendTaskMsgRouteByKey(MqMsgTypeEnum.COMMODITY_INVENTORY,mo,AmqpConstant.INVENTORY_QUEUE);
        CommodityOrderCompensateMO compensateMO = new CommodityOrderCompensateMO();
        compensateMO.setOrderNo(orderBriefBO.getOrderNo());
        mqClient.sendDelayTask(MqMsgTypeEnum.ORDER_AUTO_CANCEL, compensateMO, 30 * 1000);

    }

    @Override
    public CommodityOrderActionEnum getActionEnum() {
        return CommodityOrderActionEnum.TO_NEW_ORDER;
    }
}
