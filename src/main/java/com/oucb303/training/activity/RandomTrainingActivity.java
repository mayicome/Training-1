package com.oucb303.training.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.oucb303.training.R;
import com.oucb303.training.adpter.RandomTimeAdapter;
import com.oucb303.training.device.Device;
import com.oucb303.training.listener.ChangeBarClickListener;
import com.oucb303.training.listener.CheckBoxClickListener;
import com.oucb303.training.listener.MySeekBarListener;
import com.oucb303.training.model.CheckBox;
import com.oucb303.training.model.Constant;
import com.oucb303.training.model.PowerInfo;
import com.oucb303.training.model.TimeInfo;
import com.oucb303.training.threads.ReceiveThread;
import com.oucb303.training.threads.Timer;
import com.oucb303.training.utils.DataAnalyzeUtils;
import com.oucb303.training.utils.RandomUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by baichangcai on 2016/9/7.
 */
public class RandomTrainingActivity extends Activity
{
    @Bind(R.id.bt_distance_cancel)
    ImageView btDistanceCancel;
    @Bind(R.id.layout_cancel)
    LinearLayout layoutCancel;
    @Bind(R.id.tv_title)
    TextView tvTitle;
    @Bind(R.id.tv_training_times)
    TextView tvTrainingTimes;
    @Bind(R.id.img_training_times_sub)
    ImageView imgTrainingTimesSub;
    @Bind(R.id.bar_training_times)
    SeekBar barTrainingTimes;
    @Bind(R.id.img_training_times_add)
    ImageView imgTrainingTimesAdd;
    @Bind(R.id.tv_delay_time)
    TextView tvDelayTime;
    @Bind(R.id.img_delay_time_sub)
    ImageView imgDelayTimeSub;
    @Bind(R.id.bar_delay_time)
    SeekBar barDelayTime;
    @Bind(R.id.img_delay_time_add)
    ImageView imgDelayTimeAdd;
    @Bind(R.id.tv_over_time)
    TextView tvOverTime;
    @Bind(R.id.img_over_time_sub)
    ImageView imgOverTimeSub;
    @Bind(R.id.bar_over_time)
    SeekBar barOverTime;
    @Bind(R.id.img_over_time_add)
    ImageView imgOverTimeAdd;
    @Bind(R.id.img_action_mode_touch)
    ImageView imgActionModeTouch;
    @Bind(R.id.img_action_mode_light)
    ImageView imgActionModeLight;
    @Bind(R.id.img_action_mode_together)
    ImageView imgActionModeTogether;
    @Bind(R.id.img_light_mode_center)
    ImageView imgLightModeCenter;
    @Bind(R.id.img_light_mode_all)
    ImageView imgLightModeAll;
    @Bind(R.id.img_light_mode_beside)
    ImageView imgLightModeBeside;
    @Bind(R.id.btn_begin)
    Button btnBegin;
    @Bind(R.id.ll_params)
    LinearLayout llParams;
    @Bind(R.id.lv_times)
    ListView lvTimes;
    @Bind(R.id.tv_current_times)
    TextView tvCurrentTimes;
    @Bind(R.id.tv_lost_times)
    TextView tvLostTimes;
    @Bind(R.id.tv_average_time)
    TextView tvAverageTime;
    @Bind(R.id.tv_total_time)
    TextView tvTotalTime;
    @Bind(R.id.tv_training_time)
    TextView tvTrainingTime;
    @Bind(R.id.img_training_time_sub)
    ImageView imgTrainingTimeSub;
    @Bind(R.id.bar_training_time)
    SeekBar barTrainingTime;
    @Bind(R.id.img_training_time_add)
    ImageView imgTrainingTimeAdd;
    @Bind(R.id.ll_training_time)
    LinearLayout llTrainingTime;
    @Bind(R.id.ll_training_times)
    LinearLayout llTrainingTimes;
    @Bind(R.id.img_help)
    ImageView imgHelp;


    //感应模式和灯光模式集合
    private CheckBox actionModeCheckBox, lightModeCheckBox;
    //接收到电量信息标志
    private final int POWER_RECEIVE = 1;
    //接收到灭灯时间标志
    private final int TIME_RECEIVE = 2;
    //停止训练标志
    private final int STOP_TRAINING = 3;
    //更新时间运行次数等信息
    private final int CHANGE_TIME_LIST = 4;
    private final int IS_TRAINING_OVER = 5;

    //随机模式 0:次数随机  1:时间随机
    private int randomMode;

    private Device device;
    //运行的总次数、当前运行的次数、遗漏次数、训练的总时间
    private int totalTimes, currentTimes, lostTimes, trainingTime;
    //当前亮的灯
    private char currentLight;
    //延迟时间 单位是毫秒
    private int delayTime;
    //超时时间 单位毫秒
    private int overTime;
    //开灯命令发送后 灯持续亮的时间
    private int durationTime;
    //训练开始的时间
    private long beginTime;
    //训练开始的标志
    private boolean trainingFlag = false;
    //超时线程
    private OverTimeThread overTimeThread;
    //时间列表
    private ArrayList<TimeInfo> timeList = new ArrayList<>();
    private RandomTimeAdapter timeAdapter;

