package me.xanium.gemseconomy.event;

import org.apache.commons.lang3.Validate;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class RedisPubAPI extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final String channelRedis;
    private final String valueRedis;

    public RedisPubAPI(String channel, String value) {
        super(true);
        Validate.notNull(channel, "Channel can not be null");
        Validate.notNull(value, "Value can not be null");
        this.channelRedis = channel;
        this.valueRedis = value;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public String getChannelRedis() {
        return this.channelRedis;
    }

    public String getValueRedis() {
        return this.valueRedis;
    }

    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}