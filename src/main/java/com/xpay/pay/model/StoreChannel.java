package com.xpay.pay.model;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.xpay.pay.MemoryCache;
import com.xpay.pay.RiskEngine;
import com.xpay.pay.model.SubChannel.Status;
import com.xpay.pay.util.CommonUtils;
import com.xpay.pay.util.JsonUtils;
import com.xpay.pay.util.TimeUtils;

public class StoreChannel implements Cloneable{
	private long id;
	private String extStoreId;
	private PaymentGateway paymentGateway;
	private AtomicLong lastUseTime = new AtomicLong(0);
	private String extStoreName;
	private Long agentId;
	private String props;
	private ChannelProps channelProps;
	private Date updateDate;
	
	private SubChannel subChannel ; // 这个字段和数据库没有关联
	List<SubChannel> subChannelList = null;
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getExtStoreId() {
		return extStoreId;
	}

	public void setExtStoreId(String extStoreId) {
		this.extStoreId = extStoreId;
	}

	public PaymentGateway getPaymentGateway() {
		return paymentGateway;
	}

	public void setPaymentGateway(PaymentGateway paymentGateway) {
		this.paymentGateway = paymentGateway;
	}
	
	public long getLastUseTime() {
		return this.lastUseTime.get();
	}

	public void setLastUseTime(long lastUseTime) {
		this.lastUseTime.set(lastUseTime);;
	}
	
	public String getExtStoreName() {
		return extStoreName;
	}

	public void setExtStoreName(String extStoreName) {
		this.extStoreName = extStoreName;
	}

	public Long getAgentId() {
		return agentId;
	}

	public void setAgentId(Long agentId) {
		this.agentId = agentId;
	}

	public String getProps() {
		return props;
	}
	protected final Logger logger = LogManager.getLogger(StoreChannel.class);
	public void setProps(String props) {
		this.props = props;
		logger.info("处理channel>>"+this.getId()+"的props>>"+this.props);
		if(StringUtils.isNotBlank(props)) {
			if(this.paymentGateway == PaymentGateway.CHINAUMSH5) {
				this.channelProps = JsonUtils.fromJson(props, ChinaUmsProps.class);
			} //环讯支付
			else if(this.paymentGateway == PaymentGateway.IPSSCAN
					|| this.paymentGateway == PaymentGateway.IPSWX) {	
				this.subChannelList = RiskEngine.filterSubChannel(this.props);
				this.subChannel =  RiskEngine.buildSubChannel(this.props);
				if(this.subChannel != null && this.subChannel.getStatus() == Status.NORMAL){
					this.channelProps = JsonUtils.fromJson(this.subChannel.getProps(), IpsProps.class);
				}
				
			} 
			else if(this.paymentGateway == PaymentGateway.SUPay) {
				this.channelProps = JsonUtils.fromJson(props, SUPayProps.class);
			}
		}
 	}
	
	public ChannelProps getChannelProps() {
		return this.channelProps;
	}
	
	public void setChannelProps(ChannelProps channelProps) {
		this.channelProps = channelProps;
		if(channelProps!=null) {
			this.props = JsonUtils.toJson(channelProps);
		}
	}

	public Date getUpdateDate() {
		return updateDate;
	}

	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	public SubChannel getSubChannel() {
		return this.subChannel;
	}
	
	public StoreChannel calcSubChannel() {
		logger.info("计算子商户开始>>>>");
		if(this.subChannelList == null || this.subChannelList.size() ==0){
			this.subChannelList = RiskEngine.filterSubChannel(this.props);
		}		
		if(this.subChannelList != null){
			this.subChannel = RiskEngine.buildSubChannel(this.subChannelList);
		}
		
		if(this.subChannel != null && this.subChannel.getId() != null && this.subChannel.getStatus() == Status.NORMAL){						
			this.channelProps = JsonUtils.fromJson(this.subChannel.getProps(), IpsProps.class);
		}
		try{
			return (StoreChannel)this.clone();
			
		}catch(Exception e){
			logger.info("转换跑出异常>>"+e.toString());
			return this;
		}
	}
	
