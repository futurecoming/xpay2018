package com.xpay.pay.proxy.ips.userupdate.req;

import com.xpay.pay.proxy.ips.common.RequestHead;

public class UpdateUserReqXml {
	
	private RequestHead head;
	
	private Body body;
	
	public RequestHead getHead() {
		return head;
	}
	
	public void setHead(RequestHead head) {
		this.head = head;
	}
	
	public Body getBody() {
		return body;
	}
	
	public void setBody(Body body) {
		this.body = body;
	}

}
