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

package ru.jimbot.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Properties;
import java.util.Vector;

import ru.jimbot.table.UserPreference;

/**
 * Основные настройки бота
 * 
 * @author Prolubnikov Dmitry
 */
public class MainProps {
    public static final String VERSION = "v.0.4.0 pre 4 (06/07/2009)";
    public static final int VER_INT = 18;
//    public static final String VER_DESC ="test version";
    private static int ver_no = 0;
    private static long ver_last_read = 0;
    private static String ver_desc = "";
//    public static final String VER_DESC = "Поправлена работа при условии нестабильного соединения с MySQL;" +
//    		"Кеширование скриптов;" +
//    		"";
    public static final String PROG_TITLE = "jImBot";
    public static final String PROPS_FILE = "./jimbot.xml";
    public static final String ENCODING = "windows-1251";
    private static Properties appProps;
    private static Properties langProps;
    private static boolean isLoaded = false;
    private static Vector servers = new Vector();
    private static String currentServer = "";
    private static int currentPort = 0;
    private static int countServer = 0;
    private static HashSet<String> ignor;
    
    /** Creates a new instance of MainProps */
    public MainProps() {
    }
    
    public static final void setDefault() {
        appProps = new Properties();
        setStringProperty("icq.serverDefault","login.icq.com");
        setIntProperty("icq.portDefault",5190);
        setBooleanProperty("main.autoStart",true);
        setBooleanProperty("main.StartHTTP",true);
        setStringProperty("http.user","admin"); // юзер для админки
        setStringProperty("http.pass","admin"); // пароль для доступа в админку
        setIntProperty("http.delay",10);
        setIntProperty("http.maxErrLogin",3);
        setIntProperty("http.timeErrLogin",10);
        setIntProperty("http.timeBlockLogin",20);
        setIntProperty("srv.servicesCount",0);
        setBooleanProperty("main.checkNewVer", true);
        
        setStringProperty("xmpp.status", "1");
    }
    
    private static String JabberStatus[][] = {{"1", "В сети"},{"2", "Готов поболтать"},
{"3", "В гостях"}, {"4", "....."}, {"5", "Не беспокоить"}};

    
    public static UserPreference[] getUserPreference(){
        UserPreference[] p = {
            new UserPreference(UserPreference.CATEGORY_TYPE,"main", "Основные настройки",""),
            new UserPreference(UserPreference.BOOLEAN_TYPE,"main.checkNewVer","Уведомлять о новых версиях",getBooleanProperty("main.checkNewVer")),
            new UserPreference(UserPreference.BOOLEAN_TYPE,"main.autoStart","Автозапуск при загрузке",getBooleanProperty("main.autoStart")),
            new UserPreference(UserPreference.BOOLEAN_TYPE,"main.StartHTTP","Запускать HTTP сервер",getBooleanProperty("main.StartHTTP")),
            new UserPreference(UserPreference.INTEGER_TYPE,"http.delay","Время жизни HTTP сессии",getIntProperty("http.delay")),
            new UserPreference(UserPreference.INTEGER_TYPE,"http.maxErrLogin","Число ошибочных входов для блокировки",getIntProperty("http.maxErrLogin")),
            new UserPreference(UserPreference.INTEGER_TYPE,"http.timeErrLogin","Допустимый период между ошибками",getIntProperty("http.timeErrLogin")),
            new UserPreference(UserPreference.INTEGER_TYPE,"http.timeBlockLogin","Время блокировки входа",getIntProperty("http.timeBlockLogin")),
            new UserPreference(UserPreference.CATEGORY_TYPE,"bot", "Настройки бота",""),
            new UserPreference(UserPreference.SELECT_TYPE, "xmpp.status", "Jabber Статус", getStringProperty("xmpp.status"),JabberStatus),
            new UserPreference(UserPreference.STRING_TYPE,"icq.serverDefault","ICQ Сервер",getStringProperty("icq.serverDefault")),
            new UserPreference(UserPreference.INTEGER_TYPE,"icq.portDefault","ICQ Порт",getIntProperty("icq.portDefault")),
        };
        return p;
    }
    
