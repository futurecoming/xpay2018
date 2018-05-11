package com.xpay.pay.proxy.tfb;

import static com.xpay.pay.model.StoreChannel.PaymentGateway.TFB;

import com.xpay.pay.model.Bill;
import com.xpay.pay.proxy.IPaymentProxy;
import com.xpay.pay.proxy.PaymentRequest;
import com.xpay.pay.proxy.PaymentResponse;
import com.xpay.pay.proxy.tfb.util.*;
import com.xpay.pay.exception.GatewayException;
import com.xpay.pay.util.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.TreeMap;


@Component
public class TfbProxy implements IPaymentProxy {

    protected final Logger logger = LogManager.getLogger("AccessLog");
    private static final String hostName = "http://apitest2.tfb8.com";
    public static String cardPayApplyApi = hostName + "/cgi-bin/v2.0/api_mwappay_apply.cgi?cipher_data={cipherData}";// 银联mwap支付请求地址
    public static String orderQueryApi = hostName + "/cgi-bin/v2.0/api_single_qry_order.cgi";// 支付结果单笔查询
    public static String orderRefundApi = hostName + "/cgi-bin/v2.0/api_cardpay_refundreq.cgi";// 单笔退款接口退款
    public static String orderRefundqryApi = hostName + "/cgi-bin/v2.0/api_single_qry_refundorder.cgi";// 单笔退款接口退款

    // MD5密钥，国采分配给商户的用于签名和验签的key
    public static String key = "12345";

    // 商户/平台在国采注册的账号。国采维度唯一，固定长度10位,1800212300
    private static String spid = "1800044038";

    // 用户号，持卡人在商户/平台注册的账号。商户/平台维度唯一，必须为纯数字
    public static String sp_userid = "101347613";

    // 服务器编码类型
    public static String serverEncodeType = "GBK";

    // 订单有效时长，以国采服务器时间为准的订单有效时间长度。单位:秒，如果不填则采用默认值
    private static String expire_time = "180000";

    // 订单金额的类型。1 – 人民币(单位: 分)
    public static String cur_type = "1";

    // 商户的用户使用的终端类型。1 – PC端，2 – 手机端
    public static String channel = "1";

    // 签名的方法。目前支持: MD5，RSA
    private static String encode_type = "MD5";

    // 网银支付结果页面通知地址。需要相互自行修改(不能含有’&=等字符)
    //public static String notify_url = "http://localhost:8080/tfb_cardpay_JAVA_UTF8_MD5_RSA/notice_url.jsp";

    // 网银支付结果后台通知地址。需要相互自行修改(不能含有’&=等字符)
    //public static String return_url = "http://api.gcdev.tfb8.com/cgi-bin/v2.0/test_sp_callback.cgi";

    // 当申请过程中出现异常时展示错误信息的页面地址，如果为空将展示国采自己的错误提示页面。(不能含有’&=等字符)
    //public static String errpage_url = "";