    private Timer timer;


    Handler handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case STOP_TRAINING:
                    stopTraining();
                    break;
                case Timer.TIMER_FLAG:
                    if (timeAdapter != null)
                    {
                        timeAdapter.notifyDataSetChanged();
                        lvTimes.setSelection(timeList.size() - 1);
                    }
                    tvCurrentTimes.setText(timeList.size() + "");
                    tvLostTimes.setText(lostTimes + "");
                    //开始到现在持续的时间
                    tvTotalTime.setText(msg.obj.toString());

                    if (randomMode == 1 && timer.time >= trainingTime)
                    {
                        stopTraining();
                    }
                    break;
                //获取到电量信息
                case POWER_RECEIVE:
                    btnBegin.setEnabled(true);
                    //获取电量信息
                    List<PowerInfo> powerInfos = DataAnalyzeUtils.analyzePowerData(msg.obj
                            .toString(), RandomTrainingActivity.this);
                    //将电量信息赋值到全局变量中
                    Device.DEVICE_LIST.clear();
                    Device.DEVICE_LIST.addAll(powerInfos);
                    break;
                case TIME_RECEIVE:
                    String data = msg.obj.toString();
                    //返回数据不为空
                    if (data != null && !data.equals(""))
                    {
                        if (data.length() >= 7)
                            timeList.addAll(DataAnalyzeUtils.analyzeTimeData(data));
                        //通知更新时间信息
                        Message msg1 = new Message();
                        msg1.what = CHANGE_TIME_LIST;
                        handler.sendMessage(msg1);
                        isTrainingOver();
                    }
                    break;
                case IS_TRAINING_OVER:
                    isTrainingOver();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_randomtraining);
        ButterKnife.bind(this);
        randomMode = getIntent().getIntExtra("randomMode", 0);
        initView();
        device = new Device(RandomTrainingActivity.this);

        device.createDeviceList(RandomTrainingActivity.this);
        // 判断是否插入协调器，
        if (device.devCount > 0)
        {
            device.connectFunction(RandomTrainingActivity.this);
            device.initConfig();
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

    @Override
    protected void onDestroy()
    {
        if (trainingFlag)
            stopTraining();
        device.disconnectFunction();
        super.onDestroy();
    }

    @Override
    public void onBackPressed()
    {
        if (trainingFlag)
        {
            Toast.makeText(RandomTrainingActivity.this, "请先停止训练后再退出!", Toast
                    .LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
    }

    public void initView()
    {
        tvTitle.setText("随机训练");
        imgHelp.setVisibility(View.VISIBLE);
        timeAdapter = new RandomTimeAdapter(this, timeList);
        lvTimes.setAdapter(timeAdapter);
        if (randomMode == 0)
        {
            //次数随机
            llTrainingTimes.setVisibility(View.VISIBLE);
            llTrainingTime.setVisibility(View.GONE);
            barTrainingTimes.setOnSeekBarChangeListener(new MySeekBarListener
                    (tvTrainingTimes, 500));
            imgTrainingTimesSub.setOnTouchListener(new ChangeBarClickListener
                    (barTrainingTimes, 0));
            imgTrainingTimesAdd.setOnTouchListener(new ChangeBarClickListener
                    (barTrainingTimes, 1));
        }
        else
        {
            //时间随机
            llTrainingTimes.setVisibility(View.GONE);
            llTrainingTime.setVisibility(View.VISIBLE);
            barTrainingTime.setOnSeekBarChangeListener(new MySeekBarListener
                    (tvTrainingTime, 30));
            imgTrainingTimeSub.setOnTouchListener(new ChangeBarClickListener
                    (barTrainingTime, 0));
            imgTrainingTimeAdd.setOnTouchListener(new ChangeBarClickListener
                    (barTrainingTime, 1));
        }
        //设置seekbar 拖动事件的监听器
        barDelayTime.setOnSeekBarChangeListener(new MySeekBarListener(tvDelayTime, 10));
        barOverTime.setOnSeekBarChangeListener(new MySeekBarListener(tvOverTime, 30));
        //设置加减按钮的监听事件
        imgDelayTimeSub.setOnTouchListener(new ChangeBarClickListener
                (barDelayTime, 0));
        imgDelayTimeAdd.setOnTouchListener(new ChangeBarClickListener
                (barDelayTime, 1));
        imgOverTimeSub.setOnTouchListener(new ChangeBarClickListener
                (barOverTime, 0));
        imgOverTimeAdd.setOnTouchListener(new ChangeBarClickListener
                (barOverTime, 1));
        //设定感应模式checkBox组合的点击事件
        ImageView[] views = new ImageView[]{imgActionModeTouch, imgActionModeLight,
                imgActionModeTogether};
        actionModeCheckBox = new CheckBox(0, views);
        new CheckBoxClickListener(actionModeCheckBox);
        //设定灯光模式checkBox组合的点击事件
        ImageView[] views1 = new ImageView[]{imgLightModeCenter, imgLightModeAll,
                imgLightModeBeside};
        lightModeCheckBox = new CheckBox(0, views1);
        new CheckBoxClickListener(lightModeCheckBox);
    }

    @OnClick({R.id.btn_begin, R.id.layout_cancel, R.id.img_help})
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.btn_begin:
                if (!device.checkDevice(RandomTrainingActivity.this))
                    return;
                if (!trainingFlag)
                    beginTraining();
                else
                    stopTraining();
                break;
            //头部返回按钮
            case R.id.layout_cancel:
                finish();
                break;
            case R.id.img_help:
                Intent intent = new Intent(this, VideoActivity.class);
                startActivity(intent);
                break;
        }
    }

    //获取设备灯编号
    private char getLightNum()
    {
        PowerInfo light = Device.DEVICE_LIST.get(RandomUtils.getRandomNum(Device.DEVICE_LIST.size()));
        char num = light.getDeviceNum();
        currentLight = num;
        Log.d(Constant.LOG_TAG, "turn on :" + num + "-" + currentTimes);
        return num;
    }

    //开始训练
    public void beginTraining()
    {
        //训练开始
        trainingFlag = true;
        btnBegin.setText("停止");
        totalTimes = new Integer(tvTrainingTimes.getText().toString().trim());
        delayTime = (int) ((new Double(tvDelayTime.getText().toString().trim())) * 1000);
        overTime = new Integer(tvOverTime.getText().toString().trim()) * 1000;
        trainingTime = (int) ((new Double(tvTrainingTime.getText().toString().trim()))
                * 60 * 1000);
        Log.d(Constant.LOG_TAG, "系统参数:" + totalTimes + "-" + delayTime + "-" +
                overTime);
        //数据清空
        currentTimes = 0;
        durationTime = 0;
        lostTimes = 0;
        timeList.clear();
        tvAverageTime.setText("---");
        timeAdapter.notifyDataSetChanged();
        //发送开灯命令
        turnOnLight();

        //开启超时线程
        overTimeThread = new OverTimeThread();
        overTimeThread.start();
        //开启接收返回灭灯时间线程
        new ReceiveThread(handler, device.ftDev, ReceiveThread.TIME_RECEIVE_THREAD,
                TIME_RECEIVE).start();
        //开启计时线程
        beginTime = System.currentTimeMillis();
        timer = new Timer(handler);
        timer.setBeginTime(beginTime);
        timer.start();
    }

    //判断训练是否结束
    public void isTrainingOver()
    {
        if (!trainingFlag)
            return;
        if (delayTime > 0)
        {
            //添加延时
            try
            {
                Thread.sleep(delayTime);
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        //次数随机
        if (randomMode == 0)
        {
            if (currentTimes < totalTimes)
                //发送开灯命令
                turnOnLight();
            else
                stopTraining();
        }
        else//时间随机
            turnOnLight();
    }

    //停止训练
    public void stopTraining()
    {
        trainingFlag = false;
        btnBegin.setText("开始");
        btnBegin.setEnabled(false);
        if (overTimeThread != null)
            overTimeThread.stopThread();
        if (timer != null)
            timer.stopTimer();
        if (device.devCount > 0)
            device.turnOffAll();

        //计算平均时间
        int totalTime = 0;
        for (TimeInfo info : timeList)
        {
            totalTime += info.getTime();
        }
        totalTime += lostTimes * overTime;
        Log.d(Constant.LOG_TAG, totalTime + "");
        tvAverageTime.setText(new DecimalFormat("0.00").format((totalTime * 1.0 / timeList.size())) + "毫秒");
        //暂停0.5秒,等全部数据返回结束接收线程
        Timer.sleep(500);
        //结束接收返回灭灯时间线程
        ReceiveThread.stopThread();
        Timer.sleep(500);
        // 发送获取全部设备电量指令
        device.sendGetPowerOrder();
        //开启接收电量的线程
        new ReceiveThread(handler, device.ftDev, ReceiveThread.POWER_RECEIVE_THREAD,
                POWER_RECEIVE).start();
    }

    //开灯
    private void turnOnLight()
    {
        device.turnOnLight(getLightNum());
        currentTimes++;
        durationTime = 0;
    }

    //关灯
    private void turnOffLight()
    {
        device.turnOffLight(currentLight);
        TimeInfo info = new TimeInfo();
        info.setDeviceNum(currentLight);
        timeList.add(info);
    }

    //超时线程
    class OverTimeThread extends Thread
    {
        private boolean stop = false;

        public void stopThread()
        {
            stop = true;
        }

        @Override
        public void run()
        {
            while (!stop)
            {
                durationTime += 100;
                if (durationTime == overTime)
                {
                    //发送关灯命令
                    turnOffLight();
                    lostTimes++;
                    Message msg = new Message();
                    msg.what = IS_TRAINING_OVER;
                    handler.sendMessage(msg);
                }
                try
                {
                    Thread.sleep(100);
                } catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
}
