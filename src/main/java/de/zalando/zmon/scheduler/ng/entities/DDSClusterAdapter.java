package de.zalando.zmon.scheduler.ng.entities;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Created by jmussler on 4/20/15.
 */
public class DDSClusterAdapter extends EntityAdapter {

    private String url;
    private final MetricRegistry metrics;
    private final Timer timer;

    private static final Logger LOG = LoggerFactory.getLogger(DDSClusterAdapter.class);

    public DDSClusterAdapter(String url, MetricRegistry metrics) {
        super("DDSClusterAdapter");
        this.url = url;
        this.metrics = metrics;
        timer = metrics.timer("entity-adapter.dds.clusters");
    }

    private static class BaseEntity extends HashMap<String, Object> {}
    private static class BaseEntityList extends ArrayList<BaseEntity> {}

    @Override
    public Collection<Entity> getCollection() {
        RestTemplate rt = new RestTemplate();

        Timer.Context tC = timer.time();
        BaseEntityList list = rt.getForObject(url, BaseEntityList.class);
        LOG.info("DDS Adapter used: {}ms", tC.stop() / 1000000);

        List<Entity> entities = new ArrayList<>();

        for(BaseEntity base : list) {
            Entity e = new Entity(base.get("cluster")+"@"+base.get("instance_name"),"DDSClusterAdapter");
            base.put("environment",Environments.getNormalized((String)base.get("environment")));
            base.remove("id");
            if(!base.containsKey("pci")) {
                base.put("pci","false");
            }
            else {
                Object o = base.get("pci");
                if(o instanceof Boolean) {
                    if((Boolean)o) {
                        base.put("pci", "true");
                    }
                    else {
                        base.put("pci", "false");
                    }
                }
            }
            e.addProperties(base);
            entities.add(e);
        }

        return entities;
    }
}
