package com.example.demo.service.order.sharing.action;

import com.example.demo.pubdef.bo.order.CommodityOrderBriefBO;
import com.example.demo.pubdef.bo.order.CommodityOrderStatisticsChangedBO;
import com.example.demo.service.order.sharing.constant.CommodityOrderActionEnum;
import com.example.demo.service.order.sharing.msg.CommoditySysMsgMetadata;
import org.springframework.stereotype.Component;

import java.util.List;
@Component
public class CommodityOrderPayAction extends AbsCommodityOrderAction{
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

    }

    @Override
    public CommodityOrderActionEnum getActionEnum() {
        return CommodityOrderActionEnum.TO_PAY_ORDER;
    }
}
