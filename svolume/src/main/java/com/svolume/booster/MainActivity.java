package com.svolume.booster;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends Activity {
    private final int BASE=Color.rgb(6,14,28), CARD=Color.rgb(17,31,51),
       ACCENT=Color.rgb(64,219,242), WHITE=Color.WHITE, MUTED=Color.rgb(168,189,208);
    private AudioManager audio;
    private android.content.SharedPreferences prefs;
    private TextView serviceStatus, mediaLabel, boostLabel, outputInfo;
    private SeekBar mediaSlider, boostSlider;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable monitor=new Runnable(){
        @Override public void run(){sync();handler.postDelayed(this,800);}
    };
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private GradientDrawable bg(int color,int corner){
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);d.setCornerRadius(dp(corner));d.setStroke(dp(1),Color.rgb(29,62,86));
        return d;
    }
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    private TextView label(String str,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(str);t.setTextColor(color);t.setTextSize(size);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private void space(LinearLayout l,int height){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(height)));}
    private TextView action(LinearLayout p,String caption,Runnable callback){
        TextView b=label(caption,13,WHITE,true);b.setGravity(Gravity.CENTER);
        b.setPadding(dp(10),dp(10),dp(10),dp(10));b.setBackground(bg(Color.rgb(28,66,87),12));
        LinearLayout.LayoutParams lp=p.getOrientation()==LinearLayout.HORIZONTAL
             ?new LinearLayout.LayoutParams(0,dp(48),1)
             :new LinearLayout.LayoutParams(-1,dp(48));
        lp.setMargins(dp(3),dp(5),dp(3),dp(5));p.addView(b,lp);
        b.setOnClickListener(v->callback.run());return b;
    }
    private SeekBar seek(int max,int value,SeekBar.OnSeekBarChangeListener l){
        SeekBar s=new SeekBar(this);s.setMax(max);s.setProgress(Math.max(0,Math.min(max,value)));
        s.setProgressTintList(android.content.res.ColorStateList.valueOf(ACCENT));
        s.setThumbTintList(android.content.res.ColorStateList.valueOf(ACCENT));
        s.setOnSeekBarChangeListener(l);return s;
    }
    private interface Change {void onUser(int value);}
    private SeekBar.OnSeekBarChangeListener onChange(Change fn){
        return new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int p,boolean fromUser){if(fromUser)fn.onUser(p);}
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){}
        };
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        audio=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
        prefs=getSharedPreferences("svolume",MODE_PRIVATE);
        getWindow().setStatusBarColor(BASE);getWindow().setNavigationBarColor(BASE);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        createUI();
    }
    private void createUI(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        LinearLayout root=col();root.setBackgroundColor(BASE);
        root.setPadding(dp(18),dp(22),dp(18),dp(24));
        scroll.addView(root);setContentView(scroll);
        TextView heading=label("S  VOLUME BOOSTER",25,ACCENT,true);
        heading.setGravity(Gravity.CENTER);root.addView(heading);
        TextView sub=label("Premium Sound Control  •  Based on BoostX",12,MUTED,false);
        sub.setGravity(Gravity.CENTER);root.addView(sub);
        space(root,18);
        LinearLayout panel=col();panel.setPadding(dp(18),dp(16),dp(18),dp(16));
        panel.setBackground(bg(CARD,24));root.addView(panel);
        panel.addView(label("SOUND BOOST  •  EXPERIMENTAL",16,ACCENT,true));
        TextView note=label("Attempts an Android output-mix effect used by BoostX. It only works if your phone supports global audio effects.",13,MUTED,false);
        panel.addView(note);space(panel,12);
        int initial=Math.max(0,Math.min(12,prefs.getInt("boost_steps",0)));
        boostLabel=label("Boost: +"+(initial/2f)+" dB",32,WHITE,true);
        boostLabel.setGravity(Gravity.CENTER);panel.addView(boostLabel);
        boostSlider=seek(12,initial,onChange(v->{
            prefs.edit().putInt("boost_steps",v).apply();
            boostLabel.setText(String.format(java.util.Locale.US,"Boost: +%.1f dB",v/2f));
            if(prefs.getBoolean("running",false))startOrUpdate();
        }));panel.addView(boostSlider);
        TextView caution=label("Limited to +6 dB. No guarantee of extra loudness or distortion-free audio. Keep sound comfortable.",12,MUTED,false);
        panel.addView(caution);
        LinearLayout row=new LinearLayout(this);row.setOrientation(0);panel.addView(row);
        action(row,"▶  ENABLE BOOST",()->enable());
        action(row,"■  STOP BOOST",()->disable());
        serviceStatus=label("Status: OFF",13,MUTED,true);panel.addView(serviceStatus);
        space(root,12);
        LinearLayout normal=col();normal.setPadding(dp(17),dp(16),dp(17),dp(16));
        normal.setBackground(bg(CARD,22));root.addView(normal);
        normal.addView(label("SYSTEM MEDIA VOLUME",16,ACCENT,true));
        normal.addView(label("Adjusts the normal volume shared by music and video apps (within the phone's existing limit).",12,MUTED,false));
        mediaLabel=label("",21,WHITE,true);normal.addView(mediaLabel);
        int max=Math.max(1,audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        mediaSlider=seek(max,audio.getStreamVolume(AudioManager.STREAM_MUSIC),onChange(v->{
            try{audio.setStreamVolume(AudioManager.STREAM_MUSIC,v,0);}catch(Exception ignored){}
            sync();
        }));normal.addView(mediaSlider);
        LinearLayout mr=new LinearLayout(this);mr.setOrientation(0);normal.addView(mr);
        action(mr,"−  LOWER",()->adjust(-1));
        action(mr,"＋  RAISE",()->adjust(1));
        space(root,12);
        LinearLayout device=col();device.setPadding(dp(17),dp(16),dp(17),dp(16));
        device.setBackground(bg(CARD,22));root.addView(device);
        device.addView(label("AUDIO DEVICES & SETTINGS",16,ACCENT,true));
        outputInfo=label("Detecting output devices...",13,MUTED,false);device.addView(outputInfo);
        action(device,"⚙  OPEN PHONE SOUND SETTINGS",()->{
            try{startActivity(new Intent(Settings.ACTION_SOUND_SETTINGS));}
            catch(Exception e){serviceStatus.setText("Sound settings unavailable on this device.");}
        });
        space(root,16);
        TextView footer=label("Experimental global effect may be blocked or ignored on recent Android devices. It does not change call audio or override hardware limits.\n\nBased on BoostX by Aum Gupta (GPL-3.0). This is a modified, separately branded version.",12,MUTED,false);
        footer.setGravity(Gravity.CENTER);root.addView(footer);
        sync();
    }
    private void adjust(int delta){
        try{
            int level=audio.getStreamVolume(AudioManager.STREAM_MUSIC);
            audio.setStreamVolume(AudioManager.STREAM_MUSIC,Math.max(0,Math.min(audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC),level+delta)),0);
        }catch(Exception ignored){}
        sync();
    }
    private void enable(){
        if(Build.VERSION.SDK_INT>=33 &&
           checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},100);
        }
        startOrUpdate();
    }
    private void startOrUpdate(){
        Intent i=new Intent(this,BoostService.class);
        i.setAction(BoostService.ACTION_START);
        try{
            if(Build.VERSION.SDK_INT>=26)startForegroundService(i);
            else startService(i);
            if(serviceStatus!=null)serviceStatus.setText("Starting experimental output-mix effect...");
        }catch(Exception e){
            if(serviceStatus!=null)serviceStatus.setText("Android blocked starting the effect on this device.");
        }
    }
    private void disable(){
        Intent i=new Intent(this,BoostService.class);i.setAction(BoostService.ACTION_STOP);
        try{startService(i);}catch(Exception ignored){}
        prefs.edit().putBoolean("running",false).apply();
        sync();
    }
    private void sync(){
        if(audio==null)return;
        try{
            int max=Math.max(1,audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            int cur=audio.getStreamVolume(AudioManager.STREAM_MUSIC);
            if(mediaSlider!=null && mediaSlider.getProgress()!=cur)mediaSlider.setProgress(cur);
            if(mediaLabel!=null)mediaLabel.setText("Volume: "+Math.round(cur*100f/max)+"%");
            if(serviceStatus!=null){
                boolean running=prefs.getBoolean("running",false);
                boolean initialized=prefs.getBoolean("effect_initialized",false);
                String status=!running?"Status: OFF":initialized
                   ?"Effect initialized (audible result is device-dependent)"
                   :"Effect unavailable on this Android device";
                serviceStatus.setText(status);
                serviceStatus.setTextColor(running&&initialized?ACCENT:MUTED);
            }
            if(outputInfo!=null){
                AudioDeviceInfo[] outs=audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
                StringBuilder s=new StringBuilder("Detected audio outputs: ");
                if(outs.length==0)s.append("Unknown");
                else {
                    for(int j=0;j<Math.min(outs.length,4);j++){
                        if(j>0)s.append(", ");
                        s.append(outs[j].getProductName());
                    }
                }
                outputInfo.setText(s.toString());
            }
        }catch(Exception ignored){}
    }
    @Override protected void onResume(){super.onResume();handler.removeCallbacks(monitor);handler.post(monitor);}
    @Override protected void onPause(){handler.removeCallbacks(monitor);super.onPause();}
    @Override protected void onDestroy(){handler.removeCallbacks(monitor);super.onDestroy();}
}