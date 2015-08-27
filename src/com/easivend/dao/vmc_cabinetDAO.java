package com.easivend.dao;

import java.util.ArrayList;
import java.util.List;

import com.easivend.model.Tb_vmc_cabinet;
import com.easivend.model.Tb_vmc_class;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class vmc_cabinetDAO
{
	private DBOpenHelper helper;// 创建DBOpenHelper对象
    private SQLiteDatabase db;// 创建SQLiteDatabase对象
    //SimpleDateFormat sdf = new SimpleDateFormat( " yyyy-MM-dd HH:mm:ss " );
    // 定义构造函数
	public vmc_cabinetDAO(Context context) 
	{
		helper=new DBOpenHelper(context);// 初始化DBOpenHelper对象
	}
	//添加柜数据
	public void add(Tb_vmc_cabinet tb_vmc_cabinet)throws SQLException
	{
		db = helper.getWritableDatabase();// 初始化SQLiteDatabase对象
		// 执行添加		
		db.execSQL("insert into vmc_cabinet (cabID,cabType) values (?,?)",
				new Object[] { tb_vmc_cabinet.getCabID(), tb_vmc_cabinet.getCabType() });
        
        
		db.close(); 
	}
	/**
     * 获取全部柜信息
     *     
     * @return
     */
    public List<Tb_vmc_cabinet> getScrollData() 
    {
        List<Tb_vmc_cabinet> tb_inaccount = new ArrayList<Tb_vmc_cabinet>();// 创建集合对象
        db = helper.getWritableDatabase();// 初始化SQLiteDatabase对象
        // 获取所有收入信息
        Cursor cursor = db.rawQuery("select cabID,cabType from vmc_cabinet", null);
        //遍历所有的收入信息
        while (cursor.moveToNext()) 
        {	
            // 将遍历到的收入信息添加到集合中
            tb_inaccount.add(new Tb_vmc_cabinet(cursor.getString(cursor.getColumnIndex("cabID")),cursor.getInt(cursor.getColumnIndex("cabType"))));
        }
        if (!cursor.isClosed()) 
 		{  
 			cursor.close();  
 		}  
 		db.close(); 
        return tb_inaccount;// 返回集合
    }
    
    //删除该柜
  	public void detele(String cabID) 
  	{       
          db = helper.getWritableDatabase();// 初始化SQLiteDatabase对象
          // 执行删除该柜商品表
          db.execSQL("delete from vmc_cabinet where cabID=?", 
          		new Object[] { cabID});    
          
          db.close(); 
  	}
  	
    //删除该柜
  	public void deteleall() 
  	{       
          db = helper.getWritableDatabase();// 初始化SQLiteDatabase对象
          // 执行删除表
          db.execSQL("delete from vmc_class"); 
          db.execSQL("delete from vmc_classproduct"); 
          db.execSQL("delete from vmc_product"); 
          db.execSQL("delete from vmc_cabinet"); 
          db.execSQL("delete from vmc_column");    
          
          db.close(); 
  	}
}
