package nl.koppeltaal.smartserviceregistration.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "identity_provider")
public class IdentityProvider extends DbEntity {

    private String name;
    @Column(name = "openid_config_endpoint")
    private String openidConfigEndpoint;
    @Column(name = "client_id")
    private String clientId;
    @Column(name = "client_secret")
    private String clientSecret;
    @Column(name = "username_attribute")
    private String usernameAttribute;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOpenidConfigEndpoint() {
        return openidConfigEndpoint;
    }

    public void setOpenidConfigEndpoint(String openidConfigEndpoint) {
        this.openidConfigEndpoint = openidConfigEndpoint;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getUsernameAttribute() {
        return usernameAttribute;
    }

    public void setUsernameAttribute(String usernameAttribute) {
        this.usernameAttribute = usernameAttribute;
    }

    @Override
    public String toString() {
        return "IdentityProvider{" +
                "name='" + name + '\'' +
                ", openidConfigEndpoint='" + openidConfigEndpoint + '\'' +
                ", clientId='" + clientId + '\'' +
                ", clientSecret='" + clientSecret + '\'' +
                ", usernameAttribute='" + usernameAttribute + '\'' +
                "} " + super.toString();
    }
}
