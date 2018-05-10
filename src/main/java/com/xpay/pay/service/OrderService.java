package com.xpay.pay.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xpay.pay.MemoryCache;
import com.xpay.pay.dao.OrderMapper;
import com.xpay.pay.exception.Assert;
import com.xpay.pay.model.Agent;
import com.xpay.pay.model.Order;
import com.xpay.pay.model.Store;
import com.xpay.pay.model.StoreChannel;
import com.xpay.pay.model.StoreExtGoods;
import com.xpay.pay.model.StoreGoods;
import com.xpay.pay.model.StoreGoods.ExtGoods;
import com.xpay.pay.po.StoreChannelInfo;
import com.xpay.pay.po.SubChannelMatrix;
import com.xpay.pay.proxy.IPaymentProxy.PayChannel;
import com.xpay.pay.proxy.PaymentResponse.OrderStatus;
import com.xpay.pay.util.AppConfig;
import com.xpay.pay.util.CommonUtils;
import com.xpay.pay.util.TimeUtils;

@Service
public class OrderService {
	protected final Logger logger = LogManager.getLogger(OrderService.class);
	@Autowired
	protected OrderMapper orderMapper;
	@Autowired
	protected AppService appService;
	@Autowired
	protected StoreService storeService;
	@Autowired
	protected StoreGoodsService goodsService;
	@Autowired
	private LockerService lockerService;
	@Autowired
	private StoreExtGoodsService storeExtGoodsService;
	

	public List<Order> findByOrderNo(String orderNo) {
		List<Order> orders = orderMapper.findByOrderNo(orderNo);
		Assert.notNull(orders, "Order not found - " + orderNo);
		for (Order order : orders) {
			order.setApp(appService.findById(order.getAppId()));
			order.setStore(storeService.findById(order.getStoreId()));
			order.setGoods(goodsService.findById(order.getGoodsId()));
		}
		return orders;
	}
	
	public Order findPaidBySellerOrderNo(String orderNo) {
		List<Order> orders = orderMapper.findBySellerOrderNo(orderNo);
		if(CollectionUtils.isEmpty(orders)) {
			return null;
		}
		Order order = orders.stream().filter(x -> x.getStatus().equals(OrderStatus.SUCCESS)).findAny().orElse(null);
		return order;
	}
	
	public Order findActiveByOrderNo(String orderNo) {
		List<Order> orders = orderMapper.findByOrderNo(orderNo);
		Assert.notNull(orders, "Order not found - " + orderNo);
		Order order = orders.stream().filter(x -> !x.getStatus().equals(OrderStatus.PAYERROR)).findAny().orElse(null);
		Assert.notNull(order, "Order not found - " + orderNo);
		order.setApp(appService.findById(order.getAppId()));
		order.setStore(storeService.findById(order.getStoreId()));
		order.setStoreChannel(storeService.findStoreChannelById(order.getStoreChannelId()));
		order.setGoods(goodsService.findById(order.getGoodsId()));
		return order;
	}
	
	public Order findAnyByOrderNo(String orderNo) {
		List<Order> orders = orderMapper.findAnyByOrderNo(orderNo);
		Assert.notNull(orders, "Order not found - " + orderNo);
		Order order = orders.stream().filter(x -> !x.getStatus().equals(OrderStatus.PAYERROR)).findAny().orElse(null);
		Assert.notNull(order, "Order not found - " + orderNo);
		order.setApp(appService.findById(order.getAppId()));
		order.setStore(storeService.findById(order.getStoreId()));
		order.setStoreChannel(storeService.findStoreChannelById(order.getStoreChannelId()));
		order.setGoods(goodsService.findById(order.getGoodsId()));
		return order;
	}
	
	public Order findActiveByExtOrderNo(String extOrderNo) {
		List<Order> orders = orderMapper.findByExtOrderNo(extOrderNo);
		Assert.notNull(orders, "Order not found - " + extOrderNo);
		Order order = orders.stream().filter(x -> !x.getStatus().equals(OrderStatus.PAYERROR)).findAny().orElse(null);
		Assert.notNull(order, "Order not found - " + extOrderNo);
		order.setApp(appService.findById(order.getAppId()));
		order.setStore(storeService.findById(order.getStoreId()));
		order.setStoreChannel(storeService.findStoreChannelById(order.getStoreChannelId()));
		order.setGoods(goodsService.findById(order.getGoodsId()));
		return order;
	}
	
