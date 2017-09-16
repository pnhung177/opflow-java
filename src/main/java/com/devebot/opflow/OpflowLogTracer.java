package com.devebot.opflow;

import com.google.gson.Gson;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author drupalex
 */
public class OpflowLogTracer {
    private final static Logger LOG = LoggerFactory.getLogger(OpflowLogTracer.class);
    private final static String OPFLOW_VERSION = "0.1.x";
    private final static String INSTANCE_ID = OpflowUtil.getUUID();
    private final static Gson GSON = new Gson();
    
    private final OpflowLogTracer parent;
    private final String key;
    private final Object value;
    private final Map<String, Object> fields = new LinkedHashMap<String, Object>();
    
    public final static OpflowLogTracer ROOT = new OpflowLogTracer();
    
    public OpflowLogTracer() {
        this(null, "instanceId", OpflowUtil.getSystemProperty("OPFLOW_INSTANCE_ID", INSTANCE_ID));
    }
    
    private OpflowLogTracer(OpflowLogTracer ref, String key, Object value) {
        this.parent = ref;
        this.key = key;
        this.value = value;
        this.reset();
    }
    
    public OpflowLogTracer branch(String key, Object value) {
        return new OpflowLogTracer(this, key, value);
    }
    
    public final OpflowLogTracer reset(int mode) {
        this.fields.clear();
        this.fields.put("message", null);
        if (mode > 0) {
            if (mode == 1) {
                if (this.parent != null) {
                    this.fields.put(this.parent.key, this.parent.value);
                }
            } else {
                OpflowLogTracer ref = this.parent;
                while(ref != null) {
                    this.fields.put(ref.key, ref.value);
                    ref = ref.parent;
                }
            }
        }
        this.fields.put(key, value);
        return this;
    }
    
    public final OpflowLogTracer reset() {
        String treepath = OpflowUtil.getSystemProperty("OPFLOW_LOGTREEPATH", null);
        if ("parent".equals(treepath)) return this.reset(1);
        if ("full".equals(treepath)) return this.reset(2);
        return this.reset(2);
    }
    
    public OpflowLogTracer copy() {
        OpflowLogTracer target = new OpflowLogTracer();
        target.fields.putAll(fields);
        return target;
    }
    
    public OpflowLogTracer copy(String[] copied) {
        OpflowLogTracer target = copy();
        for(String key: target.fields.keySet()) {
            if (!OpflowUtil.arrayContains(copied, key)) {
                target.fields.remove(key);
            }
        }
        return target;
    }
    
    public OpflowLogTracer put(String key, Object value) {
        fields.put(key, value);
        return this;
    }
    
    public Object get(String key) {
        return fields.get(key);
    }
    
    @Override
    public String toString() {
        return GSON.toJson(fields);
    }
    
    public static void bootstrap() {
        if (LOG.isInfoEnabled()) {
            LOG.info(new OpflowLogTracer()
                    .put("message", "Opflow Library Information")
                    .put("lib_name", "opflow-java")
                    .put("lib_version", getVersionNameFromManifest())
                    .put("os_name", System.getProperty("os.name"))
                    .put("os_version", System.getProperty("os.version"))
                    .put("os_arch", System.getProperty("os.arch"))
                    .toString());
        }
    }
    
    private static String getVersionNameFromPOM() {
        try {
            Properties props = new Properties();
            String POM_PROPSFILE = "META-INF/maven/com.devebot.opflow/opflow-core/pom.properties";
            props.load(OpflowLogTracer.class.getClassLoader().getResourceAsStream(POM_PROPSFILE));
            return props.getProperty("version");
        } catch (Exception ioe) {}
        return OPFLOW_VERSION;
    }
    
    private static String getVersionNameFromManifest() {
        try {
            InputStream manifestStream = OpflowLogTracer.class.getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF");
            if (manifestStream != null) {
                Manifest manifest = new Manifest(manifestStream);
                Attributes attributes = manifest.getMainAttributes();
                return attributes.getValue("Implementation-Version");
            }
        } catch (Exception ioe) {}
        return OPFLOW_VERSION;
    }
    
    static {
        bootstrap();
    }
}
