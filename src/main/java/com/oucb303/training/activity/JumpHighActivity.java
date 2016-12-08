package com.oucb303.training.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.oucb303.training.R;
import com.oucb303.training.adpter.GroupListViewAdapter;
import com.oucb303.training.adpter.JumpHighAdapter;
import com.oucb303.training.device.Device;
import com.oucb303.training.device.Order;
import com.oucb303.training.listener.AddOrSubBtnClickListener;
import com.oucb303.training.listener.CheckBoxClickListener;
import com.oucb303.training.listener.MySeekBarListener;
import com.oucb303.training.listener.SpinnerItemSelectedListener;
import com.oucb303.training.model.CheckBox;
import com.oucb303.training.model.DeviceInfo;
import com.oucb303.training.model.TimeInfo;
import com.oucb303.training.threads.ReceiveThread;
import com.oucb303.training.threads.Timer;
import com.oucb303.training.utils.DataAnalyzeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class JumpHighActivity extends AppCompatActivity
{

    @Bind(R.id.tv_title)
    TextView tvTitle;
    @Bind(R.id.img_help)
    ImageView imgHelp;
    @Bind(R.id.tv_training_time)
    TextView tvTrainingTime;
    @Bind(R.id.img_training_time_sub)
    ImageView imgTrainingTimeSub;
    @Bind(R.id.bar_training_time)
    SeekBar barTrainingTime;
    @Bind(R.id.img_training_time_add)
    ImageView imgTrainingTimeAdd;
    @Bind(R.id.sp_dev_num)
    Spinner spDevNum;
    @Bind(R.id.sp_group_num)
    Spinner spGroupNum;
    @Bind(R.id.img_action_mode_light)
    ImageView imgActionModeLight;
    @Bind(R.id.img_action_mode_touch)
    ImageView imgActionModeTouch;
    @Bind(R.id.img_action_mode_together)
    ImageView imgActionModeTogether;
    @Bind(R.id.img_light_mode_beside)
    ImageView imgLightModeBeside;
    @Bind(R.id.img_light_mode_center)
    ImageView imgLightModeCenter;
    @Bind(R.id.img_light_mode_all)
    ImageView imgLightModeAll;
    @Bind(R.id.img_light_color_blue)
    ImageView imgLightColorBlue;
    @Bind(R.id.img_light_color_red)
    ImageView imgLightColorRed;
    @Bind(R.id.img_light_color_blue_red)
    ImageView imgLightColorBlueRed;
    @Bind(R.id.lv_group)
    ListView lvGroup;
    @Bind(R.id.sv_container)
    ScrollView svContainer;
    @Bind(R.id.btn_begin)
    Button btnBegin;
    @Bind(R.id.tv_total_time)
    TextView tvTotalTime;
    @Bind(R.id.cb_voice)
    android.widget.CheckBox cbVoice;
    @Bind(R.id.cb_end_voice)
    android.widget.CheckBox cbEndVoice;
    @Bind(R.id.lv_scores)
    ListView lvScores;

    private final int TIME_RECEIVE = 1, UPDATE_SCORES = 2;

    private Device device;
    private CheckBox actionModeCheckBox, lightModeCheckBox, lightColorCheckBox;
    private GroupListViewAdapter groupListViewAdapter;
    private Timer timer; //计时器
    //同一组的最小间隔
    private int duration = 200;
    private JumpHighAdapter jumpHighAdapter;
    private List<JumpHighTrainingInfo> groupTrainingInfos = new ArrayList<>();
    private List<HashMap<String, Object>> scores = new ArrayList<>();
    //训练的总时间
    private int trainingTime;
    //每组设备个数、分组数
    private int groupSize = 1, groupNum;
    //是否正在训练标志
    private boolean trainingFlag = false;


    private Handler handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            String data = msg.obj.toString();
            switch (msg.what)
            {
                //更新计时
                case Timer.TIMER_FLAG:
                    tvTotalTime.setText(data);
                    for (int i = 0; i < groupNum; i++)
                    {
                        JumpHighTrainingInfo trainingInfo = groupTrainingInfos.get(i);
                        //距离最先挥灭的灯 超过200毫秒
                        if (trainingInfo.deviceList.size() > 0 && System.currentTimeMillis() - trainingInfo.firstReceivedTime > duration)
                        {
                            Map<String, Object> map = scores.get(i);
                            String lights = "";
                            int score = 0;
                            if (map.get("score") != null)
                                score = (int) map.get("score");
                            int s = 0;
                            for (String str : trainingInfo.deviceList)
                            {
                                lights += str + " ";
                                int temp = findPosition(str.charAt(0)) + 1;
                                if (temp > s)
                                    s = temp;
                            }
                            score += s;
                            map.put("lights", lights);
                            map.put("score", score);

                            jumpHighAdapter.notifyDataSetChanged();
                            trainingInfo.deviceList.clear();
                        }
                    }
                    if (timer.time >= trainingTime)
                    {
                        timer.stopTimer();
                        stopTraining();
                    }
                    break;

                case TIME_RECEIVE://接收到设备返回的时间
                    analyseData(data);
                    break;
                //更新成绩
                case UPDATE_SCORES:
                    jumpHighAdapter.notifyDataSetChanged();
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jump_high);
        ButterKnife.bind(this);
        initView();
        device = new Device(this);
        device.createDeviceList(this);
        // 判断是否插入协调器，
        if (device.devCount > 0)
        {
            device.connectFunction(this);
            device.initConfig();
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (device.devCount > 0)
            device.disconnectFunction();
    }


    private void initView()
    {
        tvTitle.setText("纵跳摸高");
        imgHelp.setVisibility(View.VISIBLE);
        jumpHighAdapter = new JumpHighAdapter(this, scores);
        lvScores.setAdapter(jumpHighAdapter);

        ///初始化分组listView
        groupListViewAdapter = new GroupListViewAdapter(JumpHighActivity.this, groupSize);
        lvGroup.setAdapter(groupListViewAdapter);
        //解决listView 与scrollView的滑动冲突
        lvGroup.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent)
            {
                //从listView 抬起时将控制权还给scrollview
                if (motionEvent.getAction() == MotionEvent.ACTION_UP)
                    svContainer.requestDisallowInterceptTouchEvent(false);
                else
                    svContainer.requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        //初始化训练时间拖动条
        barTrainingTime.setOnSeekBarChangeListener(new MySeekBarListener(tvTrainingTime, 20));
        imgTrainingTimeSub.setOnTouchListener(new AddOrSubBtnClickListener(barTrainingTime, 0));
        imgTrainingTimeAdd.setOnTouchListener(new AddOrSubBtnClickListener(barTrainingTime, 1));

        spDevNum.setOnItemSelectedListener(new SpinnerItemSelectedListener(this, spDevNum, new String[]{"一个", "两个", "三个", "四个"})
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
            {
                groupSize = i + 1;
                if (Device.DEVICE_LIST.size() / groupSize < groupNum)
                {
                    Toast.makeText(JumpHighActivity.this, "当前设备数量为" + Device.DEVICE_LIST.size() + ",不能分成" + i + "组!",
                            Toast.LENGTH_LONG).show();
                    spGroupNum.setSelection(0);
                    groupNum = 0;
                }
                groupListViewAdapter.setGroupNum(groupNum);
                groupListViewAdapter.setGroupSize(groupSize);
                groupListViewAdapter.notifyDataSetChanged();
            }
        });

        spGroupNum.setOnItemSelectedListener(new SpinnerItemSelectedListener(this, spGroupNum, new String[]{" ", "一组", "两组", "三组", "四组"})
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
            {
                groupNum = i;
                if (Device.DEVICE_LIST.size() / groupSize < groupNum)
                {
                    Toast.makeText(JumpHighActivity.this, "当前设备数量为" + Device.DEVICE_LIST.size() + ",不能分成" + i + "组!",
                            Toast.LENGTH_LONG).show();
                    spGroupNum.setSelection(0);
                    groupNum = 0;
                }
                groupListViewAdapter.setGroupNum(groupNum);
                groupListViewAdapter.notifyDataSetChanged();
            }
        });

        //设定感应模式checkBox组合的点击事件
        ImageView[] views = new ImageView[]{imgActionModeLight, imgActionModeTouch, imgActionModeTogether};
        actionModeCheckBox = new CheckBox(1, views);
        new CheckBoxClickListener(actionModeCheckBox);
        //设定灯光模式checkBox组合的点击事件
        ImageView[] views1 = new ImageView[]{imgLightModeBeside, imgLightModeCenter, imgLightModeAll,};
        lightModeCheckBox = new CheckBox(1, views1);
        new CheckBoxClickListener(lightModeCheckBox);
        //设定灯光颜色checkBox组合的点击事件
        ImageView[] views2 = new ImageView[]{imgLightColorBlue, imgLightColorRed, imgLightColorBlueRed};
        lightColorCheckBox = new CheckBox(1, views2);
        new CheckBoxClickListener(lightColorCheckBox);


    }


    @OnClick({R.id.layout_cancel, R.id.btn_begin})
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.layout_cancel:
                this.finish();
                break;
            case R.id.btn_begin:
                if (!device.checkDevice(this))
                    return;
                if (groupNum <= 0)
                {
                    Toast.makeText(this, "未选择分组,不能开始!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (trainingFlag)
                    stopTraining();
                else
                    startTraining();
                break;
        }
    }

    //开始训练
    private void startTraining()
    {
        btnBegin.setText("停止");
        trainingFlag = true;
        trainingTime = (int) (new Double(tvTrainingTime.getText().toString()) * 60 * 1000);


        groupTrainingInfos.clear();
        scores.clear();
        for (int i = 0; i < groupNum; i++)
        {
            groupTrainingInfos.add(new JumpHighTrainingInfo());
            scores.add(new HashMap<String, Object>());
        }
        jumpHighAdapter.notifyDataSetChanged();

        //开启接收时间线程
        new ReceiveThread(handler, device.ftDev, ReceiveThread.TIME_RECEIVE_THREAD, TIME_RECEIVE).start();

        //开启全部灯
        for (int i = 0; i < groupNum * groupSize; i++)
        {
            turnOnLights(Device.DEVICE_LIST.get(i).getDeviceNum());
        }
        //开启计时器
        timer = new Timer(handler);
        timer.setBeginTime(System.currentTimeMillis());
        timer.start();
    }

    //结束训练
    private void stopTraining()
    {
        timer.stopTimer();
        btnBegin.setText("开始");
        trainingFlag = false;
        ReceiveThread.stopThread();
        device.turnOffAllTheLight();
    }

    private void analyseData(final String data)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                if (data.length() < 7)
                    return;
                List<TimeInfo> infos = DataAnalyzeUtils.analyzeTimeData(data);
                for (TimeInfo info : infos)
                {
                    int groupId = findGroupId(info.getDeviceNum());
                    JumpHighTrainingInfo traningInfo = groupTrainingInfos.get(groupId);

                    if (traningInfo.deviceList.size() == 0)
                    {
                        traningInfo.firstReceivedTime = System.currentTimeMillis();
                    }
                    traningInfo.deviceList.add(info.getDeviceNum() + "");

                    turnOnLights(info.getDeviceNum());
                }
            }
        }).start();
    }

    //开启一组的全部灯
    private void turnOnLights(final char deviceNum)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                Timer.sleep(500);
                device.sendOrder(deviceNum, Order.LightColor.values()[lightColorCheckBox.getCheckId()],
                        Order.VoiceMode.values()[cbVoice.isChecked() ? 1 : 0],
                        Order.BlinkModel.NONE,
                        Order.LightModel.values()[lightModeCheckBox.getCheckId()],
                        Order.ActionModel.values()[actionModeCheckBox.getCheckId()],
                        Order.EndVoice.values()[cbEndVoice.isChecked() ? 1 : 0]);

            }
        }).start();
    }


    //查找设备所属分组
    private int findGroupId(char deviceNum)
    {
        int i = 0;
        for (DeviceInfo info : Device.DEVICE_LIST)
        {
            if (info.getDeviceNum() == deviceNum)
                break;
            i++;
        }
        return i / groupSize;
    }

    private int findPosition(char deviceNum)
    {
        int i = 0;
        for (DeviceInfo info : Device.DEVICE_LIST)
        {
            if (info.getDeviceNum() == deviceNum)
                break;
            i++;
        }
        return i % groupSize;
    }

    //记录每次挥灭灯的信息
    public class JumpHighTrainingInfo
    {
        //最先挥灭灯的时间
        public long firstReceivedTime;
        //一次性挥灭所有灯的灯编号
        public ArrayList<String> deviceList = new ArrayList<>();
    }
}