	private static final long timeWindowStart = AppConfig.XPayConfig.getProperty("time.window.start", 60000L);
	private static final long timeWindoEnd = AppConfig.XPayConfig.getProperty("time.window.end", -12000L);
	public Order findActiveByOrderTime(String extStoreCode, String extOrderNo, Float amount, String subject, Date orderTime) {
		Date startTime = TimeUtils.timeBefore(orderTime, timeWindowStart);
		Date endTime = TimeUtils.timeBefore(orderTime, timeWindoEnd);
		List<Order> orders = orderMapper.findLastByExtStoreCode(extStoreCode,subject, startTime, endTime);
		if(CollectionUtils.isNotEmpty(orders)) {
			//Order order = orders.stream().filter(x ->  (Math.abs(x.getTotalFee()-amount)<=0.5f)).findFirst().orElse(null);
			Order order = orders.stream().findFirst().orElse(null);
			if(order == null) {
				return null;
			}
			order.setStore(storeService.findById(order.getStoreId()));
			order.setGoods(goodsService.findById(order.getGoodsId()));
			return order;
		}
		logger.info("find orders " + orders == null?0:orders.size() +", amount = "+amount + ",extStoreCode="+extStoreCode+", subject="+subject+", startTime="+startTime+", orderTime="+endTime);
		return null;
	}
	
	public StoreChannel findUnusedChannelByStore(Store store, String orderNo) {
		if(store == null) {
			return null;
		}
		StoreChannel channel = null;
		if(!CollectionUtils.isEmpty(store.getChannels())) {
			//channel = findUnusedChannel(store.getChannels(), orderNo); // 第1个版本的轮询实现
			channel = this.findSmartStoreChannel(store.getChannels(), store.getId());// 第2个版本的轮询实现
		} 

		return channel;
	}
	
	public StoreChannel findUnusedChannelByAgent(long agentId,long storeId, String orderNo) {
		List<StoreChannel> agentChannes = storeService.findChannelsByAgentId(agentId);
		//StoreChannel channel = findUnusedChannel(agentChannes, orderNo);// 第1个版本的轮询实现
		StoreChannel channel = this.findSmartStoreChannel(agentChannes, storeId);// 第2个版本的轮询实现
		return channel;
	}

	/*
	 * 根据订单号找出使用的支付渠道
	 */
	public StoreChannel findUnusedChannel(List<StoreChannel> channels, String orderNo) {
		// FROM bill_order WHERE bill_order.order_no = #{orderNo}
		List<Order> orders = this.findByOrderNo(orderNo);
		List<Long> usedChannels = CollectionUtils.isEmpty(orders) ? null : orders
				.stream().map(x -> x.getStoreChannelId())
				.collect(Collectors.toList());

		StoreChannel channel = channels.stream().filter(x -> x.available() && !CommonUtils.in(usedChannels, x.getId()))
				.collect(Collectors.collectingAndThen(Collectors.toList(), collected -> {
				      Collections.shuffle(collected);
				      return collected.stream();
				  })).findFirst().orElse(null);
		if(channel!=null) {
			channel.setLastUseTime(System.currentTimeMillis());
		}
		return channel;
	}
	
	public List<Order> findByStoreIdAndTime(String storeCode, Date startTime, Date endTime) {
		Store store = storeService.findByCode(storeCode);
		Assert.notNull(store, String.format("Unknown store - %s", storeCode));
		Assert.notNull(startTime, "Start time cant't be null");
		
		Date thisEndTime = endTime==null?new Date():endTime;
		return orderMapper.findByStoreIdAndTime(store.getId(), startTime, thisEndTime);
	}
	
