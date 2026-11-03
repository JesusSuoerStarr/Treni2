package com.mytrain.log;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import java.util.ArrayList;
import java.util.List;

/**
 * Шагомер на аппаратном датчике TYPE_STEP_COUNTER.
 * Датчик считает шаги сам, даже когда экран выключен и приложение закрыто.
 * Значение - общее число шагов с последней перезагрузки телефона.
 */
@CapacitorPlugin(
    name = "Steps",
    permissions = {
        @Permission(strings = { "android.permission.ACTIVITY_RECOGNITION" }, alias = "activity"),
        @Permission(strings = { "android.permission.POST_NOTIFICATIONS" }, alias = "notif")
    }
)
public class StepsPlugin extends Plugin implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private boolean listening = false;
    private float lastValue = -1f;
    private final List<PluginCall> waiting = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void load() {
        sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        }
    }

    @PluginMethod
    public void isAvailable(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("available", stepSensor != null);
        call.resolve(ret);
    }

    @PluginMethod
    public void getCounter(PluginCall call) {
        if (stepSensor == null) {
            call.reject("no_sensor");
            return;
        }
        if (getPermissionState("activity") != PermissionState.GRANTED) {
            requestPermissionForAlias("activity", call, "activityCallback");
            return;
        }
        readCounter(call);
    }

    @PermissionCallback
    private void activityCallback(PluginCall call) {
        if (getPermissionState("activity") == PermissionState.GRANTED) {
            readCounter(call);
        } else {
            call.reject("permission_denied");
        }
    }

    @PluginMethod
    public void requestNotifications(PluginCall call) {
        if (Build.VERSION.SDK_INT < 33 || getPermissionState("notif") == PermissionState.GRANTED) {
            call.resolve();
            return;
        }
        requestPermissionForAlias("notif", call, "notifCallback");
    }

    @PermissionCallback
    private void notifCallback(PluginCall call) {
        call.resolve();
    }

    private void readCounter(final PluginCall call) {
        // Перерегистрация слушателя заставляет датчик сразу прислать текущее значение
        stopListening();
        waiting.add(call);
        startListening();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (waiting.remove(call)) {
                    if (lastValue >= 0) {
                        resolveValue(call);
                    } else {
                        call.reject("timeout");
                    }
                }
            }
        }, 3000);
    }

    private void resolveValue(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("counter", (double) lastValue);
        call.resolve(ret);
    }

    private void startListening() {
        if (!listening && stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
            listening = true;
        }
    }

    private void stopListening() {
        if (listening && sensorManager != null) {
            sensorManager.unregisterListener(this);
            listening = false;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        lastValue = event.values[0];
        List<PluginCall> calls = new ArrayList<>(waiting);
        waiting.clear();
        for (PluginCall c : calls) {
            resolveValue(c);
        }
        stopListening();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void handleOnDestroy() {
        stopListening();
    }
}
