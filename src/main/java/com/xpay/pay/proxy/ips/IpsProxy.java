package com.xpay.pay.proxy.ips;

import com.xpay.pay.exception.GatewayException;
import com.xpay.pay.proxy.ips.common.IpsRequest;
import com.xpay.pay.proxy.ips.common.IpsResponse;
import com.xpay.pay.proxy.ips.common.RequestHead;
import com.xpay.pay.proxy.ips.transfer.req.TransferReqXml;
import com.xpay.pay.proxy.ips.transfer.rsp.TransferRespXml;
import com.xpay.pay.proxy.ips.useropen.req.OpenUserReqXml;
import com.xpay.pay.proxy.ips.useropen.rsp.Body;
import com.xpay.pay.proxy.ips.useropen.rsp.OpenUserRespXml;
import com.xpay.pay.proxy.ips.userupdate.req.UpdateUserReqXml;
import com.xpay.pay.proxy.ips.userupdate.rsp.UpdateUserRespXml;
import com.xpay.pay.proxy.ips.withdrawal.req.WithdrawalReqXml;
import com.xpay.pay.proxy.ips.withdrawal.rsp.WithdrawalRespXml;
import com.xpay.pay.util.CryptoUtils;
import com.xpay.pay.util.TimeUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Created by sunjian on Date: 2017/12/25 下午5:04
 * Description:
 */
@Service
public class IpsProxy {

  private static final String OPEN_URL = "https://ebp.ips.com.cn/fpms-access/action/user/open";
  private static final String UPDATE_URL = "https://ebp.ips.com.cn/fpms-access/action/user/update.html";
  private static final String TRANSFER_URL = "https://ebp.ips.com.cn/fpms-access/action/trade/transfer.do";
  private static final String WITHDRAWAL_URL = "https://ebp.ips.com.cn/fpms-access/action/withdrawal/withdrawal.html";
  private static final String QUERY_URL = "https://ebp.ips.com.cn/fpms-access/action/trade/queryOrdersList";
  private static final String MD5_SIGNATURE = "jBTSbBLE3HFBhm81FhAVVFl79TUVaDNhoQrVhBUvzv0738ZaYeLYaFNOuNVHwgbtoXhRweNdmR3dtnHTGMoSMvkGsoZTKKHKdPCe8dXUCaAZqDDAJBd8kUYdW5H3FCpy";//"wmm7x3LwJdEPgmOQxcpBugQOmQewxXjwQD5ASRbKcFq7QTK6kOolYMpG9Ov5qt8RfNwaCoXX5edKt9HWbyxL5Qr1Z32oZE85Env04BUq4bX7RCHdxhT8u0rEX5BqCDsg";
  private static final String DES_IV = "pz2E1tme";//"2BBNEs28";
  private static final String DES_KEY = "lUhBAfw8BhKZw0He7tbYRbvJ";//"oov94m6YhhFxAySqF99MoLRV";

  protected final Logger logger = LogManager.getLogger(getClass());

  @Qualifier("userOpenUnmarshaller")
  @Autowired
  protected Unmarshaller userOpenUnmarshaller;

  @Qualifier("userUpdateUnmarshaller")
  @Autowired
  protected Unmarshaller userUpdateUnmarshaller;

  @Qualifier("transferUnmarshaller")
  @Autowired
  protected Unmarshaller transferUnmarshaller;

  @Qualifier("withdrawalUnmarshaller")
  @Autowired
  protected Unmarshaller withdrawalUnmarshaller;

  @Qualifier("ipsMarshaller")
  @Autowired
  protected Marshaller marshaller;

  @Autowired
  RestTemplate restTemplate;

