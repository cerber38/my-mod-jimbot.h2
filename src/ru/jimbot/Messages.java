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

package ru.jimbot;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * Класс управления локализованными текстовыми ресурсами
 * 
 * @author Prolubnikov Dmitriy
 *
 */
public class Messages {
	
    private static final String BUNDLE_NAME = "ru.jimbot.messages"; //$NON-NLS-1$

    // TODO Подумать как переделать для java 1.5
//    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
//            .getBundle(BUNDLE_NAME, new ResourceBundle.Control() {
//                public List<String> getFormats(String baseName) {
//                    if (baseName == null)
//                        throw new NullPointerException();
//                    return Arrays.asList("xml");
//                }
//                public ResourceBundle newBundle(String baseName,
//                                                Locale locale,
//                                                String format,
//                                                ClassLoader loader,
//                                                boolean reload)
//                                 throws IllegalAccessException,
//                                        InstantiationException,
//                                        IOException {
//                    if (baseName == null || locale == null
//                          || format == null || loader == null)
//                        throw new NullPointerException();
//                    ResourceBundle bundle = null;
//                    if (format.equals("xml")) {
//                        String bundleName = toBundleName(baseName, locale);
//                        String resourceName = toResourceName(bundleName, format);
//                        InputStream stream = null;
//                        if (reload) {
//                            URL url = loader.getResource(resourceName);
//                            if (url != null) {
//                                URLConnection connection = url.openConnection();
//                                if (connection != null) {
//                                    // Disable caches to get fresh data for
//                                    // reloading.
//                                    connection.setUseCaches(false);
//                                    stream = connection.getInputStream();
//                                }
//                            }
//                        } else {
//                            stream = loader.getResourceAsStream(resourceName);
//                        }
//                        if (stream != null) {
//                            BufferedInputStream bis = new BufferedInputStream(stream);
//                            bundle = new XMLResourceBundle(bis);
//                            bis.close();
//                        }
//                    }
//                    return bundle;
//                }
//            });
    private static final ResourceBundle RESOURCE_BUNDLE = getNewBundle();

    private static ResourceBundle getNewBundle(){
    	ResourceBundle bundle = null;
    	InputStream stream = null;
    	try {
    	stream = Messages.class.getResourceAsStream("/ru/jimbot/messages.xml");
        if (stream != null) {
            BufferedInputStream bis = new BufferedInputStream(stream);
            bundle = new XMLResourceBundle(bis);
            bis.close();
        }
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
        return bundle;
    }

    private Messages() {
    }

    public static synchronized String getString(String key) {
        try {
//            return new PropertyResourceBundle()
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }
    
    public static synchronized String getString(String key, Object[] arg) {
        try {
            return java.text.MessageFormat.format(RESOURCE_BUNDLE.getString(key), arg);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }
    
    private static class XMLResourceBundle extends ResourceBundle {
        private Properties props;
        XMLResourceBundle(InputStream stream) throws IOException {
            props = new Properties();
            props.loadFromXML(stream);
        }
        protected Object handleGetObject(String key) {
            return props.getProperty(key);
        }
        /* (non-Javadoc)
         * @see java.util.ResourceBundle#getKeys()
         */
        @Override
        public Enumeration<String> getKeys() {
            // TODO Auto-generated method stub
            return null;
        }
    }

}
