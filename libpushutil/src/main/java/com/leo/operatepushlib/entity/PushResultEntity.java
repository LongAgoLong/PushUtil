package com.leo.operatepushlib.entity;


public class PushResultEntity {
    public int isMiPush;
    public String miMsgId;

    public int isJpush;
    public String jpushMsgId;

    public int isHuaweiPush;
    public long huaweiSuccessCount;
    public long huaweiFailCount;

    public long timeDiff;
}
