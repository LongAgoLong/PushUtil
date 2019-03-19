package com.leo.operatepushlib;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.leo.operatepushlib.entity.PushResultEntity;
import com.leo.operatepushlib.http.HttpUtil;
import com.leo.operatepushlib.http.Respond;
import com.leo.operatepushlib.util.MetaValueUtil;
import com.xiaomi.xmpush.server.Constants;
import com.xiaomi.xmpush.server.Message;
import com.xiaomi.xmpush.server.Result;
import com.xiaomi.xmpush.server.Sender;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.jpush.api.JPushClient;
import cn.jpush.api.push.model.Options;
import cn.jpush.api.push.model.Platform;
import cn.jpush.api.push.model.PushPayload;
import cn.jpush.api.push.model.audience.Audience;
import cn.jpush.api.push.model.notification.AndroidNotification;
import cn.jpush.api.push.model.notification.IosNotification;
import cn.jpush.api.push.model.notification.Notification;
import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;

/**
 * create by LEO
 * on 2019/3/6 22:26
 * describe 推送工具类
 */
public final class OperatePush {
    private static OperatePush operatePush;
    private String PACKAGE_NAME;
    /*
     * 小米
     * */
    private String MI_APP_SECRET_KEY;
    /*
     * 华为
     * */
    private String HUAWEI_APP_ID;
    private String HUAWEI_APP_SECRET_KEY;
    /*
     * 极光
     * */
    private String JPUSH_APP_KEY;
    private String JPUSH_MASTER_SECRET;

    private static String accessToken;//下发通知消息的认证Token
    private static long tokenExpiredTime;  //accessToken的过期时间
    private static final int singleNumber = 600;

    private OperatePush(Context context) {
        PACKAGE_NAME = MetaValueUtil.getMetaValue(context, "PACKAGE_NAME");
        PACKAGE_NAME = PACKAGE_NAME.replace("leopush_", "");
        MI_APP_SECRET_KEY = MetaValueUtil.getMetaValue(context, "MI_APP_SECRET_KEY");
        MI_APP_SECRET_KEY = MI_APP_SECRET_KEY.replace("leopush_", "");
        HUAWEI_APP_ID = MetaValueUtil.getMetaValue(context, "HUAWEI_APP_ID");
        HUAWEI_APP_ID = HUAWEI_APP_ID.replace("leopush_", "");
        HUAWEI_APP_SECRET_KEY = MetaValueUtil.getMetaValue(context, "HUAWEI_APP_SECRET_KEY");
        HUAWEI_APP_SECRET_KEY = HUAWEI_APP_SECRET_KEY.replace("leopush_", "");
        JPUSH_APP_KEY = MetaValueUtil.getMetaValue(context, "JPUSH_APP_KEY");
        JPUSH_APP_KEY = JPUSH_APP_KEY.replace("leopush_", "");
        JPUSH_MASTER_SECRET = MetaValueUtil.getMetaValue(context, "JPUSH_MASTER_SECRET");
        JPUSH_MASTER_SECRET = JPUSH_MASTER_SECRET.replace("leopush_", "");
    }

    public static OperatePush getInstance(Context context) {
        if (null == operatePush) {
            synchronized (OperatePush.class) {
                if (null == operatePush) {
                    operatePush = new OperatePush(context);
                }
            }
        }
        return operatePush;
    }

    public Observable<PushResultEntity> push(@NonNull String title, @NonNull String content,
                                             @Nullable Map<String, String> paramsMap,
                                             @Nullable ArrayList<String> emuiPushTokens,
                                             @NonNull String tag) {
        PushResultEntity resultEntity = new PushResultEntity();
        return rxHuaweiPush(resultEntity, emuiPushTokens, title, content, paramsMap)
                .flatMap(pushResultEntity -> rxMiPush(pushResultEntity, title, content, tag, paramsMap))
                .flatMap(pushResultEntity -> rxJpush(pushResultEntity, title, content, tag, paramsMap));
    }

    public Observable<PushResultEntity> push(@NonNull String title, @NonNull String content,
                                             @Nullable Map<String, String> paramsMap,
                                             @Nullable ArrayList<String> emuiPushTokens) {
        PushResultEntity resultEntity = new PushResultEntity();
        return rxHuaweiPush(resultEntity, emuiPushTokens, title, content, paramsMap)
                .flatMap(pushResultEntity -> rxMiPush(pushResultEntity, title, content, null, paramsMap))
                .flatMap(pushResultEntity -> rxJpush(pushResultEntity, title, content, null, paramsMap));
    }
    
    public Observable<PushResultEntity> pushMI(@NonNull String title, @NonNull String content,
                                             @Nullable Map<String, String> paramsMap,
                                             @Nullable String tag) {
        return rxMiPush(new PushResultEntity(), title, content, tag, paramsMap);
    }

    public Observable<PushResultEntity> pushHuawei(@NonNull String title, @NonNull String content,
                                             @Nullable Map<String, String> paramsMap,
                                             @Nullable ArrayList<String> emuiPushTokens) {
        return rxHuaweiPush(new PushResultEntity(), emuiPushTokens, title, content, paramsMap);
    }
    
