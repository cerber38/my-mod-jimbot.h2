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
package ru.jimbot.protocol;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.packet.Presence.Type;
import ru.jimbot.modules.AbstractProps;
import ru.jimbot.modules.MsgOutQueue;
import ru.jimbot.util.Log;
import ru.jimbot.util.MainProps;

/**
 * Работа с протоколом XMPP
 * @author Sergey
 */
public class XmppProtocol extends Protocol implements MessageListener, ChatManagerListener, ConnectionListener {
private XMPPConnection con = null;
private AbstractProps props;
private boolean connected = false;
private ConcurrentHashMap<String, String> JID = new ConcurrentHashMap<String, String>();

public XmppProtocol(AbstractProps props, int num) {
this.props = props;
mq = new MsgOutQueue(this, props.getIntProperty("bot.pauseOut"),props.getIntProperty("bot.pauseRestart"),props.getIntProperty("bot.msgOutLimit"), num);
}

/**
* Производит непосредственное подключение. Устанавливает нужные свойства.
*/
@Override public void connect() {
Log.info("Connect "+protocolIds(baseUin)+" - " + screenName);
mq.start();
try {
ConnectionConfiguration config = new ConnectionConfiguration(server, port, server);
SASLAuthentication.supportSASLMechanism("PLAIN", 0);
con = new XMPPConnection(config);
con.connect();
con.login(screenName, password, "Only");
con.getChatManager().addChatListener(this);
setStatus(Integer.parseInt(MainProps.getStringProperty("xmpp.status")), props.getStringProperty("icq.STATUS_MESSAGE2"));
Log.info(protocolIds(baseUin)+" - " + baseUin.split("@")[0]+ " - online");
connected = true;
} catch (XMPPException ex) {
Log.info("ERROR: " + ex.getMessage());
connected = false;
}
}

/**
* Производит непосредственное отключение. Освобождает ресурсы.
*/
@Override public void disconnect() {
mq.stop();
connected = false;
try {
con.disconnect();
con.getChatManager().removeChatListener(this);
Log.info(protocolIds(baseUin) +" - " + screenName.split("@")[0] + " - offline");
} catch (Exception ex) {
Log.info("ERROR: " + ex.getMessage());
}
}

/**
 * Производит непосредственное переподключение.
 */
@Override public void reConnect() {
disconnect();
try {
Thread.sleep(1000);
} catch (InterruptedException ex) {
ex.printStackTrace();
}
connect();
}

/**
 * Определим id jabber's
 * @param baseUin
 * @return 
 */
public String protocolIds(String baseUin){
String profile;
if (baseUin.contains("@vkmessenger.com") || baseUin.contains("@vk.com")) profile = "[Vkontakte] Login/ID";
if (baseUin.contains("@odnoklassniki.ru")) profile = "[Odnoklassniki] Login";
else if (baseUin.contains("@yandex.ru") || baseUin.contains("@ya.ru")) profile = "[Yandex] Login";
else if (baseUin.contains("@qip.ru")) profile = "[QIP] Login";
else if (baseUin.contains("@livejournal.com")) profile = "[LiveJournal] Login";
else profile = "[Jabber] JID";
return profile;
}

/**
 *
 * @param sendSN
 * @param recivSN
 * @param msg
 * @param isOffline
 */
public void getMsg(String sendSN, String recivSN, String msg, boolean isOffline) {
protList.getMsg(sendSN, recivSN, msg, isOffline);
}

/**
 * Тест на онлайн
 * @return
 */
@Override public boolean isOnLine() {
if (con == null) return false;
return connected;
}

/**
 * Отправка сообщения
 * @param to - кому
 * @param message - сообщение
 */
@Override public void sendMsg(String to, String message) {
try {
if (screenName.equalsIgnoreCase(to)) return;
if (JID.containsKey(to)) to = JID.get(to); // отправляем ответ с учетом ресурса (при мультилогине юзера)
Chat chat = con.getChatManager().getThreadChat(to);
if (chat == null) chat = con.getChatManager().createChat(to, to, this);
chat.sendMessage(message);
} catch (Exception e) {
Log.info("ERROR send message: " + e.getMessage());
}
}

/**
 * Входящее сообщение
 * @param chat
 * @param message - сообщение
 */
@Override public void processMessage(Chat chat, org.jivesoftware.smack.packet.Message message) {
try{
if (message.getType() == org.jivesoftware.smack.packet.Message.Type.chat) {
String SenderID = chat.getParticipant().split("/")[0];
// Запомним с какого клиента было послденее сообщение (при мультилогине)
JID.put(SenderID, message.getFrom());
// игнорим извещения о наборе сообщения
if (message.getBody() == null) return;
if ("Too frequently, try to confirm your mobile number".indexOf(message.getBody())>0) return;
// принимаем сообщение для дальнейших действий
protList.getMsg(SenderID, screenName, message.getBody(), false);
}
} catch (Exception e) {
Log.info("ERROR send message: " + e.getMessage());
}
}

/**
 * Меняем статус
 * @param status
 * @param statustxt
 */
public void setStatus(int status, String statustxt) {
Presence.Type types = Type.available;
Presence.Mode mode = Mode.chat;
switch (status) {
case 1:mode = Mode.available;break;
case 2:mode = Mode.chat;break;
case 3:mode = Mode.away;break;
case 4:mode = Mode.dnd;break;
case 5:mode = Mode.xa;break;
default:mode = Mode.available;
break;
}
Presence presence = new Presence(types, statustxt, 30, mode);
if (con == null) return;
con.sendPacket(presence);
}

/**
 * Создадим чат
 * @param chat - чат
 * @param created - "истина", "ложь"
 */
@Override public void chatCreated(Chat chat, boolean created) {
if (!created) chat.addMessageListener(this);
}

/**
 * Вернет экземпляр основного конфига
 * @return
 */
public AbstractProps getProps() {
return this.props;
}

/**
 * Закроем соединение
 */
@Override public void connectionClosed() {
connected = false;
reconnectingIn(15);
}

/**
 * Закроем соединение из-за ошибки
 */
@Override public void connectionClosedOnError(Exception ex) {
Log.error(ex.getMessage(), ex);
connected = false;
reconnectingIn(20);
}

@Override public void reconnectingIn(int t) {
Timer tm = new Timer();
TimerTask task = new TimerTask() {
public void run() {
reConnect();
cancel();
}
};
tm.schedule(task, 1000 * t);
Log.debug("reconnecting after " + t + " sec");
}

@Override public void reconnectionFailed(Exception ex) {
Log.error(ex.getMessage(), ex);
connected = false;
reconnectingIn(20);
}

@Override public void reconnectionSuccessful() {
connected = true;
}
}