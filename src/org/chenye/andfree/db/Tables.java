package org.chenye.andfree.db;

import org.chenye.andfree.func.StrFunc;
import org.chenye.andfree.func.log;
import org.chenye.andfree.obj.BaseLog;
import org.chenye.andfree.obj.Line;

import android.content.ContentValues;

public class Tables extends BaseLog{
	protected DB db;
	protected dbParse dbp;
	
	private Line _queryData = Line.def();
	public Tables(){
		db = DB.getInstance();
	}
	
	public Tables(Class<?> name){
		this();
		setDBParse(name);
	}
	
	public void setDBParse(Class<?> name){
		dbp = new dbParse(name);
	}
	
	@Override
	public String toString(){
		return "[Table]" + dbp.getName();
	}
	
	public Tables select(dbField... fields){
		String sql = "";
		for (dbField field: fields){
			sql += ", " + field.toString();
		}
		if (sql.startsWith(", ")) sql = sql.substring(2);
		_queryData.put("select", sql);
		return this;
	}
	
	public Tables select(String fields){
		_queryData.put("select", fields);
		return this;
	}
	
	public Tables where(String... wheres){
		String where = StrFunc.arraytoString(wheres, " AND ");
		_queryData.put("where", where);
		return this;
	}
	
	public Tables limit(int limit){
		
		return limit("0, " + limit);
	}
	
	public int getLimit(){
		String str = _queryData.str("limit");
		str = str.substring(str.lastIndexOf(" ") + 1);
		return Integer.parseInt(str);
	}
	
	public Tables limit(String limit){
		_queryData.put("limit", limit);
		return this;
	}
	
	public Tables order(dbField field){
		return order(field, "DESC");
	}
	
	public Tables order(dbField field, String forward){
		_queryData.put("order", field + " " + forward + " ");
		return this;
	}
	
	public Tables join(String sql){
		_queryData.put("join", sql);
		return this;
	}
	
	public Line result(){
		String sql = "";
		_queryData.putIfNotExist("select", "*");
		
		sql += String.format("SELECT %s FROM %s %s", _queryData.str("select"), dbp.getName(), dbp.getName().substring(0, 1));
		
		if (_queryData.contains("join")){
			sql += " LEFT JOIN " + _queryData.str("join");
		}
				
		_queryData.putIfNotExist("where", "1=1");
		_queryData.putIfNotExist("order", String.format("%s DESC", dbp.getPrimaryKey()));
		sql += String.format(" WHERE %s ORDER BY %s", _queryData.str("where"), _queryData.str("order"));
		
		if (_queryData.contains("limit")) {
			sql += " LIMIT " + _queryData.str("limit");
		}
		return query(sql);
	}
	
	public Line get(){
		Line d = result().line(0);
		if (_queryData.str("select").contains(",")) return d;
		Line d2 = d.line(_queryData.str("select"));
		if (d2.invalid()) return d;
		return d2;
	}
	
	public Line getPrimary(int id){
		where(String.format("`%s` = %s", dbp.getPrimaryKey(), id));
		return get();
	}
	
	public String getField(){
		return get().str(_queryData.str("select"));
	}
	
	public int count(){
		String select = "";
		if (_queryData.contains("select")){
			select = _queryData.str("select");
		}
		select("COUNT(*) as count");
		Line ret = get();
		if (select.length() == 0){
			_queryData.remove("select");
		} else {
			_queryData.put("select", select);
		}
		return ret.integer("count");
	}
	
	public void build(){
		String[] columnsName = dbp.getColumnsName();
		String[] columnsType = dbp.getColumnsType();
		String sql = "CREATE TABLE " + dbp.getName() + "(";
		for (int i=0; i<columnsName.length; i++){
			sql += "`" + columnsName[i] + "` " + columnsType[i];
			if (i<columnsName.length-1) sql += ",";
		}
		sql += ")";
		db.query(sql);
	}
	
	public Line backup(){
		Line l = result();
		return new Line().put(dbp.getName(), l);
	}
	
	public Tables drop(){
		db.query("DROP TABLE IF EXISTS " + dbp.getName());
		return this;
	}
	
	public Tables AddField(String field){
		if ( ! dbp.containColumn(field)) return this;
		AddField(field, dbp.getType(field));
		return this;
	}
	
	public Tables AddField(String field, String type){
		try{
			String sql = "ALTER TABLE " + dbp.getName() + " ADD " + field + " " + type.toUpperCase();
			db.fetch(sql);
		}catch(Exception ex){
			e(ex);
		}
		return this;
	}
	
	public long insert(Line data){
		try{
			
			return db.insert(dbp.getName(), dbp.getPrimaryKey(), dbp.filter(data));
		}catch(Exception ex){
			e(ex);
			return 0;
		}
	}
	
	
	public long update(Line line, String field, String value){
		if (field.contains("`")){
			return update(line, field, value, "1=1");
		}
		return update(line, "`" + field + "`= '" + value + "'");
	}
	
	public long update(Line line, int id){
		return update(line, dbp.getPrimaryKey(), id + "");
	}

	public long update(Line data, String... wheres){
		String where = "";
		try{
			for (String w: wheres){
				where += " AND " + w;
			}
			if (where.startsWith(" AND ")){
				where = where.substring(5);
			}
			ContentValues cv = dbp.filter(data);
			log.d(this, "[update " + dbp.getName() + "]" + cv + "[where] " + where);
			return db.update(dbp.getName(), cv, where);
		}catch(Exception ex){
			e(ex);
			return -1;
		}
	}
	
	public Line query(String sql){
		Line line = db.fetch(sql);
		line.setTable(dbp);
		return line;
	}
	
	public void delete(int id){
		delete(dbp.getPrimaryKey() + " = '" + id + "'");
	}
	
	/**
	 * delete the cur data automatic(find the id field)
	 * @param cur
	 */
	public void delete(Line line){
		if (line._field().length() <= 0) {
			log.e(this, "delete cur failure! Cause by not appoint the table name or id field");
			return;
		}
		String where = line._field() + " = " + line.str(line._field());
		delete(where);
	}
	
	/**
	 * delete with where string 
	 * @param where
	 */
	public void delete(String... wheres){
		String where = StrFunc.arraytoString(wheres, " and ");
		String sql = "DELETE FROM " + dbp.getName() + " WHERE " + where;
		db.query(sql);
		i("[delete " + dbp.getName() + "] " + where);
	}

	//---------------------------------------------------------------
	
	public final static void BuildAll(){
		Tables[] tabs = Tables.All(); 
		for (int i=0; i<tabs.length; i++){
			tabs[i].build();
		}
	}
	
	public final static void DropAll(){
		Tables[] tabs = Tables.All(); 
		for (int i=0; i<tabs.length; i++){
			tabs[i].drop();
		}
	}
	
	
	
	//-------------------------------------------------------------------------------------------------------------------------------static
	
	public final static Tables[] All(){
		Class<?>[] dbnames = dbParse.all();
		Tables[] tables = new Tables[dbnames.length];
		for(int i=0; i<dbnames.length; i++){
			tables[i] = new Tables(dbnames[i]);
		}
		return tables;
	}
	
	public void e(Exception ex){
		log.e(this, ex);
	}
	
	public void i(String str){
		log.i(this, str);
	}
}
