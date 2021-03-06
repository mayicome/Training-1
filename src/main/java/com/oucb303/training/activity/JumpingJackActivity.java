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
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.oucb303.training.dialugue.CustomDialog;
import com.oucb303.training.listener.AddOrSubBtnClickListener;
import com.oucb303.training.listener.CheckBoxClickListener;
import com.oucb303.training.listener.MySeekBarListener;
import com.oucb303.training.listener.SpinnerItemSelectedListener;
import com.oucb303.training.model.CheckBox;
import com.oucb303.training.model.DeviceInfo;
import com.oucb303.training.model.TimeInfo;
import com.oucb303.training.service.MusicService;
import com.oucb303.training.threads.ReceiveThread;
import com.oucb303.training.threads.Timer;
import com.oucb303.training.utils.Battery;
import com.oucb303.training.utils.DataAnalyzeUtils;
import com.oucb303.training.utils.DialogUtils;
import com.oucb303.training.utils.OperateUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Bai ChangCai on 2017/8/31.
 * 开合跳：
 *       固定时间内完成次数
 */
public class JumpingJackActivity extends AppCompatActivity{

    @Bind(R.id.tv_title)
    TextView tvTitle;
    @Bind(R.id.img_help)
    ImageView imgHelp;
    @Bind(R.id.img_set)
    ImageView imgSet;
    @Bind(R.id.sp_group_num)
    Spinner spGroupNum;
    @Bind(R.id.lv_group)
    ListView lvGroup;
    @Bind(R.id.btn_begin)
    Button btnBegin;
    @Bind(R.id.tv_total_time)
    TextView tvTotalTime;
    @Bind(R.id.img_start)
    TextView imgSatart;
    @Bind(R.id.lv_scores)
    ListView lvScores;
    @Bind(R.id.img_save_new)
    ImageView imgSaveNew;

    private final int TIME_RECEIVE = 1, UPDATE_SCORES = 2;
    @Bind(R.id.btn_on)
    Button btnOn;
    @Bind(R.id.btn_off)
    Button btnOff;


    private Device device;
    private GroupListViewAdapter groupListViewAdapter;
    private Timer timer; //计时器
    //同一组的最小间隔
    private int duration = 300;
    private JumpHighAdapter jumpHighAdapter;
    private List<JumpHighTrainingInfo> groupTrainingInfos = new ArrayList<>();
    private List<HashMap<String, Object>> scores = new ArrayList<>();
    //训练的总时间
    private int trainingTime=108*1000;
    //每组设备个数、分组数
    private int groupNum, maxGroupSize;
    //是否正在训练标志
    private final int groupSize=2;
    private boolean trainingFlag = false;

    private int colors[];
    private int level=2;

