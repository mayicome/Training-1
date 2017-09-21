package com.oucb303.training.activity;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
/*import android.view.MotionEvent;*/
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
/*import android.widget.ScrollView;*/
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
import com.oucb303.training.utils.DialogUtils;
import com.oucb303.training.utils.OperateUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 纵跳摸高
 */
public class JumpHighActivity extends AppCompatActivity {

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
    /*   @Bind(R.id.img_action_mode_light)
       ImageView imgActionModeLight;
       @Bind(R.id.img_action_mode_touch)
       ImageView imgActionModeTouch;
       @Bind(R.id.img_action_mode_together)
       ImageView imgActionModeTogether;*/
//    @Bind(R.id.img_light_mode_beside)
//    ImageView imgLightModeBeside;
//    @Bind(R.id.img_light_mode_center)
//    ImageView imgLightModeCenter;
//    @Bind(R.id.img_light_mode_all)
//    ImageView imgLightModeAll;
   /* @Bind(R.id.img_light_color_blue)
    ImageView imgLightColorBlue;
    @Bind(R.id.img_light_color_red)
    ImageView imgLightColorRed;
    @Bind(R.id.img_light_color_blue_red)
    ImageView imgLightColorBlueRed;*/
    @Bind(R.id.lv_group)
    ListView lvGroup;
    /*   @Bind(R.id.sv_container)
       ScrollView svContainer;*/
    @Bind(R.id.btn_begin)
    Button btnBegin;
    @Bind(R.id.btn_stop)
    Button btnStop;
    @Bind(R.id.tv_total_time)
    TextView tvTotalTime;
    //  @Bind(R.id.cb_voice)
    android.widget.CheckBox cbVoice;
    //   @Bind(R.id.cb_end_voice)
    android.widget.CheckBox cbEndVoice;
    @Bind(R.id.lv_scores)
    ListView lvScores;
    @Bind(R.id.layout_cancel)
    LinearLayout layoutCancel;
    @Bind(R.id.img_save_new)
    ImageView imgSaveNew;
    @Bind(R.id.btn_result)
    Button btnResult;
    @Bind(R.id.img_set)
    ImageView imgSet;
   /* @Bind(R.id.img_level_sub)
    ImageView imgLevelsub;
    @Bind(R.id.img_level_add)
    ImageView imgLevelAdd;
    @Bind(R.id.bar_level)
    SeekBar barLevel;
    @Bind(R.id.tv_level)
    TextView tvLevel;*/

    private final int TIME_RECEIVE = 1, UPDATE_SCORES = 2;
    @Bind(R.id.btn_on)
    Button btnOn;
    @Bind(R.id.btn_off)
    Button btnOff;
    /*@Bind(R.id.img_blink_mode_none)
    ImageView imgBlinkModeNone;
    @Bind(R.id.img_blink_mode_slow)
    ImageView imgBlinkModeSlow;
    @Bind(R.id.img_blink_mode_fast)
    ImageView imgBlinkModeFast;*/

    private Device device;
    private CheckBox actionModeCheckBox, lightModeCheckBox, lightColorCheckBox,blinkModeCheckBox;
    private GroupListViewAdapter groupListViewAdapter;
//    private JumpHighAdapter1 jumpHighAdapter;
    private Timer timer; //计时器
    //同一组的最小间隔
    private int duration = 500;
    private JumpHighAdapter jumpHighAdapter;
    private List<JumpHighTrainingInfo> groupTrainingInfos = new ArrayList<>();
    private List<HashMap<String, Object>> scores = new ArrayList<>();
//    //训练成绩
//    private int[] scores;
//    //key : 组号 value： 成绩
//    private Map<Integer, Integer> timeMap = new HashMap<Integer, Integer>();
//    //存放排序后的key值
//    private int[] keyId;
    //训练的总时间
    private int trainingTime;
    //每组设备个数、分组数
    private int groupSize = 1, groupNum, maxGroupSize;
    //是否正在训练标志
    private boolean trainingFlag = false;

