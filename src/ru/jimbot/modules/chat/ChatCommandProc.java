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

package ru.jimbot.modules.chat;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import ru.jimbot.Messages;
import ru.jimbot.modules.AbstractCommandProcessor;
import ru.jimbot.modules.AbstractServer;
import ru.jimbot.modules.Cmd;
import ru.jimbot.modules.CommandExtend;
import ru.jimbot.modules.CommandParser;
import ru.jimbot.modules.FloodElement;
import ru.jimbot.modules.WorkScript;
import ru.jimbot.protocol.Protocol;
import ru.jimbot.util.Log;
import ru.jimbot.util.MainProps;

/**
 * Обработка команд чата
 * 
 * @author Prolubnikov Dmitry
 */
public class ChatCommandProc extends AbstractCommandProcessor {
    public ChatServer srv;
    public RobAdmin radm=null;
    private ConcurrentHashMap <String,String> up; // Запоминаем последний пришедший приват
    private ConcurrentHashMap <String, KickInfo> statKick; // Расширенная статистика киков
    private HashSet<String> warnFlag; // Флаг предупреждения о молчании
    public int state=0;
    private CommandParser parser;
    private String uid = ""; //Текущий ИД для запроса версии, отвечает тока на свои ИД
    private long new_ver_test_time = 0;
    public ChatProps psp;
    private boolean firstStartMsg = false;
    // Счетчики для контроля флуда: одинаковых сообщений, отброшеных сообщений
    private ConcurrentHashMap <String, FloodElement> floodMap, floodMap2, floodNoReg;
    private HashMap<String, CommandExtend> comMap;
    // Для хранения доступных объектов авторизации
    private HashMap<String, String> authObj = new HashMap<String, String>(22);
    // Для хранения доступных команд
    private HashMap<String, Cmd> commands = new HashMap<String, Cmd>(50);
    
    class KickInfo {
        public int id=0;
        public int len=0;
        public int moder_id=0;
        public int count=0;
        public String reason = "";
        
        KickInfo(int id, int moder_id, String r, int len) {
            this.id = id;
            this.len = len;
            this.moder_id = moder_id;
            this.reason = r;
            count = 0;
        }
        
        public int inc() {return count++;}
    }
    
    /** Creates a new instance of ChatCommandProc */
    public ChatCommandProc(ChatServer s) {
    	parser = new CommandParser(commands);
        srv = s;
        int usr= srv.us.statUsersCount();
        psp = ChatProps.getInstance(srv.getName());
        up = new ConcurrentHashMap<String, String>(usr/2);
        statKick = new ConcurrentHashMap<String, KickInfo>(srv.us.statKickCount()+2);
        floodMap = new ConcurrentHashMap <String, FloodElement>(usr);
        floodMap2 = new ConcurrentHashMap <String, FloodElement>(usr);
        floodNoReg = new ConcurrentHashMap <String, FloodElement>(usr);
        comMap = new HashMap<String, CommandExtend>(usr/2);
        warnFlag = new HashSet<String>(usr);
        init();
    }
    
    /**
     * Инициализация списка команд и полномочий
     */
    private void init(){
    	authObj.put("pmsg","Отправка приватных сообщений");
    	authObj.put("reg","Смена ника");
    	authObj.put("invite","Создание приглашения");
    	authObj.put("kickone","Кик одного пользователя");
    	authObj.put("kickall","Кик всех пользователей");
    	authObj.put("ban","Забанить пользователя");
    	authObj.put("settheme","Установить тему в комнате");
    	authObj.put("adminsay","Разговаривать с админом");
    	authObj.put("adminstat","Получать статистику от админа");
    	authObj.put("info","Получать информацию о юзере");
    	authObj.put("exthelp","Расширенная помощь");
    	authObj.put("authread","Получение инфы о полномочиях");
    	authObj.put("authwrite","Изменение полномочий пользователей");
    	authObj.put("whouser","Просмотр инфы о смене ников юзером");
    	authObj.put("room","Смена комнаты");
    	authObj.put("whoinv","Команда !whoinvite");
    	authObj.put("kickhist","Команда !kickhist");
    	authObj.put("chgkick","Изменение времени кика");
    	authObj.put("dblnick","Разрешено дублировать ник");
    	authObj.put("anyroom","Переход в любую комнату");
    	authObj.put("wroom","Создавать и изменять комнаты");
    	
    	commands.put("!help", new Cmd("!help","",2));
    	commands.put("!chat", new Cmd("!chat","",3));
    	commands.put("!exit", new Cmd("!exit","",4));
    	commands.put("!rules", new Cmd("!rules","",5));
    	commands.put("!stat", new Cmd("!stat","",6));
    	commands.put("!gofree", new Cmd("!gofree","",7));
    	commands.put("!go", new Cmd("!go","$n",8));
    	commands.put("!invite", new Cmd("!invite","",9));
    	commands.put("!banlist", new Cmd("!banlist","",10));
    	commands.put("!kicklist", new Cmd("!kicklist","",11));
    	commands.put("!info", new Cmd("!info","$c",12));
    	commands.put("!kick", new Cmd("!kick","$c $n $s",13));
    	commands.put("!kickall", new Cmd("!kickall","",14));
    	commands.put("!listauth", new Cmd("!listauth","",15));
    	commands.put("!who", new Cmd("!who", "$n",16));
    	commands.put("!listgroup", new Cmd("!listgroup","",17));
    	commands.put("!checkuser", new Cmd("!checkuser","$n",18));
    	commands.put("!setgroup", new Cmd("!setgroup","$n $c",19));
    	commands.put("!grant", new Cmd("!grant","$n $c",20));
    	commands.put("!revoke", new Cmd("!revoke","$n $c",21));
    	commands.put("!ban", new Cmd("!ban","$c $s",22));
    	commands.put("!uban", new Cmd("!uban","$c",23));
    	commands.put("!reg", new Cmd("!reg","$c $c",24));
    	commands.put("+a", new Cmd("+a","",25));
    	commands.put("+а", new Cmd("+а","",25));
    	commands.put("+f", new Cmd("+f","",25));
    	commands.put("+ф", new Cmd("+ф","",25));
    	commands.put("+p", new Cmd("+p","$n $s",26));
    	commands.put("+р", new Cmd("+р","$n $s",26));
    	commands.put("+pp", new Cmd("+pp","$s",27));
    	commands.put("+рр", new Cmd("+рр","$s",27));
    	commands.put("!settheme", new Cmd("!settheme","$s",28));
    	commands.put("!getinfo", new Cmd("!getinfo","$c",29));
    	commands.put("!room", new Cmd("!room","$n",33));
    	commands.put("!whoinvite", new Cmd("!whoinvite","$n",34));
    	commands.put("!kickhist", new Cmd("!kickhist","",35));
    	commands.put("!adm", new Cmd("!adm","$s",36));
    	commands.put("!banhist", new Cmd("!banhist","",37));
    	commands.put("+aa", new Cmd("+aa","",38));
    	commands.put("+аа", new Cmd("+аа","",38));
    	commands.put("!lroom", new Cmd("!lroom","",40));
    	commands.put("!crroom", new Cmd("!crroom","$n $s",41));
    	commands.put("!chroom", new Cmd("!chroom","$n $s",42));
    	WorkScript.getInstance(srv.getName()).installAllChatCommandScripts(this);
    	commands.put("!about", new Cmd("!about","",1));
    }
    
    /**
     * Выдает список полномочий для работы
     * @return
     */
    public HashMap<String, String> getAuthObjects(){
    	return this.authObj;
    }
    
    /**
     * Выдает список команд
     * @return
     */
    public HashMap<String, Cmd> getCommands(){
    	return this.commands;
    }
    
    /**
     * Добавление нового объекта полномочий
     * @param name
     * @param comment
     * @return - истина, если такой объект уже был в таблице
     */
    public boolean addAuth(String name, String comment){
    	boolean f = authObj.containsKey(name);
    	authObj.put(name, comment);
    	return f;
    }
    
    /**
     * Добавление новой команды
     * @param name
     * @param c
     * @return - истина, если команда уже существует
     */
    public boolean addCommand(String name, Cmd c) {
    	boolean f = commands.containsKey(name);
    	commands.put(name, c);
    	return f;
    }
    
    /**
     * Возвращает экземпляр парсера
     * @return
     */
    public CommandParser getParser(){
    	return parser;
    }
    
    /**
     * Проверка на первое сообщение. Выдает админам извещение о запуске чата.
     * @param proc
     */
    private void firstMsg(Protocol proc){
    	if(!firstStartMsg){
    		String[] s = srv.getProps().getAdmins();
    		for(int i=0;i<s.length;i++){
    		    String ss = "Бот успешно запущен!\n";
    		    if(MainProps.checkNewVersion())
    		        ss += "На сайте http://jimbot.ru Доступна новая версия!\n" + MainProps.getNewVerDesc();
    		    else
    		        ss += "Вся информация о боте из первых рук только на сайте: http://jimbot.ru";
    			proc.mq.add(s[i], ss);
    		}
    		firstStartMsg=true;
    	}
    }
    
    public AbstractServer getServer(){
    	return srv;
    }
    
