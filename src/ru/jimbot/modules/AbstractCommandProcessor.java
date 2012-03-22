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
package ru.jimbot.modules;

import ru.jimbot.modules.chat.Users;
import ru.jimbot.protocol.Protocol;

/**
*
* @author Prolubnikov Dmitry
*/
public class AbstractCommandProcessor {

public AbstractServer getServer() {
return null;
}

public void parse(Protocol proc, String uin, String msg) {}

public void parseStatus(Protocol proc, String uin, int status) {}

public void testState(int uin) {}

public void parseInfo(Users u, int type) {}

public void parseFloodNotice(String uin, String msg, Protocol proc) {}
}