    private int colors[];
    private int level=2;
    private Dialog set_dialog;
    private Context context;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String data = msg.obj.toString();
            switch (msg.what) {
                //更新计时
                case Timer.TIMER_FLAG:
                    tvTotalTime.setText(data);
                    for (int i = 0; i < groupNum; i++) {
                        JumpHighTrainingInfo trainingInfo = groupTrainingInfos.get(i);
                        //距离最先挥灭的灯 超过200毫秒
                        if (trainingInfo.deviceList.size() > 0 && System.currentTimeMillis() - trainingInfo.firstReceivedTime > duration) {
                            Map<String, Object> map = scores.get(i);
                            String lights = "";
                            int score = 0;
                            if (map.get("score") != null)
                                score = (int) map.get("score");
                            int s = 0;
                            for (String str : trainingInfo.deviceList) {
                                lights += str + " ";
                                int temp = findPosition(str.charAt(0)) + 1;
                                if (temp > s)
                                    s = temp;
                            }
                            score += s;
                            map.put("lights", lights);
                            map.put("score", score);
                            turnOnLights(i);
                            jumpHighAdapter.notifyDataSetChanged();
                            trainingInfo.deviceList.clear();
                        }
                    }
                    if (timer.time >= trainingTime) {
                        timer.stopTimer();
                        stopTraining();
                    }
                    break;
                case Timer.TIMER_DOWN:
                    tvTotalTime.setText("倒计时："+msg.obj.toString());
                    break;
                case TIME_RECEIVE://接收到设备返回的时间
                    if (data.length() < 7)
                        return;
                    analyzeData(data);
                    break;
                //更新成绩
                case UPDATE_SCORES:

                    jumpHighAdapter.notifyDataSetChanged();
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jump_high);
        ButterKnife.bind(this);
        context = this;
//        level = getIntent().getIntExtra("level", 1);
//        level=2;
        initView();
        device = new Device(this);
        device.createDeviceList(this);
        // 判断是否插入协调器，
        if (device.devCount > 0) {
            device.connect(this);
            device.initConfig();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        imgSaveNew.setEnabled(false);
        set_dialog = createLightSetDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (device.devCount > 0)
            device.disconnect();
    }

    private void initView() {
        tvTitle.setText("原地纵跳摸高");
        imgHelp.setVisibility(View.VISIBLE);
        imgSaveNew.setVisibility(View.VISIBLE);
       jumpHighAdapter = new JumpHighAdapter(this,scores);
        lvScores.setAdapter(jumpHighAdapter);
        imgHelp.setVisibility(View.VISIBLE);

        ///初始化分组listView
        groupListViewAdapter = new GroupListViewAdapter(JumpHighActivity.this, groupSize);
        lvGroup.setAdapter(groupListViewAdapter);
        //解决listView 与scrollView的滑动冲突
    /*    lvGroup.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                //从listView 抬起时将控制权还给scrollview
                if (motionEvent.getAction() == MotionEvent.ACTION_UP)
                    svContainer.requestDisallowInterceptTouchEvent(false);
                else
                    svContainer.requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });*/
        //初始化训练强度拖动条
    /*    barLevel.setOnSeekBarChangeListener(new MySeekBarListener(barTrainingTime,tvLevel, 2));
        imgLevelsub.setOnTouchListener(new AddOrSubBtnClickListener(barLevel, 0));
        imgLevelAdd.setOnTouchListener(new AddOrSubBtnClickListener(barLevel, 1));*/
        //初始化训练时间拖动条
        barTrainingTime.setOnSeekBarChangeListener(new MySeekBarListener(tvTrainingTime, 10));
        imgTrainingTimeSub.setOnTouchListener(new AddOrSubBtnClickListener(barTrainingTime, 0));
        imgTrainingTimeAdd.setOnTouchListener(new AddOrSubBtnClickListener(barTrainingTime, 1));

//        switch (level) {
//            case 1:
//                level = 2;
//                break;
//            case 2:
//                level = 4;
//                break;
//            case 3:
//                level = 10;
//                break;
//        }

        barTrainingTime.setProgress(level);

        maxGroupSize = Device.DEVICE_LIST.size();
        String[] lightNum = new String[maxGroupSize];
        for (int i = 0; i < lightNum.length; i++)
            lightNum[i] = (i + 1) + "个";

        spDevNum.setOnItemSelectedListener(new SpinnerItemSelectedListener(this, spDevNum, lightNum) {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                groupSize = i + 1;
//                Log.d("groupNum",""+groupNum);

//                if (Device.DEVICE_LIST.size() / groupSize < groupNum) {
////                    Toast.makeText(JumpHighActivity.this, "当前设备数量为" + Device.DEVICE_LIST.size() + ",不能分成" + groupNum + "组!",
////                            Toast.LENGTH_LONG).show();
//                    spGroupNum.setSelection(0);
//                    groupNum = 0;
//                }


                int totalGroupNum = Device.DEVICE_LIST.size() / groupSize;
                String[] trainGroupNum = new String[totalGroupNum + 1];
                trainGroupNum[0] = "";
                for (int j = 1; j <= totalGroupNum; j++)
                    trainGroupNum[j] = j + "组";

                ArrayAdapter<String> adapterGroupNum = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, trainGroupNum);
                adapterGroupNum.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spGroupNum.setAdapter(adapterGroupNum);
//                if (Device.DEVICE_LIST.size() / groupSize < groupNum) {
//                    Toast.makeText(JumpHighActivity.this, "当前设备数量为" + Device.DEVICE_LIST.size() + ",不能分成" + i + "组!",
//                            Toast.LENGTH_LONG).show();
//                    spGroupNum.setSelection(0);
//                    groupNum = 0;
//                }
//                groupListViewAdapter.setGroupNum(groupNum)
            }
        });
        spGroupNum.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                groupNum = position;

                groupListViewAdapter.setGroupSize(groupSize);
                groupListViewAdapter.setGroupNum(groupNum);
                groupListViewAdapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
