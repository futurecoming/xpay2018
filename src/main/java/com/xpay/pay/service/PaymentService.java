package com.xpay.pay.service;

import static com.xpay.pay.ApplicationConstants.CODE_COMMON;
import static com.xpay.pay.ApplicationConstants.STATUS_BAD_REQUEST;
import static com.xpay.pay.ApplicationConstants.STATUS_UNAUTHORIZED;
import static com.xpay.pay.proxy.IPaymentProxy.NO_RESPONSE;

import java.util.List;

import com.xpay.pay.model.StoreChannel.IpsProps;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xpay.pay.ApplicationConstants;
import com.xpay.pay.MemoryCache;
import com.xpay.pay.RiskEngine;
import com.xpay.pay.dao.SubChannelMapper;
import com.xpay.pay.exception.Assert;
import com.xpay.pay.exception.GatewayException;
import com.xpay.pay.model.App;
import com.xpay.pay.model.Bill;
import com.xpay.pay.model.Order;
import com.xpay.pay.model.Store;
import com.xpay.pay.model.StoreChannel;
import com.xpay.pay.model.StoreChannel.PaymentGateway;
import com.xpay.pay.model.SubChannel.Status;
import com.xpay.pay.model.StoreGoods;
import com.xpay.pay.model.SubChannel;
import com.xpay.pay.proxy.IPaymentProxy;
import com.xpay.pay.proxy.IPaymentProxy.PayChannel;
import com.xpay.pay.proxy.PaymentProxyFactory;
import com.xpay.pay.proxy.PaymentRequest;
import com.xpay.pay.proxy.PaymentResponse;
import com.xpay.pay.proxy.PaymentResponse.OrderStatus;
import com.xpay.pay.util.AppConfig;
import com.xpay.pay.util.CommonUtils;
import com.xpay.pay.util.IDGenerator;

@Service
public class PaymentService {
	@Autowired
	private PaymentProxyFactory paymentProxyFactory;
	@Autowired
	private OrderService orderService;
	@Autowired
	private StoreService storeService;
	@Autowired
	protected SubChannelMapper subChannelMapper;
	
	protected final Logger logger = LogManager.getLogger(PaymentService.class);
	public Order createOrder(App app, String uid, String orderNo, Store store, PayChannel channel,
			String deviceId, String ip, Float totalFee, String orderTime,
			String sellerOrderNo, String attach, String notifyUrl,String returnUrl,
			String subject, String storeChannelId,Long subChannelId) {
		StoreChannel storeChannel = null;
		//商户指定storeChannelId优先级最高
		if(StringUtils.isNotBlank(storeChannelId)) {
			storeChannel = storeService.findStoreChannelById(Long.valueOf(storeChannelId));
		} else {			
			storeChannel = orderService.findUnusedChannelByStore(store, orderNo);
		    if(storeChannel == null){
		    	storeChannel = orderService.findUnusedChannelByAgent(store.getAgentId(),store.getId(), orderNo);
		    }
		}
		Assert.notNull(storeChannel, String.format("No avaiable store channel, please try later, sellerOrderNo: %s", StringUtils.trimToEmpty(sellerOrderNo)));		
		Order order = new Order();
		order.setApp(app);
		order.setOrderNo(orderNo);
		order.setStore(store);
		order.setStoreId(store.getId());
		order.setStoreChannel(storeChannel);		
		//如果商户指定了sub channel 
		StoreChannel newStoreChannel = null;
		if(subChannelId != null && subChannelId >0){
			 newStoreChannel = storeChannel.calcSubChannel(subChannelId); // !importance 每次下订单都要重置
		}else{
			newStoreChannel = storeChannel.calcSubChannel(); // !importance 每次下订单都要重置
			
		}
		if(newStoreChannel.getSubChannel() != null && newStoreChannel.getSubChannel().getId() != null){
			order.setStoreChannel(newStoreChannel);
			order.setSubChannelId(newStoreChannel.getSubChannel().getId());
			logger.info("订单>>"+order.getOrderNo()+" 使用子渠道:"+newStoreChannel.getSubChannel().getId());
		}
		order.setPayChannel(channel);
		order.setDeviceId(deviceId);
		order.setIp(ip);
		order.setTotalFee(totalFee);
		order.setOrderTime(orderTime);
		order.setSellerOrderNo(sellerOrderNo);
		order.setAttach(attach);
		order.setNotifyUrl(notifyUrl);
		order.setReturnUrl(returnUrl);
		order.setSubject(subject);
		orderService.insert(order);

		return order;
	}
	
