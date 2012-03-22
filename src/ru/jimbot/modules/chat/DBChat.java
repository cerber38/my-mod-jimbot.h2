/**
 * JimBot - Java IM Bot
 * Copyright (C) 2006-2009 JimBot project
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package ru.jimbot.modules.chat;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Vector;

import ru.jimbot.db.DBAdaptor;
import ru.jimbot.util.Log;

/**
 *
 * @author Prolubnikov Dmitry
 */
public class DBChat extends DBAdaptor{
    private String serviceName = "";
    
    /** Creates a new instance of DBChat */
    public DBChat(String name) throws Exception {
    	serviceName = name;
//        this.DRIVER = "org.hsqldb.jdbcDriver";
//        this.URL = "jdbc:hsqldb:file:";
//        this.dbName = "db/users";
//        this.openConnection();
    }
    
    public void createDB(){
//        Log.info("DB chat not found. Create new DB...");
//        try{
//            executeQuery("CREATE MEMORY TABLE USERS(ID INTEGER NOT NULL PRIMARY KEY," +
//                    "sn varchar, nick varchar, localnick varchar, fname varchar, lname varchar," +
//                    "email varchar, city varchar, homepage varchar, gender integer, " +
//                    "birthyear integer, birthmonth integer, birthday integer, " +
//                    "age integer, country integer, language integer, state integer, " +
//                    "basesn varchar, createtime timestamp, room integer)");
//            executeQuery("create memory table invites(ID INTEGER NOT NULL PRIMARY KEY," +
//                    "user_id integer, time timestamp, invite varchar, new_user integer, create_time timestamp)");
//            executeQuery("CREATE CACHED TABLE LOG(ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 0) " +
//                    "NOT NULL PRIMARY KEY,TIME TIMESTAMP,USER_ID INTEGER, user_sn varchar(50), TYPE VARCHAR(10)," +
//                    "MSG LONGVARCHAR, room integer)");
//            executeQuery("CREATE CACHED TABLE EVENTS(ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 0) " +
//                    "NOT NULL PRIMARY KEY,TIME TIMESTAMP,USER_ID INTEGER, user_sn varchar(50), TYPE VARCHAR(10)," +
//                    "USER_ID2 INTEGER, user_sn2 varchar(50), " +
//                    "MSG LONGVARCHAR)");
//            executeQuery("create memory table user_props (user_id integer not null, name varchar, val varchar)");
//            commit();
//        }catch(Exception ex){
//            ex.printStackTrace();
//        }
    }
    
