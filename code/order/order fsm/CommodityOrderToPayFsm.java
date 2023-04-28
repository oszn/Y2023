package com.example.demo.service.order.sharing.fsm;

import com.example.demo.component.mq.BusinessException;
import com.example.demo.pubdef.dto.ResultEnum;
import org.springframework.stereotype.Service;


public class CommodityOrderToPayFsm extends BaseCommodityFsm{
    public CommodityOrderToPayFsm(CommodityOrderFsmManage commodityOrderFsmManage) {
        super(commodityOrderFsmManage);
    }

    @Override
    protected void checkAction() {
        if(!isToPayOrder()&&!isToCancelOrder()&&!isToAutoCancelOrder()){
            throw new BusinessException(ResultEnum.ORDER_FSM_ACTION_UNSUPPORT);
        }
    }

    @Override
    protected void checkParam() {

    }
    @Override
    protected void doHandle() {
        removeCache(getOrderBriefBO().getOrderNo());

        if(isToPayOrder()) {
            doPayOrder();
        }else if(isToCancelOrder()||isToAutoCancelOrder()){
            doCancelOrder();
        }
    }
}
