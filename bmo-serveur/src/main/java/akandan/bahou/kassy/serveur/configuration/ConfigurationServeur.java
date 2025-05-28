package akandan.bahou.kassy.serveur.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration ConfigurationServeur
 */
@Component
@ConfigurationProperties(prefix = "bmo")
public class ConfigurationServeur {

    private String host = "localhost";
    private int port = 8080;

    // Getters et Setters
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
}
