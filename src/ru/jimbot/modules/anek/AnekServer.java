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
package ru.jimbot.modules.anek;

import ru.jimbot.db.DBH2;
import ru.jimbot.modules.*;
import ru.jimbot.protocol.Protocol;
import ru.jimbot.util.MainProps;

/**
*
* @author Prolubnikov Dmitry
*/
public class AnekServer extends AbstractServer {

public AbstractConnection con;
public AnekWork an;
public MsgInQueue inq;
private AnekProps props;
private String[] icq;

/**
* Creates a new instance of AnekServer
*/
public AnekServer(String name) {
this.setName(name);
AnekProps.getInstance(name).load();
an = new AnekWork(this.getName());
cmd = new AnekCommandProc(this);
con = new AbstractConnection(this);
con.server = MainProps.getServer();
con.port = MainProps.getPort();
}

public void start() {
icq = new String[AnekProps.getInstance(this.getName()).uinCount()];
String[] pass = new String[AnekProps.getInstance(this.getName()).uinCount()];
for (int i = 0; i < AnekProps.getInstance(this.getName()).uinCount(); i++) {
icq[i] = AnekProps.getInstance(this.getName()).getUin(i);
pass[i] = AnekProps.getInstance(this.getName()).getPass(i);
}
con.uins = new UINmanager(icq, pass,
AnekProps.getInstance(this.getName()));
inq = new MsgInQueue(cmd, 35);
con.uins.start();
for (Protocol p : con.uins.proc.values()) {
inq.addReceiver(p);
}
inq.start();
isRun = true;
}

public void stop() {
closeDB();
an = null;
inq.stop();
con.uins.stop();
con.uins = null;
System.gc(); 
isRun = false;
}

public void closeDB() {
an.closeDB();
}

public DBH2 getDB() {
return an.db;
}

public AbstractProps getProps() {
return props = AnekProps.getInstance(this.getName());
}

public int getIneqSize() {
return inq.size();
}

public Protocol getIcqProcess(int baseUin) {
if (con.uins.proc.get(icq[baseUin]) != null) return con.uins.proc.get(icq[baseUin]);
return con.uins.proc.get(icq[0]);
}
}