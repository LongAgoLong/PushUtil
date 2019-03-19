package com.leo.operatepushlib;

public class OperateConfig {

    //用户在华为开发者联盟申请的appId和appSecret（会员中心->我的产品，点击产品对应的Push服务，点击“移动应用详情”获取）
    public static final String HUAWEI_TOKEN_URL = "https://login.cloud.huawei.com/oauth2/v2/token"; //获取认证Token的URL
    public static final String HUAWEI_API_URL = "https://api.push.hicloud.com/pushsend.do"; //应用级消息下发API
}
