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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import ru.jimbot.util.Log;

/**
 * Очередь сообщений чата
 * @author Prolubnikov Dmitry
 */
public class ChatQueue implements Runnable {
    private Thread th;
    private ChatServer srv;
    private ChatProps psp;
    // Обшая очередь сообщений
    private ConcurrentLinkedQueue <MsgElement> mq;
    // Очереди сообщений разбитые по комнатам. Очищаются после каждой итерации
    // обхода по юзерам
    private ConcurrentHashMap<Integer, ConcurrentLinkedQueue<MsgElement>> mmq;
    // Список активных юзеров
    public ConcurrentHashMap <String,UinElement> uq;
    private int sleepAmount = 10000;
    public int msgCounter=1; //счетчик сообщений
    
    /** Creates a new instance of ChatQueue */
    public ChatQueue(ChatServer s) {
        srv = s;
        psp = ChatProps.getInstance(srv.getName());
        sleepAmount = psp.getIntProperty("chat.pauseOut");
        int usr=srv.us.statUsersCount();
        mq = new ConcurrentLinkedQueue<MsgElement>();
        uq = new ConcurrentHashMap<String,UinElement>(usr);
        mmq = new ConcurrentHashMap<Integer, ConcurrentLinkedQueue<MsgElement>>(usr);
    }
    
    /**
     * Возвращает число активных юзеров
     */
    public int statUsers() {
        return uq.size();
    }
    
    /**
     * Длина очереди сообщений в чате
     */
    public int statQueue() {
        return mq.size();
    }
    
    /**
     * Общее число отправленных сообщений
     */
    public int statSend() {
        return msgCounter;
    }
    
    /**
     * Добавить сообщение в очередь
     */
    public void addMsg(String m, String user, int room) {
        Log.debug("CHAT: Add msg "+m);
        mq.add(new MsgElement(m, msgCounter++, user, room));
    }
    
    /**
     * Добавить активного юзера
     */
    public void addUser(String uin, String buin, int room) {
        Log.debug("CHAT: Add user "+uin + ", " + buin);
        uq.put(uin,new UinElement(uin,buin,0,room));
    }
    
    /**
     * Изменить базовый уин
     */
    public void changeUser(String uin, String buin){
        UinElement u = uq.get(uin);
        u.baseUin = buin;
        uq.put(uin, u);
    }
    
    /**
     * Изменить комнату
     */
    public void changeUserRoom(String uin, int room){
        UinElement u = uq.get(uin);
        u.room = room;
        uq.put(uin, u);
    }
    
    /**
     * Удалить активного юзера
     */
    public void delUser(String uin){
        Log.debug("CHAT: Delete active user "+uin);
        uq.remove(uin);
    }
    
    /**
     * Проверка на наличие юзера в списке активных
     */
    public boolean testUser(String uin) {
        return uq.containsKey(uin);
    }
    
            /**
            * Формирование общего сообщения для юзера из списка еще непрочитанных сообщений
            */
            public synchronized String createMsg(String uin) {
            String s = "";
            UinElement u = uq.get(uin);
            ConcurrentLinkedQueue<MsgElement> Queue = mmq.get(u.room);
            boolean first = true;
            if (Queue == null) return "";
            if (Queue.isEmpty()) return "";
            for (MsgElement m : Queue) {
            if (m.countID > u.lastMsg) {
            if (!m.userSN.equalsIgnoreCase(uin) || !psp.getBooleanProperty("chat.ignoreMyMessage")) {
            if (first) {
            s += m.msg;
            first = false;
            } else s += "\r\n" + m.msg;
            }
            u.lastMsg = m.countID;
            }
            }
            return s;
            }
    
                public synchronized void send() {
                try {
                if (uq.isEmpty()) {
                mq.clear();
                return;
                }
                if (mq.isEmpty()) return;
                // Распихиваем все сообщения по очередям комнат
                MsgElement m;
                while ((m = mq.poll()) != null) {
                ConcurrentLinkedQueue<MsgElement> q = mmq.get(m.room);
                if (q == null) {
                q = new ConcurrentLinkedQueue<MsgElement>();
                mmq.put(m.room, q);
                }
                q.add(m);
                }
                // Перебираем юзеров, формируем сообщения на отправку, если что-то
                // есть в очередях
                for (Map.Entry<String, UinElement> c : uq.entrySet()) {
                String uin = c.getKey();
                String s = createMsg(uin);
                if (s.length() == 0) {
                continue;
                }
                ((ChatCommandProc) srv.cmd).testState(uin);
                UinElement user = c.getValue();
                if (user != null) {
                srv.getIcqProcess(user.baseUin).mq.add(user.uin, s);
                }
                }
                // Очищаем очереди чата (все сообщения в них уже обработаны
                for (ConcurrentLinkedQueue<MsgElement> queue : mmq.values()) {
                queue.clear();
                }
                } catch (Exception ex) {
                ex.printStackTrace();
                Log.error("Error: " + ex);
                }
                }
  
    public void start(){
        th = new Thread(this,"ChatQueue");
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
            send();
            try {
                th.sleep(sleepAmount);
            } catch (InterruptedException e) { break; }             
        }
        th=null;
    }
    
    /**
     * Элемент очереди сообщений
     * @author spec
     *
     */
    public class MsgElement {
        public String msg="";
        public int countID=0;
        public String userSN=""; // Отсекать посылку своих сообщений себе
        public int room=0;
        
        MsgElement(String s, int id, String user, int room) {
            msg = s;
            countID = id;
            userSN = user;
            this.room = room;
        }
    }
    
    /**
     * Элемент списка активных пользователей чата
     * @author spec
     *
     */
    public class UinElement {
        public String uin="";
        public String baseUin="";
        public int lastMsg=0;
        public int room = 0;
        
        UinElement(String u, String bu, int lm, int room) {
            uin = u;
            baseUin = bu;      
            lastMsg = lm;
            this.room = room;
        }
    }
}
