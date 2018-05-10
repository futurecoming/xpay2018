package com.xpay.pay.controller;

import java.io.IOException;
import java.net.URLEncoder;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import com.xpay.pay.model.Order;
import com.xpay.pay.model.StoreChannel.PaymentGateway;
import com.xpay.pay.proxy.IPaymentProxy;
import com.xpay.pay.proxy.PaymentRequest;
import com.xpay.pay.proxy.PaymentResponse.OrderStatus;
import com.xpay.pay.proxy.chinaumsh5.ChinaUmsH5Proxy;
import com.xpay.pay.proxy.chinaumswap.ChinaUmsWapProxy;
import com.xpay.pay.proxy.kekepay.KekePayProxy;
import com.xpay.pay.proxy.miaofu.MiaoFuProxy;
import com.xpay.pay.proxy.txf.TxfProxy;
import com.xpay.pay.proxy.tfb.TfbProxy;
import com.xpay.pay.proxy.upay.UPayProxy;
import com.xpay.pay.service.OrderService;
import com.xpay.pay.service.PaymentService;

public class JsPayServlet extends HttpServlet {

  private static final long serialVersionUID = 4657984343307637580L;
  protected final Logger logger = LogManager.getLogger("AccessLog");
  @Autowired
  protected OrderService orderService;
  @Autowired
  protected PaymentService paymentService;
  @Autowired
  protected MiaoFuProxy miaoFuProxy;
  @Autowired
  protected ChinaUmsH5Proxy chinaUmsH5Proxy;
  @Autowired
  protected ChinaUmsWapProxy chinaUmsWapProxy;
  @Autowired
  protected UPayProxy upayProxy;
  @Autowired
  protected KekePayProxy kekePayProxy;
  @Autowired
  protected TxfProxy txfProxy;
  @Autowired
  protected TfbProxy tfbProxy;

  @Override
  public void init(ServletConfig config) throws ServletException {
    SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this,
        config.getServletContext());
  }

  @Override
  protected void doGet(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {
    doPost(request, response);
  }

  @Override
  protected void doPost(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {
    String uri = request.getRequestURI();
    String orderNo = uri.substring(uri.lastIndexOf("/") + 1);

    Order order = orderService.findActiveByOrderNo(orderNo);
    if (order == null) {
      response.sendError(400, "无效订单");
      return;
    }
    String path = request.getPathInfo();
    String parameters =
        StringUtils.isBlank(request.getQueryString()) ? "" : "?" + request.getQueryString();
    logger.info("Jspay: " + path + parameters);

    if (PaymentGateway.CHINAUMSH5.equals(order.getStoreChannel().getPaymentGateway())) {
      if (!OrderStatus.NOTPAY.equals(order.getStatus())) {
        response.sendError(400, "订单已支付");
        return;
      }
      PaymentRequest paymentRequest = paymentService.toPaymentRequest(order);
      paymentRequest.setGatewayOrderNo(order.getExtOrderNo());
      String jsUrl = chinaUmsH5Proxy.getJsUrl(paymentRequest);
      response.setCharacterEncoding("utf-8");
      response.setHeader("Content-type", "text/html;charset=UTF-8");
      response.sendRedirect(jsUrl);
    } else if (PaymentGateway.CHINAUMSWAP.equals(order.getStoreChannel().getPaymentGateway())) {
      if (!OrderStatus.NOTPAY.equals(order.getStatus())) {
        response.sendError(400, "订单已支付");
        return;
      }
      PaymentRequest paymentRequest = paymentService.toPaymentRequest(order);
      paymentRequest.setGatewayOrderNo(order.getExtOrderNo());
      String jsUrl = chinaUmsWapProxy.getJsUrl(paymentRequest);
      response.setCharacterEncoding("utf-8");
      response.setHeader("Content-type", "text/html;charset=UTF-8");
      response.sendRedirect(jsUrl);
    } else if (PaymentGateway.UPAY.equals(order.getStoreChannel().getPaymentGateway())) {
      if (!OrderStatus.NOTPAY.equals(order.getStatus())) {
        response.sendError(400, "订单已支付");
        return;
      }
      PaymentRequest paymentRequest = paymentService.toPaymentRequest(order);
      String jsUrl = upayProxy.getJsUrl(paymentRequest);
      response.setCharacterEncoding("utf-8");
      response.setHeader("Content-type", "text/html;charset=UTF-8");
      response.sendRedirect(jsUrl);
    } else if (PaymentGateway.MIAOFU.equals(order.getStoreChannel().getPaymentGateway())) {
      PaymentRequest paymentRequest = paymentService.toPaymentRequest(order);
      String jsUrl = miaoFuProxy.getJsUrl(paymentRequest);
      response.setCharacterEncoding("utf-8");
      response.setHeader("Content-type", "text/html;charset=UTF-8");
      response.sendRedirect(jsUrl);
    } else if (PaymentGateway.KEKEPAY.equals(order.getStoreChannel().getPaymentGateway())) {
      if (uri.contains(IPaymentProxy.TOPAY)) {
        PaymentRequest paymentRequest = paymentService.toPaymentRequest(order);
        response.sendRedirect(kekePayProxy.getJsUrl(paymentRequest));
      } else if (uri.contains(IPaymentProxy.PAYED)) {
        response.sendRedirect(order.getReturnUrl());
      }
    } else if (PaymentGateway.TXF.equals(order.getStoreChannel().getPaymentGateway())) {
    	if (uri.contains(IPaymentProxy.TOPAY)) {
    		request.getRequestDispatcher("/txf.jsp?orderNo="+orderNo+"&amount="+order.getTotalFee()).forward(request, response);
    		return;
    	} else {
	        PaymentRequest paymentRequest = paymentService.toPaymentRequest(order);
	        String cardType = request.getParameter("cardType");
	        String bankId = request.getParameter("bankId");
	        paymentRequest.setCardType(cardType);
	        paymentRequest.setBankId(bankId);
	        response.setCharacterEncoding("utf-8");
	        response.setHeader("Content-type", "text/html;charset=UTF-8");
	        response.sendRedirect(txfProxy.getJsUrl(paymentRequest));
    	}
    } else if(PaymentGateway.TFB.equals(order.getStoreChannel().getPaymentGateway())) {
        PaymentRequest paymentRequest = paymentService.toPaymentRequest(order);
        String jsUrl = tfbProxy.getJsUrl(paymentRequest);
        String cipherData = jsUrl.substring(jsUrl.lastIndexOf("=") + 1);
        String  encodeCipherData = URLEncoder.encode(cipherData, "GBK");
        jsUrl = jsUrl.replace(cipherData, encodeCipherData);
        String cardType = request.getParameter("cardType");
        String bankId = request.getParameter("bankId");
        paymentRequest.setCardType(cardType);
        paymentRequest.setBankId(bankId);
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-type", "text/html;charset=UTF-8");

        response.sendRedirect(jsUrl);
    } else {
        response.sendError(400, "无效订单");
    }
  }
}