	public List<Order> findByAgentAndTime(Agent agent, Date startTime, Date endTime) {
		Assert.notNull(startTime, "Start time cant't be null");
		
		List<Store> stores = storeService.findByAgent(agent);
		Assert.notEmpty(stores, String.format("No store found under agent - %s", agent.getId()));
		
		List<Order> result = new ArrayList<Order>();
		for(Store store: stores) {
			List<Order> orders = orderMapper.findByStoreIdAndTime(store.getId(), startTime, endTime);
			if(CollectionUtils.isNotEmpty(orders)) {
				result.addAll(orders);
			}
		}
		
		result.sort((x, y) -> {
			return (int)(x.getId()-y.getId());
		});
		
		return result;
	}
	
	public boolean insert(Order order) {
		return orderMapper.insert(order);
	}
	
	public boolean update(Order order) {
		return orderMapper.updateById(order);
	}

	private static final long bailStoreId = 1L;
	private static final long storeLockTime = AppConfig.XPayConfig.getProperty("store.lock.time", 10000L);
	private static final long goodsLockTime = AppConfig.XPayConfig.getProperty("goods.lock.time", 10000L);
	private static final long checkInterval = 1000L;
	public String findAvaiableQrCode(Store store, StoreGoods goods) {
		//logic for one nayou Goods mapped to multi china ums goods in multi ums stores
		List<StoreExtGoods> storeExtGoods = storeExtGoodsService.findByGoodsId(goods.getId());
		if(!CollectionUtils.isEmpty(storeExtGoods)) {
			return findAvaiableQrCode(goods, storeExtGoods);
		}
		
		if(storeLockTime>0) {
			boolean stoceLock  = aquireLock(store.getCode(), storeLockTime, checkInterval);
			if(!stoceLock) {
				logger.error("No lock found: "+store.getCode());
			}
			Assert.isTrue(stoceLock, "No avaiable channel");
		}
		
		StoreGoods thisGoods = null;
		if(store.isNextBailPay(goods.getAmount())) {
			thisGoods = goodsService.findByStoreIdAndAmount(bailStoreId, goods.getAmount());
		}
		thisGoods = thisGoods == null? goods: thisGoods;
		
		String qrCode = lockerService.findOldestByKeys(thisGoods.getExtQrCodes());
		ExtGoods extGoods = thisGoods.getExtGoodsList().stream().filter(x -> x.getExtQrCode().equals(qrCode)).findAny().orElse(null);
		
		if(extGoods!=null) {
			goods.setName(StringUtils.trim(goods.getName())+StringUtils.trim(extGoods.getNote()));
			
		}
		
		if(goodsLockTime>0) {
			boolean lock = aquireLock(qrCode, goodsLockTime, checkInterval);
			if(!lock) {
				logger.error("No lock found: "+qrCode);
			}
			Assert.isTrue(lock, "No avaiable channel");
		}
		
		return qrCode;
	}

	private String findAvaiableQrCode(StoreGoods goods, List<StoreExtGoods> storeExtGoods) {
		StoreExtGoods extGoods = storeExtGoods.stream().filter(x -> CollectionUtils.isNotEmpty(x.getExtGoodsList())).collect(Collectors.collectingAndThen(Collectors.toList(), collected -> {
		      Collections.shuffle(collected);
		      return collected.stream();
		  })).findFirst().orElse(null);
		
		if(storeLockTime>0) {
			boolean stoceLock  = aquireLock(extGoods.getExtStoreId(), storeLockTime, checkInterval);
			if(!stoceLock) {
				logger.error("No lock found: "+extGoods.getExtStoreId());
			}
			Assert.isTrue(stoceLock, "No avaiable channel");
		}
		
		String qrCode = lockerService.findOldestByKeys(extGoods.getExtQrCodes());
		ExtGoods umsGoods = extGoods.getExtGoodsList().stream().filter(x -> x.getExtQrCode().equals(qrCode)).findAny().orElse(null);
		
		if(extGoods!=null) {
			goods.setName(StringUtils.trim(extGoods.getName())+StringUtils.trim(umsGoods.getNote()));
			goods.setExtStoreId(StringUtils.trim(extGoods.getExtStoreId()));
		}
		
		if(goodsLockTime>0) {
			boolean lock = aquireLock(qrCode, goodsLockTime, checkInterval);
			if(!lock) {
				logger.error("No lock found: "+qrCode);
			}
			Assert.isTrue(lock, "No avaiable channel");
		}
		
		return qrCode;
	}

