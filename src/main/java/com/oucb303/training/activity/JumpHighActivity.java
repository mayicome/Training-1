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

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.oucb303.training.R;
import com.oucb303.training.adpter.GroupListViewAdapter;
import com.oucb303.training.adpter.JumpHighAdapter;

import com.oucb303.training.device.Device;
import com.oucb303.training.device.Order;
import com.oucb303.training.dialugue.CustomDialog;
import com.oucb303.training.listener.AddOrSubBtnClickListener;
import com.oucb303.training.listener.CheckBoxClickListener;
import com.oucb303.training.listener.MySeekBarListener;
import com.oucb303.training.listener.SpinnerItemSelectedListener;
import com.oucb303.training.model.CheckBox;
import com.oucb303.training.model.DeviceInfo;
import com.oucb303.training.model.TimeInfo;
import com.oucb303.training.threads.ReceiveThread;
import com.oucb303.training.threads.Timer;
import com.oucb303.training.utils.Battery;
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


    @Bind(R.id.lv_group)
    ListView lvGroup;

    @Bind(R.id.btn_begin)
    Button btnBegin;
    @Bind(R.id.btn_stop)
    Button btnStop;
    @Bind(R.id.tv_total_time)
    TextView tvTotalTime;

    android.widget.CheckBox cbVoice;

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
    @Bind(R.id.img_start)
    TextView imgSatart;

    private final int TIME_RECEIVE = 1, UPDATE_SCORES = 2;
    @Bind(R.id.btn_on)
    Button btnOn;
    @Bind(R.id.btn_off)
    Button btnOff;


    private Device device;
    private CheckBox actionModeCheckBox, lightModeCheckBox, lightColorCheckBox,blinkModeCheckBox;
    private GroupListViewAdapter groupListViewAdapter;

    private Timer timer; //计时器
    //同一组的最小间隔
    private int duration = 500;
    private JumpHighAdapter jumpHighAdapter;
    private List<JumpHighTrainingInfo> groupTrainingInfos = new ArrayList<>();
    private List<HashMap<String, Object>> scores = new ArrayList<>();
    Battery battery;
    //训练的总时间
    private int trainingTime;
    //每组设备个数、分组数
    private int groupSize = 1, groupNum, maxGroupSize;
    //是否正在训练标志
    private boolean trainingFlag = false;
    int[] Setting_return_data = new int[5];
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

        initView();
        device = new Device(this);
        device.createDeviceList(this);
        // 判断是否插入协调器，
        if (device.devCount > 0) {
            device.connect(this);
            device.initConfig();
        }
        battery = new Battery(device,imgSatart,handler_battery );
    }

    @Override
    protected void onStart() {
        super.onStart();
        imgSaveNew.setEnabled(false);
        set_dialog = createLightSetDialog();
        Setting_return_data[0]=0;
        Setting_return_data[1]=0;
        Setting_return_data[2]=0;
        Setting_return_data[3]=1;
        Setting_return_data[4]=1;
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


        //初始化训练时间拖动条
        barTrainingTime.setOnSeekBarChangeListener(new MySeekBarListener(tvTrainingTime, 10));
        imgTrainingTimeSub.setOnTouchListener(new AddOrSubBtnClickListener(barTrainingTime, 0));
        imgTrainingTimeAdd.setOnTouchListener(new AddOrSubBtnClickListener(barTrainingTime, 1));



        barTrainingTime.setProgress(level);

        maxGroupSize = Device.DEVICE_LIST.size();
        String[] lightNum = new String[maxGroupSize];
        for (int i = 0; i < lightNum.length; i++)
            lightNum[i] = (i + 1) + "个";

        spDevNum.setOnItemSelectedListener(new SpinnerItemSelectedListener(this, spDevNum, lightNum) {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                groupSize = i + 1;

                int totalGroupNum = Device.DEVICE_LIST.size() / groupSize;
                String[] trainGroupNum = new String[totalGroupNum + 1];
                trainGroupNum[0] = "";
                for (int j = 1; j <= totalGroupNum; j++)
                    trainGroupNum[j] = j + "组";

                ArrayAdapter<String> adapterGroupNum = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, trainGroupNum);
                adapterGroupNum.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spGroupNum.setAdapter(adapterGroupNum);
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

    }


    @OnClick({R.id.layout_cancel, R.id.btn_begin, R.id.img_help,  R.id.btn_on, R.id.btn_off,R.id.img_save_new,
            R.id.btn_result,R.id.img_set,R.id.btn_stop,R.id.img_start})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.img_set:
                CustomDialog dialog = new  CustomDialog(JumpHighActivity.this,"From btn 2",new CustomDialog.ICustomDialogEventListener() {
                    @Override
                    public void customDialogEvent(int id) {
                        // TextView imageView = (TextView)findViewById(R.id.main_image);

                        int aaa = id;
                        String a  =String.valueOf(aaa);
                        for(int i=0;i<5;i++){
                            Setting_return_data[i] = aaa % 10;
                            aaa = aaa/10;
                            Log.i("----------",i+"   "+Setting_return_data[i]+"");
                            //imageView.append(Setting_shuju[i]+"   ");
                        }
                    }
                },R.style.dialog_rank);

                dialog.show();
                OperateUtils operateUtils = new OperateUtils();
                operateUtils.setScreenWidth(JumpHighActivity.this,dialog, 0.95, 0.7);

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

                }
                else
                    startTraining();
                break;
            case R.id.btn_stop:
                if(trainingFlag){
                    stopTraining();
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
            case R.id.img_start:
                battery.initDevice();
                break;
        }
    }

    //开始训练
    private void startTraining() {

        trainingFlag = true;
        btnOn.setClickable(false);
        btnOff.setClickable(false);




        groupTrainingInfos.clear();
        scores.clear();
        for (int i = 0; i < groupNum; i++) {
            groupTrainingInfos.add(new JumpHighTrainingInfo());
            scores.add(new HashMap<String, Object>());
        }

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
        btnOn.setClickable(true);
        btnOff.setClickable(true);
    }

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
        device.sendOrder(deviceNum, Order.LightColor.values()[Setting_return_data[3]],
                Order.VoiceMode.values()[Setting_return_data[1]],
                Order.BlinkModel.values()[Setting_return_data[2]],
                Order.LightModel.OUTER,
                Order.ActionModel.values()[Setting_return_data[4]],
                Order.EndVoice.values()[Setting_return_data[0]]);
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
    Handler handler_battery = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    maxGroupSize = Device.DEVICE_LIST.size();
                    String[] lightNum = new String[maxGroupSize];
                    for (int i = 0; i < lightNum.length; i++)
                        lightNum[i] = (i + 1) + "个";

                    spDevNum.setOnItemSelectedListener(new SpinnerItemSelectedListener(JumpHighActivity.this, spDevNum, lightNum) {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                            groupSize = i + 1;

                            int totalGroupNum = Device.DEVICE_LIST.size() / groupSize;
                            String[] trainGroupNum = new String[totalGroupNum + 1];
                            trainGroupNum[0] = "";
                            for (int j = 1; j <= totalGroupNum; j++)
                                trainGroupNum[j] = j + "组";

                            ArrayAdapter<String> adapterGroupNum = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, trainGroupNum);
                            adapterGroupNum.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spGroupNum.setAdapter(adapterGroupNum);
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
                    break;
                default:
                    break;
            }
        }
    };
}
