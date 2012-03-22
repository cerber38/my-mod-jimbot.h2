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

/**
 *
 * @author Prolubnikov Dmitry
 */
public class Users{
    public int id = 0;
    public String sn = "";
    public String nick="";
    public String localnick = "";
    public String fname="";
    public String lname="";
    public String email="";
    public String city="";
    public String homepage="";
    public int gender= -1;
    public int birthyear= -1;
    public int birthmonth= -1;
    public int birthday= -1;
    public int age= -1;
    public int country= -1;
    public int language= -1;
    public int state = 0;
    public String basesn="";
    public long createtime=0;
    public int room = 0;
    public long lastKick = System.currentTimeMillis();
    public String group = "";
    
    /** Creates a new instance of Users */
    public Users() {
        createtime = System.currentTimeMillis();
    }
    
    public Users (int _id, 
            String _sn,
            String _nick,
            String _localNick,
            String _fname,
            String _lname,
            String _email,
            String _city,
            String _homepage,
            int _gender,
            int _birthYear,
            int _birthMonth,
            int _birthDay,
            int _age,
            int _country) {
        id = _id;
        sn = _sn;
        nick = _nick;
        localnick = _localNick;
        fname = _fname;
        lname = _lname;
        email = _email;
        city = _city;
        homepage = _homepage;
        gender = _gender;
        birthyear = _birthYear;
        birthmonth = _birthMonth;
        birthday = _birthDay;
        age = _age;
        country = _country;
    }    
    
    public String getInfo(){
        String s = "User info id = " + id + '\n';
        s += "SN="+sn+'\n';
        s += "local nick=" + localnick + '\n';
        s += "nick="+nick+'\n';
        s += "fname="+fname+'\n';
        s += "lname="+lname+'\n';
        s += "email="+email+'\n';
        s += "city="+city+'\n';
        s += "homepage="+homepage+'\n';
        s += "gender="+gender+'\n';
        s += "birthYear="+birthyear+'\n';
        s += "birthMonth="+birthmonth+'\n';
        s += "birthDay="+birthday+'\n';
        s += "age="+age;
        if(state==-1) s += "\nuser banned";
        return s;
    }
    
}
