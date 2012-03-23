/**
* JimBot - Java IM Bot Copyright (C) 2006-2012 JimBot project This program is
* free software; you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation;
* either version 2 of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*
* You should have received a copy of the GNU General Public License along with
* this program; if not, write to the Free Software Foundation, Inc., 51
* Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/
package ru.jimbot.modules.chat;

import ru.jimbot.db.DBH2;
import ru.jimbot.modules.*;
import ru.jimbot.protocol.Protocol;
import ru.jimbot.util.MainProps;

/**
*
* @author Prolubnikov Dmitry
*/
public class ChatServer extends AbstractServer {

public AbstractConnection con;
public UserWork us;
public ChatQueue cq;
public MsgInQueue inq;
private String[] icq;
private ChatProps props = null;

/**
* Creates a new instance of ChatServer
*/
public ChatServer(String name) {
this.setName(name);
ChatProps.getInstance(name).load();
us = new UserWork(name);
cmd = new ChatCommandProc(this);
con = new AbstractConnection(this);
con.server = MainProps.getServer();
con.port = MainProps.getPort();
}

public void start() {
icq = new String[ChatProps.getInstance(this.getName()).uinCount()];
String[] pass = new String[ChatProps.getInstance(this.getName()).uinCount()];
for (int i = 0; i < ChatProps.getInstance(this.getName()).uinCount(); i++) {
icq[i] = ChatProps.getInstance(this.getName()).getUin(i);
pass[i] = ChatProps.getInstance(this.getName()).getPass(i);
}
con.uins = new UINmanager(icq, pass, ChatProps.getInstance(this.getName()));
cq = new ChatQueue(this);
cq.start();
inq = new MsgInQueue(cmd, us.statUsersCount());
con.uins.start();
for (Protocol p : con.uins.proc.values()) inq.addReceiver(p);
inq.start();
((ChatCommandProc) cmd).radm.start();// робот-админ
isRun = true;
}

public void stop() {
closeDB();
us = null;
inq.stop();
cq.stop();   	
con.uins.stop();
con.uins = null;
((ChatCommandProc) cmd).radm.stop();// робот-админ
System.gc(); 
isRun = false;
}

public void closeDB() {
us.closeDB();
}

public Protocol getIcqProcess(String baseUin) {
if (con.uins.proc.get(baseUin) != null) return con.uins.proc.get(baseUin);
return con.uins.proc.get(icq[0]);
}

public DBH2 getDB() {
return us.db;
}

public AbstractProps getProps() {
return props = ChatProps.getInstance(this.getName());
}

public Protocol getIcqProcess(int baseUin) {
if (con.uins.proc.get(icq[baseUin]) != null) return con.uins.proc.get(icq[baseUin]);
return con.uins.proc.get(icq[0]);
}

public int getIneqSize() {
return inq.size();
}
}