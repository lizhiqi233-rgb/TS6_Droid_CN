package dev.tslib;

/**
 * Server information (immutable snapshot).
 */
public class ServerInfo {
    public final String name;
    public final String platform;
    public final String version;
    public final int maxClients;
    public final int clientsOnline;
    public final int channelsOnline;
    public final long uptime;
    public final String welcomeMessage;
    public final long iconId;

    public ServerInfo(String name, String platform, String version,
                      int maxClients, int clientsOnline, int channelsOnline,
                      long uptime, String welcomeMessage, long iconId) {
        this.name = name;
        this.platform = platform;
        this.version = version;
        this.maxClients = maxClients;
        this.clientsOnline = clientsOnline;
        this.channelsOnline = channelsOnline;
        this.uptime = uptime;
        this.welcomeMessage = welcomeMessage;
        this.iconId = iconId;
    }

    public ServerInfo(String name, String platform, String version,
                      int maxClients, int clientsOnline, int channelsOnline,
                      long uptime, String welcomeMessage) {
        this(name, platform, version, maxClients, clientsOnline,
             channelsOnline, uptime, welcomeMessage, 0L);
    }

    public ServerInfo(String name, String platform, String version,
                      int maxClients, int clientsOnline,
                      String welcomeMessage, long iconId) {
        this(name, platform, version, maxClients, clientsOnline,
             0, 0L, welcomeMessage, iconId);
    }

    public ServerInfo(String name, String platform, String version,
                      int maxClients, int clientsOnline,
                      String welcomeMessage) {
        this(name, platform, version, maxClients, clientsOnline,
             welcomeMessage, 0L);
    }

    @Override
    public String toString() {
        return "ServerInfo(name='" + name + "')";
    }
}
