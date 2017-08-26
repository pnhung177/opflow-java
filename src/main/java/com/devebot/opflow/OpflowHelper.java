package com.devebot.opflow;

import com.devebot.opflow.exception.OpflowConstructorException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author drupalex
 */
public class OpflowHelper {
    public final static String DEFAULT_CONFIGURATION_KEY = "opflow.configuration";
    public final static String DEFAULT_CONFIGURATION_FILE = "opflow.properties";
    
    private final static Logger LOG = LoggerFactory.getLogger(OpflowHelper.class);
    
    public static OpflowRpcMaster createRpcMaster() throws OpflowConstructorException {
        return createRpcMaster(null, null, true);
    }
    
    public static OpflowRpcMaster createRpcMaster(String propFile) throws OpflowConstructorException {
        return createRpcMaster(propFile, null, true);
    }
    
    public static OpflowRpcMaster createRpcMaster(Properties defaultProps) throws OpflowConstructorException {
        return createRpcMaster(null, defaultProps, false);
    }
    
    public static OpflowRpcMaster createRpcMaster(String propFile, Properties defaultProps, boolean useDefaultFile) throws OpflowConstructorException {
        if (LOG.isTraceEnabled()) LOG.trace("Create new OpflowRpcMaster with properties file: " + propFile);
        
        Properties props = loadProperties(propFile, defaultProps, useDefaultFile);
        Map<String, Object> params = new HashMap<String, Object>();
        
        extractEngineParameters("master", params, props);
        
        params.put("responseName", props.getProperty("opflow.master.responseName"));
        
        transformParameters(params);
        
        if (LOG.isTraceEnabled()) LOG.trace("OpflowRpcMaster has been created successfully");
        
        return new OpflowRpcMaster(params);
    }
    
    public static OpflowRpcWorker createRpcWorker() throws OpflowConstructorException {
        return createRpcWorker(null, null, true);
    }
    
    public static OpflowRpcWorker createRpcWorker(String propFile) throws OpflowConstructorException {
        return createRpcWorker(propFile, null, true);
    }
    
    public static OpflowRpcWorker createRpcWorker(Properties defaultProps) throws OpflowConstructorException {
        return createRpcWorker(null, defaultProps, false);
    }
    
    public static OpflowRpcWorker createRpcWorker(String propFile, Properties defaultProps, boolean useDefaultFile) throws OpflowConstructorException {
        if (LOG.isTraceEnabled()) LOG.trace("Create new OpflowRpcWorker with properties file: " + propFile);
        
        Properties props = loadProperties(propFile, defaultProps, useDefaultFile);
        Map<String, Object> params = new HashMap<String, Object>();
        
        extractEngineParameters("worker", params, props);
        
        if (props.getProperty("opflow.worker.operatorName") != null) {
            params.put("operatorName", props.getProperty("opflow.worker.operatorName"));
        } else {
            params.put("operatorName", props.getProperty("opflow.queueName"));
        }
        
        params.put("responseName", props.getProperty("opflow.worker.responseName"));
        
        transformParameters(params);
        
        if (LOG.isTraceEnabled()) LOG.trace("OpflowRpcWorker has been created successfully");
        
        return new OpflowRpcWorker(params);
    }
    
    public static OpflowPubsubHandler createPubsubHandler() throws OpflowConstructorException {
        return createPubsubHandler(null, null, true);
    }
    
    public static OpflowPubsubHandler createPubsubHandler(String propFile) throws OpflowConstructorException {
        return createPubsubHandler(propFile, null, true);
    }
    
    public static OpflowPubsubHandler createPubsubHandler(Properties defaultProps) throws OpflowConstructorException {
        return createPubsubHandler(null, defaultProps, false);
    }
    
    public static OpflowPubsubHandler createPubsubHandler(String propFile, Properties defaultProps, boolean useDefaultFile) throws OpflowConstructorException {
        if (LOG.isTraceEnabled()) LOG.trace("Create new OpflowPubsubHandler with properties file: " + propFile);
        
        Properties props = loadProperties(propFile, defaultProps, useDefaultFile);
        Map<String, Object> params = new HashMap<String, Object>();
        
        extractEngineParameters("pubsub", params, props);
        
        if (props.getProperty("opflow.pubsub.subscriberName") != null) {
            params.put("subscriberName", props.getProperty("opflow.pubsub.subscriberName"));
        } else {
            params.put("subscriberName", props.getProperty("opflow.queueName"));
        }
        
        params.put("recyclebinName", props.getProperty("opflow.pubsub.recyclebinName"));
        
        params.put("prefetch", props.getProperty("opflow.pubsub.prefetch"));
        
        params.put("subscriberLimit", props.getProperty("opflow.pubsub.subscriberLimit"));
        
        params.put("redeliveredLimit", props.getProperty("opflow.pubsub.redeliveredLimit"));
        
        transformParameters(params);
        
        if (LOG.isTraceEnabled()) LOG.trace("OpflowPubsubHandler has been created successfully");
        
        return new OpflowPubsubHandler(params);
    }
    