    /*   // 商户的私钥,需要PKCS8格式
       public static String PRIVATE_KEY = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAK+LzCZnUWIsRSxKyGZrZI+BU+Y+wnTXPpVbKcm5LT1fg/+o7aQR6B7pheWSEH5xLiFmtUkWSgZ7tYJhjovJkwgIJ91BQBg3rVT3xPCjeVu88mrdvzQOe6sS5WNPu3Wxbht9uACO16zupdDrruhjRUaCX5tkLukccU3bqp9FpkkNAgMBAAECgYBx8mB1nSLqgqnz8ibatGL185CuJ5a5mO36rM4XLqf66oEX9mMq2KS/S/2p4oHqUTUMYUrTQjCSvMI4+3I3soRI4k4J5VsyP9zHyHzafvNUTUyp2ybaVgmh3oxU4sx015fd+3Qc219l+Jdod+rIi68NJqhhMUU+q7yxmesCUCkZAQJBAOWH5bu9FmFIiSjWHVj6XE0904KOWSoHsenymzMZfM0s1kck1hUvwntUcmUhkiuz4BBmiKOy65MtNyJ6ChE3UP0CQQDDyi/gX/xOhCOpWoDMnYyKGyQH7GMJBIwK/X80Yha3Qtl/WrdqrpNV/ZHyQJgcIQFoMNLbNotoUOMAjthkrR1RAkAU5RAmzQnShVXnH8bAKNpqNayhf+/iAZ1SnMFAH5va2bAP/ex3NUfRDljzl+DElbVaCNt7e3gyh7UzMETmWFDJAkAwFtw1jz3ohxo/QYR7PYNEdLAf5hbZIy3GkUcKNcGAl8HWPxDn+iMkLtkHGIiD+DNhRQS1ZStOnvdyrqNF7yNRAkEAxm2MZmPHl+7jbDjHG6c+3SE6e0s7iZyatgh2gosKXdpqUWe3zVXPN04kLarZ7tasl1IBqHr1LpzdHEUReiNRBQ==";
   */    // 商户的私钥,需要PKCS8格式
    public static String PRIVATE_KEY = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAK+LzCZnUWIsRSxKyGZrZI+BU+Y+wnTXPpVbKcm5LT1fg/+o7aQR6B7pheWSEH5xLiFmtUkWSgZ7tYJhjovJkwgIJ91BQBg3rVT3xPCjeVu88mrdvzQOe6sS5WNPu3Wxbht9uACO16zupdDrruhjRUaCX5tkLukccU3bqp9FpkkNAgMBAAECgYBx8mB1nSLqgqnz8ibatGL185CuJ5a5mO36rM4XLqf66oEX9mMq2KS/S/2p4oHqUTUMYUrTQjCSvMI4+3I3soRI4k4J5VsyP9zHyHzafvNUTUyp2ybaVgmh3oxU4sx015fd+3Qc219l+Jdod+rIi68NJqhhMUU+q7yxmesCUCkZAQJBAOWH5bu9FmFIiSjWHVj6XE0904KOWSoHsenymzMZfM0s1kck1hUvwntUcmUhkiuz4BBmiKOy65MtNyJ6ChE3UP0CQQDDyi/gX/xOhCOpWoDMnYyKGyQH7GMJBIwK/X80Yha3Qtl/WrdqrpNV/ZHyQJgcIQFoMNLbNotoUOMAjthkrR1RAkAU5RAmzQnShVXnH8bAKNpqNayhf+/iAZ1SnMFAH5va2bAP/ex3NUfRDljzl+DElbVaCNt7e3gyh7UzMETmWFDJAkAwFtw1jz3ohxo/QYR7PYNEdLAf5hbZIy3GkUcKNcGAl8HWPxDn+iMkLtkHGIiD+DNhRQS1ZStOnvdyrqNF7yNRAkEAxm2MZmPHl+7jbDjHG6c+3SE6e0s7iZyatgh2gosKXdpqUWe3zVXPN04kLarZ7tasl1IBqHr1LpzdHEUReiNRBQ==";

    // 天付宝公钥
    public static String TFB_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCjDrkoVbyv4jTxeKtKEiK2mZiezQvfJV3sGhiwOnB+By5sa5Sa6Ls4dt5AGVqKHxyQVKRpu/utwtEt2MijWx45P1y2xGe7oDz2hUXP0j8sSa1NP26TmWHwO7czgJxxrdJ6RNqskSfjwsa5YMsqmcrumxUIxeCg5EOkgU26bnPoZQIDAQAB";

    @Autowired
    RestTemplate tfbProxy;

