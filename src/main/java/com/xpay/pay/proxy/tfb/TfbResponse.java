package com.xpay.pay.proxy.tfb;

public class TfbResponse {
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