	public boolean aquireLock(String key, long lockTime, long checkInterval) {
		boolean lock = false;
		for(int i=0;i<10;i++) {
			lock = lockerService.lock(key, lockTime);
			if(lock) {
				break;
			}
			try {
				Thread.sleep(checkInterval);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return lock;
	}

	public List<Order> finByPayChannelAndTime(PayChannel payChannel, Date startTime, Date endTime) {
		return orderMapper.findByPayChannelAndTime(payChannel.name(), startTime, endTime);
	}
	
	/*
	 * 根据一定的轮询算法获取支付渠道
	 * channels - 商户配置可用的商户渠道列表
	 */
	public StoreChannel findSmartStoreChannel(List<StoreChannel> channels, long storeId) {
		
		if(channels == null || channels.size() == 0 || storeId <= 0){
			return null;
		}
		StoreChannel resultStoreChannel = null;
		/*List<StoreChannelInfo> sotreChannelInfo = orderMapper.findStoreChannelInfoByStoreId(storeId);
		如果根据订单表获取不到支付渠道信息，那么返回商户的第一个配置渠道；
		if(sotreChannelInfo == null || sotreChannelInfo.size() == 0){			
			return channels.get(0);
		}
		
		第一步：如果我配置的列表比使用过的列表多，从配置列表找出没使用过的渠道；
		StringBuilder infoLog = new StringBuilder();
		List<Long> channelInfoIdList = new ArrayList<Long>();
		for(StoreChannelInfo item :sotreChannelInfo){
			channelInfoIdList.add(item.getId());
			infoLog.append(item.getId()+"-"+item.getCnt()+",");
		}
		logger.info(infoLog.toString());
		resultStoreChannel = channels.stream().filter(x->!CommonUtils.in(channelInfoIdList,x.getId()) && !CommonUtils.in(MemoryCache.STORE_CHANNEL_BLACK_LIST,x.getId())).findFirst().orElse(null);
		if(resultStoreChannel != null ){
			logger.info("计算出商户:"+storeId+"应选择支付渠道:"+resultStoreChannel.getId()+",该渠道第一次被使用!");
			return resultStoreChannel;
		}
		如果列表的所有渠道都被使用过了，那么会执行如下
		List<Long> channelIdList = new ArrayList<Long>();
		for(StoreChannel channel :channels){
			channelIdList.add(channel.getId());
		}
		
		StoreChannelInfo filterMap = sotreChannelInfo.stream().filter(x->CommonUtils.in(channelIdList,x.getId()) && !CommonUtils.in(MemoryCache.STORE_CHANNEL_BLACK_LIST,x.getId())
				).sorted(Comparator.comparing(StoreChannelInfo::getCnt)).findFirst().orElse(null);
		logger.info("计算出商户:"+storeId+"应选择支付渠道:"+filterMap.getId()+",该渠道共使用了:"+filterMap.getCnt());
		
		 resultStoreChannel =  channels.stream().filter(x->x.getId() == filterMap.getId()).findFirst().orElse(null);
		*/
		//一个平台的商户有可能配置多个支付渠道
		resultStoreChannel = channels.stream().collect(Collectors.collectingAndThen(Collectors.toList(), collected -> {
		      Collections.shuffle(collected);
		      return collected.stream();
		  })).findFirst().orElse(null);
		
		 return resultStoreChannel;
	}
	public static int handleTime = 100;
	public void handleSubChannelMatrix(){
		
		if(handleTime -- <= 0 || MemoryCache.SUB_CHANNEL_MATRIX.size() == 0){

			MemoryCache.SUB_CHANNEL_MATRIX =  orderMapper.listSubChannelMatrix();		
			logger.info("from db query:"+MemoryCache.SUB_CHANNEL_MATRIX.size());
			handleTime = 100;
		}
		
	}
	
}
