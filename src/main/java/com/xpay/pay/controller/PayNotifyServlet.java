package com.xpay.pay.controller;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import com.xpay.pay.model.Order;
import com.xpay.pay.model.StoreChannel.PaymentGateway;
import com.xpay.pay.proxy.PaymentResponse;
import com.xpay.pay.proxy.PaymentResponse.OrderStatus;
import com.xpay.pay.proxy.swiftpass.SwiftpassProxy;
import com.xpay.pay.service.OrderService;
import com.xpay.pay.util.CommonUtils;
import com.xpay.pay.util.CryptoUtils;
import com.xpay.pay.util.XmlUtils;

public class PayNotifyServlet extends HttpServlet {
	private static final long serialVersionUID = -4617898543988945707L;
	
	protected final Logger logger = LogManager.getLogger("AccessLog");
	
	@Autowired
	protected OrderService orderService;
	
	@Override
	 public void init(ServletConfig config) throws ServletException {
		SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this,
				config.getServletContext());
	}
	
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	String uri = request.getRequestURI();
  		String respString = "fail";
		response.setCharacterEncoding("utf-8");
		response.setHeader("Content-type", "text/html;charset=UTF-8");

		try {
  			if(uri.contains(PaymentGateway.SWIFTPASS.name().toLowerCase())) {
  				respString = handleSwiftpassNotification(request);
	    	}
    	} catch (Exception e) {
            e.printStackTrace();
        } finally {
        	response.getWriter().write(respString);
        } 
    }


	private String handleSwiftpassNotification(HttpServletRequest request) throws Exception {
		request.setCharacterEncoding("utf-8");
        byte[] buffer = new byte[request.getContentLength()];
        IOUtils.readFully(request.getInputStream(), buffer);
        String content = new String(buffer);
        logger.info("Notify content: " + content);
        
        String respString = "fail";
        if(StringUtils.isNotBlank(content)){
            Map<String,String> map = XmlUtils.fromXml(content.getBytes(), "utf-8");
            if(map.containsKey("sign")){
            	boolean checkSignature = CryptoUtils.checkSignature(map, SwiftpassProxy.appSecret, "sign", "key");
                if(!checkSignature){
                	logger.info("check signature failed");
                    respString = "fail";
                }else{
                    String status = map.get("status");
                    if(status != null && "0".equals(status)){
                        String result_code = map.get("result_code");
                        if(PaymentResponse.SUCCESS.equals(result_code)){
                          String orderNo = map.get("out_trade_no");
                          String totalFee = map.get("total_fee");
                          Order order = orderService.findActiveByOrderNo(orderNo);
                          if(order!=null && CommonUtils.toInt(totalFee) == (int)order.getTotalFeeAsFloat()*100) {
                        	  order.setStatus(OrderStatus.SUCCESS);
                        	  orderService.update(order);
                          }
                        } 
                    } 
                    respString = "success";
                }
            }
        }
        return respString;
	}
}