  public Body open(String reqIp, String merCode, String tradeCode, String userType, String customerCode,
          String identityType,
          String identityNo, String userName, String legalName, String legalCardNo, String mobiePhoneNo, String telPhoneNo, String email, String contactAddress, String remark, String pageUrl,
          String s2sUrl, String directSell, String stmsAcctNo, String ipsUserName) throws IOException {
    String request = buildOpenRequest("", "", "", reqIp, merCode, tradeCode, userType, customerCode,
            identityType, identityNo,
            userName, legalName, legalCardNo, mobiePhoneNo, telPhoneNo, email, contactAddress, remark,
            pageUrl, s2sUrl, directSell, stmsAcctNo, ipsUserName);
    logger.info("open account request: " + request);
    MultiValueMap<String, String> keyPairs = new LinkedMultiValueMap<>();
    keyPairs.add("ipsRequest", request);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.set("Accept", MediaType.ALL_VALUE);
    HttpEntity<?> httpEntity = new HttpEntity<>(keyPairs, headers);
    ResponseEntity<String> response = restTemplate.exchange(OPEN_URL, HttpMethod.POST, httpEntity, String.class);
    String responseXml = response.getBody();
    logger.info("open account response: " + responseXml);
    int start = responseXml.indexOf("ipsResponse=");
    StreamSource streamSource = new StreamSource(
            new ByteArrayInputStream(responseXml.substring(start + 12).getBytes("UTF-8")));
    IpsResponse ipsResponse = (IpsResponse) userOpenUnmarshaller.unmarshal(streamSource);
    if (!"M000000".equals(ipsResponse.getRspCode())) {
      throw new GatewayException(ipsResponse.getRspCode(), ipsResponse.getRspMsg());
    }
    String xml = CryptoUtils.decryptDESede(DES_KEY, DES_IV, ipsResponse.getP3DesXmlPara());
    streamSource = new StreamSource(new ByteArrayInputStream(xml.getBytes("UTF-8")));
    OpenUserRespXml openUserRespXml = (OpenUserRespXml) userOpenUnmarshaller
            .unmarshal(streamSource);
    return openUserRespXml.getBody();
  }

  public String buildOpenRequest(String md5Signature, String desIv, String desKey,
                                 String reqIp, String merCode, String tradeCode, String userType,
                                 String customerCode, String identityType, String identityNo, String userName,
                                 String legalName, String legalCardNo, String mobiePhoneNo, String telPhoneNo, String email,
                                 String contactAddress, String remark, String pageUrl, String s2sUrl, String directSell,
                                 String stmsAcctNo, String ipsUserName) throws IOException {
    com.xpay.pay.proxy.ips.useropen.req.Body body = new com.xpay.pay.proxy.ips.useropen.req.Body();
    body.setMerAcctNo(tradeCode);
    body.setUserType(userType);
    body.setCustomerCode(customerCode);
    body.setIdentityType(identityType);
    body.setIdentityNo(identityNo);
    body.setUserName(userName);
    body.setLegalName(legalName);
    body.setLegalCardNo(legalCardNo);
    body.setMobiePhoneNo(mobiePhoneNo);
    body.setTelPhoneNo(telPhoneNo);
    body.setEmail(email);
    body.setContactAddress(contactAddress);
    body.setRemark(remark);
    body.setPageUrl(pageUrl);
    body.setS2sUrl(s2sUrl);
    body.setDirectSell(directSell);
    body.setStmsAcctNo(stmsAcctNo);
    body.setIpsUserName(ipsUserName);

    ByteArrayOutputStream os = new ByteArrayOutputStream();
    marshaller.marshal(body, new StreamResult(os));
    String bodyStr = new String(os.toByteArray(),"UTF-8");//TODO
    bodyStr = bodyStr.substring(bodyStr.indexOf("<body>"));
    logger.info("signature body: " + bodyStr + md5Signature);
    String signature = CryptoUtils.md5(bodyStr + md5Signature);
    RequestHead head = new RequestHead();
    head.setVersion("V1.0.1");
    head.setReqIp(reqIp);
    head.setReqDate(TimeUtils.formatTime(new Date(), "yyyy-MM-dd HH:mm:ss"));
    head.setSignature(signature);
    OpenUserReqXml openUserReqXml = new OpenUserReqXml();
    openUserReqXml.setBody(body);
    openUserReqXml.setHead(head);

    IpsRequest ipsRequest = new IpsRequest();
    ipsRequest.setArgMerCode(merCode);
    os = new ByteArrayOutputStream();
    marshaller.marshal(openUserReqXml, new StreamResult(os));
    String arg3DesXmlPara = CryptoUtils.encryptDESede(desKey, desIv, new String(os.toByteArray(),"UTF-8"));//TODO
    logger.info("open account des xml[3des]:" + arg3DesXmlPara);
    ipsRequest.setArg3DesXmlPara(arg3DesXmlPara);
    os = new ByteArrayOutputStream();
    marshaller.marshal(ipsRequest, new StreamResult(os));
    return new String(os.toByteArray(),"UTF-8");//TODO
  }

