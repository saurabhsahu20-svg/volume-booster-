package com.crystal.volumebooster;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.media.*;
import android.media.audiofx.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.Locale;

public class MainActivity extends Activity {
    final int BG=Color.rgb(6,14,30), PANEL=Color.rgb(17,31,52), CYAN=Color.rgb(67,220,248), WHITE=Color.WHITE, MUTED=Color.rgb(169,192,209);
    final Handler handler=new Handler(Looper.getMainLooper());
    AudioManager audio; MediaPlayer player; LoudnessEnhancer loud; Equalizer equalizer; BassBoost bassBoost; Visualizer visualizer;
    AudioFocusRequest focusRequest; boolean enhancements=true, headphones=false, prepared=false;
    int boostDb=0, bassPct=20, treblePct=25, preset=0, systemMax;
    Uri selected; TextView status,boostLabel,trackLabel,outputLabel,playLabel,systemLabel; SeekBar boostBar,systemBar,bassBar,trebleBar;
    Wave wave; android.content.SharedPreferences prefs;
    final String[] presets={"FLAT","VOICE","BASS","BRIGHT"};
    final android.content.BroadcastReceiver noisyReceiver=new android.content.BroadcastReceiver(){
        @Override public void onReceive(Context c,Intent i){
            if(AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(i.getAction()) && player!=null && player.isPlaying()) {
                player.pause(); status("Headphones disconnected: playback paused."); updatePlayLabel();
            }
        }
    };
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        audio=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
        systemMax=audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        prefs=getSharedPreferences("crystal_settings",MODE_PRIVATE);
        boostDb=prefs.getInt("gain",0); bassPct=prefs.getInt("bass",20); treblePct=prefs.getInt("treble",25);
        headphones=prefs.getBoolean("headphones",false); preset=prefs.getInt("preset",0); enhancements=prefs.getBoolean("on",true);
        if(headphones && boostDb>3)boostDb=3;
        setupUI();
        IntentFilter f=new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        if(Build.VERSION.SDK_INT>=33) registerReceiver(noisyReceiver,f,Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(noisyReceiver,f);
    }
    GradientDrawable panel(int color,int radius) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius));
        d.setStroke(dp(1),Color.rgb(36,74,105)); return d;
    }
    int dp(int n){return (int)(getResources().getDisplayMetrics().density*n+0.5f);}
    TextView text(String v,int sz,int color,boolean bold) {
        TextView t=new TextView(this);t.setText(v);t.setTextColor(color);t.setTextSize(sz);
        if(bold)t.setTypeface(android.graphics.Typeface.DEFAULT,1);
        return t;
    }
    LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    void pad(LinearLayout l,int p){l.setPadding(dp(p),dp(p),dp(p),dp(p));}
    void gap(LinearLayout l,int h){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    void line(LinearLayout l,String title,View child) {
        TextView h=text(title,14,MUTED,true);l.addView(h);l.addView(child);
    }
    void setupUI() {
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        LinearLayout outer=vertical();outer.setBackgroundColor(BG);pad(outer,18);
        scroll.addView(outer);setContentView(scroll);
        TextView brand=text("✦ CRYSTAL VOLUME",23,CYAN,true);brand.setGravity(Gravity.CENTER);outer.addView(brand);
        TextView sub=text("Pure voice  •  Clean sound  •  Safe boost",13,MUTED,false);sub.setGravity(Gravity.CENTER);outer.addView(sub);
        gap(outer,18);
        LinearLayout hero=vertical();pad(hero,20);hero.setGravity(Gravity.CENTER);hero.setBackground(panel(PANEL,26));outer.addView(hero);
        boostLabel=text("+"+boostDb+" dB",47,WHITE,true);boostLabel.setGravity(Gravity.CENTER);hero.addView(boostLabel);
        TextView hint=text("CLEAN LOUDNESS ENHANCEMENT",12,CYAN,true);hint.setGravity(Gravity.CENTER);hero.addView(hint);
        boostBar=bar(headphones?3:5,boostDb,p->{
            boostDb=p;boostLabel.setText("+"+boostDb+" dB"); save(); applyFx();
        });hero.addView(boostBar);
        TextView caution=text("Volume boost applies only to audio played here. Start low.",12,MUTED,false);
        caution.setGravity(Gravity.CENTER);hero.addView(caution);
        gap(outer,13);
        LinearLayout controls=vertical();pad(controls,15);controls.setBackground(panel(PANEL,20));outer.addView(controls);
        TextView source=text("YOUR MUSIC",14,CYAN,true);controls.addView(source);
        trackLabel=text("No audio selected",15,WHITE,true);controls.addView(trackLabel);gap(controls,8);
        button(controls,"＋  CHOOSE AUDIO FILE",()->{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.setType("audio/*");i.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(i,123);
        });
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(0);controls.addView(actions);
        playLabel=button(actions,"▶  PLAY / PAUSE",()->playPause());
        button(actions,"■  STOP",()->stopMusic());
        gap(outer,13);
        LinearLayout fx=vertical();pad(fx,16);fx.setBackground(panel(PANEL,20));outer.addView(fx);
        TextView header=text("SOUND TUNING",16,CYAN,true);fx.addView(header);
        button(fx,enhancements?"✓  ENHANCEMENT ON":"○  ENHANCEMENT OFF",()->{
            enhancements=!enhancements;save();applyFx();setupUI();
        });
        TextView eqTitle=text("EQ PRESET: "+presets[Math.max(0,Math.min(3,preset))],13,MUTED,true);fx.addView(eqTitle);
        LinearLayout row=new LinearLayout(this);row.setOrientation(0);fx.addView(row);
        for(int i=0;i<4;i++) {
            final int index=i;
            button(row,presets[i],()->{preset=index;eqTitle.setText("EQ PRESET: "+presets[index]);save();applyFx();});
        }
        line(fx,"BASS CONTROL",bassBar=bar(60,bassPct,p->{bassPct=p;save();applyFx();}));
        line(fx,"TREBLE / SPEECH PRESENCE",trebleBar=bar(60,treblePct,p->{treblePct=p;save();applyFx();}));
        gap(outer,13);
        LinearLayout output=vertical();pad(output,16);output.setBackground(panel(PANEL,20));outer.addView(output);
        outputLabel=text("PROFILE: "+(headphones?"HEADPHONES (+3 dB max)":"SPEAKER (+5 dB max)"),14,CYAN,true);
        output.addView(outputLabel);
        button(output,"⇄  SWITCH OUTPUT PROFILE",()->{
            headphones=!headphones;
            boostDb=Math.min(boostDb,headphones?3:5);
            save();setupUI();applyFx();
        });
        gap(output,8);
        line(output,"SYSTEM MEDIA VOLUME",systemBar=bar(systemMax,audio.getStreamVolume(AudioManager.STREAM_MUSIC),p->{
            audio.setStreamVolume(AudioManager.STREAM_MUSIC,p,0);
        }));
        gap(outer,14);
        wave=new Wave(this);wave.setBackground(panel(PANEL,20));
        outer.addView(wave,new LinearLayout.LayoutParams(-1,dp(86)));
        status=text("Choose a music file to begin. Waveform requires audio permission.",12,MUTED,false);
        status.setGravity(Gravity.CENTER);outer.addView(status);
        gap(outer,6);
        TextView footer=text("For hearing safety, avoid long listening at high volume.",12,MUTED,false);
        footer.setGravity(Gravity.CENTER);outer.addView(footer);
    }
    interface ValueListener {void changed(int value);}
    SeekBar bar(int max,int value,ValueListener listener){
        SeekBar b=new SeekBar(this);b.setMax(Math.max(1,max));b.setProgress(Math.min(max,Math.max(0,value)));
        b.setProgressTintList(android.content.res.ColorStateList.valueOf(CYAN));
        b.setThumbTintList(android.content.res.ColorStateList.valueOf(CYAN));
        b.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar sb,int v,boolean fromUser){if(fromUser)listener.changed(v);}
            public void onStartTrackingTouch(SeekBar sb){}
            public void onStopTrackingTouch(SeekBar sb){}
        });return b;
    }
    TextView button(LinearLayout parent,String text,Runnable run){
        TextView b=text(text,12,WHITE,true);
        b.setGravity(Gravity.CENTER);b.setPadding(dp(10),dp(13),dp(10),dp(13));
        b.setBackground(panel(Color.rgb(23,61,87),14));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(48),1);lp.setMargins(dp(2),dp(5),dp(2),dp(5));
        parent.addView(b,lp);b.setOnClickListener(v->run.run());return b;
    }
    void save(){prefs.edit().putInt("gain",boostDb).putInt("bass",bassPct).putInt("treble",treblePct).putInt("preset",preset).putBoolean("headphones",headphones).putBoolean("on",enhancements).apply();}
    void status(String s){if(status!=null)status.setText(s);}
    @Override protected void onActivityResult(int req,int result,Intent data){
        super.onActivityResult(req,result,data);
        if(req==123&&result==RESULT_OK&&data!=null&&data.getData()!=null){
            selected=data.getData();
            try{getContentResolver().takePersistableUriPermission(selected,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
            trackLabel.setText("Selected: "+selected.getLastPathSegment());
            prepareMusic();
        }
    }
    void prepareMusic(){
        releasePlayer();
        try {
            player=new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            player.setDataSource(this,selected);
            player.setOnPreparedListener(mp->{
                prepared=true;attachFx();requestFocus();mp.start();updatePlayLabel();
                status("Playing local audio with compatible audio-session effects.");
                if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},99);
                else setupVisualizer();
            });
            player.setOnCompletionListener(mp->{updatePlayLabel();status("Playback finished.");});
            player.setOnErrorListener((mp,what,extra)->{status("Audio file could not be played.");return true;});
            player.prepareAsync();status("Loading audio...");
        }catch(Exception e){status("Cannot open this audio file.");releasePlayer();}
    }
    void requestFocus(){
        try{
            focusRequest=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
                .setOnAudioFocusChangeListener(change->{
                    if(change<=0&&player!=null&&prepared&&player.isPlaying()){
                        player.pause();updatePlayLabel();
                    }
                }).build();
            audio.requestAudioFocus(focusRequest);
        }catch(Exception ignored){}
    }
    void attachFx(){
        if(player==null)return;
        int id=player.getAudioSessionId();
        try { loud=new LoudnessEnhancer(id);loud.setEnabled(true); }catch(Exception e){loud=null;}
        try { equalizer=new Equalizer(0,id);equalizer.setEnabled(true); }catch(Exception e){equalizer=null;}
        try { bassBoost=new BassBoost(0,id);bassBoost.setEnabled(true); }catch(Exception e){bassBoost=null;}
        applyFx();
    }
    void applyFx(){
        int g=enhancements?Math.min(boostDb,headphones?3:5)*100:0;
        try{if(loud!=null)loud.setTargetGain(g);}catch(Exception ignored){}
        try{
            if(equalizer!=null){
                short[] range=equalizer.getBandLevelRange();
                int mid=(range[0]+range[1])/2;int span=range[1]-range[0];
                for(short i=0;i<equalizer.getNumberOfBands();i++){
                    int hz=equalizer.getCenterFreq(i)/1000;int amt=0;
                    if(preset==1&&hz>=300&&hz<=4400)amt+=18;
                    if(preset==2&&hz<=260)amt+=22;
                    if(preset==3&&hz>=3200)amt+=18;
                    if(hz<=300)amt+=(bassPct-20)/3;
                    if(hz>=2500)amt+=(treblePct-25)/3;
                    int val=mid+span*amt/100;
                    val=Math.max(range[0],Math.min(range[1],val));
                    equalizer.setBandLevel(i,(short)(enhancements?val:0));
                }
            }
        }catch(Exception ignored){}
        try{if(bassBoost!=null)bassBoost.setStrength((short)(enhancements?Math.min(600,bassPct*10):0));}catch(Exception ignored){}
    }
    void playPause(){
        if(player==null||!prepared){if(selected!=null)prepareMusic();else status("Choose an audio file first.");return;}
        if(player.isPlaying())player.pause();else {requestFocus();player.start();}
        updatePlayLabel();
    }
    void updatePlayLabel(){if(playLabel!=null)playLabel.setText(player!=null&&prepared&&player.isPlaying()?"Ⅱ  PAUSE":"▶  PLAY");}
    void stopMusic(){if(player!=null&&prepared){player.pause();player.seekTo(0);updatePlayLabel();status("Stopped.");}}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){
        super.onRequestPermissionsResult(request,permissions,grants);
        if(request==99&&grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED)setupVisualizer();
    }
    void setupVisualizer(){
        if(player==null||!prepared||checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)return;
        try{
            if(visualizer!=null){visualizer.release();visualizer=null;}
            visualizer=new Visualizer(player.getAudioSessionId());visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[0]);
            visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener(){
                public void onWaveFormDataCapture(Visualizer v,byte[] data,int samplingRate){
                    if(wave!=null)wave.setWave(data);
                }
                public void onFftDataCapture(Visualizer v,byte[] data,int samplingRate){}
            },Math.min(Visualizer.getMaxCaptureRate(),10000),true,false);
            visualizer.setEnabled(true);
        }catch(Exception ignored){status("Audio visualizer not supported on this device.");}
    }
    void releasePlayer(){
        try{if(visualizer!=null){visualizer.setEnabled(false);visualizer.release();}}catch(Exception ignored){}
        visualizer=null;
        try{if(loud!=null)loud.release();}catch(Exception ignored){}loud=null;
        try{if(equalizer!=null)equalizer.release();}catch(Exception ignored){}equalizer=null;
        try{if(bassBoost!=null)bassBoost.release();}catch(Exception ignored){}bassBoost=null;
        try{if(player!=null){player.reset();player.release();}}catch(Exception ignored){}player=null;prepared=false;
        if(focusRequest!=null){audio.abandonAudioFocusRequest(focusRequest);focusRequest=null;}
        updatePlayLabel();
    }
    @Override protected void onDestroy(){
        try{unregisterReceiver(noisyReceiver);}catch(Exception ignored){}
        releasePlayer();super.onDestroy();
    }
    class Wave extends View {
        Paint p=new Paint(3);byte[] values;
        Wave(Context c){super(c);}
        void setWave(byte[] x){values=x.clone();postInvalidate();}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);int bars=38;int w=getWidth(),h=getHeight();
            p.setColor(CYAN);p.setStrokeWidth(dp(3));p.setStrokeCap(Paint.Cap.ROUND);
            for(int i=0;i<bars;i++){
                float amp=0.07f;
                if(values!=null&&values.length>bars)amp=Math.max(0.07f,Math.abs((values[(i*values.length)/bars]&255)-128)/128f);
                float x=(i+1)*w/(float)(bars+1);float len=dp(5)+amp*(h-dp(30));
                c.drawLine(x,h/2f-len/2f,x,h/2f+len/2f,p);
            }
        }
    }
}
