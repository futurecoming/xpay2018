package com.xpay.pay.proxy;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.xpay.pay.proxy.PaymentRequest.Method;
import com.xpay.pay.util.AppConfig;
import com.xpay.pay.util.CommonUtils;
import com.xpay.pay.util.CryptoUtils;
import com.xpay.pay.util.JsonUtils;

@Component
public class MiaoFuProxy implements IPaymentProxy {
	protected final Logger logger = LogManager.getLogger("AccessLog");
	@Autowired
	RestTemplate miaofuProxy;
	private static final AppConfig config = AppConfig.MiaoFuCponfig;
	private static final String baseEndpoint = config.getProperty("provider.endpoint");
	private static final String appId = config.getProperty("provider.app.id");
	private static final String appSecret = config.getProperty("provider.app.secret");

	@Override
	public PaymentResponse microPay(PaymentRequest orderRequest) {
		String url = buildUrl(Method.MicroPay, orderRequest);
		System.out.println("microPay POST: " + url);
		long l = System.currentTimeMillis();
		PaymentResponse response = null;
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
			HttpEntity<?> httpEntity = new HttpEntity<>(headers);
			response = miaofuProxy.exchange(url, HttpMethod.POST, httpEntity, PaymentResponse.class).getBody();
			logger.info("microPay result: " + response.getCode() + ", took "
					+ (System.currentTimeMillis() - l) + "ms");
		} catch (RestClientException e) {
			logger.info("microPay failed, took " + (System.currentTimeMillis() - l) + "ms", e);
			throw e;
		}
		return response;
	}
	
	@Override
	public PaymentResponse unifiedOrder(PaymentRequest orderRequest) {
		String url = buildUrl(Method.UnifiedOrder, orderRequest);
		System.out.println("unifiedOrder POST: " + url);
		long l = System.currentTimeMillis();
		PaymentResponse response = null;
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
			HttpEntity<?> httpEntity = new HttpEntity<>(headers);
			response = miaofuProxy.exchange(url, HttpMethod.POST, httpEntity, PaymentResponse.class).getBody();
			logger.info("unifiedOrder result: " + response.getCode()+" "+response.getMsg() + ", took "
					+ (System.currentTimeMillis() - l) + "ms");
		} catch (RestClientException e) {
			logger.info("unifiedOrder failed, took " + (System.currentTimeMillis() - l) + "ms", e);
			throw e;
		}
		return response;
	}
	
	@Override
	public PaymentResponse query(PaymentRequest orderRequest) {
		String url = buildUrl(Method.Query, orderRequest);
		System.out.println("query POST: " + url);
		long l = System.currentTimeMillis();
		PaymentResponse response = null;
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
			HttpEntity<?> httpEntity = new HttpEntity<>(headers);
			response = miaofuProxy.exchange(url, HttpMethod.POST, httpEntity, PaymentResponse.class).getBody();
			logger.info("query result: " + response.getCode() + " "+response.getMsg()+", took "
					+ (System.currentTimeMillis() - l) + "ms");
		} catch (RestClientException e) {
			logger.info("query failed, took " + (System.currentTimeMillis() - l) + "ms", e);
			throw e;
		}
		return response;
	}
	
	private String buildUrl(Method method, PaymentRequest orderRequest) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseEndpoint).path("/"+method.module+"/"+method.method);
		List<KeyValuePair> keyPairs = getKeyPairs(orderRequest);
		for(KeyValuePair pair : keyPairs) {
			builder.queryParam(pair.getKey(), pair.getValue());
		}
		String sign = signature(keyPairs, appSecret);
		builder.queryParam("sign", sign);
		String url = builder.build().toString();	
		return url;
	}
	
	private List<KeyValuePair> getKeyPairs(PaymentRequest orderRequest) {
		if(orderRequest == null) {
			return null;
		}
		List<KeyValuePair> keyPairs = new ArrayList<KeyValuePair>();
		
		if(StringUtils.isNotBlank(orderRequest.getBusi_code())) {
			keyPairs.add(new KeyValuePair("busi_code", orderRequest.getBusi_code()));
		}
		if(StringUtils.isNotBlank(orderRequest.getDev_id())) {
			keyPairs.add(new KeyValuePair("dev_id", orderRequest.getDev_id()));
		}
		if(StringUtils.isNotBlank(orderRequest.getOper_id())) {
			keyPairs.add(new KeyValuePair("oper_id", orderRequest.getOper_id()));
		}
		if(orderRequest.getPay_channel()!=null) {
			keyPairs.add(new KeyValuePair("pay_channel", orderRequest.getPay_channel().id));
		}
		if(StringUtils.isNotBlank(orderRequest.getAmount())) {
			keyPairs.add(new KeyValuePair("amount", String.valueOf(orderRequest.getAmount())));
		}
		if(StringUtils.isNotBlank(orderRequest.getUndiscountable_amount())) {
			keyPairs.add(new KeyValuePair("undiscountable_amount", orderRequest.getUndiscountable_amount()));
		}
		if(StringUtils.isNotBlank(orderRequest.getRaw_data())) {
			keyPairs.add(new KeyValuePair("raw_data", orderRequest.getRaw_data()));
		}
		if(StringUtils.isNotBlank(orderRequest.getAuth_code())) {
			keyPairs.add(new KeyValuePair("auth_code", orderRequest.getAuth_code()));
		}
		if(StringUtils.isNotBlank(orderRequest.getDown_trade_no())) {
			keyPairs.add(new KeyValuePair("down_trade_no", orderRequest.getDown_trade_no()));
		}
		if(StringUtils.isNotBlank(orderRequest.getTrade_no())) {
			keyPairs.add(new KeyValuePair("trade_no", orderRequest.getTrade_no()));
		}
		if(orderRequest.getTrade_no_type()!=null) {
			keyPairs.add(new KeyValuePair("trade_no_type", String.valueOf(orderRequest.getTrade_no_type().id)));
		}
		if(StringUtils.isNotBlank(orderRequest.getSubject())) {
			keyPairs.add(new KeyValuePair("subject", orderRequest.getSubject()));
		}
		if(orderRequest.getGood_details()!=null) {
			keyPairs.add(new KeyValuePair("good_details", JsonUtils.toJson(orderRequest.getGood_details())));
		}
		keyPairs.add(new KeyValuePair("app_id", appId));
		keyPairs.add(new KeyValuePair("timestamp", String.valueOf(System.currentTimeMillis()/1000)));
		//keyPairs.add(new KeyValuePair("timestamp", "1489463214"));
		keyPairs.add(new KeyValuePair("version", "v3"));

		return keyPairs;
	}

	private String signature(List<KeyValuePair> keyPairs, String appSecret) {
		keyPairs.sort((x1, x2) -> {
			return x1.getKey().compareTo(x2.getKey());
		});
		
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		for(KeyValuePair pair : keyPairs) {
			builder.queryParam(pair.getKey(), pair.getValue());
		}
		builder.queryParam("APP_SECRET", appSecret);
		String params = builder.build().toString().substring(1);
		//String encoded = CommonUtils.iso88591(CommonUtils.utf8(params));
		System.out.println("sorted params: "+params);
		String md5 = CryptoUtils.md5(params);
		System.out.println("md5 upper: "+md5.toUpperCase());
		return md5 == null? null:md5.toUpperCase();
		
	}
}