    /*@Override
    public PaymentResponse unifiedOrder(PaymentRequest request) {
        String url = cardPayApplyApi;

        long l = System.currentTimeMillis();
        PaymentResponse response = null;
        try {
            TfbRequest tfbRequest = this.toTfbRequest(request);
            TreeMap<String, String> paramsMap = this.getParamsMap(tfbRequest);
            //拼接签名原串
            String paramSrc = RequestUtils.getParamSrc(paramsMap);

            //生成签名
            String sign = MD5Utils.sign(paramSrc);

            //rsa加密原串
            String encryptSrc = paramSrc + "&sign=" + sign;//加密原串

            //rsa密串
            String cipherData = RSAUtils.encrypt(encryptSrc);

            url = url.replace("{cipherData}", cipherData);
            logger.info("unifiedOrder POST: " + url+", body "+JsonUtils.toJson(tfbRequest));

            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
            HttpEntity<?> httpEntity = new HttpEntity<>(tfbRequest, headers);
            TfbResponse tfbResponse = tfbProxy.exchange(url, HttpMethod.POST, httpEntity, TfbResponse.class).getBody();
            logger.info("unifiedOrder result: " + tfbResponse.getRetcode() + " "+tfbResponse.getRetmsg() + ", took "
                    + (System.currentTimeMillis() - l) + "ms");
            response = toPaymentResponse(tfbResponse);
        } catch (RestClientException e) {
            logger.info("unifiedOrder failed, took " + (System.currentTimeMillis() - l) + "ms", e);
            throw e;
        }
        return response;
    }*/

    @Override
    public PaymentResponse unifiedOrder(PaymentRequest request) {
        request.setPayChannel(null);
        String url = DEFAULT_JSAPI_URL + request.getOrderNo();
        PaymentResponse response = new PaymentResponse();
        response.setCode(PaymentResponse.SUCCESS);
        Bill bill = new Bill();
        bill.setCodeUrl(url);
        bill.setOrderStatus(PaymentResponse.OrderStatus.NOTPAY);
        response.setBill(bill);
        return response;
    }

    public String getJsUrl(PaymentRequest request) {

        String url = cardPayApplyApi;

        long l = System.currentTimeMillis();
        try {
            TfbRequest tfbRequest = this.toTfbRequest(request);
            TreeMap<String, String> paramsMap = this.getParamsMap(tfbRequest);
            //拼接签名原串
            String paramSrc = RequestUtils.getParamSrc(paramsMap);

            //生成签名
            String sign = MD5Utils.sign(paramSrc);

            //rsa加密原串
            String encryptSrc = paramSrc + "&sign=" + sign;//加密原串

            //rsa密串
            String cipherData = RSAUtils.encrypt(encryptSrc);

            url = url.replace("{cipherData}", cipherData);
            logger.info("unifiedOrder POST: " + url+", body "+JsonUtils.toJson(encryptSrc));

            return url;
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//            headers.set("Accept", MediaType.ALL_VALUE);
//            HttpEntity<?> httpEntity = new HttpEntity<>(headers);
//            String response = tfbProxy.exchange(url, HttpMethod.POST, httpEntity, String.class).getBody();
//            logger.info("unifiedOrder result: " + response + ", took "
//                    + (System.currentTimeMillis() - l) + "ms");
//            return response;
////            TfbResponse tfbResponse = tfbProxy.exchange(url, HttpMethod.POST, httpEntity, TfbResponse.class).getBody();
////            logger.info("unifiedOrder result: " + tfbResponse.getRetcode() + " "+tfbResponse.getRetmsg() + ", took "
////                    + (System.currentTimeMillis() - l) + "ms");
////            response = toPaymentResponse(tfbResponse);
////            return response.getBill().getCodeUrl();
        } catch (RestClientException e) {
            logger.info("unifiedOrder failed, took " + (System.currentTimeMillis() - l) + "ms", e);
            throw e;
        }
    }

