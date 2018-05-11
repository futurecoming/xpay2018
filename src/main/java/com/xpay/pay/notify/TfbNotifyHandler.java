package com.xpay.pay.notify;

import com.xpay.pay.proxy.tfb.TfbProxy;
import com.xpay.pay.proxy.tfb.util.*;
import com.xpay.pay.util.CommonUtils;
import com.xpay.pay.util.CryptoUtils;
import com.xpay.pay.util.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Service
public class TfbNotifyHandler extends AbstractNotifyHandler {

    @Override
    protected NotifyBody extractNotifyBody(String url, String content) {
        String billNo = "";
        String status = "";
        String targetOrderNo = "";
        String totalFee = "";
        try {
            //获取天付宝GET过来反馈信息
            String cipherData = url.substring(url.indexOf("=")+1,url.indexOf("&retcode")-1);

            String decodedCipherData = URLDecoder.decode(cipherData, "GBK");
            //decodedCipherData = decodedCipherData.substring(0,decodedCipherData.indexOf("&retcode")-1);

            logger.info("content: "+content);
            logger.info("decodedCipherData: "+decodedCipherData);

            //数据解密
            String responseData = RSAUtils.decrypt(decodedCipherData);
            String test = RSAUtils.decrypt("WzQ7ZpHIbP9TDwNvkRJ2LNjYdYEwezxHtHsGSrQ934IdHcR7+sxUHStZBllY0y/c1Ey9eIWZNDWokZZUdYhZTHDtqpYmcsEUNxJjLiM2i53aSpRBegSIUuB+7lR2jIWp8mZ3b4Wl/MashFfSfAqvDBYNtGEO2T3gZLVBa5BUQZxQ7JzFsib+4i63n04ICQP4Y42BTeMnpUOsNWVLMtE4qs+aEG8O2ABbJuI4gCGaIM8nhKK4P3kC481um0taY1vEDxadnKXwWScHvWLnGDW4ICN5bTnY4+EN8jpP77kzg4V94rqcPqucnqTambverQCOrHRNe4mFl7imHyDVvRINQq1lIrwNAypjyQfjmPIHz/SMAAJDRXrsGVUvYvtdgkAfSmOWeoB3hoKTbR48yec1cr2n8dEpAIiUuExPMQ/jOkWL3ISC+zTmi3olp7rCWXsjXJNoaBloWVVS+3/1TTNfjT0BGF9WzpxdF4TNwgE8nvM0dnog/ArixTO8BItR7v7W");
            logger.info("test: "+test);
            logger.info("responseData: "+RSAUtils.decrypt(decodedCipherData));
            logger.info("responseData: "+responseData);

            //封装数据
            HashMap<String, String> map = RequestUtils.parseString(responseData);

            //rsa验签
            if (RSAUtils.verify(map.get("source"), map.get("sign"))) {
                logger.info("验签结果：通过");
            } else {
                logger.info("验签结果：失败");
            }

            Iterator<Map.Entry<String, String>> iter = map.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, String> entry = iter.next();
                logger.info("key= " + entry.getKey() + " and value= " + entry.getValue());
            }

            String[] params = responseData.split("&");
            String body = "";
            String sign = "";
            for (String param : params) {
                String[] pair = param.split("=");
                String key = pair[0];
                if ("params".equals(key)) {
                    String str = CommonUtils.replaceBlank(pair[1]);
                    logger.info("params: "+str);
                    body = CryptoUtils.base64Decode(str);
                } else if ("sign".equals(key)) {
                    sign = pair[1];
                }
            }
            TfbNotifyBody jsonNotify = JsonUtils.fromJson(body, TfbNotifyBody.class);
            if(jsonNotify!=null) {
                billNo = jsonNotify.getListid();
                status = jsonNotify.getResult();
                totalFee = jsonNotify.getMoney();
                targetOrderNo = jsonNotify.getSpbillno();
            }
        } catch (Exception e) {
            logger.error("TfbNotifyHandler extractNotifyBody "+content, e);
        }
        return StringUtils.isBlank(billNo)?null:new NotifyBody(billNo, null, TfbProxy.toOrderStatus(status), CommonUtils.toInt(totalFee), targetOrderNo);
    }

    private static final String SUCCESS_STR = "SUCCESS";
    @Override
    protected String getSuccessResponse() {
        return SUCCESS_STR;
    }

    private static final String FAIL_STR = "FAILED";
    @Override
    protected String getFailedResponse() {
        return FAIL_STR;
    }

    public static class TfbNotifyBody {
        private String retcode;
        private String retmsg;
        private String spid;
        private String spbillno;
        private String listid;
        private String money;
        private String cur_type;
        private String result;
        private String pay_type;
        private String user_type;
        private String attach;
        private String sign;
        private String encode_type;

        public String getRetcode() {
            return retcode;
        }
        public void setRetcode(String retcode) {
            this.retcode = retcode;
        }
        public String getRetmsg() {
            return retmsg;
        }
        public void setRetmsg(String retmsg) {
            this.retmsg = retmsg;
        }
        public String getSpid() {
            return spid;
        }
        public void setSpid(String spid) {
            this.spid = spid;
        }
        public String getListid() {
            return listid;
        }
        public void setListid(String listid) {
            this.listid = listid;
        }
        public String getSpbillno() {
            return spbillno;
        }
        public void setSpbillno(String spbillno) {
            this.spbillno = spbillno;
        }
        public String getMoney() {
            return money;
        }
        public void setMoney(String money) {
            this.money = money;
        }
        public String getCurType() {
            return cur_type;
        }
        public void setCurType(String cur_type) {
            this.cur_type = cur_type;
        }
        public String getResult() {
            return result;
        }
        public void setResult(String result) {
            this.result = result;
        }
        public String getPayType() {
            return pay_type;
        }
        public void setPayType(String pay_type) {
            this.pay_type = pay_type;
        }
        public String getUserType() {
            return user_type;
        }
        public void setUserType(String user_type) {
            this.user_type = user_type;
        }
        public String getAttach() {
            return attach;
        }
        public void setAttach(String attach) {
            this.attach = attach;
        }
        public String getSign() {
            return sign;
        }
        public void setSign(String sign) {
            this.sign = sign;
        }
        public String getEncodeType() {
            return encode_type;
        }
        public void setEncodeType(String encode_type) {
            this.encode_type = encode_type;
        }
    }
}