    public static Properties loadProperties() throws OpflowConstructorException {
        return loadProperties(null, null, true);
    }
    
    public static Properties loadProperties(String propFile) throws OpflowConstructorException {
        return loadProperties(propFile, null, propFile == null);
    }
    
    public static Properties loadProperties(String propFile, Properties props) throws OpflowConstructorException {
        return loadProperties(propFile, props, propFile == null && props == null);
    }
    
    public static Properties loadProperties(String propFile, Properties props, boolean useDefaultFile) throws OpflowConstructorException {
        try {
            if (props == null) {
                props = new Properties();
            } else {
                props = new Properties(props);
            }
            if (propFile != null || useDefaultFile) {
                URL url = getConfigurationUrl(propFile);
                if (url != null) {
                    props.load(url.openStream());
                } else {
                    propFile = (propFile != null) ? propFile : DEFAULT_CONFIGURATION_FILE;
                    throw new FileNotFoundException("property file '" + propFile + "' not found in the classpath");
                }
            }
            if (LOG.isTraceEnabled()) LOG.trace("[-] Properties: " + getPropertyAsString(props));
            return props;
        } catch (IOException exception) {
            throw new OpflowConstructorException(exception);
        }
    }
    
    private static URL getConfigurationUrl(String configFile) {
        URL url;
        String cfgFromSystem = (configFile != null) ? configFile : 
                OpflowUtil.getSystemProperty(DEFAULT_CONFIGURATION_KEY, null);
        if (LOG.isTraceEnabled()) LOG.trace("[-] configuration file: " + cfgFromSystem);
        if (cfgFromSystem == null) {
            url = OpflowUtil.getResource(DEFAULT_CONFIGURATION_FILE);
            if (LOG.isTraceEnabled()) LOG.trace("[-] default configuration: " + url);
        } else {
            try {
                url = new URL(cfgFromSystem);
            } catch (MalformedURLException ex) {
                // woa, the cfgFromSystem string is not a URL,
                // attempt to get the resource from the class path
                url = OpflowUtil.getResource(cfgFromSystem);
            }
        }
        if (LOG.isTraceEnabled()) LOG.trace("[-] final configuration path: " + url);
        return url;
    }
    
    private static String getPropertyAsString(Properties prop) {
        StringWriter writer = new StringWriter();
        prop.list(new PrintWriter(writer));
        return writer.getBuffer().toString();
    }
    
    private static void extractEngineParameters(String mode, Map<String, Object> params, Properties props) {
        for(String field: OpflowEngine.PARAMETER_NAMES) {
            String keyLevel0 = "opflow." + field;
            String keyLevel1 = "opflow." + mode + "." + field;
            if (props.getProperty(keyLevel1) != null) {
                params.put(field, props.getProperty(keyLevel1));
            } else if (props.getProperty(keyLevel0) != null) {
                params.put(field, props.getProperty(keyLevel0));
            }
        }
    }
    
    private static final String[] STRING_ARRAY_FIELDS = new String[] { "otherKeys" };
    
    private static final String[] INTEGER_FIELDS = new String[] {
        "port", "channelMax", "frameMax", "heartbeat", "prefetch", "subscriberLimit", "redeliveredLimit"
    };
    
    private static void transformParameters(Map<String, Object> params) {
        for(String key: params.keySet()) {
            if (OpflowUtil.arrayContains(STRING_ARRAY_FIELDS, key)) {
                if (params.get(key) instanceof String) {
                    params.put(key, OpflowUtil.splitByComma((String)params.get(key)));
                }
            }
            if (OpflowUtil.arrayContains(INTEGER_FIELDS, key)) {
                if (params.get(key) instanceof String) {
                    try {
                        params.put(key, Integer.parseInt(params.get(key).toString()));
                    } catch (NumberFormatException nfe) {
                        if (LOG.isTraceEnabled()) LOG.trace("transformParameters() - " + key + " field is not an integer");
                        params.put(key, null);
                    }
                }
            }
        }
    }
}
