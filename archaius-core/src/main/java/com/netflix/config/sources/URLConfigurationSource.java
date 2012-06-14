/*
 *
 *  Copyright 2012 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.config.sources;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.netflix.config.PollResult;
import com.netflix.config.PolledConfigurationSource;

/**
 * A polled configuration source based on a set of URLs. For each poll,
 * it always returns the complete union of properties defined in all files. If one property
 * is defined in more than one URL, the value in file later on the list will override
 * the value in the previous one. The content of the URL should conform to the properties file format.
 * 
 * @author awang
 *
 */
public class URLConfigurationSource implements PolledConfigurationSource {

    private final URL[] configUrls;
    
    /**
     * System property name to define a set of URLs to be used by the
     * default constructor. 
     */
    public static final String CONFIG_URL = "configurationSource.additionalUrls";
    
    /**
     * Default configuration file name to be used by default constructor. This file should
     * be on the classpath. The file name can be overridden by the value of system property
     * <code>configurationSource.defaultFileName</code>
     */
    public static final String DEFAULT_CONFIG_FILE_NAME = "config.properties";
        
    public static final String DEFAULT_CONFIG_FILE_FROM_CLASSPATH = 
        System.getProperty("configurationSource.defaultFileName") == null ? DEFAULT_CONFIG_FILE_NAME : System.getProperty("configurationSource.defaultFileName");
    
    /**
     * Create an instance with a list URLs to be used.
     * 
     * @param urls list of URLs to be used
     */
    public URLConfigurationSource(String... urls) {
       configUrls = createUrls(urls);
    }
    
    private static URL[] createUrls(String... urlStrings) {
        if (urlStrings == null || urlStrings.length == 0) {
            throw new IllegalArgumentException("urlStrings is null or empty");
        }
        URL[] urls = new URL[urlStrings.length];
        try {
            for (int i = 0; i < urls.length; i++) {
                urls[i] = new URL(urlStrings[i]);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return urls;        
    }
    
    /**
     * Create an instance with a list URLs to be used.
     * 
     * @param urls list of URLs to be used
     */
    public URLConfigurationSource(URL... urls) {
        configUrls = urls;
    }
    
    /**
     * Create the instance for the default list of URLs, which is composed by the following order
     * 
     * <ul>
     * <li>A configuration file (default name to be <code>config.properties</code>, see {@link #DEFAULT_CONFIG_FILE_NAME}) on the classpath
     * <li>A list of URLs defined by system property <code>configurationSource.additionalUrls</code> (See {@link #CONFIG_URL}) separated by comma <code>","</code>.
     * </ul>
     */
    public URLConfigurationSource() {
        List<URL> urlList = new ArrayList<URL>();
        URL configFromClasspath = getConfigFileFromClasspath();
        if (configFromClasspath != null) {
            urlList.add(configFromClasspath);
        }
        String[] fileNames = getDefaultFileSources();
        if (fileNames.length != 0) {
            urlList.addAll(Arrays.asList(createUrls(fileNames)));                    
        } 
        if (urlList.size() == 0) { 
                throw new RuntimeException("System property " + CONFIG_URL + " is undefined and default configuration file " 
                        + DEFAULT_CONFIG_FILE_FROM_CLASSPATH + " cannot be found on classpath. At least one of them has to be supplied.");
        } else {
            configUrls = urlList.toArray(new URL[urlList.size()]);
        }
    }
    
    private URL getConfigFileFromClasspath() {
        URL url = null;
        // attempt to load from the context classpath
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader != null) {
            url = loader.getResource(DEFAULT_CONFIG_FILE_FROM_CLASSPATH);
        }
        if (url == null) {
            // attempt to load from the system classpath
            url = ClassLoader.getSystemResource(DEFAULT_CONFIG_FILE_FROM_CLASSPATH);
        }
        if (url == null) {
            // attempt to load from the system classpath
            url = URLConfigurationSource.class.getResource(DEFAULT_CONFIG_FILE_FROM_CLASSPATH);
        }
        return url;
    }
    
    private static final String[] getDefaultFileSources() {
        String name = System.getProperty(CONFIG_URL);
        String[] fileNames;
        if (name != null) {
            fileNames = name.split(",");
        } else {
            fileNames = new String[0];
        }
        return fileNames;
    }
    
    
    /**
     * Retrieve the content of the property files. For each poll, it always
     * returns the complete union of properties defined in all URLs. If one
     * property is defined in content of more than one URL, the value in file later on the
     * list will override the value in the previous one. 
     * 
     * @param initial this parameter is ignored by the implementation
     * @param checkPoint this parameter is ignored by the implementation
     * @throws IOException IOException occurred in file operation
     */
    @Override
    public PollResult poll(boolean initial, Object checkPoint)
            throws IOException {        
        Map<String, Object> map = new HashMap<String, Object>();
        for (URL url: configUrls) {
            Properties props = new Properties();
            InputStream fin = url.openStream();
            props.load(fin);
            fin.close();
            for (Entry<Object, Object> entry: props.entrySet()) {
                map.put((String) entry.getKey(), entry.getValue());
            }
        }
        return PollResult.createFull(map);
    }

    @Override
    public String toString() {
        return "FileConfigurationSource [fileUrls=" + Arrays.toString(configUrls)
                + "]";
    }    
}