//        String[] trainGroupNum = new String[]


//        spGroupNum.setOnItemSelectedListener(new SpinnerItemSelectedListener(this, spGroupNum, new String[]{" ", "一组", "两组", "三组", "四组"}) {
//            @Override
//            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
//                groupNum = i;
//
//                groupListViewAdapter.setGroupNum(groupNum);
//                groupListViewAdapter.notifyDataSetChanged();
//            }
//        });

        //设定感应模式checkBox组合的点击事件
     /*   ImageView[] views = new ImageView[]{imgActionModeLight, imgActionModeTouch, imgActionModeTogether};
        actionModeCheckBox = new CheckBox(1, views);
        new CheckBoxClickListener(actionModeCheckBox);*/
        //设定灯光模式checkBox组合的点击事件
//        ImageView[] views1 = new ImageView[]{imgLightModeBeside, imgLightModeCenter, imgLightModeAll,};
//        lightModeCheckBox = new CheckBox(1, views1);
//        new CheckBoxClickListener(lightModeCheckBox);
        //设定灯光颜色checkBox组合的点击事件
       /* ImageView[] views2 = new ImageView[]{imgLightColorBlue, imgLightColorRed, imgLightColorBlueRed};
        lightColorCheckBox = new CheckBox(1, views2);
        new CheckBoxClickListener(lightColorCheckBox);*/
        //设定闪烁模式checkbox组合的点击事件
        /*ImageView[] views3 = new ImageView[]{imgBlinkModeNone, imgBlinkModeSlow, imgBlinkModeFast,};
        blinkModeCheckBox = new CheckBox(1, views3);
        new CheckBoxClickListener(blinkModeCheckBox);*/
    }


    @OnClick({R.id.layout_cancel, R.id.btn_begin, R.id.img_help,  R.id.btn_on, R.id.btn_off,R.id.img_save_new,R.id.btn_result,R.id.img_set,R.id.btn_stop})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.img_set:
                set_dialog = createLightSetDialog();
                OperateUtils.setScreenWidth(this, set_dialog, 0.95, 0.7);
                set_dialog.show();
                break;
            case R.id.layout_cancel:
                this.finish();
                device.turnOffAllTheLight();
                break;
            case R.id.btn_begin:
                if (!device.checkDevice(this))
                    return;
                if (groupNum <= 0) {
                    Toast.makeText(this, "未选择分组,不能开始!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (trainingFlag) {
                    stopTraining();
                    btnOn.setClickable(false);
                    btnOff.setClickable(false);
                }
                else
                    startTraining();
                break;
            case R.id.btn_stop:
                if(trainingFlag){
                    stopTraining();
                    btnOn.setClickable(true);
                    btnOff.setClickable(true);
                }
                break;
            case R.id.img_help:
                List<Integer> list = new ArrayList<>();
                list.add(R.string.endurance_training_method);
                list.add(R.string.endurance_training_standard);
                Dialog dialog_help = DialogUtils.createHelpDialog(JumpHighActivity.this,list);
                OperateUtils.setScreenWidth(this, dialog_help, 0.95, 0.7);
                dialog_help.show();
                break;
            case R.id.img_save_new:
                Intent it = new Intent(this, SaveActivity.class);
                Bundle bundle = new Bundle();
                //trainingCategory 1:折返跑 2:纵跳摸高 3:仰卧起坐 ...
                bundle.putString("trainingCategory", "2");
                //训练总时间
                bundle.putInt("trainingTime", trainingTime);
                //每组设备个数
                bundle.putInt("groupDeviceNum", groupSize);
                //每组得分
                ArrayList scoresList = new ArrayList<>();
                scoresList.add(scores);
                bundle.putParcelableArrayList("scores", scoresList);
                it.putExtras(bundle);
                startActivity(it);
                break;
            case R.id.btn_on:
                //groupNum组数，groupSize：每组设备个数，1：类型
                device.turnOnButton(groupNum, groupSize, 1);
                btnOn.setClickable(false);
                break;
            case R.id.btn_off:
                device.turnOffAllTheLight();
                btnOn.setClickable(true);
                break;
        }
    }

    //开始训练
    private void startTraining() {

        trainingFlag = true;


//        scores = new int[groupNum];
//        for (int i = 0; i < groupNum; i++) {
//            timeMap.put(i, 0);
//        }
//        keyId = new int[groupNum];


        groupTrainingInfos.clear();
        scores.clear();
        for (int i = 0; i < groupNum; i++) {
            groupTrainingInfos.add(new JumpHighTrainingInfo());
            scores.add(new HashMap<String, Object>());
        }
//        jumpHighAdapter.setScores(scores);
//        jumpHighAdapter.setTimeMap(timeMap,keyId);
        jumpHighAdapter.notifyDataSetChanged();
        //训练时间
        trainingTime = (int) (new Double(tvTrainingTime.getText().toString()) * 60 * 1000);

        //清除串口数据
        new ReceiveThread(handler, device.ftDev, ReceiveThread.CLEAR_DATA_THREAD, 0).start();

        //开启接收时间线程
        new ReceiveThread(handler, device.ftDev, ReceiveThread.TIME_RECEIVE_THREAD, TIME_RECEIVE).start();

        //开启全部灯
        for (int i = 0; i < groupNum * groupSize; i++) {
            int color = lightColorCheckBox.getCheckId();
            sendOrder(Device.DEVICE_LIST.get(i).getDeviceNum());
        }
        colors = new int[groupNum];
        //开启计时器
        timer = new Timer(handler, trainingTime);
        timer.setBeginTime(System.currentTimeMillis());
        timer.start();
    }

    //结束训练
    private void stopTraining() {
        timer.stopTimer();

        imgSaveNew.setEnabled(true);
        trainingFlag = false;
        ReceiveThread.stopThread();
        device.turnOffAllTheLight();
    }
