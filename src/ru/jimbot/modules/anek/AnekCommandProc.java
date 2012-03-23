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

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import ru.jimbot.modules.AbstractCommandProcessor;
import ru.jimbot.modules.AbstractServer;
import ru.jimbot.modules.Cmd;
import ru.jimbot.modules.CommandParser;
import ru.jimbot.protocol.Protocol;
import ru.jimbot.util.Log;
import ru.jimbot.util.MainProps;

/**
*
* @author Prolubnikov Dmitry
*/
public class AnekCommandProc extends AbstractCommandProcessor {

public AnekServer srv;
public ConcurrentHashMap<String, StateUin> uq;
public long state = 0; //Статистика запросов
public long state_add = 0;
public HashMap<String, Cmd> commands = new HashMap<String, Cmd>();
public CommandParser parser = null;
private boolean firstStartMsg = false;

/**
* Creates a new instance of AnekCommandProc
*
* @param s
*/
public AnekCommandProc(AnekServer s) {
srv = s;
uq = new ConcurrentHashMap<String, StateUin>();
init();
}

/**
* Инициализация списка команд и полномочий
*/
private void init() {
commands.put("1", new Cmd("1", "", 1));
commands.put("!help", new Cmd("!help", "", 2));
commands.put("!справка", new Cmd("!справка", "", 2));
commands.put("!помощь", new Cmd("!помощь", "", 2));
commands.put("!stat", new Cmd("!stat", "", 3));
commands.put("!стат", new Cmd("!стат", "", 3));
commands.put("!statads", new Cmd("!statads", "", 4));
commands.put("!рстат", new Cmd("!рстат", "", 4));
commands.put("!about", new Cmd("!about", "", 5));
commands.put("!оботе", new Cmd("!оботе", "", 5));
commands.put("!о", new Cmd("!о", "", 5));
commands.put("!add", new Cmd("!add", "$s", 6));
commands.put("!добавить", new Cmd("!добавить", "$s", 6));
commands.put("!anek", new Cmd("!anek", "$n", 7));
commands.put("!ха", new Cmd("!ха", "$n", 7));
commands.put("!refresh", new Cmd("!refresh", "", 8));
commands.put("!обновить", new Cmd("!обновить", "", 8));
commands.put("!free", new Cmd("!free", "", 9));
commands.put("!свободный", new Cmd("!свободный", "", 9));
}

private void firstMsg(Protocol proc) {
if (!firstStartMsg) {
String[] s = srv.getProps().getAdmins();
for (int i = 0; i < s.length; i++) proc.mq.add(s[i], "Бот успешно запущен!\n");
firstStartMsg = true;
}
}

public AbstractServer getServer() {
return srv;
}

public void parse(Protocol proc, String uin, String msg) {
firstMsg(proc);
try {
msg = msg.trim();
Log.debug("ANEK: " + uin + ">>" + msg);
if (msg.length() == 0) {
Log.error("Пустое сообщение в парсере команд: " + uin + ">" + msg);
return;
}
addState(uin);
//Обработка команд
int tp = parser.parseCommand(msg);
int tst = 0;
if (tp < 0) tst = 0;
else tst = tp;
switch (tst) {
case 1:
state++;
stateInc(uin);
proc.mq.add(uin, srv.an.getAnek());
break;
case 2:
proc.mq.add(uin, "Вас приветствует jImBot!\n"
+ "Для получения случайного анекдота пошлите \"1\""
+ "\nДля получения конкретного анека пошли !ха и номер"
+ "\nДля добавления анекдота используй команду !добавить"
+ "\nДля просмотра статистики бота пошлите \"!стат\""
+ "\nКоманда !свободный укажет самый свободный УИН"
+ "\n!оботе - информация о программе"
+ "\nДля получения помоши пошлите \"!справка\"\n"
+ "Не посылайте сообщения чаще,  чем раз в 3с.");
if (ru.jimbot.modules.anek.AnekProps.getInstance(srv.getName()).testAdmin(uin)) {
proc.mq.add(uin, "Дополнительный админские команды:\n"
+ "!рстат - статистика показа рекламы\n"
+ "!обновить - обновить кеш после изменений в БД");
}
break;
case 3:
proc.mq.add(uin, "Всего в базе анекдотов: " + srv.an.maxAnek
+ "\nОтправлено анекдотов: " + state
+ "\nДобавлено анекдотов: " + state_add
+ "\nУникальных UIN: " + uq.size()
+ "\nБот запущен: " + new Date(getTimeStart())
+ "\nВремя работы: " + getTime(getUpTime())
+ "\nВ среднем анекдотов в час: " + getHourStat()
+ "\nВ среднем анекдотов в сутки: " + getDayStat()
+ "\nПрочитано вами анекдотов: " + uq.get(uin).cnt);
break;
case 4:
proc.mq.add(uin, srv.an.adsStat());
break;
case 5:
proc.mq.add(uin, MainProps.getAbout());
break;
case 6:
commandAdd(proc, uin, parser.parseArgs(msg));
break;
case 7:
state++;
stateInc(uin);
proc.mq.add(uin, srv.an.getAnek((Integer) parser.parseArgs(msg).get(0)));
break;
case 8:
srv.an.refreshData();
proc.mq.add(uin, "Данные в памяти обновлены");
break;
case 9:
Protocol u = getFreeUin();
proc.mq.add(uin, "Самый свободный уин бота: " + u.baseUin);
u.mq.add(uin, "Привет! Я самый свободный УИН :)");
break;
default:
Log.info("ANEK: " + uin + ">>" + msg);
proc.mq.add(uin, "Неизвестная команда, для помощи используйте !help");
}
} catch (Exception ex) {
ex.printStackTrace();
}
}

/**
* Добавление анекдота
*/
public void commandAdd(Protocol proc, String uin, Vector v) {
try {
String s =(String)v.get(0); 	
if(s.equals("")) {
proc.mq.add(uin,"Пустой анекдот.");
return;
}
if(s.length()<20) return;
if(s.length()>1000) return;
srv.an.addTempAnek((String)v.get(0),uin);
Log.talk("Add anek <" + uin + ">: " + (String)v.get(0));
proc.mq.add(uin,"Анекдот сохранен. После рассмотрения администрацией он будет добавлен в базу.");
state_add++;
} catch (Exception ex) {
ex.printStackTrace();
Log.info("Error save anek: " + ex.getMessage());
proc.mq.add(uin, "Ошибка добавления");
}
}

/**
* Определение времени запуска бота
*/
private long getTimeStart() {
long t = 0;
try {
File f = new File("./state");
t = f.lastModified();
} catch (Exception ex) {
ex.printStackTrace();
}
return t;
}

private long getUpTime() {
return System.currentTimeMillis() - getTimeStart();
}

private long getHourStat() {
if (getUpTime() > 1000 * 60 * 60) return state / (getUpTime() / 3600000);
return 0;
}

private long getDayStat() {
if (getUpTime() > 1000 * 60 * 60 * 24) return state / (getUpTime() / 86400000);
return 0;
}

private String getTime(long t) {
Date dt = new Date(t);
DateFormat df = DateFormat.getTimeInstance(DateFormat.MEDIUM);
df.setTimeZone(TimeZone.getTimeZone("GMT"));
return (t / 86400000) + " дней " + df.format(dt);
}

/**
* Возвращает наименее загруженный номер
*
* @return
*/
public Protocol getFreeUin() {
Protocol u = null;
int k = 99;
int c = 0;
for (int i = 0; i < srv.getProps().uinCount(); i++) {
if (srv.getIcqProcess(i).isOnLine()) {
c = srv.getIcqProcess(i).mq.size();
if (c == 0) return srv.getIcqProcess(i);
if (k > c) {
k = c;
u = srv.getIcqProcess(i);
}
}
}
return u;
}

public void addState(String uin) {
if (!uq.containsKey(uin)) {
StateUin u = new StateUin(uin, 0);
uq.put(uin, u);
}
}

public void stateInc(String uin) {
StateUin u = uq.get(uin);
u.cnt++;
uq.put(uin, u);
}

public class StateUin {

public String uin = "";
public int cnt = 0;

public StateUin(String u, int c) {
uin = u;
cnt = c;
}
}
}