	public Order createGoodsOrder(Store store, StoreGoods goods, String uid, String orderNo) {
		Assert.isTrue(goods!=null && CollectionUtils.isNotEmpty(goods.getExtGoodsList()), "No avaiable goods");
		validateQuota(store);
		
		Order order = new Order();
		order.setCodeUrl(orderService.findAvaiableQrCode(store, goods));
		order.setSubject(goods.getName());
		order.setTotalFee(goods.getAmount());
		if(StringUtils.isNotBlank(orderNo)) {
			order.setOrderNo(orderNo);
		} else {
			order.setOrderNo(IDGenerator.buildQrOrderNo(goods.getStoreId()));
		}
		order.setSellerOrderNo(uid);
		order.setStoreId(store.getId());
		order.setNotifyUrl(store.getNotifyUrl());
		order.setReturnUrl(store.getReturnUrl());
		order.setGoodsId(goods.getId());
		order.setExtStoreCode(goods.getExtStoreId());
		order.setStatus(OrderStatus.NOTPAY);
		order.setOrderTime(IDGenerator.formatNow(IDGenerator.TimePattern14));
		if(order.getCodeUrl().startsWith("https://qr.chinaums.com")) {
			order.setPayChannel(PayChannel.XIAOWEI);
		} else {
			order.setPayChannel(PayChannel.XIAOWEI_H5);
		}
		order.setAppId(store.getAppId());
		
		orderService.insert(order);
		return order;
	}

	public Bill unifiedOrder(Order order) {
		Long subChannelId = null;
		try{
			PaymentRequest request = this.toPaymentRequest(order);
			PaymentGateway gateway = order.getStoreChannel().getPaymentGateway();
			PaymentResponse response =  null;
			if(order.getStoreChannel() != null && order.getStoreChannel().getSubChannel() != null){
				 subChannelId =  order.getStoreChannel().getSubChannel().getId();
				 
				// MemoryCache.IPS_STORE_CHANNEL_BLACK_LIST.remove(subChannelId);
			}
			//如果是ips，那么触发轮询算法
			if(gateway != null && (gateway == PaymentGateway.IPSQUICK || gateway == PaymentGateway.IPSSCAN)){
				this.handleIPSLoop(order);
			}
			//创建支付代理
			IPaymentProxy paymentProxy = paymentProxyFactory.getPaymentProxy(order.getStoreChannel().getPaymentGateway());			
			response = paymentProxy.unifiedOrder(request);
  
			Bill bill = response.getBill();
			Assert.isTrue(!StringUtils.isBlank(bill.getCodeUrl()) || !StringUtils.isBlank(bill.getTokenId()),
					ApplicationConstants.STATUS_BAD_GATEWAY, NO_RESPONSE,
					response.getMsg());
			bill.setOrder(order);
			
			return bill;
		}catch(GatewayException e){
			if("-2222222".equals(e.getCode())){
				//【轮询规则埋点】商户被禁用了,把渠道加到我们的黑名单
				//先删再增加，避免重复
				logger.info("发现异常，子商户："+subChannelId+"加入黑名单");
				if(subChannelId != null){
					MemoryCache.IPS_STORE_CHANNEL_BLACK_LIST.remove(subChannelId);
					MemoryCache.IPS_STORE_CHANNEL_BLACK_LIST.add(subChannelId);
				}
				
			}
			throw e;
		}
	}

	public boolean updateBill(Order order, Bill bill) {
		if(bill == null) {
			order.setStatus(OrderStatus.PAYERROR);
		} else {
			order.setExtOrderNo(bill.getGatewayOrderNo());
			order.setCodeUrl(bill.getCodeUrl());
			order.setPrepayId(bill.getPrepayId());
			order.setTokenId(bill.getTokenId());
			order.setPayInfo(bill.getPayInfo());
			order.setStatus(bill.getOrderStatus());
		}
		return orderService.update(order);
	}

