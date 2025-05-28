package com.example.tentest01;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private MqttManager mqttManager=null;
    private RecyclerView recyclerView;
    private TVInfoAdapter tvInfoAdapter;
    private List<String> listInfo;
    private ImageView imageView[];
    private final int ImageViewId[]={R.id.imled1,R.id.imled2,R.id.imled3,R.id.imled4};
    private TextView tvTemp,tvHumidity;
    public static boolean []bLedStatus;
    public MHandler mHandler=null;
    public final static int Msg_DATA_OK=100;
    public final static int Msg_Analog_Data=101;
    public final static int Msg_Led_Data=102;
    private boolean bOnline=false;
    private boolean bStart=false;
    GetDeviceThread getDeviceThread=null;

    class MHandler extends Handler
    {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what)
            {
                case Msg_DATA_OK:
                    int result=(int)msg.obj;
                    ProcessDeviceState(result);
                    break;
                case Msg_Analog_Data:
                    AnaDataBean anaDataBean=(AnaDataBean) msg.obj;
                    tvTemp.setText("温度："+anaDataBean.getTemp()+"°C");
                    tvHumidity.setText("湿度："+anaDataBean.getHumidity()+"%");
                    break;
                // 修复LED状态显示
                case Msg_Led_Data:
                    updateAllLedIcons();
                    break;
            }
            super.handleMessage(msg);
        }
    }

    private void ProcessDeviceState(int nStatus)
    {
        String strMsg="";
        boolean bflag=true;
        switch (nStatus)
        {
            case 0:
                strMsg="当前设备不在线，不能进行远程控制！！！";
                bflag=false;
                break;
            case 1:
                strMsg="当前设备在线！";
                bflag=true;
                break;
            case -1:
                strMsg="无法正常获取设备状态，不能进行远程控制！";
                bflag=false;
                break;
        }
        if(!bStart)//第一次上送的数据必须更新
        {
            RefreshListView(strMsg);
            bOnline=bflag;
            bStart=true;
            return;
        }
        if (bflag!=bOnline)//状态不相同时才更新界面信息
        {
            RefreshListView(strMsg);
            bOnline=bflag;
        }
    }

    @Override
    protected void onDestroy() {
        if (getDeviceThread!=null)
            getDeviceThread.setbKillMe(true);
        if (mqttManager!=null)
            mqttManager.DisConnectMQTT();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        InitView();//初始化界面控件
        mHandler=new MHandler();
        InitMqtt();//初始化MQTT连接
        getDeviceThread=new GetDeviceThread();
        getDeviceThread.start();
    }

    private void InitView()
    {
        listInfo=new ArrayList<String>();
        tvInfoAdapter=new TVInfoAdapter(listInfo,this);
        recyclerView=findViewById(R.id.rvlist);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(tvInfoAdapter);
        imageView=new ImageView[4];
        bLedStatus=new boolean[4];
        for(int i=0;i<4;i++)
        {
            imageView[i]=findViewById(ImageViewId[i]);
            imageView[i].setOnClickListener(this);
            bLedStatus[i]=false;
        }
        tvTemp=findViewById(R.id.tvTemp);
        tvHumidity=findViewById(R.id.tvHumidity);

        // 初始化所有LED图标为关闭状态
        updateAllLedIcons();
    }

    private void InitMqtt() {
        if (mqttManager!=null)
            return;
        mqttManager=new MqttManager("115.28.209.116","AndroidAPPClient",mHandler);
        mqttManager.setUserName("bkrc");
        mqttManager.setPassWord("88888888");
        mqttManager.setDevId("49ab87b81ddaa6d3");
        mqttManager.setUptopic("device/49ab87b81ddaa6d3/up");
        mqttManager.setDowntopic("device/49ab87b81ddaa6d3/down");
        mqttManager.GetDeviceStatus();
        mqttManager.Connect();
        if (mqttManager.Connect())
            RefreshListView("MQTT服务器连接成功");
        else
            RefreshListView("MQTT服务器连接失败");
    }

    private void RefreshListView(String msg)
    {
        listInfo.add(msg);
        tvInfoAdapter.notifyDataSetChanged();
        // 自动滚动到最新消息
        recyclerView.scrollToPosition(listInfo.size() - 1);
    }

    //根据灯的状态和标号控制灯的亮灭
    //"{\"message\":\"{\\\"led4\\\":true}\",\"type\":\"msg\"}"
    private void ControlLed(boolean bflag, int LedNo)
    {
        if (!bOnline)
        {
            Toast.makeText(MainActivity.this,"当前设备不在线，不能进行控制操作！",Toast.LENGTH_LONG).show();
            return;
        }
        try {
            JSONObject msg=new JSONObject();
            msg.put("led",LedNo);
            msg.put("status",bflag);
            JSONObject jsonObject=new JSONObject();
            jsonObject.put("message",msg);
            jsonObject.put("type","msg");

            if (mqttManager!=null)
            {
                mqttManager.SendMessageFromMQTT(jsonObject.toString());
                if (bflag) {
                    RefreshListView("灯" + LedNo + "打开！");
                }
                else {
                    RefreshListView("灯" + LedNo + "关闭！");
                }
                // 立即更新对应的LED图标
                updateLedIcon(LedNo - 1, bflag);
            }
        }catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.imled1:
                bLedStatus[0]=!bLedStatus[0];
                ControlLed(bLedStatus[0],1);
                break;
            case R.id.imled2:
                bLedStatus[1]=!bLedStatus[1];
                ControlLed(bLedStatus[1],2);
                break;
            case R.id.imled3:
                bLedStatus[2]=!bLedStatus[2];
                ControlLed(bLedStatus[2],3);
                break;
            case R.id.imled4:
                bLedStatus[3]=!bLedStatus[3];
                ControlLed(bLedStatus[3],4);
                break;
        }
    }

    class GetDeviceThread extends Thread{
        private boolean bKillMe=false;

        public void setbKillMe(boolean bKillMe) {
            this.bKillMe = bKillMe;
        }

        @Override
        public void run() {
            while (!bKillMe && mqttManager != null) {
                mqttManager.GetDeviceStatus();
                try {
                    sleep(3000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    // 更新单个LED图标
    private void updateLedIcon(int index, boolean isOn) {
        if (index >= 0 && index < 4 && imageView[index] != null) {
            imageView[index].setImageResource(isOn ? R.drawable.ic_bulb_on : R.drawable.ic_bulb_off);
        }
    }

    // 更新所有LED图标
    private void updateAllLedIcons() {
        for (int i = 0; i < 4; i++) {
            updateLedIcon(i, bLedStatus[i]);
        }
    }

    // 保留原来的方法以防其他地方调用
    private void updateButtonIcon(ImageButton button, boolean isOn) {
        button.setImageResource(isOn ? R.drawable.ic_bulb_on : R.drawable.ic_bulb_off);
    }
}