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
package ru.jimbot.http;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.garret.httpserver.JHttpServletRequest;
import org.garret.httpserver.JHttpServletResponse;

/**
*
* @author Prolubnikov Dmitry
*
*/
public class HttpConnection {

private JHttpServletRequest request;
private JHttpServletResponse response;
private PrintWriter writer;
private OutputStream os;
private Hashtable newParams;
private int length;

public void print(String s) throws IOException {
length += s.length();
PrintStream ps = new PrintStream(os, false, "windows-1251");
ps.print(s);
}

public String get(String name) {
if (newParams != null) {
String val = (String) newParams.get(name);
if (val != null) return val;
}
return request.getParameter(name);
}

public void addPair(String name, String val) {
if (newParams == null) newParams = new Hashtable();
newParams.put(name, val);
}

public String getURI() {
return request.getRequestURI();
}

public void send() throws IOException {
response.setContentType("text/html; charset=\"windows-1251\"");
response.setContentLength(length);
response.flushBuffer();
}

public HttpConnection(HttpServletRequest request, HttpServletResponse response) throws IOException {
this.request = (JHttpServletRequest) request;
this.response = (JHttpServletResponse) response;
response.setContentType("text/html; charset=\"windows-1251\"");
response.setLocale(new Locale("ru", "RU", ""));
os = response.getOutputStream();
}
}