  public com.xpay.pay.proxy.ips.userupdate.rsp.Body update(String md5Signature, String desKey, String desIv,
                                                           String reqIp, String merCode, String customerCode, String pageUrl, String s2sUrl) throws IOException {
    String request = buildUpdateRequest(md5Signature, desKey, desIv, reqIp, merCode, customerCode, pageUrl, s2sUrl);
    logger.info("update account request: " + request);
    MultiValueMap<String, String> keyPairs = new LinkedMultiValueMap<>();
    keyPairs.add("ipsRequest", request);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.set("Accept", MediaType.ALL_VALUE);
    HttpEntity<?> httpEntity = new HttpEntity<>(keyPairs, headers);
    String response = restTemplate.exchange(UPDATE_URL, HttpMethod.POST, httpEntity, String.class).getBody();
    logger.info("update account response: " + response);
    int start = response.indexOf("ipsResponse=");
    StreamSource streamSource = new StreamSource(
            new ByteArrayInputStream(response.substring(start + 12).getBytes("UTF-8")));
    IpsResponse ipsResponse = (IpsResponse) userUpdateUnmarshaller.unmarshal(streamSource);
    if (!"M000000".equals(ipsResponse.getRspCode())) {
      throw new GatewayException(ipsResponse.getRspCode(), ipsResponse.getRspMsg());
    }
    String xml = CryptoUtils.decryptDESede(desKey, desIv, ipsResponse.getP3DesXmlPara());
    streamSource = new StreamSource(new ByteArrayInputStream(xml.getBytes("UTF-8")));
    UpdateUserRespXml updateUserRespXml = (UpdateUserRespXml) userUpdateUnmarshaller
            .unmarshal(streamSource);
    return updateUserRespXml.getBody();
  }

  public String buildUpdateRequest(String md5Signature, String desKey, String desIv,
                                   String reqIp, String merCode, String customerCode, String pageUrl, String s2sUrl)throws IOException {
    com.xpay.pay.proxy.ips.userupdate.req.Body body = new com.xpay.pay.proxy.ips.userupdate.req.Body();
    body.setCustomerCode(customerCode);
    body.setPageUrl(pageUrl);
    body.setS2sUrl(s2sUrl);

    ByteArrayOutputStream os = new ByteArrayOutputStream();
    marshaller.marshal(body, new StreamResult(os));
    String bodyStr = new String(os.toByteArray(),"UTF-8");//TODO
    bodyStr = bodyStr.substring(bodyStr.indexOf("<body>"));
    logger.info("signature body: " + bodyStr + md5Signature);
    String signature = CryptoUtils.md5(bodyStr + md5Signature);
    RequestHead head = new RequestHead();
    head.setVersion("V1.0.1");
    head.setReqIp(reqIp);
    head.setReqDate(TimeUtils.formatTime(new Date(), "yyyy-MM-dd HH:mm:ss"));
    head.setSignature(signature);
    UpdateUserReqXml updateUserReqXml = new UpdateUserReqXml();

    IpsRequest ipsRequest = new IpsRequest();
    ipsRequest.setArgMerCode(merCode);
    os = new ByteArrayOutputStream();
    marshaller.marshal(updateUserReqXml, new StreamResult(os));
    String arg3DesXmlPara = CryptoUtils.encryptDESede(desKey, desIv, os.toString());
    logger.info("update account des xml[3des]:" + arg3DesXmlPara);
    ipsRequest.setArg3DesXmlPara(arg3DesXmlPara);
    os = new ByteArrayOutputStream();
    marshaller.marshal(ipsRequest, new StreamResult(os));
    return new String(os.toByteArray(),"UTF-8");//TODO
  }

