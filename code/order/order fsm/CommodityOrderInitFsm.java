package com.example.demo.service.order.sharing.fsm;

import com.example.demo.component.mq.BusinessException;
import com.example.demo.pubdef.dto.ResultEnum;
import com.example.demo.pubdef.dto.order.CommodityOrderNewDTO;
import com.example.demo.service.order.sharing.action.CommodityOrderActionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
@Slf4j
public class CommodityOrderInitFsm extends BaseCommodityFsm{
    public CommodityOrderInitFsm(CommodityOrderFsmManage commodityOrderFsmManage) {
        super(commodityOrderFsmManage);
    }

    @Override
    protected void checkAction() {
        if(!isToNewOrder()&&!isToNewAndPayOrder()&&!isToCancelOrder()&&!isToAutoCancelOrder()){
            throw new BusinessException(ResultEnum.ORDER_FSM_ACTION_UNSUPPORT);
        };
    }

    @Override
    protected void checkParam() {
        if(!(getParam()instanceof CommodityOrderNewDTO)){
            throw new BusinessException(ResultEnum.ORDER_FSM_INVALID_PARA);
        }
//        checkInventory();
    }

    @Override
    protected void doHandle() {
//        long s1=System.currentTimeMillis();
        if(isToNewOrder()||isToNewAndPayOrder()){
            doSave();
        }
//        long s2=System.currentTimeMillis();
        if(isToAutoCancelOrder()){
            doCancelOrder();
        }
////        long s3=System.currentTimeMillis();
//        log.info("record time{},pay time{}", s2 - s1, s3 - s2);
    }


}
