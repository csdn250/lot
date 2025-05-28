package com.example.tentest01;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class MqttManager {
    private String Uptopic        = "device/49ab87b81ddaa6d3/up";
    private String Downtopic        = "device/49ab87b81ddaa6d3/down";
    private String content      = "Message from MqttPublishSample";
    private int qos             = 2;
    private String broker       ="tcp://115.28.209.116:1883";// "tcp://10.57.15.62:61613";
    private String clientId     = "2def79e6cec9d38b67c246866857886b";
    private String userName = "bkrc";
    private String passWord ="88888888";//password";
    private String devId="49ab87b81ddaa6d3";
    private MqttClient sampleClient=null;
    private boolean bConnected=false;
    private Handler mHandler=null;

    public boolean isbConnected() {
        return bConnected;
    }

    public void setbConnected(boolean bConnected) {
        this.bConnected = bConnected;
    }

    public MqttManager(String ServerIp, String clentId)
    {
        this.broker="tcp://"+ServerIp+":1883";
        this.clientId=clentId;
    }

    public MqttManager(String ServerIp, String clentId, Handler handler)
    {
        this.broker="tcp://"+ServerIp+":1883";
        this.clientId=clentId;
        this.mHandler=handler;
    }

    public void setUptopic(String uptopic) {
        this.Uptopic  = uptopic;
    }

    public void setDowntopic(String downtopic) {
        Downtopic = downtopic;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPassWord(String passWord) {
        this.passWord = passWord;
    }

    public void setDevId(String devId) {
        this.devId = devId;
    }

    private class MyMqttCallback implements MqttCallback
    {

        @Override
        public void connectionLost(Throwable throwable) {
            //连接丢失后，一般在这里面进行重连
            Log.i("MQTT","connectionLost----------");
            bConnected=false;
        }

        @Override
        public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
            //subscribe后得到的消息会执行到这里面
           Log.i("MQTT","messageArrived----------" + mqttMessage.toString());
            ProcessMQTTJsonData(mqttMessage.toString());
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
            //publish后会执行到这里
            Log.i("MQTT","deliveryComplete---------" + iMqttDeliveryToken.isComplete());
        }
    }

    public boolean Connect()
    {
        MqttConnectOptions options;
        MemoryPersistence persistence = new MemoryPersistence();
        try {
            sampleClient = new MqttClient(broker, clientId, persistence);
            options = new MqttConnectOptions();
            //设置是否清空session,这里如果设置为false表示服务器会保留客户端的连接记录，这里设置为true表示每次连接到服务器都以新的身份连接
            options.setCleanSession(true);
            /*如果您使用缺省 MqttConnectOptions，或者在连接客户机之前将 MqttConnectOptions.cleanSession 设置为 true，那么在客户机建立连接时，将除去客户机的任何旧预订。当客户机断开连接时，会除去客户机在会话期间创建的任何新预订。
             *
             *  如果您在连接之前将 MqttConnectOptions.cleanSession 设置为 false，那么客户机创建的任何预订都会被添加至客户机在连接之前就已存在的所有预订。当客户机断开连接时，所有预订仍保持活动状态。
             *  */
            //设置连接的用户名
            options.setUserName(userName);
            //设置连接的密码
            options.setPassword(passWord.toCharArray());
            // 设置超时时间 单位为秒
            options.setConnectionTimeout(10);
            // 设置会话心跳时间 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制
            options.setKeepAliveInterval(200);
            //设置回调
            sampleClient.setCallback(new MyMqttCallback());
            System.out.println("Connecting to broker: " + broker);
            sampleClient.connect(options);
            System.out.println("Connected");
            sampleClient.subscribe(Downtopic, 1);
            sampleClient.subscribe(Uptopic,0 );
            bConnected=true;
        //    SendMQTTonline();
        }
        catch(MqttException me)
        {
            Log.i("MQTT","reason "+me.getReasonCode());
            Log.i("MQTT","msg "+me.getMessage());
            Log.i("MQTT","loc "+me.getLocalizedMessage());
            Log.i("MQTT","cause "+me.getCause());
            Log.i("MQTT","excep "+me);
            me.printStackTrace();
            return false;
        }
        return true;
    }
    //获得设备的在线状态 0-不在线，1-在线，-1-连接错误
    public void  GetDeviceStatus()
    {
        OkHttpClient okHttpClient = new OkHttpClient();
        String strURL="https://www.r8c.com/index/iot/api/"+devId+"/get-device.html";
        Request request = new Request.Builder().url(strURL).get().build();
        Call call = okHttpClient.newCall(request);
        // 开启异步线程访问网络
        call.enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String res = response.body().string(); //获取店铺数据
                int result=ParseJsonStatus(res);
                SendMessageToMain(result);
            }
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("test",e.getMessage());
                SendMessageToMain(-1);//连接错误
            }
        });
    }
    //发送设备在线状态数据
    private void SendMessageToMain(int result)
    {
        Message msg=new Message();
        msg.what=MainActivity.Msg_DATA_OK;
        msg.obj=result;
        mHandler.sendMessage(msg);
    }
    //发送模拟数据
    private void SendMessageToMain(AnaDataBean anaDataBean)
    {
        Message msg=new Message();
        msg.what=MainActivity.Msg_Analog_Data;
        msg.obj=anaDataBean;
        mHandler.sendMessage(msg);
    }

    //发送灯的状态数据
    private void SendMessagetToMain()
    {
        Message msg=new Message();
        msg.what=MainActivity.Msg_Led_Data;
        mHandler.sendMessage(msg);
    }

    /**根据JSON数据获得在线信息，JSON数据格式样例如下
     * {
     *     "code": 200,
     *     "message": "获取设备信息成功",
     *     "success": true,
     *     "data": {
     *         "id": 8730,
     *         "name": "智能家居系统",
     *         "type": "1",
     *         "protocol_type": "3",
     *         "imei": "",
     *         "online": "0",
     *         "logo": null,
     *         "auth_code": null,
     *         "sign": "a95ede5154d2852b",
     *         "data_type": "1",
     *         "create_time": "2024-06-05 14:24:55",
     *         "type_str": "智慧城市",
     *         "protocol_type_str": "MQTT",
     *         "data_type_str": "JSON",
     *         "online_str": "离线"
     *     }
     * }
     * @param Jsondata
     * @return 0-不在线，1-在线，-1-连接错误
     */
    private int ParseJsonStatus(String Jsondata)
    {
        int result=-1;
        try {
            JSONObject jsonObj = JSON.parseObject(Jsondata);
            if (jsonObj.getBoolean("success"))
            {
                JSONObject data= JSON.parseObject(jsonObj.getString("data"));
                String strOnline=data.getString("online");
                if (strOnline.equals("0"))
                    result =0;
                else
                    result=1;

            }
        }catch (Exception e)
        {
            Log.i("MQTT",e.getMessage());
        }
        return result;
    }

    public void DisConnectMQTT() {
        try {
            if (sampleClient != null)
                sampleClient.disconnect();
            Log.i("MQTT","MQTT连接关闭！");
        }catch (Exception e)
        {
            Log.i("MQTT","MQTT连接断开错误");
        }
    }

    //{"sign":"3764b9d0bf36245d","type":1,"data":{"ledstatus":{"led1":true,"led2":false,"led3":true,"led4":false}}}
    //str="{\"sign\":\"284b0a158b7f29cd\",\"type\":1,\"data\":{\"air_quality\":{\"led\":true,\"otherLed\":true}}}";
    public void SendMessageFromMQTT(String str){
        try{
            System.out.println(str);
            MqttMessage message = new MqttMessage(str.getBytes());
            message.setQos(qos);
            sampleClient.publish(Downtopic, message);
        }catch (Exception e)
        {
            Log.i("MQTT","发送到MQTT数据错误，原因:"+e.getMessage());
        }
    }
    //通过云平台订阅的控制板上送的模拟数据格式：
    //{"sign":"a95ede5154d2852b","type":1,"data":{"AnalogData":{"Humidity":440,"temp":44}}}
    //{"sign":"a95ede5154d2852b","type":1,"data":{"AnalogData":{"Humidity":160,"temp":16},"ledstatus":{"led1":true,"led2":true,"led3":false}}}
    public void ProcessMQTTJsonData(String Jsondata)
    {
        try {
            JSONObject jsonObj = JSON.parseObject(Jsondata);
            String type = jsonObj.getString("type");
            boolean bStatus = false;
            if (type.equals("1")) {
                String gasstr = jsonObj.getString("data");
                JSONObject AllData = JSON.parseObject(gasstr);
                String anaData = AllData.getString("tempHumidity");
                if(anaData!=null && !anaData.equals("")) {
                    JSONObject value = JSON.parseObject(anaData);
                    AnaDataBean anaDataBean = new AnaDataBean();
                    anaDataBean.setTemp(value.getInteger("temp"));
                    anaDataBean.setHumidity(value.getInteger("humidity"));
                    SendMessageToMain(anaDataBean);
                }

                String ledData=AllData.getString("ledstatus");
                if (ledData!=null && !ledData.equals(""))
                {
                    JSONObject value = JSON.parseObject(ledData);
                    for (int i = 0; i < 4; i++)
                        MainActivity.bLedStatus[i] = value.getBoolean("led" + (i + 1));
                    SendMessagetToMain();
                }

            }
        }catch (Exception e)
        {
            Log.i("MQTT","msg "+e.getMessage());
        }
        return;
    }
}
