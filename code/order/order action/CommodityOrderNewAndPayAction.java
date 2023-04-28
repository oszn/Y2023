package com.example.demo.service.order.sharing.action;

import com.example.demo.component.mq.MqMsgTypeEnum;
import com.example.demo.component.mq.amqp.AmqpConstant;
import com.example.demo.pubdef.bo.order.CommodityOrderBriefBO;
import com.example.demo.pubdef.bo.order.CommodityOrderDecorateBO;
import com.example.demo.pubdef.bo.order.CommodityOrderStatisticsChangedBO;
import com.example.demo.pubdef.bo.order.CommodityOrderUserStatisticsBO;
import com.example.demo.pubdef.mo.CommodityDecorateListMO;
import com.example.demo.pubdef.mo.CommodityDecorateMO;
import com.example.demo.pubdef.mo.CommodityOrderCompensateMO;
import com.example.demo.service.order.sharing.constant.CommodityOrderActionEnum;
import com.example.demo.service.order.sharing.msg.CommoditySysMsgMetadata;
import com.example.demo.service.order.sharing.userstatistics.CommodityOrderUserStatistics;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class CommodityOrderNewAndPayAction extends AbsCommodityOrderAction {
    @Override
    public CommodityOrderActionEnum getActionEnum() {
        return CommodityOrderActionEnum.TO_NEW_AND_PAY_ORDER;
    }

    @Override
    public List<CommoditySysMsgMetadata> genSystemMqMsg(CommodityOrderBriefBO orderBriefBO) {
        if(!orderBriefBO.toPayOrder()) {
            return null;
        }
        return Arrays.asList(
                new CommoditySysMsgMetadata(orderBriefBO.getUid(),orderBriefBO.getUid(),"订单未付款",orderBriefBO),
                new CommoditySysMsgMetadata(orderBriefBO.getToUid(),orderBriefBO.getUid(),"用户下订单，但未付款",orderBriefBO));
    }

    @Override
    public CommodityOrderStatisticsChangedBO getOrderStatisticsChangedBO(CommodityOrderBriefBO orderBriefBO) {
        return new CommodityOrderStatisticsChangedBO().setChangeTotalCount(1).setChangeInProcessCount(1);
    }

    @Override
    protected void notifyOrderChanged(CommodityOrderBriefBO orderBriefBO) {
        orderBriefBO = (CommodityOrderDecorateBO) orderBriefBO;
        List<CommodityDecorateMO> list = ((CommodityOrderDecorateBO) orderBriefBO).getMos();
        CommodityDecorateListMO mo=new CommodityDecorateListMO();
        mo.setDecorateMOS(list);
        mo.setOrderNo(orderBriefBO.getOrderNo());
        for (CommodityDecorateMO m : list) {
            mqClient.sendTaskMsgRouteByKey(MqMsgTypeEnum.COMMODITY_INVENTORY_SINGLE, m, AmqpConstant.INVENTORY_QUEUE);
        }
//        mqClient.sendTaskMsgRouteByKey(MqMsgTypeEnum.COMMODITY_INVENTORY_SINGLE,mo, AmqpConstant.INVENTORY_QUEUE);
        CommodityOrderCompensateMO compensateMO=new CommodityOrderCompensateMO();
        compensateMO.setOrderNo(orderBriefBO.getOrderNo());
        mqClient.sendDelayTask(MqMsgTypeEnum.ORDER_AUTO_CANCEL,compensateMO,30*1000);
    }
}
