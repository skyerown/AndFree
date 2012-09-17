package org.chenye.andfree.db;

import org.chenye.andfree.func.log;
import org.chenye.andfree.obj.Line;
import org.chenye.andfree.obj.cursor;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class DB {
	private Context mContext;
	public SQLiteDatabase conn;
	public dbInit dbinit;
	private DB(){
		
	}
	protected static DB instance = null;
	public static DB getInstance(Context m){
		if (instance != null) return instance;
		instance = new DB(m);
		return instance;
	}
	
	public static DB getInstance(){
		if (instance == null){
			log.e(new DB(), "null instance");
		}
		return instance;
	}
	
	public Context getContext(){
		return mContext;
	}
	
	/* ���캯��,���context*/
	public DB(Context context){
		mContext = context;
		open();
		instance = this;
	}
	public DB(SQLiteDatabase db){
		conn = db;
		instance = this;
	}
	// ����ݿ�,������ݿ����
	public void open(){
		if (dbinit != null) return;
		dbinit = new dbInit(mContext);
		try{
			conn = dbinit.getReadableDatabase();
		} catch (Exception ex){
			log.e(this, ex);
		}
		d(name() + " open ");
	}
	
	private String name(){
		String str = mContext.getClass().getSimpleName();
		return str;
	}
	
	public Line fetch(String sql){
		return fetch(sql, null);
	}
	
	public Line fetch(String sql, String[] args){
		try{
			if ( ! conn.isOpen()) return null;
			Cursor cur = conn.rawQuery(sql, args);			
			if (cur == null){
				e("fetch get null cursor");
				return null;
			}			
			cursor c = new cursor(cur);
			Line l = c.toLine();
			c.close();
			return l;
		}catch(Exception ex){
			e(ex);
			return null;
		}
	}
	
	public boolean query(String sql){
		try{
			if ( ! conn.isOpen()) return false;
			conn.execSQL(sql);
			return true;
		}catch(SQLException ex){
			e(ex);
			return false;
		}
	}
	
	private boolean delayclose = false;
	public void delayclose(){
		delayclose = true;
	}
	
	
	public boolean closed(){
		return ! conn.isOpen();
	}

	/**
	 * close the database
	 */
	public void close(){
		if (delayclose){
			delayclose = false;
			return;
		}
		d(name() + " close");
		if (conn.isOpen()) conn.close();
		try{
			instance = null;
			dbinit.close();
		}catch(Exception ex){
			e(ex);
		}
	}
	
	public long update(String table, ContentValues values, String whereClause){
		try{
			if ( ! conn.isOpen()) return -1;
			return conn.update(table, values, whereClause, null);
		}catch(SQLException ex){
			e(ex);
			return -1;
		}
	}
	
	public long insert(String tablename, String idField, ContentValues data){
		try{
			if ( ! conn.isOpen()) return 0;
			
			return conn.insert(tablename, idField, data);
		}catch(Exception ex){
			e(ex);
			return 0;
		}
	}
	
	private void e(String str){
		log.e(this, str);
	}
	
	private void e(Exception ex){
		log.e(this, ex);
	}
	
	private void d(String str){
		log.d(this, str);
	}
}