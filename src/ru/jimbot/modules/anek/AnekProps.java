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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Properties;
import ru.jimbot.modules.AbstractProps;
import ru.jimbot.table.UserPreference;
import ru.jimbot.util.Log;
import ru.jimbot.util.MainProps;

/**
*
* @author Prolubnikov Dmitry
*/
public class AnekProps implements AbstractProps {

public static HashMap<String, AnekProps> props = new HashMap<String, AnekProps>(MainProps.getServicesCount());
public String PROPS_FILE = "";
private String PROPS_FOLDER = "";
public Properties appProps;

/**
* Creates a new instance of AnekProps
*/
public AnekProps() {
}

public static AnekProps getInstance(String name) {
if (props.containsKey(name)) {
return props.get(name);
} else {
AnekProps p = new AnekProps();
p.PROPS_FILE = "./services/" + name + "/" + name + ".xml";
p.PROPS_FOLDER = "./services/" + name;
p.setDefault();
props.put(name, p);
return p;
}
}

public void setDefault() {
appProps = new Properties();
setIntProperty("conn.uinCount", 1);
setStringProperty("conn.uin0", "111");
setStringProperty("conn.pass0", "Password");
setIntProperty("conn.MaxOutMsgSize0", 1000);
setStringProperty("icq.status", "32");
setStringProperty("icq.xstatus", "32");
setStringProperty("icq.private.status", "1");
setStringProperty("icq.flag.status", "1");
setStringProperty("icq.client", "1");
setIntProperty("bot.pauseIn", 3000); //Пауза входящих сообщений
setIntProperty("bot.pauseOut", 500); //Пауза исходящих сообщений
setIntProperty("bot.msgOutLimit", 20); //Ограничение очереди исходящих сообщений
setIntProperty("bot.pauseRestart", 11 * 60 * 1000); //Пауза перед запуском упавшего коннекта
setStringProperty("bot.adminUIN", "111111;222222");
setStringProperty("icq.STATUS_MESSAGE1", "");
setStringProperty("icq.STATUS_MESSAGE2", "");
setBooleanProperty("bot.useAds", false);
setIntProperty("bot.adsRate", 3);
setBooleanProperty("main.StartBot", false);
setIntProperty("chat.MaxOutMsgSize", 500);
setIntProperty("chat.MaxOutMsgCount", 5);
setStringProperty("db.user", "sergey");
setStringProperty("db.pass", "sergey");
}

private String[][] icqStatus = {{"0", "В сети"}, {"268435456", "Не в сети"}, {"10", "Занят"},
{"4", "Недоступен"}, {"512", "Невидим для всех"}, {"256", "Невидим"}, {"2", "Не беспокоить"},
{"32", "Готов поболтать"}, {"1", "Отошёл"}, {"8193", "Кушаю"}, {"24576", "Работа"}, {"20480", "Дома"},
{"16384", "Депрессия"}, {"12288", "Злой"}};
private String[][] icqXstatus = {{"0", "Нет"}, {"1", "Злой"}, {"2", "Купаюсь"}, {"3", "Устал"},
{"4", "Вечеринка"}, {"5", "Пиво"}, {"6", "Думаю"}, {"7", "Кушаю"}, {"8", "ТВ"}, {"9", "Друзья"},
{"10", "Кофе"}, {"11", "Слушаю музыку"}, {"12", "Дела"}, {"13", "Кино"}, {"14", "Весело"},
{"15", "Телефон"}, {"16", "Играю"}, {"17", "Учусь"}, {"18", "Магазины"}, {"19", "Болею"}, {"20", "Сплю"},
{"21", "Отрываюсь"}, {"22", "Интернет"}, {"23", "Работаю"}, {"24", "Печатаю"}, {"25", "Пикник"},
{"26", "Готовлю"}, {"27", "Курю"}, {"28", "Релакс"}, {"29", "Туалет"}, {"30", "Вопрос"},
{"31", "Дорога"}, {"32", "Любовь"}, {"33", "Поиск"}, {"34", "Дневник"}, {"35", "Секс"},
{"36", "RuLove"}, {"37", "Курю"}};
private String[][] icqClient = {{"0", "None"}, {"1", "QIP PDA Symbian"}, {"2", "QIP PDA Windows"},
{"3", "Licq"}, {"4", "MCHAT"}, {"5", "CAP MICQ"}, {"6", "QUTIM"}, {"7", "BAYANICQ"}, {"8", "KOPETE"},
{"9", "PALMJICQ"}, {"10", "vmICQ"}, {"11", "Yapp"}, {"12", "DICHAT"}, {"13", "MIPCLIENT"},
{"14", "JAPSERGO"}, {"15", "QIP Mobile iPhone"}, {"16", "JIT"}, {"17", "Miranda"}, {"18", "Climm"},
{"19", "Anastasia"}, {"20", "Inlux Messenger"}, {"21", "Mail Agent"}, {"22", "MobiQ"}, {"23", "QIP Infium"},
{"24", "LocID"}, {"25", "Adium"}, {"26", "IMplus"}, {"27", "icq_7_ver9"},
{"28", "PyICQ_t_ver0"}, {"29", "QIP_Mobile_Java_ver11"},
{"30", "icq_6_5_ver9"}, {"31", "icq_2003bPro_ver10"}, {"32", "icq_5_1_ver9"}, {"33", "icq_5_ver9"}, {"34", "icq_Lite_4_ver9"},
{"35", "icq_Lite_ver9"}, {"36", "icq_2001_ver8"}, {"37", "icq_for_Mac_ver7"}, {"38", "icq_Adium_ver0"}, {"39", "QIP_Infium_ver11"},
{"40", "Slick_ver0"}, {"41", "TICQClient_ver8"}, {"42", "Fring_ver9"}, {"43", "Mail_Ru_Agent_ver9"}, {"44", "CitronIM_ver0"}, {"45", "Spambot_ver11"},
{"46", "ICQ2Go_ver7"}, {"47", "Icq_Bot_org_ver11"}, {"48", "jImBot_ver11"}};
private String[][] icqPrivateStatus = {
{"1", "Видимый для всех"}, {"2", "Невидимый для всех"},
{"3", "Видимый для списка видящих"}, {"4", "Невидимый для списка невидящих"}, {"5", "Видимый для списка контактов"}};
private String[][] icqFlagStatus = {{"0", "Индексация в поиске"}, {"1", "Показывать IP"}, {"2", "День рожденье"},
{"3", "STATUS_WEBFRONT"}, {"4", "STATUS_DCDISABLED"}, {"5", "Авторизация"}, {"6", "STATUS_DCCONT"}, {"7", "STATUS_DCALLOWED"}, {"8", "Нету"}};

public UserPreference[] getUserPreference() {
UserPreference[] p = {
new UserPreference(UserPreference.CATEGORY_TYPE, "main", "Основные настройки", ""),
new UserPreference(UserPreference.BOOLEAN_TYPE, "main.StartBot", "Запускать анекдотный бот", getBooleanProperty("main.StartBot")),
new UserPreference(UserPreference.CATEGORY_TYPE, "bot", "Настройки Icq", ""),
new UserPreference(UserPreference.SELECT_TYPE, "icq.client", "ID icq клиента", getStringProperty("icq.client"), icqClient),
new UserPreference(UserPreference.SELECT_TYPE, "icq.status", "ICQ статус", getStringProperty("icq.status"), icqStatus),
new UserPreference(UserPreference.SELECT_TYPE, "icq.xstatus", "x-статус (0-37)", getStringProperty("icq.xstatus"), icqXstatus),
new UserPreference(UserPreference.SELECT_TYPE, "icq.private.status", "Приватный статус", getStringProperty("icq.private.status"), icqPrivateStatus),
new UserPreference(UserPreference.SELECT_TYPE, "icq.flag.status", "Флаг статус", getStringProperty("icq.flag.status"), icqFlagStatus),
new UserPreference(UserPreference.TEXTAREA_TYPE, "icq.STATUS_MESSAGE1", "Сообщение статуса", getStringProperty("icq.STATUS_MESSAGE1")),
new UserPreference(UserPreference.TEXTAREA_TYPE, "icq.STATUS_MESSAGE2", "Сообщение x-статуса", getStringProperty("icq.STATUS_MESSAGE2")),
new UserPreference(UserPreference.INTEGER_TYPE, "bot.pauseIn", "Пауза для входящих сообщений", getIntProperty("bot.pauseIn")),
new UserPreference(UserPreference.INTEGER_TYPE, "bot.pauseOut", "Пауза для исходящих сообщений", getIntProperty("bot.pauseOut")),
new UserPreference(UserPreference.INTEGER_TYPE, "bot.msgOutLimit", "Ограничение очереди исходящих", getIntProperty("bot.msgOutLimit")),
new UserPreference(UserPreference.INTEGER_TYPE, "chat.MaxOutMsgSize", "Максимальный размер одного исходящего сообщения", getIntProperty("chat.MaxOutMsgSize")),
new UserPreference(UserPreference.INTEGER_TYPE, "chat.MaxOutMsgCount", "Максимальное число частей исходящего сообщения", getIntProperty("chat.MaxOutMsgCount")),
new UserPreference(UserPreference.INTEGER_TYPE, "bot.pauseRestart", "Пауза перед перезапуском коннекта", getIntProperty("bot.pauseRestart")),
new UserPreference(UserPreference.STRING_TYPE, "bot.adminUIN", "Админские UIN", getStringProperty("bot.adminUIN")),
new UserPreference(UserPreference.BOOLEAN_TYPE, "bot.useAds", "Использовать рекламу в боте", getBooleanProperty("bot.useAds")),
new UserPreference(UserPreference.INTEGER_TYPE, "bot.adsRate", "Частота рекламы", getIntProperty("bot.adsRate")),
new UserPreference(UserPreference.CATEGORY_TYPE, "db", "Настройки Hypersonic 2", ""),
new UserPreference(UserPreference.STRING_TYPE, "db.user", "Пользователь", getStringProperty("db.user")),
new UserPreference(UserPreference.PASS_TYPE, "db.pass", "Пароль", getStringProperty("db.pass")),            new UserPreference(UserPreference.STRING_TYPE,"db.dbname","Название базы данных",getStringProperty("db.dbname"))
};
return p;
}

public UserPreference[] getUINPreference() {
UserPreference[] p = new UserPreference[uinCount() * 2 + 1];
p[0] = new UserPreference(UserPreference.CATEGORY_TYPE, "conn", "Настройки UINов для подключения", "");
for (int i = 0; i < uinCount(); i++) {
p[i * 2 + 1] = new UserPreference(UserPreference.STRING_TYPE, "conn.uin" + i, "UIN" + i, getProperty("conn.uin" + i, ""));
p[i * 2 + 2] = new UserPreference(UserPreference.PASS_TYPE, "conn.pass" + i, "Password" + i, getProperty("conn.pass" + i, ""));
}
return p;
}

public boolean isAutoStart() {
return getBooleanProperty("main.StartBot");
}

public int uinCount() {
return getIntProperty("conn.uinCount");
}

public String getUin(int i) {
return getStringProperty("conn.uin" + i);
}

public String getPass(int i) {
return getStringProperty("conn.pass" + i);
}

public int getMaxOut(int i) {
return getIntProperty("conn.MaxOutMsgSize" + i);
}

/**
* Изменение уина
*
* @param i
* @param uin
* @param pass
*/
public void setUin(int i, String uin, String pass, int lenght) {
setStringProperty("conn.uin" + i, uin);
setStringProperty("conn.pass" + i, pass);
setIntProperty("conn.MaxOutMsgSize" + i, lenght);
}

/**
* Добавление нового уина в настройки
*
* @param uin - уин
* @param pass - пароль
* @return - порядковый номер нового уина
*/
public int addUin(String uin, String pass, int lenght) {
int c = uinCount();
setIntProperty("conn.uinCount", c + 1);
setStringProperty("conn.uin" + c, uin);
setStringProperty("conn.pass" + c, pass);
setIntProperty("conn.MaxOutMsgSize" + c, lenght);
return c;
}

/**
* Удаление уина из настроек
*
* @param c
*/
public void delUin(int c) {
// Сдвигаем элементы после удаленного
for (int i = 0; i < (uinCount() - 1); i++) {
if (i >= c) {
setStringProperty("conn.uin" + i, getUin(i + 1));
setStringProperty("conn.pass" + i, getPass(i + 1));
setIntProperty("conn.MaxOutMsgSize" + i, getMaxOut(i + 1));
}
}
//Удаляем самый последний элемент
appProps.remove("conn.uin" + (uinCount() - 1));
appProps.remove("conn.pass" + (uinCount() - 1));
appProps.remove("conn.MaxOutMsgSize" + (uinCount() - 1));
setIntProperty("conn.uinCount", uinCount() - 1);
}

public boolean testAdmin(String uin) {
if (uin.equals("0")) return true; //Выртуальный админ
String s = getStringProperty("bot.adminUIN");
if (s.isEmpty()) return true;
String[] ss = s.split(";");
try {
for (int i = 0; i < ss.length; i++) {
if (ss[i].equalsIgnoreCase(uin)) return true;
}
} catch (Exception ex) {
ex.printStackTrace();
}
return false;
}

public String[] getAdmins() {
return getStringProperty("bot.adminUIN").split(";");
}

public final void load() {
File file = new File(PROPS_FILE);
if (!file.exists()) save();
setDefault();
try {
FileInputStream fi = new FileInputStream(file);
appProps.loadFromXML(fi);
fi.close();
Log.info("Load Anek preferences ok");
} catch (Exception ex) {
ex.printStackTrace();
Log.error("Error opening Anek preferences");
}
}

public final void save() {
File file = new File(PROPS_FILE);
File dir = new File(this.PROPS_FOLDER);
try {
if (!dir.exists()) dir.mkdirs();
FileOutputStream fo = new FileOutputStream(file);
appProps.storeToXML(fo, "jImBot properties");
fo.close();
Log.info("Save Anek preferences ok");
} catch (Exception ex) {
ex.printStackTrace();
Log.error("Error saving Anek preferences");
}
}

public void registerProperties(Properties _appProps) {
appProps = _appProps;
}

public String getProperty(String key) {
return appProps.getProperty(key);
}

public String getStringProperty(String key) {
return appProps.getProperty(key);
}

public String getProperty(String key, String def) {
return appProps.getProperty(key, def);
}

public void setProperty(String key, String val) {
appProps.setProperty(key, val);
}

public void setStringProperty(String key, String val) {
appProps.setProperty(key, val);
}

public void setIntProperty(String key, int val) {
appProps.setProperty(key, Integer.toString(val));
}

public void setBooleanProperty(String key, boolean val) {
appProps.setProperty(key, val ? "true" : "false");
}

public int getIntProperty(String key) {
return Integer.parseInt(appProps.getProperty(key));
}

public boolean getBooleanProperty(String key) {
return Boolean.valueOf(appProps.getProperty(key)).booleanValue();
}

public Properties getProps() {
return appProps;
}
}