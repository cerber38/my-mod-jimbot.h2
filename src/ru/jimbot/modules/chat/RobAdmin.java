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

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import ru.jimbot.protocol.Protocol;
import ru.jimbot.util.Log;

/**
* Виртуальный админ в чате
* 
* @author Prolubnikov Dmitry
*/
public class RobAdmin implements Runnable {

public String NICK = "Админ";
public String ALT_NICK = "admin;админ";
//Определение зафлуживания админа
private String lastSN = "";
private long lastTime = 0;
public int lastCount = 0;
private Thread th;
int sleepAmount = 1000;
long cTime = System.currentTimeMillis(); //Время последнего сообщения, для определения паузы
long stTime = 0; //Время последнего вывода статистики
public ChatServer srv;
public ConcurrentLinkedQueue<MsgElement> mq;
public ConcurrentHashMap<String, Integer> uins;
ConcurrentHashMap<String, Integer> test1, test2;
private String[][] chg = {{"y", "у"}, {"Y", "у"}, {"k", "к"}, {"K", "к"}, {"e", "е"},
{"E", "е"}, {"h", "н"}, {"H", "н"}, {"r", "г"}, {"3", "з"}, {"x", "х"}, {"X", "х"},
{"b", "в"}, {"B", "в"}, {"a", "а"}, {"A", "а"}, {"p", "р"}, {"P", "р"}, {"c", "с"},
{"C", "с"}, {"6", "б"}};
private Random r = new Random();

public class MsgElement {

public String msg = "";
public String uin = "";
public int room;
public Protocol proc;

MsgElement(String m, String u, Protocol p, int room) {
msg = m;
uin = u;
proc = p;
this.room = room;
}
}

/**
* Creates a new instance of RobAdmin
*/
public RobAdmin(ChatServer s) {
srv = s;
mq = new ConcurrentLinkedQueue<MsgElement>();
int usr = srv.us.statUsersCount();
uins = new ConcurrentHashMap<java.lang.String, java.lang.Integer>(usr);
uins.put("0", 0);
test1 = new ConcurrentHashMap<String, Integer>(usr);
test2 = new ConcurrentHashMap<String, Integer>(usr);
}

/**
* Добавление в очередь нового задания
*/
public void parse(Protocol proc, String uin, String msg, int room) {
cTime = System.currentTimeMillis();
mq.add(new MsgElement(msg, uin, proc, room));
}

/**
* Замена похожих букв на русские
*/
public String changeChar(String s) {
for (int i = 0; i < chg.length; i++) {
s = s.replace(chg[i][0], chg[i][1]);
}
return s;
}

private void parse() {
if (mq.isEmpty()) return;
MsgElement ms = mq.poll();
if (srv.getProps().getBooleanProperty("useMatFilter") && testMat1(changeChar(ms.msg))) {
say(srv.us.getUser(ms.uin).localnick + " не ругайся!", ms.room);
int i = 0;
if (!uins.containsKey(ms.uin)) uins.put(ms.uin, i);
else {
i = uins.get(ms.uin);
i++;
uins.put(ms.uin, i);
}
if (i >= 2) {
((ChatCommandProc) srv.cmd).akick(ms.proc, ms.uin);
uins.put(ms.uin, 0);
}
return;
}
if (!srv.getProps().getBooleanProperty("useSayAdmin")) return;
if (testName(ms.msg) && testHi(ms.msg)) {
if (!srv.us.authorityCheck(ms.uin, "adminsay")) return;
say(getHi(srv.us.getUser(ms.uin).localnick), ms.room);
return;
}
if (testName(ms.msg) && testStat(ms.msg)) {
if (!srv.us.authorityCheck(ms.uin, "adminstat")) return;
sayStat(ms.room);
return;
}
if (testName(ms.msg)) {
if (!srv.us.authorityCheck(ms.uin, "adminsay")) {
return;
}
if (testFlood(ms.uin)) {
lastCount++;
if (lastCount == (srv.getProps().getIntProperty("maxSayAdminCount") - 1)) {
say("Достали... ща пинаться начну!", ms.room);
} else if (lastCount >= srv.getProps().getIntProperty("maxSayAdminCount")) {
((ChatCommandProc) srv.cmd).akick(ms.proc, ms.uin);
lastCount = 0;
} else say(getAdmin(), ms.room);
} else say(getAdmin(), ms.room);
}
return;
}

/**
* Обработка событий по времени
*/
private void timeEvent() {
if (testTime()) {
cTime = System.currentTimeMillis();
if (testRnd(ChatProps.getInstance(srv.getName()).getIntProperty("sayAloneProbability"))) {
if (srv.cq.uq.size() <= 0) {
return;
}
say(getAlone(), 0);
}
}
}

public void say(String m, int room) {
cTime = System.currentTimeMillis();
String s = NICK + ": " + m;
Log.info(s);
srv.us.db.log(0, "admin", "OUT", s, room);
srv.cq.addMsg(s, "", room);
}

/**
* Проверка на мат, вариант 1
*/
public boolean testMat1(String msg) {
String[] s = msg.trim().split(" ");
for (int i = 1; i < s.length; i++) {
if (!test(s[i], ChatProps.getInstance(srv.getName()).getStringProperty("noMatString").split(";"))) {
if (test(s[i], ChatProps.getInstance(srv.getName()).getStringProperty("matString").split(";"))) {
return true;
}
}
}
return false;
}

/**
* Проверка на совпадение слов в сообщении
*/
public boolean test(String msg, String[] testStr) {
for (int i = 0; i < testStr.length; i++) {
if (msg.toLowerCase().indexOf(testStr[i]) >= 0) {
return true;
}
}
return false;
}

/**
* Проверка на наличие имени админа
*/
public boolean testName(String s) {
return test(s, ALT_NICK.split(";"));
}

/**
* Проверка на наличие приветствия
*/
public boolean testHi(String s) {
String t = "прив;прев;здоров;здрас;здрав;хай;хой;хелл;добр;даро";
return test(s, t.split(";"));
}

public boolean testStat(String s) {
String t = "stat;стат";
return test(s, t.split(";"));
}

public boolean testFlood(String sn) {
if (sn.equalsIgnoreCase(lastSN)) {
if ((System.currentTimeMillis() - lastTime) < ChatProps.getInstance(srv.getName()).getIntProperty("maxSayAdminTimeout") * 60 * 1000) {
return true;
} else {
lastTime = System.currentTimeMillis();
lastCount = 0;
return false;
}
} else {
lastSN = sn;
lastTime = System.currentTimeMillis();
lastCount = 0;
return false;
}
}

/**
* Вывод статистики по запросу
*/
public void sayStat(int room) {
long test = ChatProps.getInstance(srv.getName()).getIntProperty("getStatTimeout") * 60 * 1000;
if ((System.currentTimeMillis() - stTime) < test) {
say("Ну вас нафиг... нашли дурака... работай тут, считай... дайте передохнуть хоть немного.", room);
return;
}
stTime = System.currentTimeMillis();
String s = "За последние сутки:\n";
s += "Всего зашло в чат: " + srv.us.statUsersCount();
s += "\nОтправлено сообщений: " + srv.us.statMsgCount();
s += "\nКикнутых юзеров: " + srv.us.statKickUsersCount();
s += "\nВсего киков: " + srv.us.statKickCount();
s += "\nЗабанено юзеров: " + srv.us.statBanUsersCount();
s += "\nСамые болтливые пользователи:\n" + srv.us.statUsersTop();
say(s, room);
}

/**
* Проверка на первышение интервала ожидания
*/
public boolean testTime() {
return (System.currentTimeMillis() - cTime) > ChatProps.getInstance(srv.getName()).getIntProperty("sayAloneTime") * 60000;
}

public int getRND(int i) {
return r.nextInt(i);
}

/**
* Событие с вероятностью 1/i
*/
public boolean testRnd(int i) {
if (i <= 1) {
return false;
} else {
return r.nextInt(i) == 1;
}
}

/**
* Приветствие
*/
public String getHi(String name) {
String[] s = {"Привет", "Хай", "Приветствую", "Здравствуй", "Здоров"};
return s[getRND(s.length)] + " " + name + "!";
}

/**
* Фразы про одиночество
*/
public String getAlone() {
String[] s = {
"Здесь так тихо...",
"Ну и чего все замолчали?",
"Ну и молчите дальше, я тоже буду молчать :-\\",
"Алле! тут есть кто-нибудь? А-а-а-а!!! Я что тут один?!"
};
return s[getRND(s.length)];
}

/**
* Фразы при упоминении админа
*/
public String getAdmin() {
String[] s = {
"А, че?",
"хр-р-р... хр-р-р...",
"Опять про то же самое?",
"Не о чем больше поговорить?",
"А с тобой я ваще больше не разговариваю",
"Ты о своем, а я о своем",
"Не, я так не думаю",
"Ты серьезно?",
"С тобой так интересно!",
"Ха-ха, очень смешно...",
"Если ты высокого мения о своем интеллекте, то должен тебя разочаровать"
};
return s[getRND(s.length)];
}

public void start() {
th = new Thread(this);
th.setPriority(Thread.NORM_PRIORITY);
th.start();
}

public synchronized void stop() {
th = null;
notify();
}

public void run() {
Thread me = Thread.currentThread();
while (th == me) {
parse();
timeEvent();
try {
th.sleep(sleepAmount);
} catch (InterruptedException e) {
break;
}
}
th = null;
}
}