  public com.xpay.pay.proxy.ips.transfer.rsp.Body transfer(String md5Signature, String desIv, String desKey,
                                                           String reqIp, String merBillNo,
                                                           String merCode, String merAcctNo,
                                                           String customerCode, String transferAmount, String collectionItemName, String remark)
          throws IOException {
    com.xpay.pay.proxy.ips.transfer.req.Body body = new com.xpay.pay.proxy.ips.transfer.req.Body();
    body.setMerBillNo(merBillNo);
    body.setTransferType("2");
    body.setMerAcctNo(merAcctNo);
    body.setCustomerCode(customerCode);
    body.setTransferAmount(transferAmount);
    body.setCollectionItemName(collectionItemName);
    body.setRemark(remark);
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    marshaller.marshal(body, new StreamResult(os));

    String bodyStr = new String(os.toByteArray(),"UTF-8");//TODO os.toString();
    bodyStr = bodyStr.substring(bodyStr.indexOf("<body>"));
    logger.info("signature body: " + bodyStr + md5Signature);
    String signature = CryptoUtils.md5(bodyStr + md5Signature);
    RequestHead head = new RequestHead();
    head.setVersion("V1.0.1");
    head.setReqIp(reqIp);
    head.setReqDate(TimeUtils.formatTime(new Date(), "yyyy-MM-dd HH:mm:ss"));
    head.setSignature(signature);
    TransferReqXml transferReqXml = new TransferReqXml();
    transferReqXml.setBody(body);
    transferReqXml.setHead(head);
    IpsRequest ipsRequest = new IpsRequest();
    ipsRequest.setArgMerCode(merCode);
    os = new ByteArrayOutputStream();
    marshaller.marshal(transferReqXml, new StreamResult(os));
    String arg3DesXmlPara = CryptoUtils.encryptDESede(desKey, desIv, new String(os.toByteArray(),"UTF-8"));//TODO
    logger.info("transfer des xml[3des]:" + arg3DesXmlPara);
    ipsRequest.setArg3DesXmlPara(arg3DesXmlPara);
    os = new ByteArrayOutputStream();
    marshaller.marshal(ipsRequest, new StreamResult(os));
    String request = new String(os.toByteArray(),"UTF-8");//TODO os.toString();
    logger.info("transfer request: " + request);
    MultiValueMap<String, String> keyPairs = new LinkedMultiValueMap<>();
    keyPairs.add("ipsRequest", request);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.set("Accept", MediaType.ALL_VALUE);
    HttpEntity<?> httpEntity = new HttpEntity<>(keyPairs, headers);
    String response = restTemplate.exchange(TRANSFER_URL, HttpMethod.POST, httpEntity, String.class)
            .getBody();
    logger.info("transfer response: " + response);
    StreamSource streamSource = new StreamSource(
            new ByteArrayInputStream(response.getBytes("UTF-8")));
    IpsResponse ipsResponse = (IpsResponse) transferUnmarshaller.unmarshal(streamSource);
    if (!"M000000".equals(ipsResponse.getRspCode())) {
      throw new GatewayException(ipsResponse.getRspCode(), ipsResponse.getRspMsg());
    }
    String xml = CryptoUtils.decryptDESede(desKey, desIv, ipsResponse.getP3DesXmlPara());
    logger.info("transfer body xml: " + xml);
    streamSource = new StreamSource(new ByteArrayInputStream(xml.getBytes("UTF-8")));
    TransferRespXml transferRespXml = (TransferRespXml) transferUnmarshaller
            .unmarshal(streamSource);
    return transferRespXml.getBody();
  }


