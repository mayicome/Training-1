package com.oucb303.training.model;

/**
 * Created by huzhiming on 16/10/12.
 * Description：
 */

public class PowerInfo
{
    public PowerInfo()
    {

    }

    public PowerInfo(char num)
    {
        this.deviceNum = num;
    }

    //设备编号
    private char deviceNum;
    //电量
    private int power;
    //所属分组
    private int GroupNum;

    public char getDeviceNum()
    {
        return deviceNum;
    }

    public void setDeviceNum(char deviceNum)
    {
        this.deviceNum = deviceNum;
    }

    public int getPower()
    {
        return power;
    }

    public void setPower(int power)
    {
        this.power = power;
    }

    public int getGroupNum()
    {
        return GroupNum;
    }

    public void setGroupNum(int groupNum)
    {
        GroupNum = groupNum;
    }
}
