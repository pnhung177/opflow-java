package com.devebot.opflow;

import com.devebot.opflow.supports.OpflowConverter;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author acegik
 */
public class OpflowRpcWatcher implements AutoCloseable {
    public final static long RPC_DETECTION_INTERVAL = 30000;
    
    private final static Logger LOG = LoggerFactory.getLogger(OpflowRpcWatcher.class);
    
    private final String instanceId;
    private final OpflowLogTracer logTracer;
    
    private final OpflowRpcChecker rpcChecker;
    private final boolean enabled;
    private final long interval;
    private final Timer timer = new Timer(true);
    private final TimerTask timerTask;
    
    private boolean congested = false;
    
    public OpflowRpcWatcher(OpflowRpcChecker _rpcChecker) {
        this(_rpcChecker, null);
    }
    
    public OpflowRpcWatcher(OpflowRpcChecker _rpcChecker, Map<String, Object> kwargs) {
        if (kwargs == null) {
            instanceId = OpflowUtil.getLogID();
            enabled = true;
            interval = RPC_DETECTION_INTERVAL;
        } else {
            instanceId = OpflowUtil.getOptionField(kwargs, "instanceId", true);
            enabled = OpflowConverter.convert(OpflowUtil.getOptionField(kwargs, "enabled", Boolean.TRUE), Boolean.class);
            interval = OpflowConverter.convert(OpflowUtil.getOptionField(kwargs, "interval", RPC_DETECTION_INTERVAL), Long.class);
        }
        
        logTracer = OpflowLogTracer.ROOT.branch("rpcWatcherId", instanceId);
        rpcChecker = _rpcChecker;
        timerTask = new TimerTask() {
            @Override
            public void run() {
                long current = OpflowUtil.getCurrentTime();
                OpflowLogTracer logTask = logTracer.branch("timestamp", current);
                if (logTask.ready(LOG, "debug")) LOG.debug(logTask
                        .put("threadCount", Thread.activeCount())
                        .text("Detector[${rpcWatcherId}].run(), threads: ${threadCount}")
                        .stringify());
                try {
                    OpflowRpcChecker.Pong result = rpcChecker.send(new OpflowRpcChecker.Ping());
                    congested = false;
                    if (logTask.ready(LOG, "debug")) LOG.debug(logTask
                            .text("Detector[${rpcWatcherId}].run(), the queue is drained")
                            .stringify());
                } catch (Throwable exception) {
                    congested = true;
                    if (logTask.ready(LOG, "debug")) LOG.debug(logTask
                            .text("Detector[${rpcWatcherId}].run(), the queue is congested")
                            .stringify());
                }
            }
        };
    }
    
    public boolean isCongested() {
        return congested;
    }
    
    public void setCongested(boolean _congested) {
        congested = _congested;
    }
    
    public void start() {
        if (logTracer.ready(LOG, "debug")) LOG.debug(logTracer
                .text("Detector[${rpcWatcherId}].start()")
                .stringify());
        if (enabled) {
            if (interval > 0) {
                timer.scheduleAtFixedRate(timerTask, 0, interval);
                if (logTracer.ready(LOG, "debug")) LOG.debug(logTracer
                        .put("interval", interval)
                        .text("Detector[${rpcWatcherId}] has been started with interval: ${interval}")
                        .stringify());
            } else {
                if (logTracer.ready(LOG, "debug")) LOG.debug(logTracer
                        .put("interval", interval)
                        .text("Detector[${rpcWatcherId}] is not available. undefined interval")
                        .stringify());
            }
        } else {
            if (logTracer.ready(LOG, "debug")) LOG.debug(logTracer
                    .text("Detector[${rpcWatcherId}] is disabled")
                    .stringify());
        }
    }

    @Override
    public void close() {
        if (logTracer.ready(LOG, "debug")) LOG.debug(logTracer
                .text("Detector[${rpcWatcherId}].close()")
                .stringify());
        timer.cancel();
        timer.purge();
    }
}
