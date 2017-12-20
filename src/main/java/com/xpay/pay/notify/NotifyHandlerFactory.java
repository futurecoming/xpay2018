package com.xpay.pay.notify;

import com.xpay.pay.model.StoreChannel.PaymentGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotifyHandlerFactory {

  @Autowired
  ChinaUmsNotifyHandler chinaUmsHandler;
  @Autowired
  ChinaUmsH5NotifyHandler chinaUmsH5Handler;
  @Autowired
  JuZhenNotifyHandler juZhenHandler;
  @Autowired
  KeFuNotifyHandler keFuHandler;
  @Autowired
  KekePayNotifyHandler keKeHandler;
  @Autowired
  MiaoFuNotifyHandler miaoFuHandler;
  @Autowired
  UPayNotifyHandler uPayHandler;
  @Autowired
  QftxNotifyHandler qftxNotifyHandler;
  @Autowired
  IpsNotifyHandler ipsNotifyHandler;
  @Autowired
  SUPayNotifyHandler suPayNotifyHandler;
  @Autowired
  TxfNotifyHandler txfNotifyHandler;

  public INotifyHandler getNotifyHandler(String uri) {
    if (uri.contains(PaymentGateway.CHINAUMSH5.name().toLowerCase()) || uri
        .contains(PaymentGateway.CHINAUMSWAP.name().toLowerCase())
        || uri.contains(PaymentGateway.CHINAUMSV3.name().toLowerCase())) {
      return chinaUmsH5Handler;
    } else if (uri.contains(PaymentGateway.CHINAUMS.name().toLowerCase())) {
      return chinaUmsHandler;
    } else if (uri.contains(PaymentGateway.UPAY.name().toLowerCase())) {
      return uPayHandler;
    } else if (uri.contains(PaymentGateway.JUZHEN.name().toLowerCase())) {
      return juZhenHandler;
    } else if (uri.contains(PaymentGateway.KEKEPAY.name().toLowerCase())) {
      return keKeHandler;
    } else if (uri.contains(PaymentGateway.KEFU.name().toLowerCase())) {
      return keFuHandler;
    } else if (uri.contains(PaymentGateway.MIAOFU.name().toLowerCase())) {
      return miaoFuHandler;
    } else if (uri.contains(PaymentGateway.QFTXMP.name().toLowerCase())) {
      return qftxNotifyHandler;
    } else if (uri.contains(PaymentGateway.IPSQUICK.name().toLowerCase()) ||
        uri.contains(PaymentGateway.IPSSCAN.name().toLowerCase())) {
      return ipsNotifyHandler;
    } else if (uri.contains(PaymentGateway.SUPay.name().toLowerCase())) {
      return suPayNotifyHandler;
    } else if (uri.contains(PaymentGateway.TXF.name().toLowerCase())) {
        return txfNotifyHandler;
      } 
    else {
      return null;
    }
  }
}