	public boolean updateTradeAmount(Order order) {
		if(order == null) {
			return true;
		}
		Store store = order.getStore();
		if(order.getGoods() == null || order.getGoods().getStoreId()==order.getStoreId()) {
			float newNonBail = store.getNonBail() + order.getTotalFee();
			store.setNonBail(newNonBail);
			return storeService.updateById(store);
		} else if(order.getGoods() != null && order.getGoods().getStoreId()!=order.getStoreId()) {
			float newBail = store.getBail() + order.getTotalFee();
			store.setBail(newBail);
			return storeService.updateById(store);
		}
		return true;
	}

	public void validateQuota(Store store) {
		Assert.notNull(store, "No store found");
		Assert.isTrue(-1 ==store.getQuota() || store.getNonBail()<store.getQuota(), "No enough quota remained");
		Assert.isTrue(-1 == store.getDailyLimit() || store.getNonBail() < store.getDailyLimit(), "Exceed transaction limit");
	}

	public void validateStoreLink(Store store, String returnUrl) {
		Assert.notNull(store, "No store found");
		Assert.notEmpty(returnUrl, STATUS_BAD_REQUEST, CODE_COMMON, "ReturnUrl cannot be null");
		Assert.isTrue(store.isValidStoreLink(returnUrl), STATUS_UNAUTHORIZED, CODE_COMMON, "Unauthorized returnUrl");
	}

	
	public Bill query(Long appId, String orderNo, String storeCode, boolean isCsr) {
		Order order = orderService.findActiveByOrderNo(orderNo);
		Assert.isTrue(storeCode.equals(order.getStore().getCode()), "No such order found for the store");
		Assert.isTrue(appId == order.getAppId(), "No such order found under the app");
		Assert.isTrue(order.isSettle() || CommonUtils.isWithinHours(order.getOrderTime(), IDGenerator.TimePattern14, 24), "Order expired");
		if(isCsr || (order.isRemoteQueralbe() && CommonUtils.isWithinHours(order.getOrderTime(), IDGenerator.TimePattern14, 24))) {
			try {
				PaymentRequest paymentRequest = toQueryRequest(order);
				IPaymentProxy paymentProxy = paymentProxyFactory.getPaymentProxy(order.getStoreChannel().getPaymentGateway());
				PaymentResponse response = paymentProxy.query(paymentRequest);
				Bill bill = response.getBill();
				bill.setOrder(order);
				if(bill!=null && !bill.getOrderStatus().equals(order.getStatus())) {
					order.setStatus(bill.getOrderStatus());
					if(StringUtils.isNotBlank(bill.getTargetOrderNo())) {
						order.setTargetOrderNo(bill.getTargetOrderNo());
					}
					orderService.update(order);
				}
				return bill;
			} catch(Exception e) {

			}
		}
		return toBill(order);
	}

	public Bill refund(Long appId, String orderNo, String storeCode, boolean isCsr) {
		Order order = orderService.findActiveByOrderNo(orderNo);
		Assert.isTrue(storeCode.equals(order.getStore().getCode()), "No such order found for the store");
		Assert.isTrue(appId == order.getAppId(), "No such order found under the app");
		Assert.isTrue(!order.isRechargeOrder(), "Recharge order can't be refunded");

		if(isCsr || (order.isRefundable()  && CommonUtils.isWithinHours(order.getOrderTime(), IDGenerator.TimePattern14, 24))) {
			PaymentRequest paymentRequest = toQueryRequest(order);
			paymentRequest.setTotalFee(order.getTotalFee());
			IPaymentProxy paymentProxy = paymentProxyFactory.getPaymentProxy(order.getStoreChannel().getPaymentGateway());
			PaymentResponse response = paymentProxy.refund(paymentRequest);

			Bill bill = response.getBill();
			if(bill !=null && (OrderStatus.REFUND.equals(bill.getOrderStatus()) || OrderStatus.REFUNDING.equals(bill.getOrderStatus())) || OrderStatus.REVOKED.equals(bill.getOrderStatus())) {
				bill.setOrder(order);
				order.setRefundOrderNo(bill.getRefundOrderNo());
				order.setRefundExtOrderNo(bill.getGatewayRefundOrderNo());
				order.setRefundTime(bill.getRefundTime());
				order.setStatus(bill.getOrderStatus());
				orderService.update(order);
			}
			return bill;
		} else {
			return toBill(order);
		}
	}

