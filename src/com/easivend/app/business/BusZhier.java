package com.easivend.app.business;

import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import com.easivend.app.maintain.GoodsManager;
import com.easivend.app.maintain.ParamManager;
import com.easivend.common.ProPictureAdapter;
import com.easivend.common.ToolClass;
import com.easivend.dao.vmc_system_parameterDAO;
import com.easivend.http.Zhifubaohttp;
import com.easivend.model.Tb_vmc_system_parameter;
import com.example.evconsole.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class BusZhier extends Activity
{
	public static BusZhier BusZhierAct=null;
	private final static int REQUEST_CODE=1;//���������ʶ
	TextView txtbuszhiercount=null,txtbuszhiamerount=null,txtbuszhierrst=null,txtbuszhiertime=null;
	ImageButton imgbtnbuszhierqxzf=null,imgbtnbuszhierqtzf=null;
	ImageView ivbuszhier=null;
	private int recLen = 180; 
	private int queryLen = 0; 
    private TextView txtView; 
    Timer timer = new Timer(); 
	private String proID = null;
	private String productID = null;
	private String proType = null;
	private String cabID = null;
	private String huoID = null;
    private String prosales = null;
    private String count = null;
    private String reamin_amount = null;
    private String zhifutype = "0";//0����ʹ�÷��ֽ�,1����ʹ���ֽ�
    private float amount=0;
    //�߳̽���֧������ά�����
    private Thread thread=null;
    private Handler mainhand=null,childhand=null;
    private String id="";
    private String out_trade_no=null;
    Zhifubaohttp zhifubaohttp=null;
    private int iszhier=0;//1�ɹ������˶�ά��,0û�гɹ����ɶ�ά��
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.buszhier);
		BusZhierAct = this;
		//����Ʒҳ����ȡ����ѡ�е���Ʒ
		Intent intent=getIntent();
		Bundle bundle=intent.getExtras();
		proID=bundle.getString("proID");
		productID=bundle.getString("productID");
		proType=bundle.getString("proType");
		cabID=bundle.getString("cabID");
		huoID=bundle.getString("huoID");
		prosales=bundle.getString("prosales");
		count=bundle.getString("count");
		reamin_amount=bundle.getString("reamin_amount");
		amount=Float.parseFloat(prosales)*Integer.parseInt(count);
		txtbuszhiercount= (TextView) findViewById(R.id.txtbuszhiercount);
		txtbuszhiercount.setText(count);
		txtbuszhiamerount= (TextView) findViewById(R.id.txtbuszhiamerount);
		txtbuszhiamerount.setText(String.valueOf(amount));
		txtbuszhierrst= (TextView) findViewById(R.id.txtbuszhierrst);
		txtbuszhiertime= (TextView) findViewById(R.id.txtbuszhiertime);
		ivbuszhier= (ImageView) findViewById(R.id.ivbuszhier);
		timer.schedule(task, 1000, 1000);       // timeTask 
		imgbtnbuszhierqxzf = (ImageButton) findViewById(R.id.imgbtnbuszhierqxzf);
		imgbtnbuszhierqxzf.setOnClickListener(new OnClickListener() {
		    @Override
		    public void onClick(View arg0) {
		    	if(BusZhiSelect.BusZhiSelectAct!=null)
		    		BusZhiSelect.BusZhiSelectAct.finish(); 
		    	if(BusgoodsSelect.BusgoodsSelectAct!=null)
					BusgoodsSelect.BusgoodsSelectAct.finish(); 
		    	finishActivity();
		    }
		});
		imgbtnbuszhierqtzf = (ImageButton) findViewById(R.id.imgbtnbuszhierqtzf);
		imgbtnbuszhierqtzf.setOnClickListener(new OnClickListener() {
		    @Override
		    public void onClick(View arg0) {
		    	finishActivity();
		    }
		});
		//***********************
		//�߳̽���֧������ά�����
		//***********************
		mainhand=new Handler()
		{

			@Override
			public void handleMessage(Message msg) {
				//barzhifubaotest.setVisibility(View.GONE);
				// TODO Auto-generated method stub				
				switch (msg.what)
				{
					case Zhifubaohttp.SETMAIN://���߳̽������߳���Ϣ
						ivbuszhier.setImageBitmap(ToolClass.createQRImage(msg.obj.toString()));
						txtbuszhierrst.setText("���׽��:"+msg.obj.toString());
						iszhier=1;
						break;
					case Zhifubaohttp.SETPAYOUTMAIN://���߳̽������߳���Ϣ
						txtbuszhierrst.setText("���׽��:�˿�ɹ�");
						finish();
						break;
					case Zhifubaohttp.SETDELETEMAIN://���߳̽������߳���Ϣ
						txtbuszhierrst.setText("���׽��:�����ɹ�");
						timer.cancel(); 
						finish();
						break;	
					case Zhifubaohttp.SETQUERYMAINSUCC://���׳ɹ�
						txtbuszhierrst.setText("���׽��:���׳ɹ�");
						reamin_amount=String.valueOf(amount);
						timer.cancel(); 
						tochuhuo();
						break;
					case Zhifubaohttp.SETQUERYMAIN://���߳̽������߳���Ϣ
					case Zhifubaohttp.SETFAILPROCHILD://���߳̽������߳���Ϣ
					case Zhifubaohttp.SETFAILBUSCHILD://���߳̽������߳���Ϣ	
					case Zhifubaohttp.SETFAILQUERYPROCHILD://���߳̽������߳���Ϣ
					case Zhifubaohttp.SETFAILQUERYBUSCHILD://���߳̽������߳���Ϣ	
					case Zhifubaohttp.SETFAILPAYOUTPROCHILD://���߳̽������߳���Ϣ		
					case Zhifubaohttp.SETFAILPAYOUTBUSCHILD://���߳̽������߳���Ϣ
					case Zhifubaohttp.SETFAILDELETEPROCHILD://���߳̽������߳���Ϣ		
					case Zhifubaohttp.SETFAILDELETEBUSCHILD://���߳̽������߳���Ϣ	
						txtbuszhierrst.setText("���׽��:"+msg.obj.toString());
						break;	
				}				
			}
			
		};	
		//�����û��Լ��������
		zhifubaohttp=new Zhifubaohttp(mainhand);
		thread=new Thread(zhifubaohttp,"Zhifubaohttp Thread");
		thread.start();
		//���Ͷ���
		sendzhier();
	}
	
	//���Ͷ���
	private void sendzhier()
	{		
		vmc_system_parameterDAO parameterDAO = new vmc_system_parameterDAO(BusZhier.this);// ����InaccountDAO����
	    // �õ��豸ID��
    	Tb_vmc_system_parameter tb_inaccount = parameterDAO.find();
    	if(tb_inaccount!=null)
    	{
    		id=tb_inaccount.getDevhCode().toString();
    	}
    	Log.i("EV_JNI","Send0.0="+id);
    	// ����Ϣ���͵����߳���
    	childhand=zhifubaohttp.obtainHandler();
		Message childmsg=childhand.obtainMessage();
		childmsg.what=Zhifubaohttp.SETCHILD;
		JSONObject ev=null;
		try {
			ev=new JSONObject();
			SimpleDateFormat tempDate = new SimpleDateFormat("yyyyMMddhhmmssSSS"); //��ȷ������ 
	        String datetime = tempDate.format(new java.util.Date()).toString(); 					
	        out_trade_no=id+datetime;
	        ev.put("out_trade_no", out_trade_no);
			ev.put("total_fee", String.valueOf(amount));
			Log.i("EV_JNI","Send0.1="+ev.toString());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		childmsg.obj=ev;
		childhand.sendMessage(childmsg);
	}
	//��ѯ����
	private void queryzhier()
	{
		// ����Ϣ���͵����߳���
    	childhand=zhifubaohttp.obtainHandler();
		Message childmsg=childhand.obtainMessage();
		childmsg.what=Zhifubaohttp.SETQUERYCHILD;
		JSONObject ev=null;
		try {
			ev=new JSONObject();
			ev.put("out_trade_no", out_trade_no);		
			//ev.put("out_trade_no", "000120150301113215800");	
			Log.i("EV_JNI","Send0.1="+ev.toString());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		childmsg.obj=ev;
		childhand.sendMessage(childmsg);
	}
	//��������
	private void deletezhier()
	{
		// ����Ϣ���͵����߳���
    	childhand=zhifubaohttp.obtainHandler();
		Message childmsg=childhand.obtainMessage();
		childmsg.what=Zhifubaohttp.SETDELETECHILD;
		JSONObject ev=null;
		try {
			ev=new JSONObject();
			ev.put("out_trade_no", out_trade_no);		
			//ev.put("out_trade_no", "000120150301092857698");	
			Log.i("EV_JNI","Send0.1="+ev.toString());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		childmsg.obj=ev;
		childhand.sendMessage(childmsg);
	}
	//�˿�
	private void payoutzhier()
	{
		// ����Ϣ���͵����߳���
    	childhand=zhifubaohttp.obtainHandler();
		Message childmsg=childhand.obtainMessage();
		childmsg.what=Zhifubaohttp.SETPAYOUTCHILD;
		JSONObject ev=null;
		try {
			ev=new JSONObject();
			ev.put("out_trade_no", out_trade_no);		
			ev.put("refund_amount", String.valueOf(amount));
			SimpleDateFormat tempDate = new SimpleDateFormat("yyyyMMddhhmmssSSS"); //��ȷ������ 
	        String datetime = tempDate.format(new java.util.Date()).toString(); 					
	        ev.put("out_request_no", id+datetime);
			Log.i("EV_JNI","Send0.1="+ev.toString());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		childmsg.obj=ev;
		childhand.sendMessage(childmsg);
	}
	//���õ���ʱ��ʱ��
	TimerTask task = new TimerTask() { 
        @Override 
        public void run() { 
  
            runOnUiThread(new Runnable() {      // UI thread 
                @Override 
                public void run() { 
                    recLen--; 
                    txtbuszhiertime.setText("����ʱ:"+recLen); 
                    if(recLen <= 0)
                    { 
                        timer.cancel(); 
                        finishActivity();
                    } 
                    //���Ͳ�ѯ����ָ��
                    if(iszhier==1)
                    {
	                    queryLen++;
	                    if(queryLen>=10)
	                    {
	                    	queryLen=0;
	                    	queryzhier();
	                    }
                    }
                } 
            }); 
        } 
    };
	//��������
	private void finishActivity()
	{
		if(iszhier==1)
			deletezhier();
		else 
		{
			timer.cancel(); 
			finish();
		}
	}
	
	//��������ҳ��
	private void tochuhuo()
	{
		Intent intent = null;// ����Intent����                
    	intent = new Intent(BusZhier.this, BusHuo.class);// ʹ��Accountflag���ڳ�ʼ��Intent
    	intent.putExtra("proID", proID);
    	intent.putExtra("productID", productID);
    	intent.putExtra("proType", proType);
    	intent.putExtra("cabID", cabID);
    	intent.putExtra("huoID", huoID);
    	intent.putExtra("prosales", prosales);
    	intent.putExtra("count", count);
    	intent.putExtra("reamin_amount", reamin_amount);
    	intent.putExtra("zhifutype", zhifutype);
    	startActivityForResult(intent, REQUEST_CODE);// ��Accountflag
	}

	//����BusHuo������Ϣ
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		if(requestCode==REQUEST_CODE)
		{
			if(resultCode==BusZhier.RESULT_CANCELED)
			{
				ToolClass.Log(ToolClass.INFO,"EV_JNI","APP<<�˿�amount="+amount);
				payoutzhier();//�˿�
			}			
		}
	}
	
	
	
}