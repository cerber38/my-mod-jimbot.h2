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

import java.sql.Types;
import java.util.Random;

/**
 *
 * @author Prolubnikov Dmitry
 */
public final class Invites {
    public int id = 0;
    public int user_id=0;
    public long time=0;
    public String invite="";
    public int new_user=0;
    public long create_time=0;
    
    /** Creates a new instance of Invites */
    public Invites() {
    }
    
        public Invites(int _id,
            int _user_id,
            long _time,
            String _invite,
            int _new_user,
            long _createTime){
        id = _id;
        user_id = _user_id;
        time = _time;
        invite = _invite;
        new_user = _new_user;
        create_time = _createTime;
    }
    
    public Invites(int _id,
            int _user_id){
        id = _id;
        user_id = _user_id;
        time = System.currentTimeMillis();
        invite = getUID();
        new_user=0;
        create_time=0;
    }
        
    public boolean checkPrompt(String p){
        return p.equalsIgnoreCase(invite);
    }
    
    public void setUser(int user){
        new_user = user;
        create_time = System.currentTimeMillis();
    }
    
    public String getUID(){
        String s = "123456789ABCDEFGHIJKLMNPQRSTUVWXYZ";
        Random r = new Random();
        String v="";
        for(int i=0;i<10;i++){
            v += s.charAt(r.nextInt(s.length()));
        }
        return v;
    }
}
