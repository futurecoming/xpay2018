package com.xpay.pay.proxy.tfb;

public class TfbRequest {
    private String spid;
    private String sp_userid;
    private String spbillno;
    private String money;
    private String cur_type;
    private String user_type;
    private String notify_url;
    private String return_url;
    private String errpage_url;
    private String memo;
    private String expire_time;
    private String attach;
    private String bank_accno;
    private String bank_acctype;
    private String sign;
    private String encode_type;
    private String risk_ctrl;

    public String getSpid() {
        return spid;
    }
    public void setSpid(String spid) {
        this.spid = spid;
    }

    public String getSpUserid() {
        return sp_userid;
    }
    public void setSpUserid(String sp_userid) {
        this.sp_userid = sp_userid;
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

    public String getUserType() {
        return user_type;
    }
    public void setUserType(String user_type) {
        this.user_type = user_type;
    }

    public String getNotifyUrl() {
        return notify_url;
    }
    public void setNotifyUrl(String notify_url) {
        this.notify_url = notify_url;
    }

    public String getReturnUrl() {
        return return_url;
    }
    public void setReturnUrl(String return_url) {
        this.return_url = return_url;
    }

    public String getErrpageUrl() {
        return errpage_url;
    }
    public void setErrpageUrl(String errpage_url) {
        this.errpage_url = errpage_url;
    }

    public String getMemo() {
        return memo;
    }
    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getExpireTime() {
        return expire_time;
    }
    public void setExpireTime(String expire_time) {
        this.expire_time = expire_time;
    }

    public String getAttach() {
        return attach;
    }
    public void setAttach(String attach) {
        this.attach = attach;
    }

    public String getBankAccno() {
        return bank_accno;
    }
    public void setBankAccno(String bank_accno) {
        this.bank_accno = bank_accno;
    }

    public String getBankAcctype() {
        return bank_acctype;
    }
    public void setBankAcctype(String bank_acctype) {
        this.bank_acctype = bank_acctype;
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

    public String getRiskCtrl() {
        return risk_ctrl;
    }
    public void setRiskCtrl(String risk_ctrl) {
        this.risk_ctrl = risk_ctrl;
    }
}
