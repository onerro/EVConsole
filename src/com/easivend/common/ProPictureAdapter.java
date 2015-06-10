/****************************************Copyright (c)*************************************************
**                      Fujian Junpeng Communicaiton Technology Co.,Ltd.
**                               http://www.easivend.com.cn
**--------------File Info------------------------------------------------------------------------------
** File name:           ProPictureAdapter.java
** Last modified Date:  2015-01-10
** Last Version:         
** Descriptions:        GridView适配器类，这里面配置商品设置页面的图片数据     
**------------------------------------------------------------------------------------------------------
** Created by:          guozhenzhen 
** Created date:        2015-01-10
** Version:             V1.0 
** Descriptions:        The original version       
********************************************************************************************************/


package com.easivend.common;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.evconsole.R;

public class ProPictureAdapter extends BaseAdapter {// 创建基于BaseAdapter的子类

    private LayoutInflater inflater;// 创建LayoutInflater对象
    private List<ProPicture> pictures;// 创建List泛型集合

    // 为类创建构造函数
    public ProPictureAdapter(String[] proID, String[] promarket,String[] prosales,String[] proImage,String[] procount, Context context) {
        super();
        pictures = new ArrayList<ProPicture>();// 初始化泛型集合对象
        inflater = LayoutInflater.from(context);// 初始化LayoutInflater对象
        for (int i = 0; i < proImage.length; i++)// 遍历图像数组
        {        	
            ProPicture picture = new ProPicture(proID[i],promarket[i],prosales[i], proImage[i],procount[i]);// 使用标题和图像生成ProPicture对象
            pictures.add(picture);// 将Picture对象添加到泛型集合中
            ToolClass.Log(ToolClass.INFO,"EV_JNI","APP<<proID="+proID[i]+",promarket="+promarket[i]+",prosales="+prosales[i]+",proImage="+proImage[i]+",procount="+procount[i]);
        }
    }

    @Override
    public int getCount() {// 获取泛型集合的长度
        if (null != pictures) {// 如果泛型集合不为空
            return pictures.size();// 返回泛型长度
        } else {
            return 0;// 返回0
        }
    }

    @Override
    public Object getItem(int arg0) {
        return pictures.get(arg0);// 获取泛型集合指定索引处的项
    }

    @Override
    public long getItemId(int arg0) {
        return arg0;// 返回泛型集合的索引
    }
    
    
    @Override
    public View getView(int arg0, View arg1, ViewGroup arg2) {
        ProViewHolder viewHolder;// 创建ProViewHolder对象
        if (arg1 == null) {// 判断图像标识是否为空

            arg1 = inflater.inflate(R.layout.productgv, null);// 设置图像标识
            viewHolder = new ProViewHolder();// 初始化ProViewHolder对象
            viewHolder.proID = (TextView) arg1.findViewById(R.id.proID);// 设置图像标题
            viewHolder.image = (ImageView) arg1.findViewById(R.id.proImage);// 设置图像的二进制值
            viewHolder.promarket = (TextView) arg1.findViewById(R.id.promarket);// 设置图像标题
            viewHolder.prosales = (TextView) arg1.findViewById(R.id.prosales);// 设置图像标题
            viewHolder.count = (TextView) arg1.findViewById(R.id.count);// 设置剩余数量
            
            arg1.setTag(viewHolder);// 设置提示
        } 
        else
        {
            viewHolder = (ProViewHolder) arg1.getTag();// 设置提示
        }
        if(Integer.parseInt(pictures.get(arg0).getProcount())>0)
        {
        	viewHolder.count.setText("剩余数量:"+pictures.get(arg0).getProcount());// 设置剩余数量
        }
        else
        {
        	viewHolder.count.setText("剩余数量:已售罄");// 设置剩余数量
        	viewHolder.count.setTextColor(android.graphics.Color.RED);
        }
        viewHolder.proID.setText(pictures.get(arg0).getProID());// 设置图像ID
        viewHolder.promarket.setText("原价:"+pictures.get(arg0).getPromarket());// 设置原价
        viewHolder.promarket.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG); //删除线
        viewHolder.prosales.setText("现价:"+pictures.get(arg0).getProsales());// 设置现价
        /*为什么图片一定要转化为 Bitmap格式的！！ */
        Bitmap bitmap = ToolClass.getLoacalBitmap(pictures.get(arg0).getProImage()); //从本地取图片(在cdcard中获取)  //
        viewHolder.image.setImageBitmap(bitmap);// 设置图像的二进制值
        return arg1;// 返回图像标识
    }
}

class ProViewHolder {// 创建ProViewHolder类存放控件集合

    public TextView proID;// 创建商品ID和名称
    public ImageView image;// 创建ImageView对象
    public TextView promarket;// 创建商品原价
    public TextView prosales;// 创建商品销售价
    public TextView count;// 创建商品剩余数量
}

class ProPicture {// 创建ProPicture类

    private String proID;// 定义字符串，表示图像标题
    private String proImage;//图像位置
    private String promarket;//原价
    private String prosales;//现价
    private String procount;//商品数量
	public ProPicture(String proID, String promarket,String prosales,String proImage,String procount)
	{
		super();
		this.proID = proID;
		this.proImage = proImage;
		this.promarket = promarket;
		this.prosales = prosales;
		this.procount = procount;
	}
	public String getProID() {
		return proID;
	}
	public void setProID(String proID) {
		this.proID = proID;
	}
	public String getProImage() {
		return proImage;
	}
	public void setProImage(String proImage) {
		this.proImage = proImage;
	}
	public String getPromarket() {
		return promarket;
	}
	public void setPromarket(String promarket) {
		this.promarket = promarket;
	}
	public String getProsales() {
		return prosales;
	}
	public void setProsales(String prosales) {
		this.prosales = prosales;
	}
	public String getProcount() {
		return procount;
	}
	public void setProcount(String procount) {
		this.procount = procount;
	}
	
    
}