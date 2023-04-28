package com.example.demo.service.order.sharing.action;

import com.example.demo.pubdef.bo.order.CommodityOrderBriefBO;
import com.example.demo.service.order.sharing.constant.CommodityOrderActionEnum;

public interface CommodityOrderAction {
    CommodityOrderActionEnum getActionEnum();
    void onAction(CommodityOrderBriefBO orderBriefBO);
}