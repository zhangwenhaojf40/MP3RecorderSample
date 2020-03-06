package zz.hao.mp3recordersample;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private AudioRecord audioRecord;
    private byte[] mPcmData;
    private int minBufferSize;
    private FileOutputStream outputStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                request();
            }
        });
        findViewById(R.id.btn_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                audioRecord.stop();
            }
        });
    }
    public void initAudio(){

        //获取最低AudioRecord内部音视频缓冲区大小,此大小依赖于各产商实现，最好不要自己计算
        minBufferSize = AudioRecord.getMinBufferSize(1600, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        /*
        * 1.音频采集的输入源，一般选择默认或mic模式
        * 2.指定采集音频的采样频率，比较通用的是44100(44.1kHz)，科学家们通过奈葵斯特采样定理得出的一个人能接受最佳的采样频率值
        * 3.采集几个声道的声音，CHANNEL_CONFIGURATION_MONO(单声道) 和 CHANNEL_CONFIGURATION_STEREO(双声道)。
        * 4.采样格式，常用值有 ENCODING_PCM_8BIT、ENCODING_PCM_16BIT和ENCODING_PCM_FLOAT，一般ENCODING_PCM_16BIT可以保证兼容大部分Andorid手机
        * 5.计算得出最小缓冲区
        * */
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,44100, AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
        //存放后续pcm的数据
        mPcmData = new byte[minBufferSize];
        File file = new File(Environment.getExternalStorageDirectory(), "zhangtest.mp3");
        if (!file.exists()) {
            file.mkdir();
        }
        try {
            outputStream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    public Thread mReadDataThread = new Thread(){
        @Override
        public void run() {
            super.run();
            int read;
            while (audioRecord.getState() == AudioRecord.RECORDSTATE_RECORDING) {
                //读取mRecordBufSize长度的音频数据存入mPcmData中
                read = audioRecord.read(mPcmData, 0, minBufferSize);
                //如果读取音频数据没有出现错误 ===> read 大于0
                if (read >= AudioRecord.SUCCESS) {
                    try {
                        outputStream.write(mPcmData, 0, read);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        }
    };


    public void request(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},100);
        }else{
            initAudio();
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                //可能由于权限或其他原因，没有初始化完成
                Toast.makeText(this, "初始化失败", Toast.LENGTH_SHORT).show();
                return;
            }
            audioRecord.startRecording();
            mReadDataThread.start();
        }
    }
}