  public com.xpay.pay.proxy.ips.withdrawal.rsp.Body withdrawal(String reqIp, String merBillNo,
    String merCode, String customerCode, String pageUrl, String s2sUrl, String bankCard,
    String bankCode)
     throws IOException {
    String request = buildWithdrawalRequest("", "", "", reqIp, merBillNo, merCode, customerCode, pageUrl,
    s2sUrl, bankCard, bankCode);
    logger.info("withdrawal request: " + request);
    MultiValueMap<String, String> keyPairs = new LinkedMultiValueMap<>();
    keyPairs.add("ipsRequest", request);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.set("Accept", MediaType.ALL_VALUE);
    HttpEntity<?> httpEntity = new HttpEntity<>(keyPairs, headers);
    String response = restTemplate.exchange(WITHDRAWAL_URL, HttpMethod.POST, httpEntity, String.class)
            .getBody();
    logger.info("withdrawal response: " + response);
    int start = response.indexOf("ipsResponse=");
    StreamSource streamSource = new StreamSource(
            new ByteArrayInputStream(response.substring(start + 12).getBytes("UTF-8")));
    IpsResponse ipsResponse = (IpsResponse) withdrawalUnmarshaller.unmarshal(streamSource);
    if (!"M000000".equals(ipsResponse.getRspCode())) {
      throw new GatewayException(ipsResponse.getRspCode(), ipsResponse.getRspMsg());
    }
    String xml = CryptoUtils.decryptDESede(DES_KEY, DES_IV, ipsResponse.getP3DesXmlPara());
    streamSource = new StreamSource(new ByteArrayInputStream(xml.getBytes("UTF-8")));
    WithdrawalRespXml withdrawalRespXml = (WithdrawalRespXml) withdrawalUnmarshaller
            .unmarshal(streamSource);
    return withdrawalRespXml.getBody();
  }

  public String buildWithdrawalRequest(String md5Signature, String desIv, String desKey,
                                       String reqIp, String merBillNo, String merCode,
                                       String customerCode, String pageUrl, String s2sUrl, String bankCard, String bankCode)
          throws IOException {
    com.xpay.pay.proxy.ips.withdrawal.req.Body body = new com.xpay.pay.proxy.ips.withdrawal.req.Body();
    body.setMerBillNo(merBillNo);
    body.setCustomerCode(customerCode);
    body.setPageUrl(pageUrl);
    body.setS2sUrl(s2sUrl);
    body.setBankCard(bankCard);
    body.setBankCode(bankCode);
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    marshaller.marshal(body, new StreamResult(os));

    String bodyStr = os.toString();
    bodyStr = bodyStr.substring(bodyStr.indexOf("<body>"));
    logger.info("signature body: " + bodyStr + md5Signature);
    String signature = CryptoUtils.md5(bodyStr + md5Signature);
    RequestHead head = new RequestHead();
    head.setVersion("V1.0.1");
    head.setReqIp(reqIp);
    head.setReqDate(TimeUtils.formatTime(new Date(), "yyyy-MM-dd HH:mm:ss"));
    head.setSignature(signature);
    WithdrawalReqXml withdrawalReqXml = new WithdrawalReqXml();
    withdrawalReqXml.setBody(body);
    withdrawalReqXml.setHead(head);
    IpsRequest ipsRequest = new IpsRequest();
    ipsRequest.setArgMerCode(merCode);
    os = new ByteArrayOutputStream();
    marshaller.marshal(withdrawalReqXml, new StreamResult(os));
    String arg3DesXmlPara = CryptoUtils.encryptDESede(desKey, desIv, os.toString());
    logger.info("withdrawal des xml[3des]:" + arg3DesXmlPara);
    ipsRequest.setArg3DesXmlPara(arg3DesXmlPara);
    os = new ByteArrayOutputStream();
    marshaller.marshal(ipsRequest, new StreamResult(os));
    return os.toString();
  }

