package com.xpay.pay.proxy.ips.userupdate.rsp;

public class Head {

	  private String version;
	  private String signature;

	  public String getVersion() {
	    return version;
	  }

	  public void setVersion(String version) {
	    this.version = version;
	  }

	  public String getSignature() {
	    return signature;
	  }

	  public void setSignature(String signature) {
	    this.signature = signature;
	  }
}
