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
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private AudioRecord audioRecord;
    private byte[] mPcmData;
    private int minBufferSize;
    private FileOutputStream outputStream;
    private File filePcm;
    private File fileWav;
    private int channelConfig = 1;//单声道
    int sampleRateInHz = 44100;//采样频率值，最佳
    boolean isStart = false;
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
                isStart = false;
                Toast.makeText(MainActivity.this, "录制结束", Toast.LENGTH_SHORT).show();
            }
        });
    }


    public void initAudio() {

        //获取最低AudioRecord内部音视频缓冲区大小,此大小依赖于各产商实现，最好不要自己计算
        minBufferSize = AudioRecord.getMinBufferSize(sampleRateInHz,channelConfig == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO , AudioFormat.ENCODING_PCM_16BIT);
        /*
         * 1.音频采集的输入源，一般选择默认或mic模式
         * 2.指定采集音频的采样频率，比较通用的是44100(44.1kHz)，科学家们通过奈葵斯特采样定理得出的一个人能接受最佳的采样频率值
         * 3.采集几个声道的声音，CHANNEL_CONFIGURATION_MONO(单声道) 和 CHANNEL_CONFIGURATION_STEREO(双声道)。
         * 4.采样格式，常用值有 ENCODING_PCM_8BIT、ENCODING_PCM_16BIT和ENCODING_PCM_FLOAT，一般ENCODING_PCM_16BIT可以保证兼容大部分Andorid手机
         * 5.计算得出最小缓冲区
         * */
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz,channelConfig == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO , AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
        //存放后续pcm的数据
        mPcmData = new byte[minBufferSize];

        createFile();

    }
    private void createFile() {
        File file = new File(Environment.getExternalStorageDirectory(), "Recordtest/audio");
        if (!file.exists()) {
            file.mkdirs();
        }
        filePcm = new File(file.getAbsolutePath(), "/test.pcm");
        fileWav = new File(file.getAbsolutePath(), "/test.wav");

        try {
            if (fileWav.exists()) {
                fileWav.delete();
            } else {
                fileWav.createNewFile();

            }
            if (filePcm.exists()) {
                filePcm.delete();
            }
            filePcm.createNewFile();
            outputStream = new FileOutputStream(filePcm);

        } catch (Exception e) {
            e.printStackTrace();
        }


    }




    public void request() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        } else {
            initAudio();
            if (audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
                Toast.makeText(this, "AudioRecord初始化失败", Toast.LENGTH_SHORT).show();
                return;
            }

            audioRecord.startRecording();
            isStart = true;
            Toast.makeText(this, "开始录制", Toast.LENGTH_SHORT).show();
            mReadDataThread.start();
        }
    }




    public Thread mReadDataThread = new Thread() {
        @Override
        public void run() {
            super.run();
            //线程优先级
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            int read;
            while (isStart && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                //读取mRecordBufSize长度的音频数据存入mPcmData中
                read = audioRecord.read(mPcmData, 0, minBufferSize);
                //如果读取音频数据没有出现错误 ===> read 大于0
                if (read >= AudioRecord.SUCCESS) {
                    try {
                        outputStream.write(mPcmData);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            FileUtils.pcmToWave(filePcm.getAbsolutePath(), fileWav.getAbsolutePath(), sampleRateInHz,channelConfig , minBufferSize);
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    };
}
