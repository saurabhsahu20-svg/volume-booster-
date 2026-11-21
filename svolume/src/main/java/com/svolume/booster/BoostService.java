package com.svolume.booster;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.media.audiofx.LoudnessEnhancer;
import android.os.Build;
import android.os.IBinder;

public class BoostService extends Service {
    public static final String ACTION_START="com.svolume.booster.START";
    public static final String ACTION_STOP="com.svolume.booster.STOP";
    private static final String CHANNEL="svol_active";
    private static final int NOTIFICATION_ID=9;
    private LoudnessEnhancer effect;
    private android.content.SharedPreferences prefs;
    private void record(boolean running,boolean effectReady){
        prefs.edit().putBoolean("running",running)
            .putBoolean("effect_initialized",effectReady).apply();
    }
    @Override public void onCreate(){
        super.onCreate();
        prefs=getSharedPreferences("svolume",MODE_PRIVATE);
        NotificationManager nm=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26){
            nm.createNotificationChannel(new NotificationChannel(CHANNEL,
                 "S Volume Booster effect",NotificationManager.IMPORTANCE_LOW));
        }
    }
    private Notification notification(){
        Intent open=new Intent(this,MainActivity.class);
        PendingIntent click=PendingIntent.getActivity(this,1,open,
              PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Intent stop=new Intent(this,BoostService.class).setAction(ACTION_STOP);
        PendingIntent stopAction=PendingIntent.getService(this,2,stop,
              PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this,CHANNEL)
           .setSmallIcon(R.drawable.ic_s_volume)
           .setContentTitle("S Volume Booster")
           .setContentText("Experimental global audio effect • Tap to adjust")
           .setContentIntent(click)
           .setOngoing(true)
           .addAction(new Notification.Action.Builder(
               android.R.drawable.ic_media_pause,"Stop effect",stopAction).build())
           .build();
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        String action=intent==null?ACTION_START:intent.getAction();
        if(ACTION_STOP.equals(action)){
            stopAndRelease();
            stopSelf();
            return START_NOT_STICKY;
        }
        try {
            if(Build.VERSION.SDK_INT>=34){
                startForeground(NOTIFICATION_ID,notification(),
                   ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            }else{
                startForeground(NOTIFICATION_ID,notification());
            }
        }catch(Exception e){
            record(false,false);stopSelf();return START_NOT_STICKY;
        }
        try{
            if(effect==null){
                // BoostX 1.0.0 mechanism: legacy output-mix audio session 0.
                // Global output-mix attachment is deprecated; device may ignore effect.
                effect=new LoudnessEnhancer(0);
            }
            int steps=Math.max(0,Math.min(12,prefs.getInt("boost_steps",0)));
            effect.setTargetGain(steps*50); // 0 to +600 millibels (+6 dB).
            effect.setEnabled(true);
            record(true,effect.getEnabled());
        }catch(Exception e){
            if(effect!=null){try{effect.release();}catch(Exception ignored){}effect=null;}
            record(true,false);
        }
        return START_STICKY;
    }
    private void stopAndRelease(){
        if(effect!=null){
            try{effect.setEnabled(false);effect.release();}catch(Exception ignored){}
            effect=null;
        }
        record(false,false);
        try{
            if(Build.VERSION.SDK_INT>=24)stopForeground(STOP_FOREGROUND_REMOVE);
            else stopForeground(true);
        }catch(Exception ignored){}
    }
    @Override public void onDestroy(){stopAndRelease();super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}