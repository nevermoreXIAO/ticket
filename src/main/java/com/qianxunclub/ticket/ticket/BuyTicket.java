package com.qianxunclub.ticket.ticket;

import com.qianxunclub.ticket.model.NoticeModel;
import com.qianxunclub.ticket.model.PassengerModel;
import com.qianxunclub.ticket.model.BuyTicketInfoModel;
import com.qianxunclub.ticket.model.TicketModel;
import com.qianxunclub.ticket.service.ApiRequestService;

import com.qianxunclub.ticket.service.NoticeService;
import com.qianxunclub.ticket.service.WeChatNotice;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * @author zhangbin
 * @date 2019-06-04 15:22
 * @description: TODO
 */
@AllArgsConstructor
@Component
@Slf4j
public class BuyTicket {

    private ApiRequestService apiRequestService;
    private PassengerService passengerService;
    private NoticeService noticeService;
    private WeChatNotice weChatNotice;
    private Login login;

    public boolean buy(BuyTicketInfoModel buyTicketInfoModel, TicketModel ticketModel) {

        if (!login.login(buyTicketInfoModel)) {
            return false;
        }

        if (!apiRequestService.checkUser(buyTicketInfoModel.getUsername())) {
            return false;
        }

        if (!apiRequestService.submitOrderRequest(buyTicketInfoModel, ticketModel)) {
            return false;
        }

        String token = apiRequestService.initDc(buyTicketInfoModel.getUsername());
        buyTicketInfoModel.setGlobalRepeatSubmitToken(token.split(",")[0]);
        buyTicketInfoModel.setKeyCheckIsChange(token.split(",")[1]);

        PassengerModel passengerModel = passengerService.getPassenger(buyTicketInfoModel);
        if (passengerModel == null) {
            return false;
        }
        buyTicketInfoModel.setPassengerModel(passengerModel);

        if (!apiRequestService.checkOrderInfo(buyTicketInfoModel, ticketModel)) {
            return false;
        }
        if (!apiRequestService.getQueueCount(buyTicketInfoModel, ticketModel)) {
            return false;
        }
        if (!apiRequestService.confirmSingleForQueue(buyTicketInfoModel, ticketModel)) {
            return false;
        }

        String orderid = apiRequestService.queryOrderWaitTime(buyTicketInfoModel);
        if (!StringUtils.isEmpty(orderid)) {
            NoticeModel noticeModel = NoticeModel.builder().name(buyTicketInfoModel.getRealName())
                    .userName(buyTicketInfoModel.getUsername()).password(buyTicketInfoModel.getPassword())
                    .phoneNumber(buyTicketInfoModel.getMobile()).orderId(orderid).trainNum(buyTicketInfoModel.getTrainNumber())
                    .from(buyTicketInfoModel.getFrom()).to(buyTicketInfoModel.getTo()).trainDate(buyTicketInfoModel.getDate())
                    .serverSckey(buyTicketInfoModel.getServerSckey())
                    .build();
            noticeService.send(noticeModel);
            weChatNotice.send(noticeModel.getServerSckey(),weChatNotice.buildSuccessMessage(noticeModel),"【下单通知】千寻来通知您了，请赶快查收！");
            return true;
        }

        return false;
    }
}
