package com.example.demo.service.order.sharing.fsm;

import com.example.demo.dao.entry.order.CommodityOrder;
import com.example.demo.pubdef.bo.order.CommodityOrderBriefBO;
import com.example.demo.service.order.sharing.action.CommodityOrderAction;
import com.example.demo.service.order.sharing.action.CommodityOrderActionFactory;
import com.example.demo.service.order.sharing.constant.CommodityOrderActionEnum;

public class CommodityOrderFsmManageBuilder {
    private CommodityOrderBriefBO orderBriefBO = null;
    private CommodityOrderAction action = null;
    private Object param = null;

    public CommodityOrderFsmManageBuilder() {
        //do nothing
    }

    public CommodityOrderFsmManage build() {
        CommodityOrderFsmManage fsmManage = new CommodityOrderFsmManage(this);
        fsmManage.init();
        return fsmManage;
    }

    public CommodityOrderFsmManageBuilder setActionEnum(CommodityOrderActionEnum actionEnum) {
        this.action = CommodityOrderActionFactory.newAction(actionEnum);
        return this;
    }

    public CommodityOrderAction getAction() {
        return this.action;
    }

    public Object getParam() {
        return param;
    }

    public CommodityOrderFsmManageBuilder setParam(Object param) {
        this.param = param;
        return this;
    }

    public CommodityOrderBriefBO getOrderBriefBO() {
        return orderBriefBO;
    }

    public CommodityOrderFsmManageBuilder setOrderBriefBO(CommodityOrderBriefBO orderBriefBO) {
        this.orderBriefBO = orderBriefBO;
        return this;
    }
}
