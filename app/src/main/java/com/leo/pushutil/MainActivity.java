package com.leo.pushutil;

import android.os.Bundle;

import com.leo.operatepushlib.OperatePush;
import com.leo.operatepushlib.entity.PushResultEntity;
import com.trello.rxlifecycle2.android.ActivityEvent;
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends RxAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        OperatePush operatePush = OperatePush.getInstance(this);
        Observable<PushResultEntity> observable = operatePush.push("ceshi", "ceshi",
                new HashMap<>(), new ArrayList<>());
        observable.compose(this.bindUntilEvent(ActivityEvent.DESTROY))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pushResultEntity -> {

                },throwable -> {

                });
    }
}
