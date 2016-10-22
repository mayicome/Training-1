package com.oucb303.training.adpter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.oucb303.training.R;
import com.oucb303.training.model.TimeInfo;

import java.util.List;

/**
 * Created by huzhiming on 16/9/26.
 * Description：
 */

public class RandomTimeAdapter extends BaseAdapter
{
    private List<TimeInfo> timeList;
    private Context context;
    private LayoutInflater inflater;

    public RandomTimeAdapter(Context context, List<TimeInfo> list)
    {
        this.timeList = list;
        this.context = context;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount()
    {
        if (timeList == null)
            return 0;
        else
            return timeList.size();
    }

    @Override
    public Object getItem(int i)
    {
        return timeList.get(i);
    }

    @Override
    public long getItemId(int i)
    {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup)
    {
        View v = inflater.inflate(R.layout.item_statistics_time, null);
        TextView num = (TextView) v.findViewById(R.id.tv_num);
        TextView time = (TextView) v.findViewById(R.id.tv_time);
        TextView note = (TextView) v.findViewById(R.id.tv_note);
        num.setText((i + 1) + "");
        if (timeList.get(i).getTime() == 0)
        {
            time.setText("---");
            note.setText("超时");
        }
        else
        {
            time.setText(timeList.get(i).getTime() + "毫秒");
            note.setText("---");
        }
        return v;
    }
}