    public Observable<PushResultEntity> pushJpush(@NonNull String title, @NonNull String content,
                                             @Nullable Map<String, String> paramsMap,
                                             @Nullable String tag) {
        return rxJpush(new PushResultEntity(), title, content, tag, paramsMap);
    }

    /**
     * 华为推送
     *
     * @param resultEntity  推送结果
     * @param pushTokenList 需要推送的用户token
     * @param title         标题
     * @param content       副标题
     * @param paramsMap     扩展参数
     * @return
     */
    private Observable<PushResultEntity> rxHuaweiPush(PushResultEntity resultEntity, List<String> pushTokenList, @NonNull String title,
                                                      @NonNull String content,
                                                      @Nullable final Map<String, String> paramsMap) {
        return Observable.create(e -> {
            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
                e.onError(new Exception("参数错误"));
                return;
            }
            long startTime = System.currentTimeMillis();
            int count = 0;
            int successCount = 0;
            int pushTokenCount = pushTokenList.size();
            while (count < pushTokenCount) {
                List<String> subList = pushTokenList.subList(count,
                        (count + singleNumber) > pushTokenCount ? pushTokenCount :
                                count + singleNumber);
                String deviceTokens = new Gson().toJson(subList);

                //获取下发通知消息的认证Token
                if (tokenExpiredTime <= System.currentTimeMillis()) {
                    String msgBody = MessageFormat.format(
                            "grant_type=client_credentials&client_secret={0}&client_id={1}",
                            URLEncoder.encode(HUAWEI_APP_SECRET_KEY, "UTF-8"),
                            OperateConfig.HUAWEI_APP_ID);
                    Respond response = HttpUtil.httpPost(OperateConfig.HUAWEI_TOKEN_URL, msgBody,
                            15000, 15000);
                    if (response.getCode() == 200) {
                        JSONObject obj = new JSONObject(response.getBody());
                        accessToken = obj.getString("access_token");
                        tokenExpiredTime =
                                System.currentTimeMillis() + obj.getLong("expires_in") - 5 * 60 * 1000;
                    }
                }

                JSONObject body = new JSONObject();//仅通知栏消息需要设置标题和内容，透传消息key和value为用户自定义
                body.put("title", title);//消息标题
                body.put("content", content);//消息内容体

                JSONObject param = new JSONObject();
                param.put("appPkgName", PACKAGE_NAME);//定义需要打开的appPkgName

                JSONObject action = new JSONObject();
                action.put("type", 3);//类型3为打开APP，其他行为请参考接口文档设置
                action.put("param", param);//消息点击动作参数

                JSONObject msg = new JSONObject();
                msg.put("type", 3);//3: 通知栏消息，异步透传消息请根据接口文档设置
                msg.put("action", action);//消息点击动作
                msg.put("body", body);//通知栏消息body内容

                //自定义字段
                JSONArray customize = new JSONArray();
                if (null != paramsMap) {
                    for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
                        JSONObject object = new JSONObject();
                        object.put(entry.getKey(), entry.getValue());
                    }
                }

                JSONObject ext = new JSONObject();//扩展信息，含BI消息统计，特定展示风格，消息折叠。
                ext.put("biTag", "Trump");//设置消息标签，如果带了这个标签，会在回执中推送给CP用于检测某种类型消息的到达率和状态
                ext.put("customize", customize);
//                ext.put("icon", "https://www.nyato
// .com/addons/theme/stv1/_static/expo-image/apps/icon.png");//自定义推送消息在通知栏的图标,value为一个公网可以访问的URL

                JSONObject hps = new JSONObject();//华为PUSH消息总结构体
                hps.put("msg", msg);
                hps.put("ext", ext);

                JSONObject payload = new JSONObject();
                payload.put("hps", hps);

                String postBody = MessageFormat.format(
                        "access_token={0}&nsp_svc={1}&nsp_ts={2}&device_token_list={3}&payload={4}",
                        URLEncoder.encode(accessToken, "UTF-8"),
                        URLEncoder.encode("openpush.message.api.send", "UTF-8"),
                        URLEncoder.encode(String.valueOf(System.currentTimeMillis() / 1000),
                                "UTF-8"),
                        URLEncoder.encode(deviceTokens, "UTF-8"),
                        URLEncoder.encode(payload.toString(), "UTF-8"));

                String postUrl = OperateConfig.HUAWEI_API_URL + "?nsp_ctx=" + URLEncoder.encode(
                        "{\"ver\":\"1\", \"appId\":\"" + OperateConfig.HUAWEI_APP_ID + "\"}",
                        "UTF-8");
                Respond respond = HttpUtil.httpPost(postUrl, postBody, 50000, 50000);
                if (respond.getCode() == 200) {
                    successCount += subList.size();//成功数自增
                }
                count += singleNumber;
                Thread.sleep(1000);
            }
            resultEntity.isHuaweiPush = 1;
            resultEntity.huaweiSuccessCount = successCount;
            resultEntity.huaweiFailCount = pushTokenCount - successCount;
            long endTime = System.currentTimeMillis();
            resultEntity.timeDiff = endTime - startTime;
            e.onNext(resultEntity);
            e.onComplete();
        });
    }

    /**
     * 小米推送
     *
     * @param resultEntity 推送结果
     * @param pushTitle    标题
     * @param pushContent  副标题
     * @param tag          为空表示推送全部用户
     * @param paramsMap    扩展参数
     * @return
     */
    @SuppressLint("CheckResult")
    private Observable<PushResultEntity> rxMiPush(PushResultEntity resultEntity, String pushTitle, String pushContent,
                                                  @Nullable String tag,
                                                  @Nullable Map<String, String> paramsMap) {
        return Observable.create(e -> {
            if (TextUtils.isEmpty(pushTitle) || TextUtils.isEmpty(pushContent)) {
                e.onError(new Exception("参数错误"));
                return;
            }
            long startTime = System.currentTimeMillis();
            Constants.useOfficial();
            Sender sender = new Sender(MI_APP_SECRET_KEY);
            Message message = createMsg(pushTitle, pushContent, paramsMap);
            Result result;
            if (!TextUtils.isEmpty(tag)) {
                result = sender.broadcastAll(message, 1);
            } else {
                result = sender.broadcast(message, tag, 1);
            }
            resultEntity.isMiPush = 1;
            if (!TextUtils.isEmpty(result.getMessageId())) {
                resultEntity.miMsgId = result.getMessageId();
            }
            resultEntity.timeDiff = resultEntity.timeDiff + (System.currentTimeMillis() - startTime);
            e.onNext(resultEntity);
            e.onComplete();
        });
    }

    private Message createMsg(@NonNull String title, @NonNull String content,
                              @Nullable final Map<String, String> paramsMap) {
        String messagePayload = "This is a message";
        Message.Builder builder = new Message.Builder()
                .title(title)
                .description(content)
                .payload(messagePayload)
                .restrictedPackageName(PACKAGE_NAME)
                .notifyType(1);// 使用默认提示音提示
        for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
            builder.extra(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }

    /**
     * 极光推送
     *
     * @param resultEntity 推送结果
     * @param pushTitle    标题
     * @param pushContent  副标题
     * @param tag          为空表示推送全部用户
     * @param paramsMap    扩展参数
     * @return
     */
    private Observable<PushResultEntity> rxJpush(PushResultEntity resultEntity, String pushTitle, String pushContent,
                                                 @Nullable String tag,
                                                 @Nullable Map<String, String> paramsMap) {
        return Observable.create(emitter -> {
            if (TextUtils.isEmpty(pushTitle) || TextUtils.isEmpty(pushContent)) {
                emitter.onError(new Exception("参数错误"));
                return;
            }
            long startTime = System.currentTimeMillis();
            PushPayload pushPayload = createPushPayload(tag, pushTitle, pushContent, paramsMap);
            JPushClient jpushClient = new JPushClient(JPUSH_MASTER_SECRET, JPUSH_APP_KEY);
            cn.jpush.api.push.PushResult result = jpushClient.sendPush(pushPayload);
            resultEntity.isJpush = 1;
            resultEntity.jpushMsgId = String.valueOf(result.msg_id);
            resultEntity.timeDiff = resultEntity.timeDiff + (System.currentTimeMillis() - startTime);
            emitter.onNext(resultEntity);
            emitter.onComplete();
        });
    }

    private PushPayload createPushPayload(String title, String content,
                                          @Nullable String tag,
                                          @Nullable Map<String, String> paramsMap) {
        //ios
        IosNotification.Builder iosBuilder = IosNotification.newBuilder()
                .setAlert(content)
                .incrBadge(1)
                .setSound("sound.caf");
        //android
        AndroidNotification.Builder androidBuilder = AndroidNotification.newBuilder()
                .setAlert(content)
                .setTitle(title);
        for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
            iosBuilder.addExtra(entry.getKey(), entry.getValue());
            androidBuilder.addExtra(entry.getKey(), entry.getValue());
        }
        IosNotification iosNotification = iosBuilder.build();
        AndroidNotification androidNotification = androidBuilder.build();
        PushPayload.Builder builder = PushPayload.newBuilder()
                .setPlatform(Platform.android_ios())
                .setNotification(Notification.newBuilder()
                        .setAlert(content)
                        .addPlatformNotification(iosNotification)
                        .addPlatformNotification(androidNotification)
                        .build())
                .setOptions(Options.newBuilder()
                        .setApnsProduction(true)
                        .setTimeToLive(7 * 24 * 3600)
                        .build());
        if (TextUtils.isEmpty(tag)) {
            builder.setAudience(Audience.all());//所有用户
        } else {
            builder.setAudience(Audience.tag(tag));
        }
        return builder.build();
    }
}
