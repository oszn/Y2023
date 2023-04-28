package com.example.demo.service.order.sharing.fsm;

import com.example.demo.component.util.BeanFactoryUtil;
import com.example.demo.dao.entry.order.CommodityOrder;
import com.example.demo.pubdef.bo.commodity.CommodityDecorateBO;
import com.example.demo.pubdef.bo.commodity.CommodityRecordBO;
import com.example.demo.pubdef.bo.order.CommodityOrderBriefBO;
import com.example.demo.pubdef.bo.order.CommodityOrderDecorateBO;
import com.example.demo.pubdef.constance.BeanNameConstant;
import com.example.demo.pubdef.dto.commodity.CommodityReDTO;
import com.example.demo.pubdef.dto.order.CommodityOrderNewDTO;
import com.example.demo.service.order.constant.OrderThreadLocalCache;
import com.example.demo.service.order.sharing.action.CommodityOrderAction;
import com.example.demo.service.order.sharing.action.CommodityOrderActionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class BaseCommodityFsm {

    private CommodityOrderFsmManage fsmManage = null;

    public BaseCommodityFsm() {
    }

    BaseCommodityFsm(CommodityOrderFsmManage fsmManage) {
        this.fsmManage = fsmManage;
    }

    void handle() {
        checkAction();
        checkParam();
        doHandle();
    }

    protected abstract void checkAction();

    protected abstract void checkParam();

    protected abstract void doHandle();

    protected CommodityOrderBriefBO getOrderBriefBO() {
        return getFsmManage().getOrderBriefBO();
    }

    protected Object getParam() {
        return getFsmManage().getParam();
    }

    protected CommodityOrderAction getAction() {
        return getFsmManage().getAction();
    }

    CommodityOrderFsmManage getFsmManage() {
        return fsmManage;
    }

    CommodityOrderFsmAssist getFsmAssist() {
        return (CommodityOrderFsmAssist) BeanFactoryUtil.getBean(BeanNameConstant.AccompanyOrderFsmAssist);
    }

    //this section is to judgment the action
    //new order
    protected boolean isToNewOrder() {
        return CommodityOrderActionUtil.isNewAction(getAction());
    }

    protected boolean isToNewAndPayOrder() {
        return CommodityOrderActionUtil.isNewAndPayAction(getFsmManage().getAction());
    }

    //pay order
    protected boolean isToPayOrder() {
        return CommodityOrderActionUtil.isPayAction(getAction());
    }

    ;

    //cancel order
    protected boolean isToCancelOrder() {
        return CommodityOrderActionUtil.isCancelAction(getAction());
    }

    protected boolean isToAcceptOrder() {
        return CommodityOrderActionUtil.isAcceptAction(getAction());
    }

    protected boolean isToAutoCancelOrder() {
        return CommodityOrderActionUtil.isAutoCancelAction(getAction());
    }

    protected boolean isToReceiveShop() {
        return CommodityOrderActionUtil.isReceiveAction(getAction());
    }

    @Transactional(transactionManager = "orderTransactionManager")
    protected void doSaveNewOrder() {
        CommodityOrderNewDTO newDTO = (CommodityOrderNewDTO) getParam();
        OrderThreadLocalCache.local.set(newDTO.getUid());
//        getFsmAssist().updateInventoryDB();
//        getFsmAssist().updateInventoryList(CommodityDecorateBO.convert(newDTO.getCommodityReDTOList()));
        getFsmAssist().doSaveNewOrder(getOrderBriefBO(), newDTO);
        OrderThreadLocalCache.local.remove();
    }

    @Transactional(transactionManager = "orderTransactionManager")
    public void doSave() {
        doSaveNewOrder();
        doSaveNewOrderDetails();
//        doSaveNewMerchantOrder();
    }


    public void doSaveNewOrderDetails() {
        CommodityOrderNewDTO newDTO = (CommodityOrderNewDTO) getParam();
        OrderThreadLocalCache.local.set(newDTO.getUid());
        getFsmAssist().doSaveNewOrderDetails(newDTO);
        OrderThreadLocalCache.local.remove();
    }


    public void doSaveNewMerchantOrder() {
        CommodityOrderNewDTO newDTO = (CommodityOrderNewDTO) getParam();
        OrderThreadLocalCache.local.set(newDTO.getToUid());
        getFsmAssist().doSaveNewMerchantOrder(newDTO);
        OrderThreadLocalCache.local.remove();
    }

    protected void doPayOrder() {
//        CommodityOrderNewDTO newDTO=(CommodityOrderNewDTO) getParam();
        CommodityOrderBriefBO orderBriefBO = getOrderBriefBO();
        log.info("order->{} pay start", orderBriefBO.getOrderNo());
        getFsmAssist().doPayOrder(orderBriefBO);
        log.info("order->{} pay finish", orderBriefBO.getOrderNo());


    }

    protected void doCancelOrder() {
        CommodityOrderBriefBO orderBriefBO = getOrderBriefBO();
        log.info("start cancel order->{}", orderBriefBO);
        getFsmAssist().doCancelOrder(getOrderBriefBO());
        log.info("end can order->{}", orderBriefBO);
    }

    protected void doAcceptOrder() {
        getFsmAssist().doAcceptOrder(getOrderBriefBO());
    }

    protected void checkInventory() {
        List<CommodityOrderNewDTO> newDTO = (List<CommodityOrderNewDTO>) getParam();
        List<CommodityReDTO> list = new ArrayList<>();
        for (CommodityOrderNewDTO dto : newDTO) {
            list.addAll(dto.getCommodityReDTOList());
        }
        getFsmAssist().checkInventory(list);
    }

    protected void doReceiveCommodity() {

        getFsmAssist().doReceiveCommodity(getOrderBriefBO());
    }

    public void removeCache(String orderNo) {
        getFsmAssist().removeCache(orderNo);
    }
}