	public StoreChannel calcSubChannel(Long subChannelId) {
		logger.info("计算子商户开始>>>>subChannelId");
		if(subChannelId == null){
			return this;
		}
		this.subChannelList  = RiskEngine.filterSubChannel(this.props);;
			
		if(this.subChannelList != null){
			this.subChannel = RiskEngine.filterSubChannel(subChannelId);
		}
		
		if(this.subChannel != null && this.subChannel.getId() != null && this.subChannel.getStatus() == Status.NORMAL){						
			this.channelProps = JsonUtils.fromJson(this.subChannel.getProps(), IpsProps.class);
		}
		try{
			return (StoreChannel)this.clone();
			
		}catch(Exception e){
			logger.info("转换跑出异常>>"+e.toString());
			return this;
		}		
	}



	private static final long BLOCK_TIME_DAY= 2*1000;
	private static final long BLOCK_TIME_NIGHT= 5*1000;
	public boolean available() {
		long blockTime = TimeUtils.isNowDayTime()?BLOCK_TIME_DAY:BLOCK_TIME_NIGHT;
		boolean avail = System.currentTimeMillis()-this.lastUseTime.get()>blockTime;
		return avail;
	}

	public enum PaymentGateway {
		MIAOFU("jspay", "query", "refund"), 
		SWIFTPASS("unified.trade.pay", "unified.trade.query", "unified.trade.refund"), 
		CHINAUMS("yuedan.getQRCode", "yuedan.query", "yuedan.refund"), 
		CHINAUMSV2("bills.getQRCode", "bills.query", "bills.refund", 0.6f), 
		CHINAUMSV3("trade.precreate", "query", "refund", 0.6f), 
//		CHINAUMSH5("WXPay.jsPay", "query", "refund"),
		CHINAUMSH5("WXPay.jsPay", "query", "refund", 0.6f),
		CHINAUMSWAP("WXPay.jsPay", "query", "refund", 0.6f),
		UPAY("", "query", "refund"),
		RUBIPAY("unified.trade.pay", "unified.trade.query", "unified.trade.refund"),
		BAIFU("trade.weixin.gzpay", "query", "unified.trade.refund"), 
		JUZHEN("070201", "070101", ""), 
		KEFU("msBank_WeChatPay", "msBank_ScanPayQuery", ""), 
		KEKEPAY("pay","query",""),
		QFTXMP("pay", "query", ""),
		SUPay("pay", "", ""),
		IPSSCAN("pay","query", "refund"),
		IPSQUICK("pay","query", "refund"),
		IPSWX("pay","query", "refund"),
		HM("pay","query", ""),
		TXF("", "query", "refund"),
		CHINA_PNR("pay","query", "refund")
		;
		
		String unifiedOrder;
		String query;
		String refund;
		Float baseBailPercentage;
		
		PaymentGateway(String unifiedOrder, String query, String refund) {
			this.unifiedOrder = unifiedOrder;
			this.query = query;
			this.refund = refund;
			this.baseBailPercentage = 0f;
		}
		
		PaymentGateway(String unifiedOrder, String query, String refund, Float baseBailPercentage) {
			this.unifiedOrder = unifiedOrder;
			this.query = query;
			this.refund = refund;
			this.baseBailPercentage = baseBailPercentage;
		}
		
		public String UnifiedOrder() {
			return this.unifiedOrder;
		}
		
		public String Query() {
			return this.query;
		}
		
		public String Refund() {
			return this.refund;
		}
		
		public Float getBaseBailPercentage() {
			return this.baseBailPercentage;
		}
		
	}
	
	public static interface ChannelProps {
		
	}
	
