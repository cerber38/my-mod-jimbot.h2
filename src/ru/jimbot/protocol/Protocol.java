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

package ru.jimbot.protocol;

import ru.jimbot.modules.AbstractProps;
import ru.jimbot.modules.MsgOutQueue;
import ru.jimbot.modules.MsgReceiver;

/**
 *
 * @author Black_Kot
 */
public abstract class Protocol {
    /**
     */
    public String server="";
    public int port = 0;
    public String screenName = "";
    public String serviceName = "";
    public String password = "";
    public String baseUin = "";

    public MsgOutQueue mq = null;

    protected MsgReceiver protList = null;

    /**
     */
    public void addListener(MsgReceiver p){
        protList = p;
    }

    public abstract AbstractProps getProps();
    public abstract void connect();
    public abstract void reConnect();
    public abstract void disconnect();
    public abstract boolean isOnLine();
    public abstract void sendMsg(String sn, String msg);
}