    /**
     * Запись лога в БД
     */
    public void log(int user, String sn, String type, String msg, int room){
        if(!ChatProps.getInstance(serviceName).getBooleanProperty("chat.writeAllMsgs")) return;
        try{
            PreparedStatement pst = getDb().prepareStatement("insert into log values(null, ?, ?, ?, ?, ?, ?)");
            pst.setTimestamp(1,new Timestamp(System.currentTimeMillis()));
            pst.setInt(2,user);
            pst.setString(3,sn);
            pst.setString(4,type);
            pst.setString(5,msg);
            pst.setInt(6, room);
            pst.execute();
            pst.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * Запись события в БД
     */
    public void event(int user, String sn, String type, int user2, String sn2, String msg) {
        try {
            PreparedStatement pst = getDb().prepareStatement("insert into events values(null, ?, ?, ?, ?, ?, ?, ?)");
            pst.setTimestamp(1,new Timestamp(System.currentTimeMillis()));
            pst.setInt(2,user);
            pst.setString(3,sn);
            pst.setString(4,type);
            pst.setInt(5,user2);
            pst.setString(6,sn2);            
            pst.setString(7,msg);
            pst.execute();
            pst.close();            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * Поиск параметров пользователя
     */
    public boolean existUserProps(int user_id){
        boolean f = false;
        try{
        	Vector<String[]> v = this.getValues("select count(*) from user_props where user_id="+user_id);
        	if(Integer.parseInt(v.get(0)[0])>0) f = true;
//            if(openQuery("select count(*) from user_props where user_id="+user_id)){
//                String[] s=readNext();
//                if(Integer.parseInt(s[0])>0) f=true;
//                closeQuery();
//            }
        } catch (Exception ex){
            ex.printStackTrace();
        }
        return f;
    }
    
    /**
     * Возвращает параметры пользователя
     */
    public Vector<String[]> getUserProps(int user_id){
        Vector<String[]> v=new Vector<String[]>(3);
        try{
            v=getValues("select name, val from user_props where user_id="+user_id);
        } catch (Exception ex){
            ex.printStackTrace();
        }
        return v;
    }
    
    /**
     * Устанавливает параметр пользователя
     */
    public boolean setUserProps(int user_id, String name, String val){
        boolean f = false;
        try{
            executeQuery("delete from user_props where user_id="+user_id+" and name='"+name + "'");
            PreparedStatement pst = getDb().prepareStatement("insert into user_props values(?, ?, ?)");
            pst.setString(2, name);
            pst.setString(3, val);
            pst.setInt(1, user_id);
            pst.execute();
            pst.close();
            f = true;
        } catch (Exception ex){
            ex.printStackTrace();
        }
        return f;
    }

    public Invites getInvites(String q){
        Invites in = new Invites();
        ResultSet rst=null;
        Statement stmt=null;
        try{
//        	stmt = getDb().createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            stmt = getDb().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        	Log.debug("EXEC: " + q);
        	rst = stmt.executeQuery(q);
            rst.next();
            in.id = rst.getInt(1);
            in.user_id = rst.getInt(2);
            in.time = rst.getTimestamp(3).getTime();
            in.invite = rst.getString(4);
            in.new_user = rst.getInt(5);
            in.create_time = rst.getTimestamp(6).getTime();
        } catch (Exception ex){
            ex.printStackTrace();
        } finally {
        	if(rst!=null) try{rst.close();} catch(Exception e) {};
        	if(stmt!=null) try{stmt.close();} catch(Exception e) {};
        }
        return in;
    }
    
    public void insertInvite(Invites in){
        Log.debug("INSERT invite id=" + in.id);
        try{
            PreparedStatement pst = getDb().prepareStatement("insert into invites values (?,?,?,?,?,?,?)");
            pst.setInt(1,in.id);
            pst.setInt(2,in.user_id);
            pst.setTimestamp(3,new Timestamp(in.time));
            pst.setString(4,in.invite);
            pst.setInt(5,in.new_user);
            pst.setTimestamp(6,null/*new Timestamp(in.create_time)*/);
            pst.setString(7, "");
            pst.execute();
            pst.close();
//            commit();
        } catch (Exception ex){
            ex.printStackTrace();
        }        
    }
    
    public void updateInvite(Invites in){
        Log.debug("UPDATE invite id=" + in.id);
        try{
            PreparedStatement pst = getDb().prepareStatement("update invites set user_id=?, time=?, invite=?, new_user=?, create_time=? where id=?");
            pst.setInt(6,in.id);
            pst.setInt(1,in.user_id);
            pst.setTimestamp(2,new Timestamp(in.time));
            pst.setString(3,in.invite);
            pst.setInt(4,in.new_user);
            pst.setTimestamp(5,new Timestamp(in.create_time));
            pst.execute();
            pst.close();
//            commit();
        } catch (Exception ex){
            ex.printStackTrace();
        }        
    }
    
    public void clearOldInvites(int user_id){
        try {
            PreparedStatement pst = getDb().prepareStatement("delete from invites where user_id=" + user_id + " and new_user=0 and time<?");
            pst.setTimestamp(1, new Timestamp(System.currentTimeMillis()-ChatProps.getInstance(serviceName).getIntProperty("chat.MaxInviteTime")*3600000));
            pst.execute();
            pst.close();
//            commit();
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public void clearOldInvites(String uid){
        try {
            PreparedStatement pst = getDb().prepareStatement("delete from invites where invite='" + uid + "' and new_user=0 and time<?");
            pst.setTimestamp(1, new Timestamp(System.currentTimeMillis()-ChatProps.getInstance(serviceName).getIntProperty("chat.MaxInviteTime")*3600000));
            pst.execute();
            pst.close();
//            commit();
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }
    
    public Users getObject(String q){
        Users us = new Users();
        ResultSet rSet=null;
        Statement stmt=null;
        try{
//        	stmt = getDb().createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            stmt = getDb().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        	Log.debug("EXEC: " + q);
        	rSet = stmt.executeQuery(q);
//            openQuery(q);
//            Statement stm = getDb().createStatement();
//            rSet = stm.executeQuery(q);
            rSet.next();
            us.id = rSet.getInt(1);
            us.sn = rSet.getString(2);
            us.nick = rSet.getString(3);
            us.localnick = rSet.getString(4);
            us.fname = rSet.getString(5);
            us.lname = rSet.getString(6);
            us.email = rSet.getString(7);
            us.city = rSet.getString(8);
            us.homepage = rSet.getString(9);
            us.gender = rSet.getInt(10);
            us.birthyear = rSet.getInt(11);
            us.birthmonth = rSet.getInt(12);
            us.birthday = rSet.getInt(13);
            us.age = rSet.getInt(14);
            us.country = rSet.getInt(15);
            us.language = rSet.getInt(16);
            us.state =  rSet.getInt(17);
            us.basesn = rSet.getString(18);
            us.createtime = rSet.getTimestamp(19).getTime();
            us.room = rSet.getInt(20);
            if(rSet.getLong(21)==0)
            	us.lastKick = System.currentTimeMillis();
            else
            	us.lastKick = rSet.getTimestamp(21).getTime();
//            closeQuery();
        } catch (Exception ex){
            ex.printStackTrace();
        } finally {
        	if(rSet!=null) try{rSet.close();} catch(Exception e) {};
        	if(stmt!=null) try{stmt.close();} catch(Exception e) {};
        }
        return us;
    }
    
    public Vector<Users> getObjectVector(String q){
        Vector<Users> v = new Vector<Users>(5);
        ResultSet rSet=null;
        Statement stmt=null;
        try{
//        	stmt = getDb().createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            stmt = getDb().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        	Log.debug("EXEC: " + q);
        	rSet = stmt.executeQuery(q);
//            openQuery(q);
//            Statement stm = getDb().createStatement();
//            rSet = stm.executeQuery(q);
            while(rSet.next()) {
                Users us = new Users();
                us.id = rSet.getInt(1);
                us.sn = rSet.getString(2);
                us.nick = rSet.getString(3);
                us.localnick = rSet.getString(4);
                us.fname = rSet.getString(5);
                us.lname = rSet.getString(6);
                us.email = rSet.getString(7);
                us.city = rSet.getString(8);
                us.homepage = rSet.getString(9);
                us.gender = rSet.getInt(10);
                us.birthyear = rSet.getInt(11);
                us.birthmonth = rSet.getInt(12);
                us.birthday = rSet.getInt(13);
                us.age = rSet.getInt(14);
                us.country = rSet.getInt(15);
                us.language = rSet.getInt(16);
                us.state =  rSet.getInt(17);
                us.basesn = rSet.getString(18);
                us.createtime = rSet.getTimestamp(19).getTime();
                us.room = rSet.getInt(20);
//                if(rSet.getLong(21)==0)
////                	us.lastKick = System.currentTimeMillis();
////                else
                	us.lastKick = rSet.getTimestamp(21).getTime();
                v.addElement(us);
            }
//            closeQuery();
        } catch (Exception ex){
            ex.printStackTrace();
        } finally {
        	if(rSet!=null) try{rSet.close();} catch(Exception e) {};
        	if(stmt!=null) try{stmt.close();} catch(Exception e) {};
        }
        return v;
        
    }
    
    public void insertObject(Users us){
        Log.debug("INSERT user id=" + us.id);
        try{
            PreparedStatement pst = getDb().prepareStatement("insert into users values (?, ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            pst.setInt(1,us.id);
            pst.setString(2,us.sn);
            pst.setString(3,us.nick);
            pst.setString(4,us.localnick);
            pst.setString(5,us.fname);
            pst.setString(6,us.lname);
            pst.setString(7,us.email);
            pst.setString(8,us.city);
            pst.setString(9,us.homepage);
            pst.setInt(10,us.gender);
            pst.setInt(11,us.birthyear);
            pst.setInt(12,us.birthmonth);
            pst.setInt(13,us.birthday);
            pst.setInt(14,us.age);
            pst.setInt(15,us.country);
            pst.setInt(16,us.language);
            pst.setInt(17,us.state);
            pst.setString(18,us.basesn);
            pst.setTimestamp(19,new Timestamp(us.createtime));
            pst.setInt(20,us.room);
            pst.setTimestamp(21,new Timestamp(us.lastKick));
            pst.execute();
            pst.close();
//            commit();
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public void updateObject(Users us){
        Log.debug("UPDATE user id=" + us.id);
        try{
            PreparedStatement pst = getDb().prepareStatement("update users set sn=?,nick=?," + 
                    "localnick=?,fname=?,lname=?,email=?,city=?,homepage=?,gender=?," +
                    "birthyear=?,birthmonth=?,birthday=?,age=?,country=?,language=?," +
                    "state=?,basesn=?,createtime=?,room=?, lastkick=? where id=" + us.id);
//            pst.setInt(1,us.id);
            pst.setString(1,us.sn);
            pst.setString(2,us.nick);
            pst.setString(3,us.localnick);
            pst.setString(4,us.fname);
            pst.setString(5,us.lname);
            pst.setString(6,us.email);
            pst.setString(7,us.city);
            pst.setString(8,us.homepage);
            pst.setInt(9,us.gender);
            pst.setInt(10,us.birthyear);
            pst.setInt(11,us.birthmonth);
            pst.setInt(12,us.birthday);
            pst.setInt(13,us.age);
            pst.setInt(14,us.country);
            pst.setInt(15,us.language);
            pst.setInt(16,us.state);
            pst.setString(17,us.basesn);
            pst.setTimestamp(18,new Timestamp(us.createtime));
            pst.setInt(19,us.room);
            pst.setTimestamp(20,new Timestamp(us.lastKick));
            pst.execute();
            pst.close();
//            commit();
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }
}