    /**
     * Загружает игнор-лист из файла
     */
    public static void loadIgnorList(){
    	String s;
    	ignor = new HashSet<String>();
        try{
            BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream("ignore.txt"),"windows-1251")); 
            while (r.ready()){
                s = r.readLine();
                if(!s.equals("")){
                    ignor.add(s);
                }
            }
            r.close();
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }
    
    /**
     * УИН в игноре?
     * @param uin
     * @return
     */
    public static boolean isIgnor(String uin){
    	if(ignor==null) return false;
    	return ignor.contains(uin);
    }
    
    public static Properties getProps(){
        return appProps;
    }
    
    public static int getServicesCount(){
    	return getIntProperty("srv.servicesCount");
    }
    
    public static String getServiceName(int i){
    	return getStringProperty("srv.serviceName"+i);
    }
    
    public static String getServiceType(int i) {
    	return getStringProperty("srv.serviceType"+i);
    }
    
    public static int addService(String name, String type){
    	int c = getServicesCount();
    	setIntProperty("srv.servicesCount", c+1);
    	setStringProperty("srv.serviceName"+c, name);
    	setStringProperty("srv.serviceType"+c, type);
    	return c;
    }
    
    public static void delService(String name) {
    	// Сдвигаем элементы после удаленного на его место
    	boolean f = false;
    	for(int i=0; i<(getServicesCount()-1); i++){
    		if(getServiceName(i).equals(name))
    			f = true;
    		if(f){
    			setStringProperty("srv.serviceName"+i, getServiceName(i+1));
    			setStringProperty("srv.serviceType"+i, getServiceType(i+1));
    		}
    	}
    	//Удаляем самый последний элемент
    	appProps.remove("srv.serviceName"+(getServicesCount()-1));
    	appProps.remove("srv.serviceType"+(getServicesCount()-1));
    	setIntProperty("srv.servicesCount", getServicesCount()-1);
    }
    
    
    public static String getServer() {
        if(currentServer.equals("")) 
            currentServer = getStringProperty("icq.serverDefault");
        return currentServer;
    }
    
    public static int getPort(){
        if(currentPort==0) 
            currentPort = getIntProperty("icq.portDefault");
        return currentPort;
    }
    
    public static void nextServer(){
        if(servers.isEmpty()) return;
        countServer++;
        if(countServer>=servers.size()) countServer=0;
        String s = servers.get(countServer).toString();
        if(s.indexOf(":")<0){ 
            currentPort = getIntProperty("icq.portDefault");
            currentServer = s;
        } else{
            currentPort = Integer.parseInt(s.split(":")[1]);
            currentServer = s.split(":")[0];
        }
    }
    
    /**
     * Загрузка списка ICQ серверов из файла
     */
    public static void loadServerList(){
        String s;
        try{
            BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream("servers.txt"),"windows-1251")); 
            while (r.ready()){
                s = r.readLine();
                if(!s.equals("")){
                    servers.add(s);
                }
            }
            r.close();
        } catch (Exception ex){
            ex.printStackTrace();
        }        
    }
    
    public static String getAbout(){
        return PROG_TITLE + " " + VERSION + "\n(c) Spec, 2006-2009\n" +
                "Поддержка проекта: http://jimbot.ru";
    }
    
    public static boolean isHide(){
        return Boolean.valueOf(getProperty("isHide","true")).booleanValue();
    }


    public static final void load() {
        String fileName = PROPS_FILE;
        File file = new File(fileName);
        setDefault();
        loadIgnorList();
        try {
            FileInputStream fi = new FileInputStream(file);
//            appProps.load(fi);
            appProps.loadFromXML(fi);
            fi.close();
            Log.info("Load preferences ok");
            loadServerList();
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.error("Error opening preferences: ");
        }
    }
    
    public static final void save() {
        String fileName = PROPS_FILE;
        File file = new File(fileName);
        try {
            FileOutputStream fo = new FileOutputStream(file);
//            appProps.store(fo,"jImBot properties");
            appProps.storeToXML(fo, "jImBot properties");
            fo.close();
            Log.info("Save preferences ok");
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.error("Error saving preferences: ");
        }
    }
    
    /**
     * Читает текстовый файл по URL
     * @param url
     * @return
     */
    public static String getStringFromHTTP(String u){
        String s = "";
        try {
            URL url = new URL(u);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty ( "User-agent", "JimBot/0.4 (Java" + 
                    "; U;" + System.getProperty("os.name") + " " + System.getProperty("os.arch") + " " + System.getProperty("os.version") +
                    "; ru; " + System.getProperty("java.vendor") + " " + System.getProperty("java.version") +
                    ")");
            BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
            byte[] b = new byte[1024];
            int count = 0;
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            while ((count=bis.read(b)) != -1)
                bout.write(b, 0, count);
            bout.close();
            bis.close();
            conn.disconnect();
            s = bout.toString("windows-1251");
        } catch (Exception ex) {
            Log.error("Ошибка HTTP при чтении новой версии", ex);
        }
        return s;
    }

    /**
     * Проверка на новую версию
     * @return
     */
    public static boolean checkNewVersion() {
        if(!getBooleanProperty("main.checkNewVer")) return false;
        if(ver_no==0)
            readNewVer();
        if((System.currentTimeMillis()-ver_last_read)>24*3600000)
            readNewVer();
        return ver_no>VER_INT;
    }
    
    /**
     * Возвращает описание новой версии
     * @return
     */
    public static String getNewVerDesc() {
        return ver_desc;
    }
    
    /**
     * Читает информацию о новой версии с сайта
     */
    private static void readNewVer() {
        String s = getStringFromHTTP("http://jimbot.ru/ver.txt");
        ver_no = VER_INT;
        ver_desc = "";
        ver_last_read = System.currentTimeMillis();
        if(s.equals("")) return;
        try {
            BufferedReader r = new BufferedReader(new StringReader(s));
            String sd = r.readLine();
            if(!sd.equals("#JimBot version file")) return;
            ver_no = Integer.parseInt(r.readLine());
            String cnt = r.readLine();
            counter(cnt);
            if(ver_no>VER_INT)
                while(r.ready()){
                    String st = r.readLine();
                    if(st==null) break;
                    if(!st.equals(""))
                        if(Integer.parseInt(st.split("#")[0])==ver_no)
                            ver_desc += st.split("#")[1] + '\n';
                }
            r.close();
        } catch (Exception ex){
            Log.error("Ошибка обработки описания новой версии",ex);
        }
    }

    private static void counter(String s){
        try {
            String u = s.substring(8);
            u = u.replaceAll("@", "chat_ver=" + VERSION);
            u = u.replaceAll(" ", "%20");
            getStringFromHTTP(u);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void registerProperties(Properties _appProps) {
        appProps = _appProps;
    }
    
    public static String getProperty(String key) {
        return appProps.getProperty(key);
    }
    
    public static String getStringProperty(String key) {
        return appProps.getProperty(key);
    }
    
    public static String getProperty(String key, String def) {
        return appProps.getProperty(key,def);
    }
    
    public static void setProperty(String key, String val) {
        appProps.setProperty(key,val);
    }
    
    public static void setStringProperty(String key, String val) {
        appProps.setProperty(key,val);
    }
    
    public static void setIntProperty(String key, int val) {
        appProps.setProperty(key,Integer.toString(val));
    }
    
    public static void setBooleanProperty(String key, boolean val) {
        appProps.setProperty(key, val ? "true":"false");
    }
    
    public static int getIntProperty(String key) {
        return Integer.parseInt(appProps.getProperty(key));
    }
    
    public static boolean getBooleanProperty(String key) {
        return Boolean.valueOf(appProps.getProperty(key)).booleanValue();
    }
}