	public static class ChinaUmsProps implements ChannelProps {
		private String tid;
		private String msgSrcId;
		private String msgSrc;
		private String signKey;
		private String instMid;
		public String getTid() {
			return tid;
		}
		public void setTid(String tid) {
			this.tid = tid;
		}
		public String getMsgSrcId() {
			return msgSrcId;
		}
		public void setMsgSrcId(String msgSrcId) {
			this.msgSrcId = msgSrcId;
		}
		public String getMsgSrc() {
			return msgSrc;
		}
		public void setMsgSrc(String msgSrc) {
			this.msgSrc = msgSrc;
		}
		public String getSignKey() {
			return signKey;
		}
		public void setSignKey(String signKey) {
			this.signKey = signKey;
		}
		public String getInstMid() {
			return instMid;
		}
		public void setInstMid(String instMid) {
			this.instMid = instMid;
		}
	}

	public static class IpsProps implements ChannelProps {
		private String merType;
		private String subMerCode;
		private String goodNames;
		private Boolean useH5Ext;

		public String getMerType() {
			return merType;
		}

		public void setMerType(String merType) {
			this.merType = merType;
		}

		public String getSubMerCode() {
			return subMerCode;
		}

		public void setSubMerCode(String subMerCode) {
			this.subMerCode = subMerCode;
		}

		public Boolean isUseH5Ext() {
			return useH5Ext;
		}

		public void setUseH5Ext(Boolean useH5Ext) {
			this.useH5Ext = useH5Ext;
		}

		public String getGoodNames() {
			return goodNames;
		}

		public void setGoodNames(String goodNames) {
			this.goodNames = goodNames;
		}
		
	}

  public static class SUPayProps implements ChannelProps {

		private String itemId;
		private String payType;
		private String serverCode;

		public String getItemId() {
			return itemId;
		}

		public void setItemId(String itemId) {
			this.itemId = itemId;
		}

		public String getPayType() {
			return payType;
		}

		public void setPayType(String payType) {
			this.payType = payType;
		}

		public String getServerCode() {
			return serverCode;
		}

		public void setServerCode(String serverCode) {
			this.serverCode = serverCode;
		}
		
	}

	public enum ChannelType {
		WECHAT, ALIPAY, BANK, H5
	}
	
	public static void main(String args[]){
		/*String json = "{\"merType\":\"1\",\"subMerCode\":\"2\",\"useH5Ext\":true}";
		IpsProps test = JsonUtils.fromJson(json, IpsProps.class);
		
		SubChannel s1 = new SubChannel();
		s1.setId(1l);s1.setPoolType("pool1");s1.setStatus(Status.NORMAL);
		
		SubChannel s2 = new SubChannel();
		s2.setId(2l);s2.setPoolType("pool1");s2.setStatus(Status.NORMAL);
		
		SubChannel s3 = new SubChannel();
		s3.setId(3l);s3.setPoolType("pool1");s3.setStatus(Status.NORMAL);
		
		MemoryCache.IPS_STORE_SUB_CHANNEL.add(s1);
		MemoryCache.IPS_STORE_SUB_CHANNEL.add(s2);
		MemoryCache.IPS_STORE_SUB_CHANNEL.add(s3);
		for(int i=0;i<30;i++){
			SubChannel subChannel = MemoryCache.IPS_STORE_SUB_CHANNEL.stream().filter(x->x.getPoolType()!= null 
					&& x.getPoolType().equals("pool1")
					&& !CommonUtils.in(MemoryCache.IPS_STORE_CHANNEL_BLACK_LIST,x.getId()
					)).sorted(Comparator.comparing(SubChannel::getTimestamp)).findFirst().orElse(null);
			System.out.println(subChannel.getId()+"----"+subChannel.getTimestamp());
			MemoryCache.IPS_STORE_SUB_CHANNEL.remove(subChannel);//重写了subchannel 对象的equal方法，所以可以删掉
			long subChannelUpdateTime = System.currentTimeMillis();
			subChannel.setTimestamp(subChannelUpdateTime);
			MemoryCache.IPS_STORE_SUB_CHANNEL.add(subChannel);
		}
		
		*/
		
		StoreChannel sc = new StoreChannel();
		sc.setId(1l);
		
		StoreChannel sc1 = null;
		try {
			sc1 = (StoreChannel) sc.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sc.setId(2l);
		
		System.out.println(sc.getId()+">>>"+sc1.getId());
		
	}
}
