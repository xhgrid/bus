package org.aoju.bus.goalie.reactor;

import lombok.Data;

import java.util.Objects;

/**
 * api definition
 *
 * @author Justubborn
 * @since 2020/10/27
 */
@Data
public class Asset {

    private String id;
    private String name;
    private String host;
    private String port;
    private String url;
    private String method;
    private String mode;
    private String type;
    private String token;
    private String sign;
    private String firewall;
    private String version;
    private String description;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Asset asset = (Asset) o;
        return id.equals(asset.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}