	private static final String LOCAL_ID = AppConfig.XPayConfig.getProperty("inRequest.address", CommonUtils.getLocalIP());
	private static final String DEFAULT_NOTIFY_URL = AppConfig.XPayConfig.getProperty("notify.endpoint");
	public PaymentRequest toPaymentRequest(Order order) {
		PaymentRequest request = new PaymentRequest();
		request.setExtStoreId(order.getStoreChannel().getExtStoreId());
		request.setChannelProps(order.getStoreChannel().getChannelProps());
		String deviceId = order.getDeviceId();
		deviceId = StringUtils.isBlank(deviceId)?order.getIp():deviceId;
		request.setDeviceId(deviceId);
		request.setPayChannel(order.getPayChannel());
		request.setTotalFee(order.getTotalFee());
		request.setAttach(order.getAttach());
		request.setOrderNo(order.getOrderNo());
		request.setUserOpenId(order.getDeviceId());
		request.setNotifyUrl(DEFAULT_NOTIFY_URL+order.getStoreChannel().getPaymentGateway().toString().toLowerCase());

		PaymentGateway gateway = order.getStoreChannel().getPaymentGateway();
		if(isDirectReturnChannel(gateway) ) {
			request.setReturnUrl(order.getReturnUrl());
		}
		if(PaymentGateway.JUZHEN.equals(gateway) || PaymentGateway.KEKEPAY.equals(gateway) || PaymentGateway.QFTXMP.equals(gateway)) {
			request.setServerIp(LOCAL_ID);
		} else if(PaymentGateway.MIAOFU.equals(gateway)) {
			String notifyUrl = request.getNotifyUrl() + "/"+request.getOrderNo();
			request.setNotifyUrl(notifyUrl);
		} else if(PaymentGateway.IPSSCAN.equals(gateway)
				|| PaymentGateway.IPSQUICK.equals(gateway)
				|| PaymentGateway.IPSWX.equals(gateway)){
			request.setOrderTime(order.getOrderTime());
			if(request.getChannelProps() != null) {
				IpsProps props = (IpsProps) request.getChannelProps();
				if(props.isUseH5Ext()!=null && props.isUseH5Ext()) {
					request.setExtH5(true);
				}else{
					request.setExtH5(false);
				}
			}
		}
//		else if(PaymentGateway.RUBIPAY.equals(order.getStoreChannel().getPaymentGateway())) {
//			request.setServerIp(LOCAL_ID);
//			request.setNotifyUrl(DEFAULT_NOTIFY_URL+order.getStoreChannel().getPaymentGateway().toString().toLowerCase());
//		}
//		else if(PaymentGateway.SWIFTPASS.equals(order.getStoreChannel().getPaymentGateway())) {
//			request.setServerIp(LOCAL_ID);
//			request.setNotifyUrl(DEFAULT_NOTIFY_URL+order.getStoreChannel().getPaymentGateway().toString().toLowerCase());
//		}
//

		if (StringUtils.isNotBlank(order.getSubject())) {
			request.setSubject(order.getSubject());
		} else {
			request.setSubject(DEFAULT_SUBJECT);
		}
		request.setSubject(this.customizeCsrTel(request.getSubject(), order));
		return request;
	}

