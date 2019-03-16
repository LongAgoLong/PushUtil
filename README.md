# 集成华为、小米、极光的发送推送工具库
[![](https://jitpack.io/v/LongAgoLong/PushUtil.svg)](https://jitpack.io/#LongAgoLong/PushUtil)
# 使用方法
## 1.添加依赖
```java
        allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}

        dependencies {
	        implementation 'com.github.LongAgoLong:PushUtil:$jitpack-version$'
	}
```
## 2.在清单文件中配置以下必须参数（必须以leopush_开头）
```xml
        <meta-data
            android:name="PACKAGE_NAME"
            android:value="leopush_xxx" />
        <meta-data
            android:name="MI_APP_SECRET_KEY"
            android:value="leopush_xxx" />
        <meta-data
            android:name="HUAWEI_APP_ID"
            android:value="leopush_xxx" />
        <meta-data
            android:name="HUAWEI_APP_SECRET_KEY"
            android:value="leopush_xxx" />
        <meta-data
            android:name="JPUSH_APP_KEY"
            android:value="leopush_xxx" />
        <meta-data
            android:name="JPUSH_MASTER_SECRET"
            android:value="leopush_xxx" />
```
## 2.调用api
```java
	OperatePush.getInstance(this).push()
```
