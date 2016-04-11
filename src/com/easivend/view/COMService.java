/****************************************Copyright (c)*************************************************
**                      Fujian Junpeng Communicaiton Technology Co.,Ltd.
**                               http://www.easivend.com.cn
**--------------File Info------------------------------------------------------------------------------
** File name:           COMService.java
** Last modified Date:  2015-01-10
** Last Version:         
** Descriptions:        与串口设备连接的服务      
**------------------------------------------------------------------------------------------------------
** Created by:          guozhenzhen 
** Created date:        2015-01-10
** Version:             V1.0 
** Descriptions:        The original version       
********************************************************************************************************/

package com.easivend.view;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.easivend.app.maintain.MaintainActivity;
import com.easivend.common.SerializableMap;
import com.easivend.common.ToolClass;
import com.easivend.dao.vmc_cabinetDAO;
import com.easivend.evprotocol.COMThread;
import com.easivend.evprotocol.EVprotocol;
import com.easivend.http.EVServerhttp;
import com.easivend.model.Tb_vmc_cabinet;
import com.easivend.view.EVServerService.ActivityReceiver;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;

public class COMService extends Service {
	//=====================货道==================================
	public static final int EV_CHECKALLCHILD= 1;	//查询全部柜子状态
	public static final int EV_CHECKALLMAIN	= 2;	//查询全部柜子状态返回
	public static final int EV_CHECKCHILD	= 3;	//货道查询	
	public static final int EV_CHUHUOCHILD	= 4;	//货道出货	
	public static final int EV_LIGHTCHILD	= 5;	//照明	
	public static final int EV_COOLCHILD	= 6;	//制冷	
	public static final int EV_HOTCHILD		= 7;	//加热
	
	public static final int EV_CHECKMAIN	= 8;	//货道查询	返回
	public static final int EV_OPTMAIN  	= 9;	//货道操作返回
	public static final int EV_SETHUOCHILD	= 10;	//货道设置
	
	//=====================现金设备==================================
	public static final int EV_MDB_ENABLE 	= 22;	//MDB设备使能
	public static final int EV_MDB_HEART 	= 23;	//MDB设备心跳
	public static final int EV_MDB_B_INFO 	= 24;	//MDB纸币器信息
	public static final int EV_MDB_C_INFO 	= 25;	//MDB硬币器信息
	public static final int EV_MDB_COST 	= 26;	//MDB设备扣款
	public static final int EV_MDB_PAYBACK = 27;	//MDB设备退币
	public static final int EV_MDB_PAYOUT 	= 28;	//MDB设备找币
	public static final int EV_MDB_B_CON 	= 29;	//MDB纸币器配置
	public static final int EV_MDB_C_CON 	= 30;	//MDB硬币器配置
	public static final int EV_MDB_HP_PAYOUT = 31;	//hopper硬币器找零
	