	private PaymentRequest toQueryRequest(Order order) {
		PaymentRequest request = new PaymentRequest();
		PaymentGateway gateway = order.getStoreChannel().getPaymentGateway();
		request.setExtStoreId(order.getStoreChannel().getExtStoreId());
		request.setChannelProps(order.getStoreChannel().getChannelProps());
		request.setPayChannel(order.getPayChannel());
		request.setOrderNo(order.getOrderNo());
		if(isChinaUmsChannel(gateway) || PaymentGateway.JUZHEN.equals(gateway) || PaymentGateway.KEFU.equals(gateway)) {
			request.setOrderTime(order.getOrderTime());
			request.setGatewayOrderNo(order.getExtOrderNo());
		} else if(PaymentGateway.MIAOFU.equals(gateway)) {
			request.setGatewayOrderNo(order.getExtOrderNo());
		} else if(PaymentGateway.IPSQUICK.equals(gateway)
				||PaymentGateway.IPSSCAN.equals(gateway)
				||PaymentGateway.IPSWX.equals(gateway)) {
			if(OrderStatus.REFUNDING.equals(order.getStatus()) || OrderStatus.REFUND.equals(order.getStatus()) || OrderStatus.REFUNDERROR.equals(order.getStatus())){
        request.setRefundTime(order.getRefundTime());
        request.setRefundOrderNo(order.getRefundOrderNo());
        request.setRefundGatewayOrderNo(order.getRefundExtOrderNo());
			}
			request.setOrderTime(order.getOrderTime());
      request.setTotalFee(order.getTotalFee());
		}


		return request;
	}

	private Bill toBill(Order order) {
		Bill bill = new Bill();
		bill.setCodeUrl(order.getCodeUrl());
		bill.setPrepayId(order.getPrepayId());
		bill.setTokenId(order.getTokenId());;
		bill.setPayInfo(order.getPayInfo());
		bill.setOrderNo(order.getOrderNo());
		bill.setGatewayOrderNo(order.getExtOrderNo());
		bill.setOrderStatus(order.getStatus());
		bill.setOrder(order);
		return bill;
	}

	private static final String DEFAULT_SUBJECT = "游戏";
	private static final String DEFAULT_SUBJECT_CHINAUMS = "投诉热线:95534";
	private String customizeCsrTel(String subject, Order order) {
		String storeTel = order.getStore().getCsrTel();
		if(StringUtils.isNotBlank(storeTel)) {
			return subject + storeTel;
		} else if(isChinaUmsChannel(order.getStoreChannel().getPaymentGateway())) {
			return subject+"("+DEFAULT_SUBJECT_CHINAUMS+")";
		}
		return subject;
	}

	private boolean isChinaUmsChannel(PaymentGateway gateway) {
		return PaymentGateway.CHINAUMS.equals(gateway) ||
				PaymentGateway.CHINAUMSV2.equals(gateway) ||
				PaymentGateway.CHINAUMSH5.equals(gateway) ||
				PaymentGateway.CHINAUMSV3.equals(gateway) ||
				PaymentGateway.CHINAUMSWAP.equals(gateway);
	}

