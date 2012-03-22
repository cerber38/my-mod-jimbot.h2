/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.jimbot.util;

import java.io.*;

/**
 *
 * @author Sergey
 */
public class CreateService {
private static final String TEMPLATE = "template/";
private static final String SERVICE = "services/";
    
public static void CreateService(String name) {
try {
CreateDir(name);
} catch (Exception e) {
e.printStackTrace();
Log.error(e.getMessage());
}
}

public static void CreateDir(String name) {
try {
String source = "";
source = TEMPLATE ;
File path = new File(source);
String destination = SERVICE + name;
File dest = new File(destination);
CopyDirectory(path, dest);
} catch (Exception e) {
e.printStackTrace();
Log.error(e.getMessage());
}
}

private static void CopyDirectory(File srcPath, File dstPath) throws FileNotFoundException, IOException {
if (srcPath.isDirectory()) {
if (!dstPath.exists()) dstPath.mkdirs();
String files[] = srcPath.list();
for (int i = 0; i < files.length; i++) {
CopyDirectory(new File(srcPath, files[i]), new File(dstPath, files[i]));
}
} else {
if (!srcPath.exists()) {
Log.error("File or directory does not exist.");
} else {
InputStream in = new FileInputStream(srcPath);
OutputStream out = new FileOutputStream(dstPath);
byte[] buf = new byte[1024];
int len;
while ((len = in.read(buf)) > 0) {
out.write(buf, 0, len);
}
in.close();
out.close();
}
}
}   

/**
* Добавление директории
* @param dir 
*/
public static void addDirectory(String dir) {
String Directory = ("./" + dir);
File NEW = new File(Directory);
if (!NEW.exists()) NEW.mkdirs();
}
}
