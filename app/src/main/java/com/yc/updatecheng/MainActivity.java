package com.yc.updatecheng;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {
    private final Handler handler = new Handler();
    private PackageManager pm;
    private String path;
    private int versionCode;
    private TextView descTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        descTextView = findViewById(R.id.desc);
        pm = getPackageManager();

        Intent intent = getIntent();
        if(intent != null) {
            path = intent.getStringExtra("Path");
            versionCode = intent.getIntExtra("VersionCode",  1);
        }
        if(TextUtils.isEmpty(path)) {
            descTextView.setText("没有可用更新包...\n\n 程序3秒后退出...\n\n");
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                    System.exit(0);
                }
            }, 3000);
            return;
        }

        descTextView.setText("正在安装更新程序...\n\n");
        new Thread(new Runnable() {
            @Override
            public void run() {
                PackageInfo info = pm.getPackageArchiveInfo(MainActivity.this.path, 1);
                if(info == null){
                    return;
                }
                if(info.versionCode > versionCode && install(info.packageName)){
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            descTextView.setText(descTextView.getText() +"安装成功...\n");
                            descTextView.setText(descTextView.getText()+ "3秒后重启...\n"  );
                        }
                    });

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    reboot();
                                }
                            }, 3000);
                        }
                    });
                } else {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            descTextView.setText(descTextView.getText() + "自动更新失败, 3秒后开始手动安装...\n");
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    install2();
                                }
                            }, 3000);
                        }
                    });
                }
            }
        }).start();
    }


    public  boolean install(String packageName) {
        Process process = null;
        BufferedReader successResult = null;
        BufferedReader errorResult = null;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder errorMsg = new StringBuilder();
        try {
            process = new ProcessBuilder("pm", "install","-i", "com.yc.updatecheng", "--user", "0", "-r", path).start();
            successResult = new BufferedReader(new InputStreamReader(process.getInputStream()));
            errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String s;
            while ((s = successResult.readLine()) != null) {
                successMsg.append(s);
            }
            while ((s = errorResult.readLine()) != null) {
                errorMsg.append(s);
            }
        } catch (Exception e) {

        } finally {
            try {
                if (successResult != null) {
                    successResult.close();
                }
                if (errorResult != null) {
                    errorResult.close();
                }
            } catch (Exception e) {

            }
            if (process != null) {
                process.destroy();
            }
        }
        Log.e(getString(R.string.app_name),""+ errorMsg.toString());
        return successMsg.toString().equalsIgnoreCase("success");
    }

    public  void install2() {
        Uri uriForFile;
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setFlags(268435456);
        if (Build.VERSION.SDK_INT >= 24) {
            uriForFile = FileProvider.getUriForFile(this, "com.yc.cheng.provider", new File(path));
            intent.setFlags(1);
        } else {
            uriForFile = Uri.fromFile(new File(path));
        }
        intent.setDataAndType(uriForFile, "application/vnd.android.package-archive");
        startActivity(intent);
    }


    public void reboot(){
        try {
            Runtime.getRuntime().exec("reboot");
        } catch (Exception e) {
        }
    }
}