	ActivityReceiver receiver;
	LocalBroadcastManager localBroadreceiver;
	private ExecutorService thread=null;
    private Handler mainhand=null,childhand=null; 
    COMThread comserial=null;
    Map<String,Integer> huoSet=new LinkedHashMap<String,Integer>();
    private String[] cabinetID = null;//用来分离出货柜编号    
    private int huom = 0;// 定义一个开始标识
    
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	//8.创建activity的接收器广播，用来接收内容
	public class ActivityReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent)
		{
			// TODO Auto-generated method stub
			Bundle bundle=intent.getExtras();
			int EVWhat=bundle.getInt("EVWhat");
			switch(EVWhat)
			{
			//查询全部柜子状态
			case EV_CHECKALLCHILD:
				ToolClass.Log(ToolClass.INFO,"EV_COM","COMService 货道查询全部","com.txt");
				vmc_cabinetDAO cabinetDAO = new vmc_cabinetDAO(context);// 创建InaccountDAO对象
			    // 1.获取所有柜号
			    List<Tb_vmc_cabinet> listinfos = cabinetDAO.getScrollData();
			    cabinetID = new String[listinfos.size()];// 设置字符串数组的长度
			    // 遍历List泛型集合
			    for (Tb_vmc_cabinet tb_inaccount : listinfos) 
			    {
			        cabinetID[huom] = tb_inaccount.getCabID();
			        ToolClass.Log(ToolClass.INFO,"EV_COM","获取柜号="+cabinetID[huom],"com.txt");
				    huom++;// 标识加1
			    }
			    huom=0;
			    if(listinfos.size()==0)
			    {
			    	//返回给activity广播
					Intent recintent=new Intent();
					recintent.putExtra("EVWhat", COMService.EV_CHECKALLMAIN);						
					//传递数据
			        SerializableMap myMap=new SerializableMap();
			        myMap.setMap(huoSet);//将map数据添加到封装的myMap<span></span>中
			        Bundle bundlerec=new Bundle();
			        bundlerec.putSerializable("result", myMap);
			        recintent.putExtras(bundlerec);
			        recintent.setAction("android.intent.action.comrec");//action与接收器相同
			        localBroadreceiver.sendBroadcast(recintent);
			    }
			    else
				{
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
			    	childhand=comserial.obtainHandler();
	        		Message childrec=childhand.obtainMessage();
	        		//查找货道类型
	        		vmc_cabinetDAO cabinetDAOrec = new vmc_cabinetDAO(context);// 创建InaccountDAO对象
	        		// 获取所有收入信息，并存储到List泛型集合中
	        	    Tb_vmc_cabinet listinfosrec = cabinetDAOrec.findScrollData(cabinetID[huom]);
	        		//格子柜
	        	    if(listinfosrec.getCabType()==5)
	        		{
	        			ToolClass.Log(ToolClass.INFO,"EV_COM","COMService 格子柜查询="+cabinetID[huom],"com.txt");
	        			childrec.what=COMThread.EV_BENTO_CHECKALLCHILD;
	        		}
	        		else
	        		{
	        			ToolClass.Log(ToolClass.INFO,"EV_COM","COMService 货道柜查询="+cabinetID[huom],"com.txt");
	        			childrec.what=COMThread.EV_COLUMN_CHECKALLCHILD;
	        		}
	        		JSONObject evrec=null;
		    		try {
		    			evrec=new JSONObject();
		    			evrec.put("cabinet", cabinetID[huom]);	    			  			
		    			ToolClass.Log(ToolClass.INFO,"EV_COM","ServiceSend0.1="+evrec.toString(),"com.txt");
		    		} catch (JSONException e) {
		    			// TODO Auto-generated catch block
		    			e.printStackTrace();
		    		}
		    		childrec.obj=evrec;
	        		childhand.sendMessage(childrec);	        	
				}
				break;    
			//货道查询	
			case EV_CHECKCHILD:
				
				childhand=comserial.obtainHandler();
        		Message child2=childhand.obtainMessage();
        		//查找货道类型
        		vmc_cabinetDAO cabinetDAO2 = new vmc_cabinetDAO(context);// 创建InaccountDAO对象
        	    // 获取所有收入信息，并存储到List泛型集合中
        	    Tb_vmc_cabinet listinfos2 = cabinetDAO2.findScrollData(String.valueOf(bundle.getInt("cabinet")));
        		//格子柜
        	    if(listinfos2.getCabType()==5)
        		{
        			ToolClass.Log(ToolClass.INFO,"EV_COM","COMService 格子柜查询="+String.valueOf(bundle.getInt("cabinet")),"com.txt");
        			child2.what=COMThread.EV_BENTO_CHECKCHILD;
        		}
        		else
        		{
        			ToolClass.Log(ToolClass.INFO,"EV_COM","COMService 货道柜查询="+String.valueOf(bundle.getInt("cabinet")),"com.txt");
        			child2.what=COMThread.EV_COLUMN_CHECKCHILD;
        		}
        		JSONObject ev2=null;
	    		try {
	    			ev2=new JSONObject();
	    			ev2.put("cabinet", bundle.getInt("cabinet"));	    			  			
	    			ToolClass.Log(ToolClass.INFO,"EV_COM","ServiceSend0.1="+ev2.toString(),"com.txt");
	    		} catch (JSONException e) {
	    			// TODO Auto-generated catch block
	    			e.printStackTrace();
	    		}
	    		child2.obj=ev2;
        		childhand.sendMessage(child2);
				break;
			//货道出货	
			case EV_CHUHUOCHILD:		
				Message child3=childhand.obtainMessage();
				//查找货道类型
        		vmc_cabinetDAO cabinetDAO3 = new vmc_cabinetDAO(context);// 创建InaccountDAO对象
        	    // 获取所有收入信息，并存储到List泛型集合中
        	    Tb_vmc_cabinet listinfos3 = cabinetDAO3.findScrollData(String.valueOf(bundle.getInt("cabinet")));
        		
        		JSONObject ev3=null;
	    		try {
	    			ev3=new JSONObject();
	    			ev3.put("cabinet", bundle.getInt("cabinet"));	
	    			//格子柜
	        	    if(listinfos3.getCabType()==5)
	        		{
	        	    	ToolClass.Log(ToolClass.INFO,"EV_COM","COMService 格子出货","com.txt");
	    				child3.what=COMThread.EV_BENTO_OPENCHILD;
	    				ev3.put("column", bundle.getInt("column"));
	        		}
	        		else
	        		{
	        	    	ToolClass.Log(ToolClass.INFO,"EV_COM","COMService 货道出货","com.txt");
	    				child3.what=COMThread.EV_COLUMN_OPENCHILD;
	    				ev3.put("column", ToolClass.columnChuhuo(bundle.getInt("column")));
	        		}	    			
	    			ToolClass.Log(ToolClass.INFO,"EV_COM","ServiceSend0.1="+ev3.toString(),"com.txt");
	    		} catch (JSONException e) {
	    			// TODO Auto-generated catch block
	    			e.printStackTrace();
	    		}
	    		child3.obj=ev3;
        		childhand.sendMessage(child3);	
				break;
			//货道设置
			case EV_SETHUOCHILD:		
				Message child7=childhand.obtainMessage();
				//查找货道类型
        		vmc_cabinetDAO cabinetDAO7 = new vmc_cabinetDAO(context);// 创建InaccountDAO对象
        	    // 获取所有收入信息，并存储到List泛型集合中
        	    Tb_vmc_cabinet listinfos7 = cabinetDAO7.findScrollData(String.valueOf(bundle.getInt("cabinet")));
        		
        		JSONObject ev7=null;
	    		try {
	    			ev7=new JSONObject();
	    			ev7.put("cabinet", bundle.getInt("cabinet"));	
	    			//格子柜
	        	    if(listinfos7.getCabType()==5)
	        		{
	        	    	ToolClass.Log(ToolClass.INFO,"EV_COM","COMService 格子出货","com.txt");
	    				child7.what=COMThread.EV_BENTO_OPENCHILD;
	    				ev7.put("column", bundle.getInt("column"));
	        		}
	        		else
	        		{
	        	    	ToolClass.Log(ToolClass.INFO,"EV_COM","COMService 货道出货","com.txt");
	    				child7.what=COMThread.EV_COLUMN_OPENCHILD;
	    				ev7.put("column", bundle.getInt("column"));
	        		}	    			
	    			ToolClass.Log(ToolClass.INFO,"EV_COM","ServiceSend0.1="+ev7.toString(),"com.txt");
	    		} catch (JSONException e) {
	    			// TODO Auto-generated catch block
	    			e.printStackTrace();
	    		}
	    		child7.obj=ev7;
        		childhand.sendMessage(child7);	
				break;	
			//快递柜照明	
			case EV_LIGHTCHILD:
				ToolClass.Log(ToolClass.INFO,"EV_COM","COMService 照明","com.txt");
				Message child4=childhand.obtainMessage();
				child4.what=COMThread.EV_BENTO_LIGHTCHILD;
        		JSONObject ev4=null;
	    		try {
	    			ev4=new JSONObject();
	    			ev4.put("cabinet", bundle.getInt("cabinet"));	
	    			ev4.put("opt", bundle.getInt("opt"));
	    			ToolClass.Log(ToolClass.INFO,"EV_COM","ServiceSend0.1="+ev4.toString(),"com.txt");
	    		} catch (JSONException e) {
	    			// TODO Auto-generated catch block
	    			e.printStackTrace();
	    		}
	    		child4.obj=ev4;
        		childhand.sendMessage(child4);	
				break;
			//快递柜制冷	
			case EV_COOLCHILD:
				ToolClass.Log(ToolClass.INFO,"EV_COM","COMService 制冷","com.txt");
				Message child5=childhand.obtainMessage();
				child5.what=COMThread.EV_BENTO_COOLCHILD;
        		JSONObject ev5=null;
	    		try {
	    			ev5=new JSONObject();
	    			ev5.put("cabinet", bundle.getInt("cabinet"));	
	    			ev5.put("opt", bundle.getInt("opt"));
	    			ToolClass.Log(ToolClass.INFO,"EV_COM","ServiceSend0.1="+ev5.toString(),"com.txt");
	    		} catch (JSONException e) {
	    			// TODO Auto-generated catch block
	    			e.printStackTrace();
	    		}
	    		child5.obj=ev5;
        		childhand.sendMessage(child5);	
				break;
			//快递柜加热	
			case EV_HOTCHILD:
				ToolClass.Log(ToolClass.INFO,"EV_COM","COMService 加热","com.txt");
				Message child6=childhand.obtainMessage();
				child6.what=COMThread.EV_BENTO_HOTCHILD;
        		JSONObject ev6=null;
	    		try {
	    			ev6=new JSONObject();
	    			ev6.put("cabinet", bundle.getInt("cabinet"));	
	    			ev6.put("opt", bundle.getInt("opt"));
	    			ToolClass.Log(ToolClass.INFO,"EV_COM","ServiceSend0.1="+ev6.toString(),"com.txt");
	    		} catch (JSONException e) {
	    			// TODO Auto-generated catch block
	    			e.printStackTrace();
	    		}
	    		child6.obj=ev6;
        		childhand.sendMessage(child6);	
				break;
			//现金设备使能禁能	
			case EV_MDB_ENABLE:
				ToolClass.Log(ToolClass.INFO,"EV_COM","COMService 使能禁能","com.txt");
				Message child8=childhand.obtainMessage();
				child8.what=COMThread.EV_MDB_ENABLE;
        		JSONObject ev8=null;
	    		try {
	    			ev8=new JSONObject();
	    			ev8.put("bill", bundle.getInt("bill"));	
	    			ev8.put("coin", bundle.getInt("coin"));
	    			ev8.put("opt", bundle.getInt("opt"));
	    			ToolClass.Log(ToolClass.INFO,"EV_COM","ServiceSend0.1="+ev8.toString(),"com.txt");
	    		} catch (JSONException e) {
	    			// TODO Auto-generated catch block
	    			e.printStackTrace();
	    		}
	    		child8.obj=ev8;
        		childhand.sendMessage(child8);	
				break;
				//纸币器查询接口
			case EV_MDB_B_INFO:
				ToolClass.Log(ToolClass.INFO,"EV_COM","COMService 纸币器查询接口","com.txt");
				Message child9=childhand.obtainMessage();
				child9.what=COMThread.EV_MDB_B_INFO;
        		JSONObject ev9=null;
	    		ev9=new JSONObject();	    			
				ToolClass.Log(ToolClass.INFO,"EV_COM","ServiceSend0.1="+ev9.toString(),"com.txt");
	    		child9.obj=ev9;
        		childhand.sendMessage(child9);	
				break;
			//纸币配置	
			case EV_MDB_B_CON:
				Message child12=childhand.obtainMessage();
				child12.what=COMThread.EV_MDB_B_CON;
				
				JSONObject jsonObject12 = new JSONObject(); 
				JSONObject EV_json12 = new JSONObject(); 
				JSONArray ch_r12=new JSONArray();
				JSONArray ch_d12=new JSONArray();
				try {
					jsonObject12.put("EV_type", EV_MDB_B_CON);
					jsonObject12.put("port_id", ToolClass.getCom_id());
					jsonObject12.put("acceptor", bundle.getInt("billtype"));
					jsonObject12.put("dispenser", bundle.getInt("billtype"));
					for(int i=1;i<=16;i++)
					{
						JSONObject ch12 = new JSONObject(); 
						ch12.put("ch", i);
						ch12.put("value", 0);
						ch_r12.put(ch12);
					}
					jsonObject12.put("ch_r", ch_r12);
					for(int i=1;i<=16;i++)
					{
						JSONObject ch12 = new JSONObject(); 
						ch12.put("ch", i);
						ch12.put("value", 0);
						ch_d12.put(ch12);
					}
					jsonObject12.put("ch_d", ch_d12);
					EV_json12.put("EV_json", jsonObject12);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				ToolClass.Log(ToolClass.INFO,"EV_COM","ServiceSend0.1="+EV_json12.toString(),"com.txt");
				        		
	    		child12.obj=EV_json12;
        		childhand.sendMessage(child12);	
				break;
				//硬币器查询接口
			case EV_MDB_C_INFO:
				ToolClass.Log(ToolClass.INFO,"EV_COM","COMService 硬币器查询接口","com.txt");
				Message child10=childhand.obtainMessage();
				child10.what=COMThread.EV_MDB_C_INFO;
        		JSONObject ev10=null;
	    		ev10=new JSONObject();	    			
				ToolClass.Log(ToolClass.INFO,"EV_COM","ServiceSend0.1="+ev10.toString(),"com.txt");
	    		child10.obj=ev10;
        		childhand.sendMessage(child10);	
				break;	
			case EV_MDB_C_CON:
				Message child13=childhand.obtainMessage();
				child13.what=COMThread.EV_MDB_C_CON;
				
				JSONObject jsonObject13 = new JSONObject(); 
				JSONObject EV_json13 = new JSONObject(); 
				JSONArray ch_r13=new JSONArray();
				JSONArray ch_d13=new JSONArray();
				try {
					jsonObject13.put("EV_type", EV_MDB_C_CON);
					jsonObject13.put("port_id", ToolClass.getCom_id());
					jsonObject13.put("acceptor", bundle.getInt("acceptor"));
					jsonObject13.put("dispenser", bundle.getInt("dispenser"));
					jsonObject13.put("hight_en", 0);
					//输出内容
					SerializableMap serializableMap = (SerializableMap) bundle.get("ch_r");
					Map<String, Integer> Set=serializableMap.getMap();
			        Set<Map.Entry<String,Integer>> allset=Set.entrySet();  //实例化
			        Iterator<Map.Entry<String,Integer>> iter=allset.iterator();
			        while(iter.hasNext())
			        {
			            Map.Entry<String,Integer> me=iter.next();
			            //System.out.println(me.getKey()+"--"+me.getValue());
			            JSONObject ch = new JSONObject(); 
						ch.put("ch", me.getKey());
						ch.put("value",me.getValue());
						ch_r13.put(ch);
			        } 	        
					jsonObject13.put("ch_r", ch_r13);
					
					//输出内容
					SerializableMap serializableMap2 = (SerializableMap) bundle.get("ch_d");
					Map<String, Integer> Set2=serializableMap2.getMap();
			        Set<Map.Entry<String,Integer>> allset2=Set2.entrySet();  //实例化
			        Iterator<Map.Entry<String,Integer>> iter2=allset2.iterator();
			        while(iter2.hasNext())
			        {
			            Map.Entry<String,Integer> me2=iter2.next();
			            //System.out.println(me.getKey()+"--"+me.getValue());
			            JSONObject ch = new JSONObject(); 
						ch.put("ch", me2.getKey());
						ch.put("value",me2.getValue());
						ch_d13.put(ch);
			        } 	   
					jsonObject13.put("ch_d", ch_d13);
					EV_json13.put("EV_json", jsonObject13);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				ToolClass.Log(ToolClass.INFO,"EV_COM","ServiceSend0.1="+EV_json13.toString(),"com.txt");
				        		
	    		child13.obj=EV_json13;
        		childhand.sendMessage(child13);
				break;
				//心跳查询接口
			case EV_MDB_HEART:
				ToolClass.Log(ToolClass.INFO,"EV_COM","COMService EV_MDB_HEART接口","com.txt");
				Message child11=childhand.obtainMessage();
				child11.what=COMThread.EV_MDB_HEART;
        		JSONObject ev11=null;
	    		ev11=new JSONObject();	    			
				ToolClass.Log(ToolClass.INFO,"EV_COM","ServiceSend0.1="+ev11.toString(),"com.txt");
	    		child11.obj=ev11;
        		childhand.sendMessage(child11);	
				break;	
			}			
		}

	}
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		ToolClass.Log(ToolClass.INFO,"EV_COM","COMService create","com.txt");
		super.onCreate();
		//9.注册接收器
		localBroadreceiver = LocalBroadcastManager.getInstance(this);
		receiver=new ActivityReceiver();
		IntentFilter filter=new IntentFilter();
		filter.addAction("android.intent.action.comsend");
		localBroadreceiver.registerReceiver(receiver,filter);	
	}
	
	@Override
	@Deprecated
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		super.onStart(intent, startId);
		ToolClass.Log(ToolClass.INFO,"EV_COM","COMService start","com.txt");
		//***********************
		//线程进行vmserver操作
		//***********************
		mainhand=new Handler()
		{
			@Override
			public void handleMessage(Message msg) {
				// TODO Auto-generated method stub				
				switch (msg.what)
				{
					//全部查询
					case COMThread.EV_BENTO_CHECKALLMAIN://子线程接收主线程消息签到完成
						ToolClass.Log(ToolClass.INFO,"EV_COM","COMService 货道全部查询返回="+msg.obj,"com.txt");
						String tempno6=null; 
						Map<String, Object> Set6= (Map<String, Object>) msg.obj;
						ToolClass.Log(ToolClass.INFO,"EV_COM","COMService 货道查询返回=2","com.txt");
						//输出内容
				        Set<Entry<String, Object>> allmap6=Set6.entrySet();  //实例化
				        Iterator<Entry<String, Object>> iter6=allmap6.iterator();
				        while(iter6.hasNext())
				        {
				            Entry<String, Object> me=iter6.next();
				            if(
				               (me.getKey().equals("cabinet")!=true)&&(me.getKey().equals("cool")!=true)
				               &&(me.getKey().equals("hot")!=true)&&(me.getKey().equals("light")!=true)
				               &&(me.getKey().equals("EV_TYPE")!=true)
				            )   
				            {
				            	if(Integer.parseInt(me.getKey())<10)
				    				tempno6="0"+me.getKey();
				    			else 
				    				tempno6=me.getKey();
				            	
				            	huoSet.put(cabinetID[huom]+tempno6,(Integer)me.getValue());
				            }
				        } 
				        ToolClass.Log(ToolClass.INFO,"EV_COM","COMService<<"+huoSet.size()+"货道状态:"+huoSet.toString(),"com.txt");	
				        huom++;
				        //2.继续获取所有货道号
				        if(huom<cabinetID.length)
				        {					        	
				        	childhand=comserial.obtainHandler();
			        		Message childrec=childhand.obtainMessage();
			        		//查找货道类型
			        		vmc_cabinetDAO cabinetDAOrec = new vmc_cabinetDAO(COMService.this);// 创建InaccountDAO对象
			        		// 获取所有收入信息，并存储到List泛型集合中
			        	    Tb_vmc_cabinet listinfosrec = cabinetDAOrec.findScrollData(cabinetID[huom]);
			        		//格子柜
			        	    if(listinfosrec.getCabType()==5)
			        		{
			        			ToolClass.Log(ToolClass.INFO,"EV_COM","COMService 格子柜查询="+cabinetID[huom],"com.txt");
			        			childrec.what=COMThread.EV_BENTO_CHECKALLCHILD;
			        		}
			        		else
			        		{
			        			ToolClass.Log(ToolClass.INFO,"EV_COM","COMService 货道柜查询="+cabinetID[huom],"com.txt");
			        			childrec.what=COMThread.EV_COLUMN_CHECKALLCHILD;
			        		}
			        		JSONObject evrec=null;
				    		try {
				    			evrec=new JSONObject();
				    			evrec.put("cabinet", cabinetID[huom]);	    			  			
				    			ToolClass.Log(ToolClass.INFO,"EV_COM","ServiceSend0.1="+evrec.toString(),"com.txt");
				    		} catch (JSONException e) {
				    			// TODO Auto-generated catch block
				    			e.printStackTrace();
				    		}
				    		childrec.obj=evrec;
			        		childhand.sendMessage(childrec);
				        }
				        else
				        {
				        	ToolClass.Log(ToolClass.INFO,"EV_COM","COMService 格子柜连接完成","com.txt");
				        	//返回给activity广播
							Intent recintent=new Intent();
							recintent.putExtra("EVWhat", COMService.EV_CHECKALLMAIN);						
							//传递数据
					        SerializableMap myMap=new SerializableMap();
					        myMap.setMap(huoSet);//将map数据添加到封装的myMap<span></span>中
					        Bundle bundle=new Bundle();
					        bundle.putSerializable("result", myMap);
					        recintent.putExtras(bundle);
							recintent.setAction("android.intent.action.comrec");//action与接收器相同
							localBroadreceiver.sendBroadcast(recintent);
						}												
						break;
					//查询
					case COMThread.EV_BENTO_CHECKMAIN://子线程接收主线程消息签到完成
						ToolClass.Log(ToolClass.INFO,"EV_COM","COMService 货道查询返回="+msg.obj,"com.txt");
						//返回给activity广播
						Intent recintent=new Intent();
						recintent.putExtra("EVWhat", COMService.EV_CHECKMAIN);						
						//传递数据
				        SerializableMap myMap=new SerializableMap();
				        myMap.setMap((Map<String, Integer>) msg.obj);//将map数据添加到封装的myMap<span></span>中
				        Bundle bundle=new Bundle();
				        bundle.putSerializable("result", myMap);
				        recintent.putExtras(bundle);
						recintent.setAction("android.intent.action.comrec");//action与接收器相同
						localBroadreceiver.sendBroadcast(recintent);						
						break;	
					//操作完成	
					case COMThread.EV_BENTO_OPTMAIN:
						ToolClass.Log(ToolClass.INFO,"EV_COM","COMService 货道操作="+msg.obj,"com.txt");	
						//返回给activity广播
						Intent recintent2=new Intent();
						recintent2.putExtra("EVWhat", COMService.EV_OPTMAIN);						
						//传递数据
				        SerializableMap myMap2=new SerializableMap();
				        myMap2.setMap((Map<String, Integer>) msg.obj);//将map数据添加到封装的myMap<span></span>中
				        Bundle bundle2=new Bundle();
				        bundle2.putSerializable("result", myMap2);
				        recintent2.putExtras(bundle2);
						recintent2.setAction("android.intent.action.comrec");//action与接收器相同
						localBroadreceiver.sendBroadcast(recintent2);
						break;
				}				
			}
			
		};
		//启动用户自己定义的类，启动线程
		comserial=new COMThread(mainhand);
  		thread=Executors.newFixedThreadPool(3);
  		thread.execute(comserial);	
	}
	
	

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub		
		ToolClass.Log(ToolClass.INFO,"EV_COM","COMService destroy","com.txt");
		EVprotocol.EVPortRelease(ToolClass.getBentcom_id());
		//解除注册接收器
		localBroadreceiver.unregisterReceiver(receiver);
		super.onDestroy();
	}
	

}