    private TreeMap<String, String> getParamsMap(TfbRequest tfbRequest) {
        if (tfbRequest == null) {
            return null;
        }

        TreeMap<String, String> paramsMap = new TreeMap<String, String>();
        if (StringUtils.isNotBlank(tfbRequest.getSpid())) {
            paramsMap.put("spid", tfbRequest.getSpid());
        }
        if (StringUtils.isNotBlank(tfbRequest.getSpUserid())) {
            paramsMap.put("sp_userid", tfbRequest.getSpUserid());
        }
        if (StringUtils.isNotBlank(tfbRequest.getSpbillno())) {
            paramsMap.put("spbillno", tfbRequest.getSpbillno());
        }
        if (StringUtils.isNotBlank(tfbRequest.getMoney())) {
            paramsMap.put("money", tfbRequest.getMoney());
        }
        if (StringUtils.isNotBlank(tfbRequest.getCurType())) {
            paramsMap.put("cur_type", tfbRequest.getCurType());
        }
        if (StringUtils.isNotBlank(tfbRequest.getNotifyUrl())) {
            paramsMap.put("notify_url", tfbRequest.getNotifyUrl());
        }
        if (StringUtils.isNotBlank(tfbRequest.getReturnUrl())) {
            paramsMap.put("return_url", tfbRequest.getReturnUrl());
        }
        if (StringUtils.isNotBlank(tfbRequest.getErrpageUrl())) {
            paramsMap.put("errpage_url", tfbRequest.getErrpageUrl());
        }
        if (StringUtils.isNotBlank(tfbRequest.getMemo())) {
            paramsMap.put("memo", tfbRequest.getMemo());
        }
        if (StringUtils.isNotBlank(tfbRequest.getExpireTime())) {
            paramsMap.put("expire_time", tfbRequest.getExpireTime());
        }
        if (StringUtils.isNotBlank(tfbRequest.getUserType())) {
            paramsMap.put("user_type", tfbRequest.getUserType());
        }
        if (StringUtils.isNotBlank(tfbRequest.getAttach())) {
            paramsMap.put("attach", tfbRequest.getAttach());
        }
        if (StringUtils.isNotBlank(tfbRequest.getBankAccno())) {
            paramsMap.put("bank_accno", tfbRequest.getBankAccno());
        }
        if (StringUtils.isNotBlank(tfbRequest.getBankAcctype())) {
            paramsMap.put("bank_acctype", tfbRequest.getBankAcctype());
        }
        if (StringUtils.isNotBlank(tfbRequest.getEncodeType())) {
            paramsMap.put("encode_type", tfbRequest.getEncodeType());
        }

        return paramsMap;
    }

    private TfbRequest toTfbRequest(PaymentRequest request) {
        TfbRequest tfbRequest = new TfbRequest();
        tfbRequest.setSpid(spid);
        tfbRequest.setSpUserid(request.getUserOpenId());
        tfbRequest.setSpbillno(request.getOrderNo());
        tfbRequest.setMoney(String.valueOf((int)(request.getTotalFee()*100)));
        tfbRequest.setCurType("1");
        tfbRequest.setReturnUrl(request.getReturnUrl());
        tfbRequest.setNotifyUrl(request.getNotifyUrl());
        tfbRequest.setErrpageUrl("");
        tfbRequest.setMemo(request.getSubject());
        tfbRequest.setExpireTime(expire_time);
        tfbRequest.setAttach(request.getAttach());
        tfbRequest.setUserType("1");
        tfbRequest.setBankAcctype("01");
        tfbRequest.setBankAccno("6222021001123984277");
        tfbRequest.setEncodeType(encode_type);
        return tfbRequest;
    }

    @Override
    public PaymentResponse query(PaymentRequest request) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PaymentResponse refund(PaymentRequest request) {
        // TODO Auto-generated method stub
        return null;
    }

    private PaymentResponse toPaymentResponse(TfbResponse tfbResponse) {
        if (tfbResponse == null || tfbResponse.getRetmsg() !="") {
            String code = tfbResponse == null ? NO_RESPONSE : tfbResponse
                    .getRetcode();
            String msg = tfbResponse == null ? "No response"
                    : tfbResponse.getRetmsg();
            throw new GatewayException(code, msg);
        }
        PaymentResponse response = new PaymentResponse();
        response.setCode(PaymentResponse.SUCCESS);
        Bill bill = new Bill();
        bill.setOrderNo(tfbResponse.getSpbillno());
        response.setBill(bill);
        return response;
    }

    public static PaymentResponse.OrderStatus toOrderStatus(String status) {
        if("0".equals(status)) {
            return PaymentResponse.OrderStatus.NOTPAY;
        } else if("1".equals(status)) {
            return PaymentResponse.OrderStatus.SUCCESS;
        } else {
            return PaymentResponse.OrderStatus.PAYERROR;
        }
    }

}