	private boolean isDirectReturnChannel(PaymentGateway gateway) {
		return PaymentGateway.CHINAUMS.equals(gateway) ||
				PaymentGateway.CHINAUMSV2.equals(gateway) ||
				PaymentGateway.CHINAUMSH5.equals(gateway) ||
				PaymentGateway.CHINAUMSWAP.equals(gateway) ||
				PaymentGateway.CHINAUMSV3.equals(gateway) ||
				PaymentGateway.UPAY.equals(gateway) ||
				PaymentGateway.IPSQUICK.equals(gateway) ||
				PaymentGateway.IPSWX.equals(gateway) ||
				PaymentGateway.KEKEPAY.equals(gateway) ||
				PaymentGateway.TXF.equals(gateway);
	}
	//初始化ips轮询数据
	private static long CACHE_TIMMER = 0;
	private static long CACHE_LIFE = 1000*60*10;//10 MIN
	public void initIPSLoopEnv(PaymentGateway paymentGateway){
		long now = System.currentTimeMillis();
		if(MemoryCache.IPS_STORE_SUB_CHANNEL.size() >0 && now - CACHE_TIMMER < CACHE_LIFE){
			logger.info("IPS子商户缓存数据还有:"+(CACHE_LIFE - (now- CACHE_TIMMER))/1000+"秒过期");
			return ;
		}
		StringBuilder logSB = new StringBuilder();
		//logSB.append("ips通道黑名单长度:"+MemoryCache.IPS_STORE_CHANNEL_BLACK_LIST.size());
		//logSB.append(",名单长度:"+MemoryCache.IPS_STORE_SUB_CHANNEL.size());
		for(Long blackId:MemoryCache.IPS_STORE_CHANNEL_BLACK_LIST){
			if(blackId != null && blackId >0){
				subChannelMapper.changeSubChannelStatus(blackId, Status.LOCKED.name());
				logger.info("停用子商户:"+blackId);
			}
		}
		//清空黑名单
		MemoryCache.IPS_STORE_CHANNEL_BLACK_LIST.clear();
		if(paymentGateway == PaymentGateway.IPSSCAN ||paymentGateway == PaymentGateway.IPSQUICK){
			List<SubChannel> subChannelList = subChannelMapper.findIpsAll();
			if(subChannelList == null || subChannelList.size() <=0){
				return ;
			}
			
			
			for(SubChannel item :subChannelList){				
				if(item.getStatus() == Status.LOCKED || item.getStatus() == Status.DISABLE){
					MemoryCache.IPS_STORE_SUB_CHANNEL.remove(item);
				}else{
					if(!MemoryCache.IPS_STORE_SUB_CHANNEL.contains(item)){
						MemoryCache.IPS_STORE_SUB_CHANNEL.add(item);
					}
					
				}
				
				
			}
			logSB.append("更新黑名单长度:"+MemoryCache.IPS_STORE_CHANNEL_BLACK_LIST.size());
			logSB.append(",更新名单长度:"+MemoryCache.IPS_STORE_SUB_CHANNEL.size());
			logger.info(logSB);
		}
		 try{
			 orderService.handleSubChannelMatrix();	
		    }catch(Exception e){
		    	logger.info("处理SubChannelMatrix异常>>",e);
		    }
		
		CACHE_TIMMER = System.currentTimeMillis();
	}
	//处理ips轮询,记录最新的使用情况
	private void handleIPSLoop(Order order){
		SubChannel subChannel = order.getStoreChannel().getSubChannel();
		long subChannelId = -1;
		long subChannelTime = -1;
		long subChannelUpdateTime = -1;
		//【轮询规则埋点】发起支付后把支付渠道最新的支付时间记录起来			
		if(subChannel != null && subChannel.getId() != null){
			subChannelId = subChannel.getId();
			subChannelTime = subChannel.getTimestamp();
			MemoryCache.IPS_STORE_SUB_CHANNEL.remove(subChannel);//重写了subchannel 对象的equal方法，所以可以删掉
			subChannelUpdateTime = System.currentTimeMillis();
			subChannel.setTimestamp(subChannelUpdateTime);
			MemoryCache.IPS_STORE_SUB_CHANNEL.add(subChannel);
			logger.info("更新子商户>"+subChannel.getId()+">>"+subChannel.getTimestamp());
		}
		//该子商户1.5 min内不允许发生多笔支付
		/*if(subChannelTime == -1 || subChannelUpdateTime - subChannelTime > (RiskEngine.Time_Interval)){
			
			logger.info("发起IPS通道订单支付,orderNo>>"+order.getOrderNo()+",sellerOrderNo>>"
					+order.getSellerOrderNo()+",channel>>"+order.getStoreChannel().getId()
					+",subchannel>>"+subChannelId+",subchannelTime>>"+subChannelTime+",subChannelUpdateTime>>"+subChannelUpdateTime);		
		}else{
			logger.info("发起IPS通道订单支付失败，1min内不允许重复支付,orderNo>>"+order.getOrderNo()+",sellerOrderNo>>"
					+order.getSellerOrderNo()+",channel>>"+order.getStoreChannel().getId()
					+",subchannel>>"+subChannelId+",subchannelTime>>"+subChannelTime+",subChannelUpdateTime>>"+subChannelUpdateTime);
		 throw new GatewayException(RiskEngine.frequently_code,"交易太频繁！");
		}*/
	}

}
