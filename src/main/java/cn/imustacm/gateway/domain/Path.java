package cn.imustacm.gateway.domain;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wangjianli
 * @date 2020-04-23 16:52
 */
@Component
@ConfigurationProperties(prefix="jwt")
public class Path {

    private List<String> exclusionPath = new ArrayList<String>();

    public List<String> getExclusionPath() {
        return exclusionPath;
    }

    public void setExclusionPath(List<String> exclusionPath) {
        this.exclusionPath = exclusionPath;
    }

}