//    private void analyzeTimeData(final String data) {
//        //训练已结束
//        if (!trainingFlag)
//            return;
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                List<TimeInfo> infos = DataAnalyzeUtils.analyzeTimeData(data);
//                for (TimeInfo info : infos) {
//                    int groupId = findGroupId(info.getDeviceNum());
//                    Log.d("getDeviceNum----------", "" + info.getDeviceNum());
//                    Log.d("groupID---------------", "" + groupId);
//                    char next = Device.DEVICE_LIST.get(groupId * groupSize).getDeviceNum();
//                    if (next == info.getDeviceNum()) {
//                        next = Device.DEVICE_LIST.get(groupId * groupSize + 1).getDeviceNum();
//                        scores[groupId] += 1;
//                        timeMap.put(groupId, scores[groupId]);
//                    }
//                    sendOrder(next);
//                }
//                Message msg = Message.obtain();
//                msg.obj = "";
//                msg.what = UPDATE_SCORES;
//                handler.sendMessage(msg);
//
//            }
//        }).start();
//
//    }
    private void analyzeData(final String data) {
        List<TimeInfo> infos = DataAnalyzeUtils.analyzeTimeData(data);
        for (TimeInfo info : infos) {
            int groupId = findGroupId(info.getDeviceNum());
            JumpHighTrainingInfo traningInfo = groupTrainingInfos.get(groupId);

            if (traningInfo.deviceList.size() == 0) {
                traningInfo.firstReceivedTime = System.currentTimeMillis();
            }
            traningInfo.deviceList.add(info.getDeviceNum() + "");
        }
    }

    //开灯
    private void turnOnLights(final int groupNum) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                timer.sleep(29);
                int color = lightColorCheckBox.getCheckId();
                if (color == 3) {
                    color = (colors[groupNum] + 1) % 2;
                    colors[groupNum] = color;
                    color++;
                }

                for (int i = 0; i < groupSize; i++) {
                    char num = Device.DEVICE_LIST.get(groupNum * groupSize + i).getDeviceNum();
                    sendOrder(num);

                }
            }
        }).start();
    }
    public void sendOrder(char deviceNum) {
        device.sendOrder(deviceNum, Order.LightColor.values()[lightColorCheckBox.getCheckId()],
                Order.VoiceMode.values()[cbVoice.isChecked() ? 1 : 0],
                Order.BlinkModel.values()[blinkModeCheckBox.getCheckId() - 1],
                Order.LightModel.OUTER,
                Order.ActionModel.values()[actionModeCheckBox.getCheckId()],
                Order.EndVoice.values()[cbEndVoice.isChecked()?1:0]);
    }
    //查找设备所属分组
    private int findGroupId(char deviceNum) {
        int i = 0;
        for (DeviceInfo info : Device.DEVICE_LIST) {
            if (info.getDeviceNum() == deviceNum)
                break;
            i++;
        }
        return i / groupSize;
    }

    private int findPosition(char deviceNum) {
        int i = 0;
        for (DeviceInfo info : Device.DEVICE_LIST) {
            if (info.getDeviceNum() == deviceNum)
                break;
            i++;
        }
        return i % groupSize;
    }

    //记录每次挥灭灯的信息
    public class JumpHighTrainingInfo {
        //最先挥灭灯的时间
        public long firstReceivedTime;
        //一次性挥灭所有灯的灯编号
        public ArrayList<String> deviceList = new ArrayList<>();
    }
