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
import ru.caffeineim.protocols.icq.core.OscarConnection;
import ru.caffeineim.protocols.icq.exceptions.ConvertStringException;
import ru.caffeineim.protocols.icq.integration.OscarInterface;
import ru.caffeineim.protocols.icq.integration.events.*;
import ru.caffeineim.protocols.icq.integration.listeners.MessagingListener;
import ru.caffeineim.protocols.icq.integration.listeners.OurStatusListener;
import ru.caffeineim.protocols.icq.integration.listeners.XStatusListener;
import ru.caffeineim.protocols.icq.setting.ICQClients;
import ru.caffeineim.protocols.icq.setting.enumerations.*;
import ru.jimbot.modules.AbstractProps;
import ru.jimbot.modules.MsgOutQueue;
import ru.jimbot.util.Log;
import ru.jimbot.util.MainProps;

/**
* Работа с протоколом Icq
* @author Prolubnikov Dmitry, Sergey
*/
public class IcqProtocol extends Protocol implements OurStatusListener,
MessagingListener, XStatusListener {
private OscarConnection con = null;
private AbstractProps props;
private int xStatusId;
private String xStatusText;
private boolean connected = false;
private ICQClients is = new ICQClients();

public IcqProtocol(AbstractProps props, int num){
this.props = props;
this.server = MainProps.getStringProperty("icq.serverDefault");
this.port = MainProps.getIntProperty("icq.portDefault");
mq = new MsgOutQueue(this, props.getIntProperty("bot.pauseOut"),props.getIntProperty("bot.pauseRestart"),props.getIntProperty("bot.msgOutLimit") ,num);
}

@Override public AbstractProps getProps(){
return props;
}

@Override public void connect() {
Log.info("Connect uin - " + screenName);
mq.start();
con = new OscarConnection(server, port, screenName, password, false, false);
con.addOurStatusListener(this);
con.addMessagingListener(this);
con.addXStatusListener(this);
con.connect();
}

public void reconnectingIn(int seconds) {
Timer tm = new Timer();
TimerTask task = new TimerTask() {
@Override public void run() {
reConnect();
cancel();
}
};
tm.schedule(task, 1000 * seconds);
}

@Override public void reConnect() {
disconnect();
port = MainProps.getIntProperty("icq.portDefault");
try {
Thread.sleep(1000);
} catch (InterruptedException ex) {
ex.printStackTrace();
}
connect();
}

@Override public void disconnect() {
if(con == null) return;
try {
mq.stop();
con.close();
con.removeOurStatusListener(this);
con.removeMessagingListener(this);
con.removeXStatusListener(this);
con = null;
Log.info("UIN - " + screenName + " - offline");
}catch (Exception ex){
ex.printStackTrace();
Log.error(ex.getMessage());
}
}

public void getMsg(String sendSN, String recivSN, String msg, boolean isOffline){
protList.getMsg(sendSN, recivSN, msg, isOffline);
}

@Override public boolean isOnLine() {
if(con == null) return false;
return connected;
}

@Override public void sendMsg(String sn, String msg){
try {
OscarInterface.sendBasicMessage(con, sn, msg);
} catch (ConvertStringException e) {
Log.info("ERROR send message: " + msg);
e.printStackTrace();
}
}

public boolean isNoContactList(){
return props.getBooleanProperty("chat.NoContactList");
}

@Override public synchronized void onIncomingMessage(IncomingMessageEvent e) {
try {
String msg = e.getMessage();
if(e.getSenderID().equals("1")) {
Log.error("Ошибка совместимости клиента ICQ. Будет произведена попытка переподключения...");
con.close();
return;
} 
// игнорим ненужное
if(msg.equals("null message")) return;
if(msg == null) return;
if(msg.equals(" ") || msg.equals(" ")) return;
if(msg.contains("Автоматическое сообщение")) return;
if(msg.contains("Автоответчик")) return;
if(msg.length() == 0) return;
if(msg.contains("only receives messages from contacts on his contact list or from contacts that have registered their phone number.")) return;
// принимаем сообщение для дальнейших действий
protList.getMsg(e.getSenderID(), screenName, msg, false);
} catch (Exception ex) {
ex.printStackTrace();
Log.error("error "+ex.getMessage());
}
}

/**
 * Авторизация не удалась
 * UNKNOWN_ERROR           = 0; "Неизвестная ошибка"
 * BAD_UIN_ERROR           = 1; "Плохой UIN.";
 * PASSWORD_ERROR          = 2; "Неверный пароль.";
 * NOT_EXISTS_ERROR        = 3; "Этого номер ICQ не существует.";
 * LIMIT_EXCEEDED_ERROR    = 4; "Максимальный лимит подключений привышен. Пожалуйста, попробуйте подключиться через несколько минут."
 * MAXIMUM_USERS_IP_ERROR  = 5; "Количество пользователей, подключенных от данного IP достигла максимума."
 * OLDER_ICQ_VERSION_ERROR = 6; "Вы используете старую версию ICQ. Пожалуйста, обновите."
 * CANT_REGISTER_ERROR     = 7; "Не удается зарегистрироватьcя в сети ICQ. Повторите через несколько минут."
 * @param e 
 */
@Override public void onAuthorizationFailed(LoginErrorEvent e){
Log.error("На uin`не " + screenName +  " авторизация с сервером ICQ не удалась. Причина: " +  e.getErrorMessage());
reconnectingIn(30);
connected = false;
}

/**
* Отвечаем на запрос х-статуса
*
* @param e
*/
@Override public void onXStatusRequest(XStatusRequestEvent e) {
try {
OscarInterface.sendXStatus(con, new XStatusModeEnum(Integer.parseInt(props.getStringProperty("icq.xstatus"))),
props.getStringProperty("icq.STATUS_MESSAGE1"),
props.getStringProperty("icq.STATUS_MESSAGE2"), e.getTime(), e.getMsgID(), e.getSenderID(), e.getSenderTcpVersion());
} catch (ConvertStringException ex) {
ex.printStackTrace();
Log.error(ex.getMessage());
}
}

/**
* Online
*/
@Override public void onLogin() {
// статус
setIcqStatus();
// Х-статус
setXStatus();
// приватный статус
setPrivateStatus();
// флаг статус
setFlagStatus();
// оповещаем
System.out.println("UIN - " + screenName + " online");
// загрузка контакт листа с сервера
//if (isNoContactList()) con.getContactList();
// загрузка оффлайн сообщений
if (!props.getBooleanProperty("chat.IgnoreOfflineMsg")) OscarInterface.requestOfflineMessages(con);
connected = true;// by Logachev Sergey (выполним все действия а потом только ставим true)
}

/**
* Разрыв соединения
*
* @param e
*/
@Override public void onLogout(Exception e) {
Log.error("Разрыв соединения: " + screenName + " - " + server + ":" + port + " По причине: " + e.getMessage());
connected = false;
System.out.println("Незапланированный разрыв соединения, будет повторная попытка подключения.");
reconnectingIn(30);
}

/**
* Для обновления Icq Х-Статуса налету
*/
public void setXStatus() {
int xstatus = Integer.parseInt(props.getStringProperty("icq.xstatus"));
int client = Integer.parseInt(props.getStringProperty("icq.client"));
if (client < 27) {
OscarInterface.changeProtocolVersion(con, 11);
this.setXStatus(xstatus);
} else if (client >= 27) {
is.setClient(client);
OscarInterface.changeProtocolVersion(con, is.getProtocol());
OscarInterface.changeClientInfo(con, is.getClient(), new XStatusModeEnum(xstatus));
} else {
is.setClient(28);
OscarInterface.changeProtocolVersion(con, 0);
OscarInterface.changeClientInfo(con, is.getClient(), new XStatusModeEnum(xstatus));
}
}

/**
* Для обновления Icq Статуса налету
*/
public void setIcqStatus() {
int status = Integer.parseInt(props.getStringProperty("icq.status"));
this.setStatus(status);
//OscarInterface.changeStatus(con, new StatusModeEnum(status));
}

/**
 * Приватный статус(Обновление налету)
 */
public void setPrivateStatus() {
int i = Integer.parseInt(props.getStringProperty("icq.private.status"));
int s = PrivateStatusEnum.VISIBLE_ALL;
switch (i) {
case 1: s = PrivateStatusEnum.VISIBLE_ALL; break;
case 2: s = PrivateStatusEnum.INVISIBLE_ALL; break;
case 3: s = PrivateStatusEnum.VISIBLE_TO_VISIBLE_LIST; break;
case 4: s = PrivateStatusEnum.INVISIBLE_TO_INVISIBLE_LIST; break;
case 5: s = PrivateStatusEnum.VISIBLE_TO_CONTACT_LIST; break;
}
OscarInterface.changePrivateStatus(con, new PrivateStatusEnum(s));
}

/**
 * Флаг статус(обновление налету)
 */
public void setFlagStatus() {
int i = Integer.parseInt(props.getStringProperty("icq.flag.status"));
int sf = StatusFlagEnum.STATUS_DCAUTH;
switch (i)  {
case 0 : sf = StatusFlagEnum.STATUS_WEBAWARE; break;
case 1 : sf = StatusFlagEnum.STATUS_SHOWIP; break;
case 2 : sf = StatusFlagEnum.STATUS_BIRTHDAY; break;
case 3 : sf = StatusFlagEnum.STATUS_WEBFRONT; break;
case 4 : sf = StatusFlagEnum.STATUS_DCDISABLED; break;
case 5 : sf = StatusFlagEnum.STATUS_DCAUTH; break;
case 6 : sf = StatusFlagEnum.STATUS_DCCONT; break;
case 7 : sf = StatusFlagEnum.STATUS_DCALLOWED; break;
case 8 : sf = StatusFlagEnum.STATUS_NONE; break;
}
OscarInterface.changeFlagStatus(con, new StatusFlagEnum(sf));
}

/**
 * Icq Статус
 * @param status 
 */
public void setStatus(int status) {
int s = StatusModeEnum.STATUS_FREE4CHAT;
switch (status)  {
case 0 : s = StatusModeEnum.STATUS_ONLINE; break; //В сети
case 1 : s = StatusModeEnum.STATUS_LUNCH; break; //Ем
case 2 : s = StatusModeEnum.STATUS_EVIL; break; //Злой
case 3 : s = StatusModeEnum.STATUS_DEPRESSION; break; //Депрессия
case 4 : s = StatusModeEnum.STATUS_HOME; break; //Дома
case 5 : s = StatusModeEnum.STATUS_WORK; break; //Работаю
case 6 : s = StatusModeEnum.STATUS_AWAY; break; //Отошел
case 7 : s = StatusModeEnum.STATUS_NA; break; //Недоступен
case 8 : s = StatusModeEnum.STATUS_OCCUPIED; break; //Занят
case 9 : s = StatusModeEnum.STATUS_DND; break; //Не беспокоить
case 10 : s = StatusModeEnum.STATUS_FREE4CHAT; break; //Готов поболтать
case 11 : s = StatusModeEnum.STATUS_INVISIBLE; break; //Невидимый
case 12 : s = StatusModeEnum.STATUS_INVISIBLE_ALL; break; //Невидимый для всех
case 13 : s = StatusModeEnum.STATUS_OFFLINE; break; //Не в сети
}
OscarInterface.changeStatus(con, new StatusModeEnum(s));
}

/**
* Icq х-статус
* @param number
*/
public void setXStatus(int xstatus){
int xs = XStatusModeEnum.NONE;
switch (xstatus)  {
case 0: xs = XStatusModeEnum.NONE; break;
case 1: xs = XStatusModeEnum.ANGRY; break;
case 2: xs = XStatusModeEnum.TAKING_A_BATH; break;
case 3: xs = XStatusModeEnum.TIRED; break;
case 4: xs = XStatusModeEnum.PARTY; break;
case 5: xs = XStatusModeEnum.DRINKING_BEER; break;
case 6: xs = XStatusModeEnum.THINKING; break;
case 7: xs = XStatusModeEnum.EATING; break;
case 8: xs = XStatusModeEnum.WATCHING_TV; break;
case 9: xs = XStatusModeEnum.MEETING; break;
case 10: xs = XStatusModeEnum.COFFEE; break;
case 11: xs = XStatusModeEnum.LISTENING_TO_MUSIC; break;
case 12: xs = XStatusModeEnum.BUSINESS; break;
case 13: xs = XStatusModeEnum.SHOOTING; break;
case 14: xs = XStatusModeEnum.HAVING_FUN; break;
case 15: xs = XStatusModeEnum.ON_THE_PHONE; break;
case 16: xs = XStatusModeEnum.GAMING; break;
case 17: xs = XStatusModeEnum.STUDYING; break;
case 18: xs = XStatusModeEnum.SHOPPING; break;
case 19: xs = XStatusModeEnum.FEELING_SICK; break;
case 20: xs = XStatusModeEnum.SLEEPING; break;
case 21: xs = XStatusModeEnum.SURFING; break;
case 22: xs = XStatusModeEnum.BROWSING; break;
case 23: xs = XStatusModeEnum.WORKING; break;
case 24: xs = XStatusModeEnum.TYPING; break;
case 25: xs = XStatusModeEnum.PICNIC; break;
case 26: xs = XStatusModeEnum.COOKING; break;
case 27: xs = XStatusModeEnum.SMOKING; break;
case 28: xs = XStatusModeEnum.I_HIGH; break;
case 29: xs = XStatusModeEnum.ON_WC; break;
case 30: xs = XStatusModeEnum.QUESTION; break;
case 31: xs = XStatusModeEnum.WATCHING_PRO7; break;
case 32: xs = XStatusModeEnum.LOVE; break;
case 33: xs = XStatusModeEnum.GOOGLE; break;
case 34: xs = XStatusModeEnum.NOTEPAD; break;
case 35: xs = XStatusModeEnum.SEX; break;
case 36: xs = XStatusModeEnum.RULOVE; break;
case 37: xs = XStatusModeEnum.SMOUKE; break;
}
OscarInterface.changeXStatus(con, new XStatusModeEnum(xs), new ClientModeEnum(Integer.parseInt(props.getStringProperty("icq.client"))));
}

/**
* @param n - номер х-статуса
* @param text - текст х-статуса
*/
public void setXStatus(int n, String text) {
if (n >= 0 && n <= 37) {
xStatusId = n;
props.setStringProperty("icq.xstatus", Integer.toString(xStatusId));
}
if (!text.equals("")) {
xStatusText = text;
props.setStringProperty("icq.STATUS_MESSAGE2", xStatusText);
}
this.setXStatus(xStatusId);
}

@Override public void onMessageMissed(MessageMissedEvent e) {
Log.debug("Message from " + e.getUin() + " can't be recieved because " + e.getReason()  + " count="+e.getMissedMsgCount());
}

@Override public void onMessageError(MessageErrorEvent e) {
Log.error("Message error " + e.getError().toString());
}

/**
 * Оффлайн сообщение
 * @param e 
 */
@Override public void onOfflineMessage(OfflineMessageEvent e) {
System.out.println("Оффлайн сообщение: "+e.getMessage()+" от UIN:"+e.getSenderUin()+"\n"
+ "Время: " + e.getSendDate().toString());
}

@Override public void onStatusResponse(StatusEvent e) {
//System.out.println("onStatusResponse(): Флаг - " + e.getStatusFlag() + ", "
//+ "Режим - " + e.getStatusMode());
}

@Override public void onIncomingUrl(IncomingUrlEvent e) {
System.out.println("UIN: " + e.getSenderID() + "\n"
+ "Сообщение: " + e.getMessage() + "\n"
+ "Ссылка: " + e.getUrl() + "\n"
+ "Id-сообщения: " + e.getMessageId() + "\n"
+ "SenderStatus - " + e.getSenderStatus().getMode());
}

@Override public void onMessageAck(MessageAckEvent e) {
System.out.println("RcptUin - " + e.getRcptUin() + "\n"
+ "Id-сообщения - " + e.getMessageId() + "\n"
+ "Время-сообщения - " + e.getMessageTime() + "\n"
+ "Тип-сообщения - " + e.getMessageType());
}

@Override public void onXStatusResponse(XStatusResponseEvent e) {
System.out.println("UIN - " + e.getSenderID() + "\n"
+ "X-статус - " + e.getXStatus().getXStatus() + "\n"
+ "Тема - " + e.getTitle() + "\n"
+ "Текст - " + e.getDescription() + "\n");
}
}