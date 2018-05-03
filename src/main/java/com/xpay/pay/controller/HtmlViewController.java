package com.xpay.pay.controller;

import com.xpay.pay.model.Order;
import com.xpay.pay.model.StoreChannel.PaymentGateway;
import com.xpay.pay.proxy.PaymentRequest;
import com.xpay.pay.proxy.ips.IpsProxy;
import com.xpay.pay.proxy.ips.quick.IpsQuickProxy;
import com.xpay.pay.proxy.ips.wxpay.IpsWxProxy;
import com.xpay.pay.proxy.ips.transfer.rsp.Body;
import com.xpay.pay.service.OrderService;
import com.xpay.pay.service.PaymentService;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * Created by sunjian on Date: 2017/12/15 下午2:12
 * Description:
 */
@Controller
@RequestMapping("/pay")
public class HtmlViewController {

  private static final Logger logger = LogManager.getLogger(HtmlViewController.class);

  @Autowired
  private OrderService orderService;

  @Autowired
  private IpsQuickProxy ipsQuickProxy;

  @Autowired
  private IpsWxProxy ipsWxpayProxy;

  @Autowired
  private PaymentService paymentService;

  @Autowired
  private IpsProxy ipsProxy;
  
  private static String merCode = "205531";
  private static String account = "2055310011";

  @RequestMapping(value = "/{orderNo}", method = RequestMethod.GET)
  public ModelAndView pay(@PathVariable("orderNo") String orderNo) {
    logger.info("h5pay :" + orderNo);
    Order order = orderService.findActiveByOrderNo(orderNo);
    if (order == null) {
      return new ModelAndView("h5_error");
    }
    if (PaymentGateway.IPSQUICK.equals(order.getStoreChannel().getPaymentGateway())) {
      PaymentRequest paymentRequest = paymentService.toPaymentRequest(order);
      String reqParam = ipsQuickProxy.getReqParam(paymentRequest);
      Map<String, String> model = new HashMap<>();
      model.put("pGateWayReq", reqParam);
      return new ModelAndView("ips_quick", model);
    } else if(PaymentGateway.IPSWX.equals(order.getStoreChannel().getPaymentGateway())) {
      PaymentRequest paymentRequest = paymentService.toPaymentRequest(order);
      String reqParam = ipsWxpayProxy.getReqParam(paymentRequest);
      Map<String, String> model = new HashMap<>();
      model.put("wxPayReq", reqParam);
      return new ModelAndView("ips_wx", model);
    }

    return new ModelAndView("h5_error");
  }

  @RequestMapping(value = "/ips/open", method = RequestMethod.GET)
  public ModelAndView open(@RequestParam("customerCode") String customerCode,
      @RequestParam("identityNo") String identityNo, @RequestParam("userName") String userName,
      @RequestParam("mobileNo") String mobileNo, HttpServletRequest request)
      throws IOException {
	/* try{
		 if(userName != null && !"".equals(userName)){
			   userName = URLDecoder.decode(userName,"UTF-8");
		   }
	 }catch(Exception e){
		 logger.info("URLdecode throw exception>>",e);
	 } */
    logger.info("ips>open>"+merCode+","+account+","+customerCode+","+userName+","+identityNo);
    String requestXml = ipsProxy
        .buildOpenRequest(request.getRemoteAddr(), merCode, account, "2", customerCode, "1",
            identityNo, userName, "", "",
            mobileNo, "", "", "", "", "http://www.wfpay.xyz", "http://www.wfpay.xyz", "", "",
            "");

    Map<String, String> model = new HashMap<>();
    model.put("ipsRequest", requestXml);
    return new ModelAndView("ips_open", model);
  }
  
  @RequestMapping(value = "/ips/transfer", method = RequestMethod.GET)
  public ModelAndView transfer(@RequestParam("customerCode") String customerCode,
      @RequestParam("transferAmount") String transferAmount, 
      @RequestParam("collectionItemName") String collectionItemName, 
      HttpServletRequest request)
      throws IOException {
	  /*try{
			 if(collectionItemName != null && !"".equals(collectionItemName)){
				 collectionItemName = URLDecoder.decode(collectionItemName,"UTF-8");
			   }
		 }catch(Exception e){
			 
		 }*/
    Body transferResponse = ipsProxy.transfer(request.getRemoteAddr(), "", merCode, account, customerCode, transferAmount, collectionItemName, "");

    Map<String, String> model = new HashMap<>();
    model.put("resp", transferResponse.getTradeState());
    return new ModelAndView("ips_transfer", model);
  }
  
  
  /**
   * 1100 中国工商银行 
   * 1101 中国农业银行
   * 1102 招商银行 
   * 1103 兴业银行 
   * 1104 中信银行
   * 1106 中国建设银行
   * 1107 中国银行
   * 1108 交通银行
   * 1109 浦东发展银行
   * 1110 民生银行
   * 1111 华夏银行
   * 1112 光大银行
   * 1113 北京银行
   * 1114 广发银行
   * 1116 上海银行
   * 1119 中国邮政储蓄银行
   * 1120 浙商银行
   * 1121 平安银行
   * 1123 渤海银行
   * 1827 恒丰银行
   * @param customerCode
   * @param bankCard
   * @param bankCode
   * @param request
   * @return
   * @throws IOException
   */
  @RequestMapping(value = "/ips/withdraw", method = RequestMethod.GET)
  public ModelAndView withdrawal(@RequestParam("customerCode") String customerCode,
      @RequestParam("bankCard") String bankCard, @RequestParam("bankCode") String bankCode,
      HttpServletRequest request)
      throws IOException {

    String requestXml = ipsProxy
        .buildWithdrawalRequest(request.getRemoteAddr(), "", merCode, customerCode,
            "http://www.wfpay.xyz", "http://www.wfpay.xyz", bankCard, bankCode);

    Map<String, String> model = new HashMap<>();
    model.put("ipsRequest", requestXml);
    return new ModelAndView("ips_withdraw", model);
  }
  
  
  public static void main(String args[]){
	  try {
		String  userName = URLDecoder.decode("%E5%88%98%E6%99%B4%E6%99%B4","UTF-8");
		System.out.println(userName);
	} catch (UnsupportedEncodingException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
  }
}