  public TransferAndWithdrawalResult transferAndWithdrawal(String reqIp, String merCode,
      String transferMerBillNo,
      String withdrawalMerBillNo, String merAcctNo, String customerCode, String transferAmount,
      String collectionItemName,
      String remark, String withDrawalPageUrl, String withdrawalServerUrl, String bankCard,
      String bankCode) throws IOException {
    TransferAndWithdrawalResult result = new TransferAndWithdrawalResult();
    result.setSuccess(false);
    com.xpay.pay.proxy.ips.transfer.rsp.Body transferBody = transfer("", "", "", reqIp, transferMerBillNo,
        merCode, merAcctNo, customerCode, transferAmount, collectionItemName, remark);
    if ("10".equals(transferBody.getTradeState())) {
      result.setTransferIpsBillNo(transferBody.getIpsBillNo());
      result.setTransferTradeId(transferBody.getTradeId());
      result.setTransferIpsFee(transferBody.getIpsFee());
      com.xpay.pay.proxy.ips.withdrawal.rsp.Body withdrawalBody = withdrawal(reqIp,
          withdrawalMerBillNo,
          merCode, customerCode, withDrawalPageUrl, withdrawalServerUrl, bankCard, bankCode);
      if (!"9".equals(withdrawalBody.getTradeState())) {
        result.setSuccess(true);

        result.setWithdrawalIpsBillNo(withdrawalBody.getIpsBillNo());
        result.setWithdrawalAmount(withdrawalBody.getAmount());
        result.setCustomerCode(withdrawalBody.getCustomerCode());
        result.setWithdrawalMerBillNo(withdrawalBody.getMerBillNo());
      }
      result.setWithdrawalFailMsg(withdrawalBody.getFailMsg());
    }

    return result;
  }


  public static class TransferAndWithdrawalResult {

    private boolean success;

    private String transferIpsBillNo;

    private String transferTradeId;

    private String transferIpsFee;

    private String withdrawalIpsBillNo;
    private String withdrawalAmount;
    private String customerCode;
    private String withdrawalMerBillNo;
    private String withdrawalFailMsg;

    public boolean isSuccess() {
      return success;
    }

    public void setSuccess(boolean success) {
      this.success = success;
    }

    public String getTransferIpsBillNo() {
      return transferIpsBillNo;
    }

    public void setTransferIpsBillNo(String transferIpsBillNo) {
      this.transferIpsBillNo = transferIpsBillNo;
    }

    public String getTransferTradeId() {
      return transferTradeId;
    }

    public void setTransferTradeId(String transferTradeId) {
      this.transferTradeId = transferTradeId;
    }

    public String getTransferIpsFee() {
      return transferIpsFee;
    }

    public void setTransferIpsFee(String transferIpsFee) {
      this.transferIpsFee = transferIpsFee;
    }

    public String getWithdrawalIpsBillNo() {
      return withdrawalIpsBillNo;
    }

    public void setWithdrawalIpsBillNo(String withdrawalIpsBillNo) {
      this.withdrawalIpsBillNo = withdrawalIpsBillNo;
    }

    public String getWithdrawalAmount() {
      return withdrawalAmount;
    }

    public void setWithdrawalAmount(String withdrawalAmount) {
      this.withdrawalAmount = withdrawalAmount;
    }

    public String getCustomerCode() {
      return customerCode;
    }

    public void setCustomerCode(String customerCode) {
      this.customerCode = customerCode;
    }

    public String getWithdrawalMerBillNo() {
      return withdrawalMerBillNo;
    }

    public void setWithdrawalMerBillNo(String withdrawalMerBillNo) {
      this.withdrawalMerBillNo = withdrawalMerBillNo;
    }

    public String getWithdrawalFailMsg() {
      return withdrawalFailMsg;
    }

    public void setWithdrawalFailMsg(String withdrawalFailMsg) {
      this.withdrawalFailMsg = withdrawalFailMsg;
    }
  }
  
}