package com.example.demo.service.order.sharing.fsm;


import com.example.demo.component.mq.BusinessException;
import com.example.demo.pubdef.bo.order.CommodityOrderBriefBO;
import com.example.demo.pubdef.bo.order.CommodityOrderDecorateBO;
import com.example.demo.pubdef.dto.ResultEnum;
import com.example.demo.service.order.sharing.action.CommodityOrderAction;
import com.example.demo.service.order.sharing.action.CommodityOrderActionUtil;
import com.example.demo.service.order.sharing.constant.CommodityOrderStateEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class CommodityOrderFsmManage {
    private CommodityOrderBriefBO orderBriefBO = null;
    private CommodityOrderAction action = null;
    private Object param = null;
    private CommodityOrderStateEnum currentStateEnum = CommodityOrderStateEnum.INIT;
    private Map<Integer, BaseCommodityFsm> fsmMap;

    CommodityOrderFsmManage(CommodityOrderFsmManageBuilder builder) {
        if (builder.getOrderBriefBO() != null) {
            this.orderBriefBO = builder.getOrderBriefBO();
        }
        if (builder.getAction() != null) {
            this.action = builder.getAction();
        }
        if (builder.getParam() != null) {
            this.param = builder.getParam();
        }
        fsmMap = new HashMap<>();
    }

    public void handle() {
        long s1 = System.currentTimeMillis();
        getFsm().handle();
        long s2 = System.currentTimeMillis();
        getAction().onAction(getOrderBriefBO());
        long s3 = System.currentTimeMillis();
        log.info("action {},handle time{},action time{}", getAction(), s2 - s1, s3 - s2);
    }

    void init() {
        if (isUselessOrder(orderBriefBO, getAction())) {
            throw new BusinessException(ResultEnum.ORDER_UNEXIST);
        }

        if (isNewOrder(orderBriefBO, getAction())) {
            orderBriefBO = new CommodityOrderDecorateBO();
            orderBriefBO.setState(CommodityOrderStateEnum.INIT.value());
        }

        setNextFsm(CommodityOrderStateEnum.values()[orderBriefBO.getState() + 1], getAction());
        fsmMap.put(CommodityOrderStateEnum.INIT.value(), new CommodityOrderInitFsm(this));
        fsmMap.put(CommodityOrderStateEnum.TO_PAY.value(), new CommodityOrderToPayFsm(this));
        fsmMap.put(CommodityOrderStateEnum.USER_REVOKED.value(), new CommodityOrderCancelFsm(this));
        fsmMap.put(CommodityOrderStateEnum.TO_ACCEPT.value(), new CommodityOrderAcceptFsm(this));
        fsmMap.put(CommodityOrderStateEnum.TO_RECEIVE.value(), new CommodityOrderToReceiveFsm(this));
    }

    private boolean isNewOrder(CommodityOrderBriefBO orderBriefBO, CommodityOrderAction action) {
        return (orderBriefBO == null || orderBriefBO.getOrderNo() == null) || (CommodityOrderActionUtil.isNewAction(action))
                || (CommodityOrderActionUtil.isNewAndPayAction(action));
    }

    private boolean isUselessOrder(CommodityOrderBriefBO orderBriefBO, CommodityOrderAction action) {
        return false;
    }

    private void setNextFsm(CommodityOrderStateEnum nextStateEnum, CommodityOrderAction action) {
        setCurrentStateEnum(nextStateEnum);
        setAction(action);
    }

    private BaseCommodityFsm getFsm() {
        BaseCommodityFsm fsm = fsmMap.get(getCurrentStateEnum().value());
        if (fsm == null) {
            throw new BusinessException(ResultEnum.ORDER_INVALID_STATE);
        }
        return fsm;
    }


    private void setCurrentStateEnum(CommodityOrderStateEnum stateEnum) {
        this.currentStateEnum = stateEnum;
    }

    private CommodityOrderStateEnum getCurrentStateEnum() {
        return this.currentStateEnum;
    }

    public CommodityOrderBriefBO getOrderBriefBO() {
        return this.orderBriefBO;
    }

    CommodityOrderAction getAction() {
        return action;
    }

    public void setAction(CommodityOrderAction action) {
        this.action = action;
    }

    Object getParam() {
        return param;
    }

}