//    //排序
//    public void sortTime(Map<Integer, Integer> timeMap) {
//        List<Map.Entry<Integer, Integer>> list = new ArrayList<Map.Entry<Integer, Integer>>(timeMap.entrySet());
//        Collections.sort(list, new Comparator<Map.Entry<Integer, Integer>>() {
//            //升序排序
//            public int compare(Map.Entry<Integer, Integer> o1,
//                               Map.Entry<Integer, Integer> o2) {
//                return o2.getValue().compareTo(o1.getValue());
//            }
//        });
//        int i = 0;
//        for (Map.Entry<Integer, Integer> mapping : list) {
//            Log.i("============", "" + mapping.getKey() + ":" + mapping.getValue());
////            System.out.println(mapping.getKey()+":"+mapping.getValue());
//            keyId[i] = mapping.getKey();
//            i++;
//        }
//    }


    public Dialog createLightSetDialog() {

        LayoutInflater inflater = LayoutInflater.from(this);
        View v = inflater.inflate(R.layout.layout_dialog_lightset, null);// 得到加载view

        LinearLayout layout = (LinearLayout) v.findViewById(R.id.dialog_light_set);
        ImageView imgActionModeTouch = (ImageView) layout.findViewById(R.id.img_action_mode_touch);
        ImageView imgActionModeLight = (ImageView) layout.findViewById(R.id.img_action_mode_light);
        ImageView imgActionModeTogether = (ImageView) layout.findViewById(R.id.img_action_mode_together);
        ImageView imgLightColorBlue = (ImageView) layout.findViewById(R.id.img_light_color_blue);
        ImageView imgLightColorRed = (ImageView) layout.findViewById(R.id.img_light_color_red);
        ImageView imgLightColorBlueRed = (ImageView) layout.findViewById(R.id.img_light_color_blue_red);
        ImageView imgBlinkModeNone = (ImageView) layout.findViewById(R.id.img_blink_mode_none);
        ImageView imgBlinkModeSlow = (ImageView) layout.findViewById(R.id.img_blink_mode_slow);
        ImageView imgBlinkModeFast = (ImageView) layout.findViewById(R.id.img_blink_mode_fast);
        cbVoice = (android.widget.CheckBox) layout.findViewById(R.id.cb_voice);
        cbEndVoice = (android.widget.CheckBox)layout.findViewById(R.id.cb_endvoice);
        Button btnOk = (Button) layout.findViewById(R.id.btn_ok);
        Button btnCloseSet = (Button) layout.findViewById(R.id.btn_close_set);
        final Dialog dialog = new Dialog(this, R.style.dialog_rank);

        dialog.setContentView(layout);

        //设定感应模式checkBox组合的点击事件
        ImageView[] views = new ImageView[]{imgActionModeLight, imgActionModeTouch, imgActionModeTogether};
        actionModeCheckBox = new CheckBox(1, views);
        new CheckBoxClickListener(actionModeCheckBox);
        //设定灯光颜色checkBox组合的点击事件
        ImageView[] views2 = new ImageView[]{imgLightColorBlue, imgLightColorRed, imgLightColorBlueRed};
        lightColorCheckBox = new CheckBox(1, views2);
        new CheckBoxClickListener(lightColorCheckBox);
        //设定闪烁模式checkbox组合的点击事件
        ImageView[] views3 = new ImageView[]{imgBlinkModeNone, imgBlinkModeSlow, imgBlinkModeFast};
        blinkModeCheckBox = new CheckBox(1, views3);
        new CheckBoxClickListener(blinkModeCheckBox);

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        btnCloseSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        return dialog;
    }
}