    /**
     * Основная процедура парсера команд
     */
    public void parse(Protocol proc, String uin, String mmsg) {
    	if(radm==null){
    		radm = new RobAdmin(srv);
    		radm.start();
    	}
    	firstMsg(proc);
        state++;
        Log.debug("CHAT: parse " + proc.baseUin + ", " + uin + ", " + mmsg); 
        if(psp.getBooleanProperty("chat.writeInMsgs"))
            if(mmsg.length()>1000){
                //Слишком длинные сообщения записывать в БД не нужно для избежания переполнений
                srv.us.db.log(0,uin,"IN",mmsg.substring(0, 1000),0);
            } else {
                srv.us.db.log(0,uin,"IN",mmsg,0);
            }
        String tmsg = mmsg.trim();
        if(tmsg.length()==0){
        	Log.error("Пустое сообщение в парсере команд: " + uin + ">" + mmsg);
        	return;
        }
        if(tmsg.charAt(0)=='!' || tmsg.charAt(0)=='+'){
            Log.info("CHAT COM_LOG: " + uin + ">>" + tmsg);
        }
        try {           
                if(srv.us.testUser(uin)){
                    if(isBan(uin)){
                        Log.flood2("CHAT_BAN: " + uin + ">" + mmsg);
                        return;
                    }
                    if(testKick(uin)>0){
                        infrequentSend(proc,uin,Messages.getString("ChatCommandProc.parse.7", new Object[] {testKick(uin)}));
                        Log.info("CHAT_KICK: " + uin + ">" + mmsg);
                        return;
                    }
                    if(srv.us.getUser(uin).state==UserWork.STATE_CHAT ||
                            srv.us.getUser(uin).state==UserWork.STATE_OFFLINE) goChat(proc, uin);
                } else {
                	// Для нового юзера
                	// Проверка на флуд
                	if(floodNoReg.containsKey(uin)){
                		FloodElement e = floodNoReg.get(uin);
                		if(e.getDeltaTime()<(psp.getIntProperty("chat.floodTimeLimitNoReg")*1000)){
                			e.addMsg(tmsg);
                			floodNoReg.put(uin, e);
                			Log.flood("FLOOD NO REG " + uin + "> " + tmsg);
                			return; // Слишком часто
                		}
                		if(e.isDoubleMsg(tmsg) && e.getCount()>3){
                			e.addMsg(tmsg);
                			floodNoReg.put(uin, e);
                			Log.flood("FLOOD NO REG " + uin + "> " + tmsg);
                			return; // Повтор сообщений
                		}
                		e.addMsg(tmsg);
            			floodNoReg.put(uin, e);
                	} else {
                		FloodElement e = new FloodElement(psp.getIntProperty("chat.floodTimeLimitNoReg")*1000);
                		floodNoReg.put(uin, e);
                	}
                	if(tmsg.charAt(0)!='!' && !comMap.containsKey(uin)){
                		// Если это не команда - выводим приветствие, иначе обрабатываем команду
                		if(!psp.getBooleanProperty("chat.FreeReg")){
//                			proc.mq.add(uin,"Добро пожаловать в чат!\n" +
//                					"Для помощи пошлите команду !help\n" +
//                					"Не посылайте одинаковых сообщений и сообщения чаще " +
//                					psp.getIntProperty("chat.floodTimeLimitNoReg") + " сек.\n" +
//                					psp.getStringProperty("chat.inviteDescription"));
                			proc.mq.add(uin,Messages.getString("ChatCommandProc.parse.0", 
                			        new Object[] {psp.getIntProperty("chat.floodTimeLimitNoReg"),
                			        psp.getStringProperty("chat.inviteDescription")}));
                		} else {
//                			proc.mq.add(uin,"Добро пожаловать в чат! \n" +
//                					"Чтобы зарегистрироваться используйте команду !reg <ник>\n"+
//                					"Для помощи пошлите команду !help\n" +
//                					"Не посылайте одинаковых сообщений и сообщения чаще " +
//                					psp.getIntProperty("chat.floodTimeLimitNoReg") + " сек.\n" );
                			proc.mq.add(uin,Messages.getString("ChatCommandProc.parse.1", 
                                    new Object[] {psp.getIntProperty("chat.floodTimeLimitNoReg")}));
                		}
                		return;
                	}
                }
//            checkNewVersion(proc);
            // Проверка на флуд
            if(floodMap.containsKey(uin)){
            	FloodElement e = floodMap.get(uin);
            	e.addMsg(tmsg);
            	floodMap.put(uin, e);
            } else {
            	FloodElement e = new FloodElement(psp.getIntProperty("chat.floodTimeLimit")*1000);
            	e.addMsg(tmsg);
            	floodMap.put(uin, e);
            }
            testFlood(proc,uin);
            mmsg = WorkScript.getInstance(srv.getName()).startMessagesScript(mmsg, srv);
            if(mmsg.isEmpty()) return; // Сообщение было удалено в скрипте
            int tp = 0;
            if(comMap.containsKey(uin) && srv.getProps().getBooleanProperty("chat.useCaptcha"))
            	if(!comMap.get(uin).isExpire())
            		tp = parser.parseCommand(comMap.get(uin).getCmd());
            	else {
            		tp = parser.parseCommand(tmsg);
            		comMap.remove(uin);
            	}
            else
            	tp = parser.parseCommand(tmsg);
            int tst=0;
            if(tp<0)
                tst=0;
            else
            	tst = tp;
            switch (tst){
            case 1:
                proc.mq.add(uin,MainProps.getAbout());
                break;
            case 2:
                commandHelp(proc, uin);
                break;
            case 3:
                goChat(proc, uin);
                if(psp.getBooleanProperty("chat.getUserInfoOnChat"))
                    proc.mq.add(uin, "", 1); //proc.recUserInfo(uin,"0");
                break;
            case 4:
                exitChat(proc, uin);
                break;
            case 5:
            	if(!isChat(proc,uin) && !psp.testAdmin(uin)) break;
                proc.mq.add(uin,psp.getChatRules());
                break;
            case 6:
            	if(!psp.testAdmin(uin)) break;
                proc.mq.add(uin,srv.us.getUinStat());
                break;
            case 7:
            	if(!isChat(proc,uin) && !psp.testAdmin(uin)) break;
                commandGofree(proc, uin);
                break;
            case 8:
            	//TODO Выделить объект полномочий
            	if(!psp.testAdmin(uin)) break;
                commandGo(proc, uin, parser.parseArgs(tmsg));
                break;
            case 9:
                commandInvite(proc, uin);
                break;
            case 10:
                if(!isChat(proc,uin) && !psp.testAdmin(uin)) break;
                if(!auth(proc,uin, "ban")) return;
                proc.mq.add(uin, srv.us.listUsers());
                break;
            case 11:
                if(!isChat(proc,uin) && !psp.testAdmin(uin)) break;
                if(!auth(proc,uin, "kickone")) return;
                proc.mq.add(uin, listKickUsers());
                break;
            case 12:
                commandInfo(proc, uin, parser.parseArgs(tmsg));
                break;
            case 13:
                commandKick(proc, uin, parser.parseArgs(tmsg));
                break;
            case 14:
                if(!isChat(proc,uin)) break;
                if(!auth(proc,uin, "kickall")) return;
                try{
                    kickAll(proc, uin);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                break;
            case 15:
                if(!isChat(proc,uin) && !psp.testAdmin(uin)) break;
                if(!auth(proc,uin, "authread")) return;
                proc.mq.add(uin,listAuthObjects());
                break;
            case 16:
                commandWho(proc, uin, parser.parseArgs(tmsg));
                break;
            case 17:
                if(!isChat(proc,uin) && !psp.testAdmin(uin)) break;
                if(!auth(proc,uin, "authread")) return;
                proc.mq.add(uin,psp.getStringProperty("auth.groups"));
                break;
            case 18:
                commandCheckuser(proc, uin, parser.parseArgs(tmsg));
                break;
            case 19:
                commandSetgroup(proc, uin, parser.parseArgs(tmsg));
                break;
            case 20:
                commandGrant(proc, uin, parser.parseArgs(tmsg));
                break;
            case 21:
                commandRevoke(proc, uin, parser.parseArgs(tmsg));
                break;
            case 22:
                commandBan(proc, uin, parser.parseArgs(tmsg));
                break;
            case 23:
                commandUban(proc, uin, parser.parseArgs(tmsg));
                break;
            case 24:
                commandReg(proc, uin, parser.parseArgs(tmsg), mmsg);
                break;
            case 25:
                commandA(proc, uin);
                break;
            case 26:
                commandP(proc, uin, parser.parseArgs(tmsg), mmsg);
                break;
            case 27:
                commandPP(proc, uin, parser.parseArgs(tmsg), mmsg);
                break;
            case 28:
                commandSettheme(proc, uin, parser.parseArgs(tmsg));
                break;
            case 29:
                commandGetinfo(proc, uin, parser.parseArgs(tmsg));
                break;
            case 30:
//                commandGetversion(proc, uin, parser.parseArgs(tmsg));
                break;
            case 31:
//                commandNewversion(proc, uin, parser.parseArgs(tmsg));
                break;
//            case 32:
//                commandExec(proc, uin, parser.parseArgs(tmsg));
//                break;
            case 33:
                commandRoom(proc, uin, parser.parseArgs(tmsg));
                break;
            case 34: 
                commandWhoinvite(proc, uin, parser.parseArgs(tmsg));
                break;
            case 35:
                commandKickhist(proc, uin);
                break;
            case 36:
                commandAdm(proc, uin, parser.parseArgs(tmsg));
                break;
            case 37:
                commandBanhist(proc, uin);
                break;
            case 38:
                commandAA(proc, uin);
                break;
            case 39: // Обработка команды макросом
            	String ret = WorkScript.getInstance(srv.getName()).startChatCommandScript(parser.parseCommand2(tmsg).script, tmsg, uin, proc, this);
            	if(!ret.isEmpty() && !ret.equals("ok")) proc.mq.add(uin,Messages.getString("ChatCommandProc.parse.2"));
            	break;
            case 40:
            	commandLRoom(proc,uin);
            	break;
            case 41:
                commandCrRoom(proc, uin, parser.parseArgs(tmsg));
                break;
            case 42:
                commandChRoom(proc, uin, parser.parseArgs(tmsg));
                break;
            default:
                if(srv.us.getUser(uin).state==UserWork.STATE_CHAT){
                    //Сообщения начинающиеся с "!" и "+" не выводим в чат
                    try{
                        if(mmsg.substring(0, 1).equals("!")){
                            proc.mq.add(uin,Messages.getString("ChatCommandProc.parse.3"));
                            return;
                        }
                    } catch (Exception ex){
                        ex.printStackTrace();
                    }
                    String s = "";
//                    if(auth(proc,uin, "idview")) s += "<"+srv.us.getUser(uin).id+"> ";
                    if(mmsg.indexOf("/me")==0)
                        s = mmsg.replaceFirst("/me", "*" + srv.us.getUser(uin).localnick);
                    else
                        s += srv.us.getUser(uin).localnick + psp.getStringProperty("chat.delimiter") 
                                +" " + mmsg;
                    if(s.length()>psp.getIntProperty("chat.MaxMsgSize")){
                        s = s.substring(0,psp.getIntProperty("chat.MaxMsgSize"));
//                        proc.mq.add(uin,"Слишком длинное сообщение было обрезано: " + s);
                        proc.mq.add(uin,Messages.getString("ChatCommandProc.parse.4", new Object[] {s}));
                    }
                    s = s.replace('\n',' ');
                    s = s.replace('\r',' ');
                    Log.talk("CHAT: " + uin + "<" + srv.us.getUser(uin).id +"> ["+srv.us.getUser(uin).room +"]>>" + s);
                    srv.us.db.log(srv.us.getUser(uin).id,uin,"OUT", s, srv.us.getUser(uin).room);
                    srv.cq.addMsg(s, uin, srv.us.getUser(uin).room);
                    if(psp.getBooleanProperty("adm.useAdmin")) radm.parse(proc,uin,s,srv.us.getUser(uin).room);
                } else {
                    if(srv.us.getUser(uin).state==UserWork.STATE_NO_CHAT){
//                        proc.mq.add(uin,"Для входа в чат используйте команду !chat. Для помощи пошлите команду !help\n" +
//                                "Не посылайте ваши сообщения слишком часто.");
                        proc.mq.add(uin, Messages.getString("ChatCommandProc.parse.5"));
                    } else {
//                        proc.mq.add(uin,"Для входа в чат необходимо зарегистрироваться командой !reg. \n" +
//                                "Для помощи пошлите команду !help\n" +
//                                "Не посылайте ваши сообщения слишком часто.");
                        proc.mq.add(uin, Messages.getString("ChatCommandProc.parse.6"));
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * Команды чата
     */
    
    /**
     * !help
     */
    public void commandHelp(Protocol proc, String uin){
        String[] s = psp.getHelp1().split("<br>");
        for(int i=0;i<s.length;i++){
            proc.mq.add(uin,s[i]);
        }        
        if(srv.us.authorityCheck(uin, "exthelp")){
            s = psp.getHelp2().split("<br>");
            for(int i=0;i<s.length;i++){
                proc.mq.add(uin,s[i]);
            }
        }        
    }
    
    /**
     * !gofree
     * @param proc
     * @param uin
     */
    public void commandGofree(Protocol proc, String uin){
        try{
            String s = srv.us.getFreeUin();
            changeBaseUin(uin,s);
//            proc.mq.add(uin,"Успешно завершено. Сообщения теперь будут приходить с номера " + s);
            proc.mq.add(uin, Messages.getString("ChatCommandProc.commandGofree.0", new Object[] {s}));
        }catch (Exception ex){
            ex.printStackTrace();
//            proc.mq.add(uin,"Ошибка "+ex.getMessage());
            proc.mq.add(uin, Messages.getString("ChatCommandProc.err", new Object[] {ex.getMessage()}));
        }        
    }
    
    /**
     * !go
     * @param proc
     * @param uin
     * @param v
     */
    public void commandGo(Protocol proc, String uin, Vector v){
        try{
            int k = (Integer)v.get(0);
            if(k>=psp.uinCount() || k<0){
//                proc.mq.add(uin,"Ошибочный номер");
                proc.mq.add(uin, Messages.getString("ChatCommandProc.commandGo.0"));
                return;
            }
            changeBaseUin(uin,psp.getUin(k));
//            proc.mq.add(uin,"Успешно завершено. Сообщения теперь будут приходить с номера " + psp.getUin(k));
            proc.mq.add(uin, Messages.getString("ChatCommandProc.commandGo.1", new Object[] {psp.getUin(k)}));
        } catch (Exception ex){
            ex.printStackTrace();
            proc.mq.add(uin, Messages.getString("ChatCommandProc.err", new Object[] {ex.getMessage()}));
        }    
    }

    /**
     * !invite
     * @param proc
     * @param uin
     */
    public void commandInvite(Protocol proc, String uin){
        if(!isChat(proc,uin) && !psp.testAdmin(uin)) return;
        if(psp.getBooleanProperty("chat.FreeReg")){
//            proc.mq.add(uin,"Чат открыт для свободного входа, приглашения создавать не нужно.");
            proc.mq.add(uin, Messages.getString("ChatCommandProc.commandInvite.0"));
            return;
        }
        if(!auth(proc,uin, "invite")) return;
        String s = srv.us.createInvite(srv.us.getUser(uin).id);
        if(s.isEmpty()) {
//            proc.mq.add(uin,"Не удалось создать новое приглашение, возможно вы не использовали еще старое приглашение.");
            proc.mq.add(uin, Messages.getString("ChatCommandProc.commandInvite.1"));
        } else {
//            proc.mq.add(uin,"Создано новое приглашение: " + s + 
//                    "\nСрок действия, часов: " + psp.getIntProperty("chat.MaxInviteTime"));
            proc.mq.add(uin, Messages.getString("ChatCommandProc.commandInvite.2", new Object[] {s,psp.getIntProperty("chat.MaxInviteTime")}));
        }        
    }
    
    /**
     * !info
     * @param proc
     * @param uin
     * @param v
     */
    public void commandInfo(Protocol proc, String uin, Vector v){
        if(!auth(proc,uin, "info")) return;
        try {
            String s = (String)v.get(0);
            if(s.length()>=6){
                proc.mq.add(uin,srv.us.getUserInfo(s));
            } else {
                try{
                    proc.mq.add(uin,srv.us.getUserInfo(Integer.parseInt(s)));
                } catch (Exception ex){
//                    proc.mq.add(uin,"Ошибка в команде");
                    proc.mq.add(uin, Messages.getString("ChatCommandProc.err.1"));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            proc.mq.add(uin, Messages.getString("ChatCommandProc.err", new Object[] {ex.getMessage()}));
        }        
    }
    
    /**
     * !kick
     * @param proc
     * @param uin
     * @param v
     */
    public void commandKick(Protocol proc, String uin, Vector v){
        if(!isChat(proc,uin) && !psp.testAdmin(uin)) return;
        if(!auth(proc,uin, "kickone")) return;
        try{
            int moder_id = srv.us.getUser(uin).id;
            String s = (String)v.get(0);
            int t = (Integer)v.get(1);
//            if(t<=0){
//                proc.mq.add(uin,"Время кика должно быть больше нуля");
//                return;
//            }
            String r = (String)v.get(2);
            int id=0;
            try{
                id = Integer.parseInt(s);
            } catch (Exception ex){
                proc.mq.add(uin, Messages.getString("ChatCommandProc.err.1"));
                return;
            }
            String i = srv.us.getUser(id).sn;
            if(testKick(i)>0 && !auth(proc,uin, "chgkick")){
//                proc.mq.add(uin,"Вы не можете изменить время кика");
                proc.mq.add(uin, Messages.getString("ChatCommandProc.commandKick.0"));
                return;
            }
//            if(!i.equals("")) {
                if(t==0){
                    tkick(proc, i, psp.getIntProperty("chat.defaultKickTime"), moder_id,"");
//                    proc.mq.add(uin,"Юзер выпнут на: " + testKick(i));
                    proc.mq.add(uin, Messages.getString("ChatCommandProc.commandKick.1", new Object[] {testKick(i)}));
                } else {
                    if(r.isEmpty()){
//                        proc.mq.add(uin,"Необходимо добавить причину кика");
                        proc.mq.add(uin, Messages.getString("ChatCommandProc.commandKick.2"));
                        return;
                    }
                    if(t>psp.getIntProperty("chat.maxKickTime"))
                        t=psp.getIntProperty("chat.maxKickTime");
                    tkick(proc, i, t, moder_id, r);
//                    proc.mq.add(uin,"Юзер выпнут на: " + t);
                    proc.mq.add(uin, Messages.getString("ChatCommandProc.commandKick.1", new Object[] {t}));
                }
//            }
        } catch (Exception ex) {
            ex.printStackTrace();
            proc.mq.add(uin, Messages.getString("ChatCommandProc.err", new Object[] {ex.getMessage()}));
        }        
    }
    
    /**
     * !who
     * @param proc
     * @param uin
     * @param v
     */
    public void commandWho(Protocol proc, String uin, Vector v){
        if(!isChat(proc,uin) && !psp.testAdmin(uin)) return;
        if(!auth(proc,uin, "whouser")) return;
        try{
            int id = (Integer)v.get(0);
            proc.mq.add(uin, srv.us.getUserNicks(id));
        } catch (Exception ex){
            ex.printStackTrace();
            proc.mq.add(uin, Messages.getString("ChatCommandProc.err", new Object[] {ex.getMessage()}));
        }                        
    }
    
    /**
     * !checkuser
     * @param proc
     * @param uin
     * @param v
     */
    public void commandCheckuser(Protocol proc, String uin, Vector v){
        if(!isChat(proc,uin) && !psp.testAdmin(uin)) return;
        if(!auth(proc,uin, "authread")) return;
        try{
            int id = (Integer)v.get(0);
            proc.mq.add(uin, srv.us.getUserAuthInfo(id));
        } catch (Exception ex){
            ex.printStackTrace();
            proc.mq.add(uin, Messages.getString("ChatCommandProc.err", new Object[] {ex.getMessage()}));
        }        
    }
    
    /**
     * !setgroup
     * @param proc
     * @param uin
     * @param v
     */
    public void commandSetgroup(Protocol proc, String uin, Vector v){
        if(!isChat(proc,uin) && !psp.testAdmin(uin)) return;
        if(!auth(proc,uin, "authwrite")) return;
        try{
            String s1 = (String)v.get(1);
            int id = (Integer)v.get(0);
            Users uss = srv.us.getUser(id);
            if(uss.id!=id){
//                proc.mq.add(uin,"Пользователь не найден");
                proc.mq.add(uin, Messages.getString("ChatCommandProc.commandSetgroup.0"));
                return;
            }
            if(!testUserGroup(s1)){
//                proc.mq.add(uin,"Нет такой группы пользователей");
                proc.mq.add(uin, Messages.getString("ChatCommandProc.commandSetgroup.1"));
                return;
            }
            uss.group = s1;
            boolean f = srv.us.setUserPropsValue(id, "group", s1) &&
                    srv.us.setUserPropsValue(id, "grant", "") &&
                    srv.us.setUserPropsValue(id, "revoke", "");
            srv.us.clearCashAuth(id);
            if(f)
                proc.mq.add(uin, Messages.getString("ChatCommandProc.commandSetgroup.2")); //proc.mq.add(uin,"Успешно завершено");
            else
                proc.mq.add(uin, Messages.getString("ChatCommandProc.commandSetgroup.3")); //proc.mq.add(uin,"Произошла ошибка");
        } catch (Exception ex){
            ex.printStackTrace();
            proc.mq.add(uin, Messages.getString("ChatCommandProc.err", new Object[] {ex.getMessage()}));
        }        
    }
    
    /**
     * !grant
     * @param proc
     * @param uin
     * @param v
     */
    public void commandGrant(Protocol proc, String uin, Vector v){
        if(!isChat(proc,uin) && !psp.testAdmin(uin)) return;
        if(!auth(proc,uin, "authwrite")) return;
        try{
            String s1 = (String)v.get(1);
            int id = (Integer)v.get(0);
            Users uss = srv.us.getUser(id);
            if(uss.id!=id){
//                proc.mq.add(uin,"Пользователь не найден");
                proc.mq.add(uin, Messages.getString("ChatCommandProc.commandSetgroup.0"));
                return;
            }
            if(!testAuthObject(s1)){
//                proc.mq.add(uin,"Нет такого объекта полномочий");
                proc.mq.add(uin, Messages.getString("ChatCommandProc.commandGrant.0"));
                return;
            }
            if(srv.us.grantUser(id, s1))
                proc.mq.add(uin, Messages.getString("ChatCommandProc.commandSetgroup.2")); //proc.mq.add(uin,"Успешно завершено");
            else
                proc.mq.add(uin, Messages.getString("ChatCommandProc.commandSetgroup.3")); //proc.mq.add(uin,"Произошла ошибка");
        } catch (Exception ex){
            ex.printStackTrace();
            proc.mq.add(uin, Messages.getString("ChatCommandProc.err", new Object[] {ex.getMessage()}));
        }        
    }
    
    /**
     * !revoke
     * @param proc
     * @param uin
     * @param v
     */
    public void commandRevoke(Protocol proc, String uin, Vector v){
        if(!isChat(proc,uin) && !psp.testAdmin(uin)) return;
        if(!auth(proc,uin, "authwrite")) return;
        try{
            String s1 = (String)v.get(1);
            int id = (Integer)v.get(0);
            Users uss = srv.us.getUser(id);
            if(uss.id!=id){
//                proc.mq.add(uin,"Пользователь не найден");
                proc.mq.add(uin, Messages.getString("ChatCommandProc.commandSetgroup.0"));
                return;
            }
            if(!testAuthObject(s1)){
//                proc.mq.add(uin,"Нет такого объекта полномочий");
                proc.mq.add(uin, Messages.getString("ChatCommandProc.commandGrant.0"));
                return;
            }
            if(srv.us.revokeUser(id, s1))
                proc.mq.add(uin, Messages.getString("ChatCommandProc.commandSetgroup.2")); //proc.mq.add(uin,"Успешно завершено");
            else
                proc.mq.add(uin, Messages.getString("ChatCommandProc.commandSetgroup.3")); //proc.mq.add(uin,"Произошла ошибка");
        } catch (Exception ex){
            ex.printStackTrace();
            proc.mq.add(uin, Messages.getString("ChatCommandProc.err", new Object[] {ex.getMessage()}));
        }                
    }
    
    /**
     * !ban
     * @param proc
     * @param uin
     * @param v
     */
    public void commandBan(Protocol proc, String uin, Vector v){
        if(!isChat(proc,uin) && !psp.testAdmin(uin)) return;
        if(!auth(proc,uin, "ban")) return;
        try{
            String s = (String)v.get(0);
            String m = (String)v.get(1);
            String i="";
            if(s.length()>=6){
                if(uin.equals(s)){
//                    proc.mq.add(uin,"Нельзя отправить в баню самого себя :)");
                    proc.mq.add(uin, Messages.getString("ChatCommandProc.commandBan.0"));
                    return;
                }
                if(m.isEmpty()){
//                    proc.mq.add(uin,"Необходимо добавить причину бана");
                    proc.mq.add(uin, Messages.getString("ChatCommandProc.commandBan.1"));
                    return;
                }
                ban(proc, s, uin,m);
            } else {
                int id = 0;
                try {
                    id = Integer.parseInt(s);
                } catch(Exception ex) {
//                    proc.mq.add(uin,"Ошибка в команде");
                    proc.mq.add(uin, Messages.getString("ChatCommandProc.err.1"));
                    return;
                }
                i = srv.us.getUser(id).sn;
                if(!i.isEmpty()) {
                    if(uin.equals(i)){
//                        proc.mq.add(uin,"Нельзя отправить в баню самого себя :)");
                        proc.mq.add(uin, Messages.getString("ChatCommandProc.commandBan.0"));
                        return;
                    }   
                    if(m.isEmpty()){
//                        proc.mq.add(uin,"Необходимо добавить причину бана");
                        proc.mq.add(uin, Messages.getString("ChatCommandProc.commandBan.1"));
                        return;
                    }
                    ban(proc, i, uin,m);
                }
            }   
//            proc.mq.add(uin,"Пользователь " + i + " успешно отправлен в баню");
            proc.mq.add(uin, Messages.getString("ChatCommandProc.commandBan.2", new Object[] {i}));
        } catch (Exception ex) {
            ex.printStackTrace();
            proc.mq.add(uin, Messages.getString("ChatCommandProc.err", new Object[] {ex.getMessage()}));
        }        
    }
    
    /**
     * !uban
     * @param proc
     * @param uin
     * @param v
     */
    public void commandUban(Protocol proc, String uin, Vector v){
        if(!isChat(proc,uin) && !psp.testAdmin(uin)) return;
        if(!auth(proc,uin, "ban")) return;
        try{
            String s = (String)v.get(0);
            String i="";
            if(s.length()>=6){
                uban(proc, s, uin);
            } else {
                int id = 0;
                try {
                    id = Integer.parseInt(s);
                } catch (Exception e) {
//                    proc.mq.add(uin,"Ошибка в команде");
                    proc.mq.add(uin, Messages.getString("ChatCommandProc.err.1"));
                }
                i = srv.us.getUser(id).sn;
                if(!i.isEmpty()) uban(proc, i, uin);
//                proc.mq.add(uin,"Пользователь " + i + " был выпущен из бани");
                proc.mq.add(uin, Messages.getString("ChatCommandProc.commandUban.0", new Object[]{i}));
            }            
        } catch (Exception ex) {
            ex.printStackTrace();
            proc.mq.add(uin, Messages.getString("ChatCommandProc.err", new Object[] {ex.getMessage()}));
        }        
    }
    
    /**
     * !reg
     * @param proc
     * @param uin
     * @param v
     * @param mmsg
     */
    public void commandReg(Protocol proc, String uin, Vector v, String mmsg){
        try{
        	boolean twoPart = false; // Второй заход в процедуру после ответа?
        	if(srv.getProps().getBooleanProperty("chat.useCaptcha") && comMap.containsKey(uin)){
        		if(comMap.get(uin).getMsg().equalsIgnoreCase(mmsg)){
        			twoPart=true;
        			v = comMap.get(uin).getData();
        			comMap.remove(uin);
        		} else {
//        			proc.mq.add(uin,"Вы неправильно ответили на проверочный вопрос, попытайтесь зарегистрироваться еще раз.");
        		    proc.mq.add(uin, Messages.getString("ChatCommandProc.commandReg.0"));
        			comMap.remove(uin);
        			return;
        		}
        	}
            int maxNick = psp.getIntProperty("chat.maxNickLenght");
            String lnick = (String)v.get(0);
            Users uss = srv.us.getUser(uin);
            if (lnick.isEmpty() || lnick.equals(" ")){
//                proc.mq.add(uin,"ошибка регистрации, пустой ник");
                proc.mq.add(uin, Messages.getString("ChatCommandProc.commandReg.1"));
                Log.talk(uin + " Reg error: " + mmsg);
                return;
            }
            if(lnick.length()>maxNick) {
                lnick = lnick.substring(0,maxNick);
//                proc.mq.add(uin,"Предупреждение! Ваш ник слишком длинный и будет обрезан.");
                proc.mq.add(uin, Messages.getString("ChatCommandProc.commandReg.2"));
            }
            if(!testNick(uin,lnick)){
//                proc.mq.add(uin,"Ошибочный ник, попытайтесь еще раз");
                proc.mq.add(uin, Messages.getString("ChatCommandProc.commandReg.3"));
                return;
            }
            lnick = lnick.replace('\n',' ');
            lnick = lnick.replace('\r',' ');
            if(psp.getBooleanProperty("chat.isUniqueNick") && !qauth(proc,uin,"dblnick") && !psp.testAdmin(uin))
            	if(srv.us.isUsedNick(lnick)){
//            		proc.mq.add(uin,"Такой ник уже существует. Попробуйте другой ник");
            		proc.mq.add(uin, Messages.getString("ChatCommandProc.commandReg.4"));
            		return;
            	}
            String oldNick = uss.localnick;
            //смена ника - юзер уже в чате, пароль не нужен
            if(uss.state!=UserWork.STATE_NO_REG) {
                if(!auth(proc,uin, "reg")) return;
                if(uss.state!=UserWork.STATE_CHAT) return; // Менять ник тока в чате
                if(srv.us.getCountNickChange(uss.id)>psp.getIntProperty("chat.maxNickChanged")){
//                	proc.mq.add(uin,"Вы не можете так часто менять ник.");
                	proc.mq.add(uin, Messages.getString("ChatCommandProc.commandReg.5"));
            		return;
                }
                if(oldNick.equals(lnick)){
                    if(uss.state==UserWork.STATE_NO_CHAT)
                        proc.mq.add(uin, Messages.getString("ChatCommandProc.commandReg.6")); //proc.mq.add(uin,"Ник не изменен. Для входа в чат используйте команду !chat");
                    else
                        proc.mq.add(uin, Messages.getString("ChatCommandProc.commandReg.7")); //proc.mq.add(uin,"Ник не изменен.");
                    return;
                }
                uss.localnick = lnick;
                Log.talk(uin + " update " + mmsg);
//                proc.mq.add(uin,"Обновление завершено");
                proc.mq.add(uin, Messages.getString("ChatCommandProc.commandReg.8"));
//                srv.cq.addMsg(oldNick + " сменил ник на " + lnick, "", uss.room); //Сообщение для всех
                srv.cq.addMsg(Messages.getString("ChatCommandProc.commandReg.9", new Object[] {oldNick, lnick}), "", uss.room);
                srv.us.db.log(uss.id,uin,"REG",lnick,uss.room);
                srv.us.db.event(uss.id, uin, "REG", 0, "", lnick);
                uss.basesn = proc.baseUin;
                srv.us.updateUser(uss);
                return;
            }
            if(!testNick(uin,lnick)){
//                proc.mq.add(uin,"Ошибочный ник, попытайтесь еще раз");
                proc.mq.add(uin, Messages.getString("ChatCommandProc.commandReg.3"));
                return;
            }
            // Свободная регистрация
            if(psp.getBooleanProperty("chat.FreeReg") ||
                    psp.testAdmin(uin)){
            	if(srv.getProps().getBooleanProperty("chat.useCaptcha") && !twoPart){
                	String s = getCaptcha();
//                	proc.mq.add(uin,"Для подтверждения того что вы человек, напишите ответ на несложный пример. " +
//                			"Время на раздумье 5 минут (перед ответом учите общую паузу для сообщений): " + s.split("=")[0] + "=");
                	proc.mq.add(uin, Messages.getString("ChatCommandProc.commandReg.10", new Object[] {s.split("=")[0]}));
                	comMap.put(uin, new CommandExtend(uin, mmsg, s.split("=")[1],v, 5*60000));
                	return;
                }
                uss.state=UserWork.STATE_NO_CHAT;
                uss.basesn = proc.baseUin;
                uss.localnick = lnick;
                int id = srv.us.addUser(uss);
                proc.mq.add(uin, "", 1);
                Log.talk(uin + " Reg new user: " + mmsg);
                srv.us.db.log(id,uin,"REG",lnick,uss.room);
                srv.us.db.event(id, uin, "REG", 0, "", lnick);
//                proc.mq.add(uin,"Регистрация завершена, вход pfв чат по команде !chat");
                proc.mq.add(uin, Messages.getString("ChatCommandProc.commandReg.11"));
                return;
            }
            // Регистрация по приглашению
            String inv = (String)v.get(1);
            if(inv.isEmpty()){
                Log.talk(uin + " Reg error: " + mmsg);
                proc.mq.add(uin,Messages.getString("ChatCommandProc.commandReg.12") + "\n" +
                		psp.getStringProperty("chat.inviteDescription"));
                return;                        
            }
            if(srv.us.testInvite(inv)){
                if(!srv.us.updateInvite(uin,inv)){
                    proc.mq.add(uin,Messages.getString("ChatCommandProc.commandReg.13") + "\n" +
                            psp.getStringProperty("chat.inviteDescription"));
                    Log.talk(uin + " Reg error: " + mmsg);
                } else {
                	if(srv.getProps().getBooleanProperty("chat.useCaptcha") && !twoPart){
                    	String s = getCaptcha();
//                    	proc.mq.add(uin,"Для подтверждения того что вы человек, напишите ответ на несложный пример. " +
//                    			"Время на раздумье 1 минута: " + s.split("=")[0] + "=");
                    	proc.mq.add(uin, Messages.getString("ChatCommandProc.commandReg.10", new Object[] {s.split("=")[0]}));
                    	comMap.put(uin, new CommandExtend(uin, mmsg, s.split("=")[1],v, 60000));
                    	return;
                    }
                    uss.state=UserWork.STATE_NO_CHAT;
                    uss.basesn = proc.baseUin;
                    uss.localnick = lnick;
                    int id = srv.us.addUser(uss);
                    srv.us.updateInvite(uin,inv);// До этого ИД юзера был неизвестен!!!
                    proc.mq.add(uin, "", 1);
                    Log.talk(uin + " Reg new user: " + mmsg);
//                    proc.mq.add(uin,"Регистрация завершена, вход в чат по команде !chat");
                    proc.mq.add(uin, Messages.getString("ChatCommandProc.commandReg.11"));
                    srv.us.db.log(id,uin,"REG",lnick, uss.room);
                    srv.us.db.event(id, uin, "REG", 0, "", lnick);
                }
            } else {
                Log.talk(uin + " Reg error: " + mmsg);
                proc.mq.add(uin, Messages.getString("ChatCommandProc.commandReg.14") + "\n" +
                		psp.getStringProperty("chat.inviteDescription"));//"Для регистрации в чате вам необходимо получить приглашение одного из пользователей.");
                return;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.talk(uin + " Reg error: " + mmsg);
            proc.mq.add(uin,Messages.getString("ChatCommandProc.commandReg.13"));
        }        
    }
    
    /**
     * +a
     * @param proc
     * @param uin
     */
    public void commandA(Protocol proc, String uin){
        if(!isChat(proc,uin) && !psp.testAdmin(uin)) return;
        int room = srv.us.getUser(uin).room;
//        String s = "Комната: " +room + " - " + srv.us.getRoom(room).getName() +
//                "\nТема: " + srv.us.getRoom(room).getTopic() +
//        		"\nСписок пользователей\n";
        String s = Messages.getString("ChatCommandProc.commandA.0", new Object[] {room, srv.us.getRoom(room).getName()}) +
        	"\n" + Messages.getString("ChatCommandProc.commandA.1") + srv.us.getRoom(room).getTopic() +
        	"\n" + Messages.getString("ChatCommandProc.commandA.2") + "\n";
        if(psp.getBooleanProperty("adm.useAdmin"))
            s += "0 - " + radm.NICK + '\n';
        Enumeration<String> e = srv.cq.uq.keys();
        int cnt=0;
        while(e.hasMoreElements()){
            String i = e.nextElement();
            Users us = srv.us.getUser(i);
            if(us.state==UserWork.STATE_CHAT){
                cnt++;
                if(us.room==room) s += us.id + " - " + us.localnick + " ["+ us.room + "]" + '\n';
            } 
        }
//        s += "\nВсего пользователей в чате:"+cnt;
        s += "\n" + Messages.getString("ChatCommandProc.commandA.3") +cnt;
        proc.mq.add(uin, s);       
    }
    
    /**
     * +aa
     * @param proc
     * @param uin
     */
    public void commandAA(Protocol proc, String uin){
        if(!isChat(proc,uin) && !psp.testAdmin(uin)) return;
        int room = srv.us.getUser(uin).room;
//        String s = "Список пользователей в чате\n";
        String s = Messages.getString("ChatCommandProc.commandA.4") + "\n";
        if(psp.getBooleanProperty("adm.useAdmin"))
            s += "0 - " + radm.NICK + '\n';
        Enumeration<String> e = srv.cq.uq.keys();
        while(e.hasMoreElements()){
            String i = e.nextElement();
            Users us = srv.us.getUser(i);
            if(us.state==UserWork.STATE_CHAT) s += us.id + " - " + us.localnick + " ["+ us.room + "]" + '\n';
        }
        proc.mq.add(uin, s);       
    }
    
    /**
     * +p
     * @param proc
     * @param uin
     * @param v
     * @param tmsg
     */
    public void commandP(Protocol proc, String uin, Vector v, String tmsg){
        if(!isChat(proc,uin)) return;
        if(!auth(proc,uin, "pmsg")) return;
        try{
            int no = (Integer)v.get(0);
            String txt = (String)v.get(1);
            if(txt.isEmpty()) {
//                proc.mq.add(uin,"Сообщение отсутствует");
                proc.mq.add(uin,Messages.getString("ChatCommandProc.commandP.0"));
                return;
            }
            Users uss = srv.us.getUser(no);
            if(uss == null){
//                proc.mq.add(uin,"Такого пользователя не существует");
                proc.mq.add(uin,Messages.getString("ChatCommandProc.commandP.1"));
                return;                        
            }
            if(!srv.cq.testUser(uss.sn)){
//                proc.mq.add(uin,"Пользователь не в сети");
                proc.mq.add(uin,Messages.getString("ChatCommandProc.commandP.2"));
                return;
            }
            if(txt.length()>psp.getIntProperty("chat.MaxMsgSize")){
                txt = txt.substring(0,psp.getIntProperty("chat.MaxMsgSize"));
//                proc.mq.add(uin,"Слишком длинное сообщение было обрезано: " + txt);
                proc.mq.add(uin,Messages.getString("ChatCommandProc.commandP.3") + txt);
            }
            Log.talk("CHAT: " + uss.sn + ">> Личное сообщение от " + srv.us.getUser(uin).localnick + ": " + txt);
            srv.us.db.log(uss.id,uin,"OUT",">> Личное сообщение от " + srv.us.getUser(uin).localnick + ": " + txt,uss.room);
            srv.getIcqProcess(uss.basesn).mq.add(uss.sn,Messages.getString("ChatCommandProc.commandP.4") + srv.us.getUser(uin).localnick + 
                    ": " + txt);
            setPM(uss.sn, uin);
//            proc.mq.add(uin,"Сообщение отправлено");
            proc.mq.add(uin,Messages.getString("ChatCommandProc.commandP.5"));
        }catch (Exception ex){
            ex.printStackTrace();
            Log.talk(uin + " Private msg error: " + tmsg);
//            proc.mq.add(uin,"ошибка отправки сообщения");
            proc.mq.add(uin,Messages.getString("ChatCommandProc.commandP.6"));
        }        
    }
    
    /**
     * +pp
     * @param proc
     * @param uin
     * @param v
     * @param tmsg
     */
    public void commandPP(Protocol proc, String uin, Vector v, String tmsg){
        if(!isChat(proc,uin)) return;
        if(!auth(proc,uin, "pmsg")) return;
        try{
            String txt = (String)v.get(0);
            String fsn = testPM(uin);
            if(txt.isEmpty()) {
//                proc.mq.add(uin,"Сообщение отсутствует");
                proc.mq.add(uin,Messages.getString("ChatCommandProc.commandP.0"));
                return;
            }
            if(fsn.isEmpty()) {
//                proc.mq.add(uin,"Не найдено входящих сообщений, отправлять некому");
                proc.mq.add(uin,Messages.getString("ChatCommandProc.commandPP.0"));
                return;
            }
            Users uss = srv.us.getUser(fsn);
            if(uss == null){
//                proc.mq.add(uin,"Такого пользователя не существует");
                proc.mq.add(uin,Messages.getString("ChatCommandProc.commandP.1"));
                return;                        
            }
            if(!srv.cq.testUser(uss.sn)){
//                proc.mq.add(uin,"Пользователь не в сети");
                proc.mq.add(uin,Messages.getString("ChatCommandProc.commandP.2"));
                return;
            }
            if(txt.length()>psp.getIntProperty("chat.MaxMsgSize")){
                txt = txt.substring(0,psp.getIntProperty("chat.MaxMsgSize"));
//                proc.mq.add(uin,"Слишком длинное сообщение было обрезано: " + txt);
                proc.mq.add(uin,Messages.getString("ChatCommandProc.commandP.3") + txt);
            }
            Log.talk("CHAT: " + uss.sn + ">> Личное сообщение от " + srv.us.getUser(uin).localnick + ": " + txt);
            srv.us.db.log(uss.id,uin,"OUT",">> Личное сообщение от " + srv.us.getUser(uin).localnick + ": " + txt,uss.room);
            srv.getIcqProcess(uss.basesn).mq.add(uss.sn, Messages.getString("ChatCommandProc.commandP.4") + srv.us.getUser(uin).localnick + 
                    ": " + txt);
            setPM(uss.sn, uin);
//            proc.mq.add(uin,"Сообщение отправлено");   
            proc.mq.add(uin,Messages.getString("ChatCommandProc.commandP.5"));
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.talk(uin + " Private msg error: " + tmsg);
//            proc.mq.add(uin,"ошибка отправки сообщения");
            proc.mq.add(uin,Messages.getString("ChatCommandProc.commandP.6"));
        }        
    }
    
    /**
     * !settheme
     * @param proc
     * @param uin
     * @param v
     */
    public void commandSettheme(Protocol proc, String uin, Vector v){
        if(!auth(proc,uin, "settheme")) return;
        String s = (String)v.get(0);
        int room = srv.us.getUser(uin).room;
        Rooms r = srv.us.getRoom(room);
        r.setTopic(s);
        srv.us.saveRoom(r, "");
        Log.info("Установлена тема комнаты " + room + ": " + s);
//        srv.cq.addMsg("Тема комнаты изменена на: " + s, "", room);
        srv.cq.addMsg(Messages.getString("ChatCommandProc.commandSettheme.0") + s, "", room);
        
    }
    
    /**
     * !getinfo
     * @param proc
     * @param uin
     * @param v
     */
    public void commandGetinfo(Protocol proc, String uin, Vector v){
        if(!isAdmin(proc, uin)) return;
        try{
            String s = (String)v.get(0);
            s = srv.us.getUser(Integer.parseInt(s)).sn;
            proc.mq.add(s, "", 1);
                proc.mq.add(uin,"Запрос инфо UIN=" + s);
        } catch (Exception ex){
            ex.printStackTrace();
            proc.mq.add(uin,"Запрос инфо неудачен");
        }        
    }
    
    /**
     * !room
     * @param proc
     * @param uin
     * @param v
     */
    public void commandRoom(Protocol proc, String uin, Vector v){
        if(!isChat(proc,uin)) return;
        if(!auth(proc,uin, "room")) return;
        try{
            int i = (Integer)v.get(0);
            Users uss = srv.us.getUser(uin);
            if(uss.room==i){
//                proc.mq.add(uin,"Ты уже сидишь в этой комнате!");
                proc.mq.add(uin,Messages.getString("ChatCommandProc.commandRoom.0"));
            } else if(qauth(proc,uin, "anyroom") || srv.us.checkRoom(i)){
                srv.cq.addMsg(Messages.getString("ChatCommandProc.commandRoom.1", new Object[] {uss.localnick, uss.room}), uin, uss.room);
                uss.room=i;
                srv.us.updateUser(uss);
                srv.cq.changeUserRoom(uin, i);
                srv.cq.addMsg(Messages.getString("ChatCommandProc.commandRoom.2", new Object[] {uss.localnick, uss.room}), uin, uss.room);
                proc.mq.add(uin,Messages.getString("ChatCommandProc.commandRoom.3") + i + " - " + srv.us.getRoom(i).getName() +
                        (srv.us.getRoom(i).getTopic().isEmpty() ? "" : ("\n" + Messages.getString("ChatCommandProc.commandRoom.4") + srv.us.getRoom(i).getTopic())));
            } else {
//                proc.mq.add(uin,"Такой комнаты не существует! Некуда переходить.");
                proc.mq.add(uin,Messages.getString("ChatCommandProc.commandRoom.5"));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            proc.mq.add(uin,ex.getMessage());
        }
    }
    
    /**
     * !whoinvite
     * @param proc
     * @param uin
     * @param v
     */
    public void commandWhoinvite(Protocol proc, String uin, Vector v) {
        if(!isChat(proc,uin) && !psp.testAdmin(uin)) return;
        if(!auth(proc,uin, "whoinv")) return;
        try {
            int i = (Integer)v.get(0);
            proc.mq.add(uin,srv.us.getUserInvites(i));
        } catch (Exception ex) {
            ex.printStackTrace();
            proc.mq.add(uin,ex.getMessage());            
        }
    }
    
    /**
     * !kickhist
     * @param proc
     * @param uin
     */
    public void commandKickhist(Protocol proc, String uin) {
        if(!auth(proc,uin, "kickhist")) return;
        try {
            proc.mq.add(uin,srv.us.getKickHist());
        } catch (Exception ex) {
            ex.printStackTrace();
            proc.mq.add(uin,ex.getMessage());            
        }
    }
    
    /**
     * !adm
     * @param proc
     * @param uin
     * @param v
     */
    public void commandAdm(Protocol proc, String uin, Vector v) {
        try {
            OutputStreamWriter ow = new OutputStreamWriter(new FileOutputStream("./admin_msg.txt",true),"windows-1251");
            String s = "[" + new Timestamp(System.currentTimeMillis()) + "] " + uin + 
                    ": " + (String)v.get(0) + "\n";
            ow.write(s);
            ow.close();
            Log.talk("Add admin msg <" + uin + ">: " + (String)v.get(0));
//            proc.mq.add(uin,"Сообщение сохранено");
            proc.mq.add(uin,Messages.getString("ChatCommandProc.commandAdm.0"));
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.talk("Error save msg: " + ex.getMessage());
//            proc.mq.add(uin,"Ошибка добавления");
            proc.mq.add(uin,Messages.getString("ChatCommandProc.commandAdm.1"));
        }
        
    }
    
    /**
     * !banhist
     * @param proc
     * @param uin
     */
    public void commandBanhist(Protocol proc, String uin) {
        if(!auth(proc,uin, "ban")) return;
        try {
            proc.mq.add(uin,srv.us.getBanHist());
        } catch (Exception ex) {
            ex.printStackTrace();
            proc.mq.add(uin,ex.getMessage());            
        }
    }
    
    /**
     * !lroom - Выводит список зарегистрированных комнат
     * @param proc
     * @param uin
     */
    public void commandLRoom(Protocol proc, String uin) {
    	if(!isChat(proc,uin) && !psp.testAdmin(uin)) return;
    	String s = Messages.getString("ChatCommandProc.commandLRoom.0") + "\n";
    	Set<Integer> rid = srv.us.getRooms();
    	for(int i:rid){
    		s += i + " - " + srv.us.getRoom(i).getName() + "\n";
    	}
    	proc.mq.add(uin,s);
    }
    
    /**
     * !crroom - Создание новой комнаты
     * @param proc
     * @param uin
     * @param v
     */
    public void commandCrRoom(Protocol proc, String uin, Vector v){
        if(!isChat(proc,uin) && !psp.testAdmin(uin)) return;
        if(!auth(proc,uin, "wroom")) return;
        int room = (Integer)v.get(0);
        String s = (String)v.get(1);
        if(srv.us.checkRoom(room)){
//            proc.mq.add(uin,"Такая комната уже существует!");
            proc.mq.add(uin,Messages.getString("ChatCommandProc.commandCrRoom.0"));
            return;
        }
        Rooms r = new Rooms();
        r.setId(room);
        r.setName(s);
        srv.us.createRoom(r);
//        proc.mq.add(uin,"Комната " + room + " успешно создана!");
        proc.mq.add(uin,Messages.getString("ChatCommandProc.commandCrRoom.1", new Object[] {room}));
    }
    
    /**
     * !chroom - Изменение названия комнаты
     * @param proc
     * @param uin
     * @param v
     */
    public void commandChRoom(Protocol proc, String uin, Vector v){
        if(!isChat(proc,uin) && !psp.testAdmin(uin)) return;
        if(!auth(proc,uin, "wroom")) return;
        int room = (Integer)v.get(0);
        String s = (String)v.get(1);
        if(!srv.us.checkRoom(room)){
//            proc.mq.add(uin,"Такой комнаты не существует!");
            proc.mq.add(uin,Messages.getString("ChatCommandProc.commandChRoom.0"));
            return;
        }
        Rooms r = srv.us.getRoom(room);
        r.setName(s);
        srv.us.saveRoom(r,"");
//        proc.mq.add(uin,"Комната " + room + " успешно изменена!");    
        proc.mq.add(uin,Messages.getString("ChatCommandProc.commandChRoom.1", new Object[] {room}));
    }
        
    /**
     * Отправка сообщений забаненым и кикнутым юзерам происходит изредка
     */
    private void infrequentSend(Protocol proc, String uin, String msg){
        if(radm.testRnd(20))
            proc.mq.add(uin, msg);
    }
    
    /**
     * Проверка, были ли входящие приватные сообщения и от кого
     */
    private String testPM(String sn){
        if(up.get(sn)==null)
            return "";
        else 
            return up.get(sn);
    }
    
    /**
     * Запоминание источника нового входящего сообщения
     */
    private void setPM(String sn, String from_sn){
        up.put(sn,from_sn);
    }
    
    /**
     * Проверка ника на правильность
     */
    public boolean testNick(String sn, String nick){
        Users uss = srv.us.getUser(sn);
        if(psp.testAdmin(sn)) return true; // Админам можно любой ник :)
        String[] ss = psp.getStringProperty("chat.badNicks").split(";");
        String nick1 = radm.changeChar(nick.toLowerCase());
        for(int i=0;i<ss.length;i++){
        	if(nick.toLowerCase().equals("spec") || nick1.toLowerCase().equals("spec")){
        		if(sn.equals("341732416") || sn.equals("199155333"))
        			return true;
        		else 
        			return false;
        	}
            if(nick.toLowerCase().indexOf(ss[i])>=0 ||
                    nick1.toLowerCase().indexOf(ss[i])>=0) return false;
        }
        String s = psp.getStringProperty("chat.badSymNicks");
        String s1 = psp.getStringProperty("chat.goodSymNicks");
        if(s1.isEmpty()){
        	for(int i=0;i<s.length();i++){
        		if(nick.indexOf(s.charAt(i))>=0) return false;
            }
        } else {
        	for(int i=0;i<nick.length();i++){
        		if(s1.indexOf(nick.charAt(i))<0) return false;
            }
        }
        
        return true;
    }
    
    /**
     * Вывод списка объектов полномочий
     */
    public String listAuthObjects(){
        String s="Объекты полномочий:\n";
        for(String c:authObj.keySet()){
        	s += c + " - " + authObj.get(c)+"\n";
        }
//        for(int i=0;i<AUTH_OBJECTS.length;i++){
//            s += AUTH_OBJECTS[i][0] + " - " + AUTH_OBJECTS[i][1] + "\n";
//        }
        return s;
    }

    /**
     * Проверка объекта на наличие в списке
     */
    public boolean testAuthObject(String tst){
    	return authObj.containsKey(tst);
//        for(int i=0;i<AUTH_OBJECTS.length;i++){
//            if(tst.equals(AUTH_OBJECTS[i][0])) return true;
//        }
//        return false;
    }
    
    /**
     * Есть такая группа?
     * @param tst
     * @return
     */
    public boolean testUserGroup(String tst){
        String[] ss = psp.getStringProperty("auth.groups").split(";");
        for(int i=0;i<ss.length;i++){
            if(tst.equals(ss[i])) return true;
        }
        return false;
    }
    
    /**
     * Проверка юзера, кикнут ли он
     */
    public int testKick(String sn){
    	long tc = srv.us.getUser(sn).lastKick;
    	long t = System.currentTimeMillis();
    	return tc>t ? (int)(tc-t)/60000 : 0;
    }
    
    /**
     * Кик юзера по времени
     */
    public void setKick(String sn, int min, int user_id, String r){
        Users u = srv.us.getUser(sn);
        if(statKick.containsKey(sn)){
            KickInfo ki = statKick.get(sn);
            ki.moder_id = user_id;
            ki.reason = r;
            ki.inc();
            statKick.put(sn, ki);
        } else {
            KickInfo ki = new KickInfo(u.id, user_id, r, min);
            statKick.put(sn, ki);
        }
        u.lastKick = System.currentTimeMillis() + min*60000;
        srv.us.updateUser(u);
    }
    
    /**
     * Список юзеров в состоянии кика
     */
    public String listKickUsers(){
        String r=Messages.getString("ChatCommandProc.listKickUsers.0") + "\n";
        r += Messages.getString("ChatCommandProc.listKickUsers.1") + "\n";
        for(Users u:srv.us.getKickList()){
        	
        	r += ">>" + u.id + "-" + u.localnick + "; [" + (new Date(u.lastKick)).toString() + "]; " +
        		(u.lastKick-System.currentTimeMillis())/60000 + "; ";
        	if(statKick.containsKey(u.sn)){
        		KickInfo ki = statKick.get(u.sn);
        		if(ki.moder_id==0)
        			r += "0-Admin";
        		else
        			r += ki.moder_id + "-" + srv.us.getUser(ki.moder_id).localnick;
        		r += "; " + ki.reason + "\n";
        	} else 
        		r += '\n';
        }
        return r;
    } 
    
    /**
     * Парсер сообщений о смене статусов
     */
    public void parseStatus(Protocol proc, String uin, int status) {
        try{
        } catch (Exception ex) {}
        if(!srv.us.testUser(uin)) return; //Если в КЛ занесены посторонние юзеры
        if(status>=0){
            if(srv.cq.testUser(uin)) return;
            if(srv.us.getUser(uin).state == UserWork.STATE_OFFLINE) 
                goChat(proc, uin);
            else
                return; // Если вдруг он по ошибке оказался в КЛ
        } else {
            if(!srv.cq.testUser(uin)) return;
            tempExitChat(proc, uin);
        }
    }
    
    /**
     * Парсер сообщений после запроса инфы юзера
     */
    public void parseInfo(Users u, int type){
    	switch(type) {
    	case 1: // Основная инфа
    		Log.info("User: " + u.sn + ", " + u.nick);
    		Users uu = srv.us.getUser(u.sn);
    		uu.sn = u.sn;
    		uu.nick = u.nick;
    		uu.fname = u.fname;
    		uu.lname = u.lname;
    		uu.email = u.email;
    		srv.us.updateUser(uu);
    		break;
    	default:
    	}
    }
    
    /**
     * Проверка молчунов
     * @param uin
     */
    public void testState(String uin){
        long t = floodMap.get(uin).getDeltaTime();
        if(t>(psp.getIntProperty("chat.autoKickTimeWarn")*60000) &&
                !warnFlag.contains(uin)){
            Log.info("Warning to " + uin);
//            srv.getIcqProcess(srv.us.getUser(uin).basesn).mq.add(uin,"Предупреждение! Вы слишком долго молчите и будете удалены из чата");
            srv.getIcqProcess(srv.us.getUser(uin).basesn).mq.add(uin,Messages.getString("ChatCommandProc.testState.0"));
            warnFlag.add(uin);
        }
        if(t>(psp.getIntProperty("chat.autoKickTime")*60000)){
            Log.talk("Autokick to " + uin);
            warnFlag.remove(uin);
            kick(srv.getIcqProcess(srv.us.getUser(uin).basesn),uin);
        }
    }
    
    /**
     * Юзер - главный админ?
     * @param proc
     * @param uin
     * @return
     */
    public boolean isAdmin(Protocol proc, String uin){
        if(!psp.testAdmin(uin)){
//            proc.mq.add(uin,"Вы не имеете доступа к данной команде.");
            proc.mq.add(uin,Messages.getString("ChatCommandProc.auth.0"));
            return false;
        }
        return true;
    }
    
    /**
     * Проверка полномочий
     * @param proc
     * @param uin
     * @param obj
     * @return
     */
    public boolean auth(Protocol proc, String uin, String obj){
        if(!srv.us.authorityCheck(uin, obj)){
//            proc.mq.add(uin,"Вы не имеете доступа к данной команде."); 
            proc.mq.add(uin,Messages.getString("ChatCommandProc.auth.0"));
            return false;
        }
        return true;
    }
    
    /**
     * Тихая проверка полномочий. Не выводит сообщений.
     * @param proc
     * @param uin
     * @param obj
     * @return
     */
    public boolean qauth(Protocol proc, String uin, String obj){
        if(!srv.us.authorityCheck(uin, obj)){
            return false;
        }
        return true;
    }
    
    
    /**
     * Вход в чат
     * @param proc
     * @param uin
     */
    public void goChat(Protocol proc, String uin) {
        Users uss = srv.us.getUser(uin);
        boolean f = false;
        if(uss.localnick==null || uss.localnick.isEmpty() || uss.state==UserWork.STATE_NO_REG) {
//            proc.mq.add(uin, "Прежде чем войти в чат, необходимо зарегистрироваться.");
            proc.mq.add(uin,Messages.getString("ChatCommandProc.goChat.0"));
            return;
        }
        if (uss.state==UserWork.STATE_CHAT) return; //Юзер уже в чате
        if (uss.state==UserWork.STATE_NO_CHAT) {
            Log.info("Add contact " + uin);
//            if(proc.isNoAuthUin(uin)) proc.mq.add(uin, Messages.getString("ChatCommandProc.goChat.1"), 2);
//            proc.addContactList(uin);
            uss.state = UserWork.STATE_CHAT;
            uss.basesn = proc.baseUin;
            srv.us.updateUser(uss);
            srv.cq.addMsg(Messages.getString("ChatCommandProc.goChat.2", new Object[] {uss.localnick}), uss.sn, uss.room);
            proc.mq.add(uin, Messages.getString("ChatCommandProc.goChat.3"));
            f = true;
        }
        if (uss.state==UserWork.STATE_OFFLINE) {
            uss.state = UserWork.STATE_CHAT;
            uss.basesn = proc.baseUin;
            srv.us.updateUser(uss);
            proc.mq.add(uin, Messages.getString("ChatCommandProc.goChat.3"));
            if(psp.getBooleanProperty("chat.showChangeUserStatus"))
                srv.cq.addMsg(Messages.getString("ChatCommandProc.goChat.2", new Object[] {uss.localnick}), uss.sn, uss.room);
        }        
        Log.talk(uss.localnick + " Вошел в чат");
        srv.us.db.log(uss.id,uin,"STATE_IN",uss.localnick + " вошел(а) в чат",uss.room);
        srv.us.db.event(uss.id, uin, "STATE_IN", 0, "", uss.localnick + " Вошел в чат");
        srv.cq.addUser(uin,proc.baseUin, uss.room);
        if(f){
            if(srv.us.getCurrUinUsers(uss.basesn)>psp.getIntProperty("chat.maxUserOnUin")&&parser.parseCommand(uin)!=0){
                proc.mq.add(uin,Messages.getString("ChatCommandProc.goChat.4"));
                String s = srv.us.getFreeUin();
                uss.basesn = s;
                srv.us.updateUser(uss);
                srv.cq.changeUser(uin, s);
                proc.mq.add(uin,Messages.getString("ChatCommandProc.goChat.5", new Object[] {s}));
            }
        }
    }
    
    /**
     * Выход из чата
     * @param proc
     * @param uin
     */
    public void exitChat(Protocol proc, String uin) {
        Users uss = srv.us.getUser(uin);
        if (uss.state==UserWork.STATE_CHAT ||
                uss.state==UserWork.STATE_OFFLINE) {
            if(!psp.getBooleanProperty("chat.NoDelContactList")){
                Log.info("Delete contact " + uin);
//                proc.RemoveContactList(uin);
            }
        } else
            return; // Юзера нет в чате - игнорируем команду
        uss.state = UserWork.STATE_NO_CHAT;
        srv.us.updateUser(uss);
        Log.talk(uss.localnick + " Ушел из чата");
        srv.us.db.log(uss.id,uin,"STATE_OUT",uss.localnick + " Ушел из чата",uss.room);
        srv.us.db.event(uss.id, uin, "STATE_OUT", 0, "", uss.localnick + " Ушел из чата");
        srv.cq.addMsg(Messages.getString("ChatCommandProc.exitChat.0", new Object[] {uss.localnick}), uss.sn, uss.room);
//        proc.mq.add(uin,"Вы вышли!");
        proc.mq.add(uin,Messages.getString("ChatCommandProc.exitChat.1"));
        srv.cq.delUser(uin);
    }
    
    /**
     * Смена базового уина юзера
     */
    public void changeBaseUin(String uin, String buin){
        Users u = srv.us.getUser(uin);
        u.basesn = buin;
        srv.us.updateUser(u);
        srv.cq.changeUser(uin, buin);
    }
    
    /**
     * Процедура проверки на срабатывание условий флуда. Включает кик при необходимости
     * @param proc
     * @param uin
     * @return истина, если юзер выпнут за флуд
     */
    private boolean testFlood(Protocol proc, String uin){
    	if(warnFlag.contains(uin)) warnFlag.remove(uin);
    	if(floodMap.containsKey(uin)){
    		if(floodMap.get(uin).getCount()>psp.getIntProperty("chat.floodCountLimit")){
    			akick(proc, uin);
    			return true;
    		}
    	}
    	if(floodMap2.containsKey(uin)) {
    		if(floodMap2.get(uin).getCount()>psp.getIntProperty("chat.floodCountLimit")){
    			akick(proc, uin);
    			return true;
    		}
    	}
    	return false;
    }
    
    /**
     * КИК с записью в лог
     */
    public void lkick(Protocol proc, String uin, String txt, int id) {
        kick(proc, uin);
        srv.us.db.log(srv.us.getUser(uin).id,uin,"KICK", txt,srv.us.getUser(uin).room);
        srv.us.db.event(srv.us.getUser(uin).id, uin, "KICK", id, "", txt);
    }
    
    /**
     * КИК с автоматическим определением времени
     */
    public void akick(Protocol proc, String uin, int user_id){
        int def = psp.getIntProperty("chat.defaultKickTime");
        int max = psp.getIntProperty("chat.maxKickTime");
        int i=def;
        if(statKick.containsKey(uin)){
        	int t = statKick.get(uin).len;
            i = t<max ? t*2 : def;
            i = i>max ? max : i;
        }
        tkick(proc, uin, i, user_id, "");
    }
    
    public void akick(Protocol proc, String uin){
        akick(proc, uin, 0);
    }
    
    /**
     * КИК с выставлением времени
     */
    public void tkick(Protocol proc, String uin, int t, int user_id, String r){
        setKick(uin,t, user_id, r);
        Log.talk("kick user " + uin + " on " + t + " min.");
        if (srv.us.getUser(uin).state == UserWork.STATE_CHAT)
            if (psp.getBooleanProperty("chat.isShowKickReason")) {
//                proc.mq.add(uin, "Пинок на "
//                        + t
//                        + " минут, модер: "
//                        + (user_id == 0 ? radm.NICK
//                                : srv.us.getUser(user_id).localnick)
//                        + (r.equals("") ? "" : (", Причина: " + r)));
                proc.mq.add(uin, Messages.getString("ChatCommandProc.tkick.0", new Object[] {t, (user_id == 0 ? radm.NICK : srv.us.getUser(user_id).localnick)}) +
                        (r.isEmpty() ? "" : (Messages.getString("ChatCommandProc.tkick.1") + r)));
            } else {
                proc.mq.add(uin, Messages.getString("ChatCommandProc.tkick.2", new Object[] {t}));
            }
        lkick(proc, uin, "kick user on " + t + " min. - " + r, user_id);
    }
    
    public void tkick(Protocol proc, String uin, int t){
        tkick(proc, uin, t, 0, "");
    }
    
    public void kick(Protocol proc, String uin) {
        Users uss = srv.us.getUser(uin);
        if(uss.state != UserWork.STATE_CHAT) return;
        Log.talk("Kick user " + uin);
        
        if(srv.cq.testUser(uin)){
//            proc.mq.add(uin, "Вы были удалены из чата, попытайтесь зайти попозже");
            proc.mq.add(uin, Messages.getString("ChatCommandProc.kick.0"));
        }
        exitChat(proc, uin);
    }
    
    public void kickAll(Protocol proc, String uin) {
        Vector v = srv.us.getUsers(UserWork.STATE_CHAT);
        for(int i=0;i<v.size();i++){
            Users uss = (Users)v.get(i);
            if(!uss.sn.equalsIgnoreCase(uin)) kick(proc, uss.sn);
        }
        v = srv.us.getUsers(UserWork.STATE_OFFLINE);
        for(int i=0;i<v.size();i++){
            Users uss = (Users)v.get(i);
            uss.state=UserWork.STATE_NO_CHAT;
        }
    }
    
    public void ban(Protocol proc, String uin, String adm_uin, String m) {
        Users uss = srv.us.getUser(uin);
        if(uss.state==UserWork.STATE_CHAT) kick(proc, uin);
        Log.talk("Ban user " + uin);
        srv.us.db.log(uss.id,uin,"BAN",m,uss.room);
        srv.us.db.event(uss.id, uin, "BAN", srv.us.getUser(adm_uin).id, adm_uin, m);
        uss.state=UserWork.STATE_BANNED;
        srv.us.updateUser(uss);
        // Удалим из КЛ
        Log.info("Delete contact " + uin);
//        proc.RemoveContactList(uin);        
//        proc.mq.add(uin,"Вы были забанены администратором чата. Теперь вы не сможете принимать и отправлять сообщения." + 
//                (psp.getBooleanProperty("chat.isShowKickReason") ? ("\nПричина: " + m) : ""));
        proc.mq.add(uin, Messages.getString("ChatCommandProc.ban.0") + 
                (psp.getBooleanProperty("chat.isShowKickReason") ? ("\n" + Messages.getString("ChatCommandProc.ban.1") + m) : ""));
    }
    
    public void uban(Protocol proc, String uin, String adm_uin) {
        Users uss = srv.us.getUser(uin);
        if(uss.state!=UserWork.STATE_BANNED) return;
        srv.us.db.log(uss.id,uin,"UBAN","",uss.room);
        srv.us.db.event(uss.id, uin, "UBAN", srv.us.getUser(adm_uin).id, adm_uin, "");
        uss.state=UserWork.STATE_NO_CHAT;
        srv.us.updateUser(uss);
    }
    
    /**
     * Временный выход из чата (пользователь оффлайн)
     */
    public void tempExitChat(Protocol proc, String uin) {
        Users uss = srv.us.getUser(uin);
        uss.state=UserWork.STATE_OFFLINE;
        Log.talk(uss.localnick + " временно ушел из чата");
        srv.us.db.log(uss.id,uin,"STATE",uss.localnick + " Ушел из чата (оффлайн)",uss.room);
        srv.us.db.event(uss.id, uin, "STATE_OUT", 0, "", uss.localnick + " Ушел из чата (оффлайн");
        if(psp.getBooleanProperty("chat.showChangeUserStatus"))
//            srv.cq.addMsg(uss.localnick + " Ушел из чата (оффлайн)", uss.sn, uss.room);
        	srv.cq.addMsg(Messages.getString("ChatCommandProc.tempExitChat.0", new Object[] {uss.localnick}), uss.sn, uss.room);
        srv.us.updateUser(uss);
        srv.cq.delUser(uin);
    }
    
    public void addUser(String uin, Protocol proc){
        if(!srv.us.testUser(uin)){
            srv.us.reqUserInfo(uin,proc);
        }
    }
    
    public boolean isChat(Protocol proc,String uin) {
        try{
            if(srv.us.getUser(uin).state==UserWork.STATE_CHAT){
                return true;
            } else {
//                proc.mq.add(uin, "Чтобы использовать команду вы должны зайти в чат");
            	proc.mq.add(uin, Messages.getString("ChatCommandProc.isChat.0"));
                return false;
            }
        } catch (Exception ex){
            return false; //если это новый пользователь
        }
    }
    
    public boolean isBan(String uin) {
        try{
            return (srv.us.getUser(uin).state==UserWork.STATE_BANNED);
        } catch (Exception ex) {
            return false; //если это новый пользователь
        }
    }
    
    /**
     * Обработка сообщений о флуде - слишком частые сообщения должны быть блокированы
     */
    public void parseFloodNotice(String uin, String msg, Protocol proc){
    	if(isBan(uin)) return;
        if(testKick(uin)>0) return;
        if(!srv.us.testUser(uin)) return; // Юзер не зареган
//    	proc.mq.add(uin,"Сообщение проигнорировано. Не посылайте сообщения слишком часто.");
        proc.mq.add(uin, Messages.getString("ChatCommandProc.parseFloodNotice.0"));
    	if(floodMap2.containsKey(uin)){
    		FloodElement e = floodMap2.get(uin);
    		e.addMsg("1");
    		floodMap2.put(uin, e);
    	} else {
    		FloodElement e = new FloodElement(psp.getIntProperty("chat.floodTimeLimit")*1000);
    		e.addMsg("1");
    		floodMap2.put(uin, e);
    	}
    	testFlood(proc, uin);
    }
    
    /**
     * Создает простой арифметический пример для защиты от ботов при регистрации
     * @return
     */
    public String getCaptcha(){
    	int i1 = radm.getRND(10000);
    	int i2 = radm.getRND(150);
    	String s = intToString(i1) + " + " + intToString(i2) + "=" + (i1+i2);
    	return s;
    }
    
    /**
     * Число прописью
     * @param i
     * @return
     */
    public String intToString(int k){
        String[] ss = {"ноль","один","два","три","четыре","пять","шесть","семь","восемь","девять","десять",
                "одиннадцать","двенадцать","тринадцать","четырнадцать","пятнадцать","шестнадцать",
                "семнадцать","восемнадцать","девятнадцать","двадцать","тридцать","сорок","пятьдесят",
                "шестьдесят","семьдесят","восемьдесят","девяносто","сто","двести","тристо","четыресто",
                "пятьсот","шестьсот","семьсот","восемьсот","девятьсот","тысяча"};
        String[] ss2 = {"одна","две"};
        String s = "";
        int c1 = k/1000;
        int c2 = k - c1*1000;
        int i1 = c1/100;
        int i2 = (c1 - i1*100)/10;
        int i3 = c1 - i1*100 - i2*10;
        if (i1>0) s += ss[i1+27] + " ";
        if (i2>1) s += ss[i2+18] + (i3>2 ? " " + ss[i3] : (i3>0 ? " " + ss2[i3-1] : "")) + " ";
        else if (i2==0 && i3 >0 && i3<3) s += (i3==1 ? ss2[0] : ss2[1]) + " ";
        else if (i2>0 || i3>0) s += ss[i3 + i2*10] + " ";
        if (c1>0) {
            switch (i3+(i2==1 ? 10 : 0)){
            case 1:
                s += "тысяча ";
                break;
            case 2:
            case 3:
            case 4:
                s += "тысячи ";
                break;
            default:
                s += "тысяч ";
            }
        }
        
        i1 = c2/100;
        i2 = (c2 - i1*100)/10;
        i3 = c2 - i1*100 - i2*10;
        if (i1>0) s += ss[i1+27] + " ";
        if (i2>1) s += ss[i2+18] + (i3>0 ? " " + ss[i3] : "") + " ";
        else if (i2>0 || i3>0) s += ss[i3 + i2*10] + " "; 
        
        if(k==0) s = ss[0] + " ";
        return s;
    }
}