    Battery battery;
    int[] Setting_return_data = new int[5];
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
                        //距离最先挥灭的灯 超过500毫秒
                        if (trainingInfo.deviceList.size() > 0 && System.currentTimeMillis() - trainingInfo.firstReceivedTime > duration) {
                            Map<String, Object> map = scores.get(i);
                            String lights = "";
                            int score = 0;
                            if (map.get("score") != null)
                                score = (int) map.get("score");
                            int s = 0;
                            for (String str : trainingInfo.deviceList) {
                                lights += str + " ";
                            }
                            //灭两个得1分，灭一个不得分
                            if(trainingInfo.deviceList.size()==2){
                                s=1;
                            }
                            score += s;
                            map.put("lights", lights);
                            map.put("score", score);
                            jumpHighAdapter.notifyDataSetChanged();
                            trainingInfo.deviceList.clear();
                        }
                    }
                    if (timer.time >= trainingTime) {
                        timer.stopTimer();
                        stopTraining();
                    }
                    break;

                case TIME_RECEIVE://接收到设备返回的时间
                    if (data.length() < 7)
                        return;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jumpingjack);
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

        Setting_return_data[0]=0;
        Setting_return_data[1]=0;
        Setting_return_data[2]=0;
        Setting_return_data[3]=1;
        Setting_return_data[4]=1;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (device.devCount > 0)
            device.disconnect();
    }

    private void initView() {
        tvTitle.setText(R.string.navigation_rhythm_item1);
        imgHelp.setVisibility(View.VISIBLE);
        imgSaveNew.setVisibility(View.VISIBLE);
        jumpHighAdapter = new JumpHighAdapter(this, scores);
        lvScores.setAdapter(jumpHighAdapter);
        imgHelp.setVisibility(View.VISIBLE);

        ///初始化分组listView
        groupListViewAdapter = new GroupListViewAdapter(JumpingJackActivity.this, groupSize);
       lvGroup.setAdapter(groupListViewAdapter);

        maxGroupSize = Device.DEVICE_LIST.size();
        String[] lightNum = new String[maxGroupSize];
        for (int i = 0; i < lightNum.length; i++)
            lightNum[i] = (i + 1) + "个";


        int totalGroupNum = Device.DEVICE_LIST.size() / groupSize;
        String[] trainGroupNum = new String[totalGroupNum + 1];
        trainGroupNum[0] = "";
        for (int j = 1; j <= totalGroupNum; j++)
            trainGroupNum[j] = j + "组";

        ArrayAdapter<String> adapterGroupNum = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, trainGroupNum);
        adapterGroupNum.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spGroupNum.setAdapter(adapterGroupNum);

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


    @OnClick({R.id.btn_stop,R.id.img_set,R.id.layout_cancel, R.id.btn_begin, R.id.img_help,
            R.id.img_save_new, R.id.btn_on, R.id.btn_off,R.id.img_start})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.img_set:
                CustomDialog dialog = new CustomDialog(JumpingJackActivity.this,"From btn 2",new CustomDialog.ICustomDialogEventListener() {
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
                operateUtils.setScreenWidth(JumpingJackActivity.this,dialog, 0.95, 0.7);
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
                if (trainingFlag)
                    stopTraining();
                else
                    startTraining();
                break;
            case R.id.img_help:
                List<Integer> list = new ArrayList<>();
                list.add(R.string.endurance_training_method);
                list.add(R.string.endurance_training_standard);
                Dialog dialog_help = DialogUtils.createHelpDialog(JumpingJackActivity.this,list);
                OperateUtils.setScreenWidth(this, dialog_help, 0.95, 0.7);
                dialog_help.show();
                break;
            case R.id.img_save_new:
                Intent it = new Intent(this, SaveActivity.class);
                Bundle bundle = new Bundle();
                //trainingCategory 1:折返跑 2:纵跳摸高、滞空练习、开合跳 3:仰卧起坐 ...
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
                break;
            case R.id.btn_off:
                device.turnOffAllTheLight();
                break;
            case R.id.btn_stop:
                if(trainingFlag){
                    stopTraining();
                }
                break;
            case R.id.img_start:
                battery.initDevice();
                break;
        }
    }

    //开始训练
    private void startTraining() {
//        btnBegin.setText("停止");
        trainingFlag = true;
        Intent intent = new Intent(JumpingJackActivity.this,MusicService.class);
        startService(intent);

        groupTrainingInfos.clear();
        scores.clear();
        for (int i = 0; i < groupNum; i++) {
            groupTrainingInfos.add(new JumpHighTrainingInfo());
            scores.add(new HashMap<String, Object>());
        }
        jumpHighAdapter.notifyDataSetChanged();

        //清除串口数据
        new ReceiveThread(handler, device.ftDev, ReceiveThread.CLEAR_DATA_THREAD, 0).start();

        //开启接收时间线程
        new ReceiveThread(handler, device.ftDev, ReceiveThread.TIME_RECEIVE_THREAD, TIME_RECEIVE).start();

        //开启全部灯
        for (int i = 0; i < groupNum * groupSize; i++) {
            int color = Setting_return_data[3];
            device.sendOrder(Device.DEVICE_LIST.get(i).getDeviceNum(),
                    Order.LightColor.values()[color],
                    Order.VoiceMode.values()[Setting_return_data[1]],
                    Order.BlinkModel.values()[Setting_return_data[2]],
                    Order.LightModel.OUTER,
                    Order.ActionModel.values()[Setting_return_data[4]],
                    Order.EndVoice.values()[Setting_return_data[0]]);
        }
        TurnOnLightThread turnOnLightThread = new TurnOnLightThread();
        turnOnLightThread.start();
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
        Intent intent = new Intent(JumpingJackActivity.this,MusicService.class);
        stopService(intent);
        ReceiveThread.stopThread();
        timer.sleep(800);
        device.turnOffAllTheLight();
    }

    private void analyseData(final String data) {
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

                int color = Setting_return_data[3];
                if (color == 3) {
                    color = (colors[groupNum] + 1) % 2;
                    colors[groupNum] = color;
                    color++;
                }

                for (int i = 0; i < groupSize; i++) {
                    char num = Device.DEVICE_LIST.get(groupNum * groupSize + i).getDeviceNum();
                    device.sendOrder(num,
                            Order.LightColor.values()[color],
                            Order.VoiceMode.values()[Setting_return_data[1]],
                            Order.BlinkModel.values()[Setting_return_data[2]],
                            Order.LightModel.OUTER,
                            Order.ActionModel.values()[Setting_return_data[4]],
                            Order.EndVoice.values()[Setting_return_data[0]]);

                }

    }

    class TurnOnLightThread extends Thread{
        private int duration_new;
        @Override
        public void run() {
            while(trainingFlag){
                if(timer.time>0&&timer.time<=1000*9){
                    duration_new = 1500;
                }else if(timer.time>9&&timer.time<=1000*14){
                    duration_new = 900;
                }else if(timer.time>14&&timer.time<=1000*71){
                    duration_new = 1300;
                }else if(timer.time>71&&timer.time<=1000*81){
                    duration_new = 1000;
                }else {
                    duration_new = 800;
                }
                timer.sleep(duration_new);
                int color = Setting_return_data[3];
                if (color == 3) {
                    color = (colors[groupNum] + 1) % 2;
                    colors[groupNum] = color;
                    color++;
                }
                for(int j=0;j<groupNum;j++){
                    for (int i = 0; i < groupSize; i++) {
                        char num = Device.DEVICE_LIST.get(j * groupSize + i).getDeviceNum();
                        device.sendOrder(num,
                                Order.LightColor.values()[color],
                                Order.VoiceMode.values()[Setting_return_data[1]],
                                Order.BlinkModel.values()[Setting_return_data[2]],
                                Order.LightModel.OUTER,
                                Order.ActionModel.values()[Setting_return_data[4]],
                                Order.EndVoice.values()[Setting_return_data[0]]);

                    }
                }

            }

        }
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

    //记录每次挥灭灯的信息
    public class JumpHighTrainingInfo {
        //最先挥灭灯的时间
        public long firstReceivedTime;
        //一次性挥灭所有灯的灯编号
        public ArrayList<String> deviceList = new ArrayList<>();
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


                    int totalGroupNum = Device.DEVICE_LIST.size() / groupSize;
                    String[] trainGroupNum = new String[totalGroupNum + 1];
                    trainGroupNum[0] = "";
                    for (int j = 1; j <= totalGroupNum; j++)
                        trainGroupNum[j] = j + "组";

                    ArrayAdapter<String> adapterGroupNum = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, trainGroupNum);
                    adapterGroupNum.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spGroupNum.setAdapter(adapterGroupNum);

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
