package me.xanium.gemseconomy.data;

import me.xanium.gemseconomy.GemsEconomy;
import me.xanium.gemseconomy.event.RedisPubAPI;
import org.bukkit.Bukkit;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.ArrayList;
import java.util.List;

public class RedisImplementation {

    private final GemsEconomy plugin;
    private JedisPool pool;
    private Jedis jedis;
    private boolean isConnected = false;
    private final String RedisHostname;
    private final int RedisPort;
    private final int RedisTimeOut;
    private final int RedisMaxPool;
    private final boolean RedisUseSSL;
    private final JedisPoolConfig poolCfg;
    private final String RedisPassword;
    private final String RedisChannelPub;
    private JedisPubSub jedisPubSub;

    public RedisImplementation(GemsEconomy economy) {
        this.plugin = economy;
        this.RedisMaxPool = getDatabase_redis_maxPool();
        this.RedisHostname = getDatabase_redis_hostname();
        this.RedisPort = getDatabase_redis_port();
        this.RedisTimeOut = getDatabase_redis_timeOut();
        this.RedisUseSSL = isDatabase_redis_useSSL();
        this.poolCfg = new JedisPoolConfig();
        this.poolCfg.setMaxTotal(this.RedisMaxPool);
        this.RedisPassword = getDatabase_redis_password();
        this.RedisChannelPub = getDatabase_redis_channel();

        this.onLoad();
        this.subThread();
    }

    public void onLoad() {
        try {
            this.pool = new JedisPool(this.poolCfg, this.RedisHostname, this.RedisPort, this.RedisTimeOut, this.RedisPassword, this.RedisUseSSL);
            this.jedis = this.pool.getResource();
            this.isConnected = true;
        } catch (Exception e) {
            this.isConnected = false;
            e.printStackTrace();
        }
    }

    public void onClose() {
        this.pool.destroy();
        this.isConnected = false;
        this.jedisPubSub.unsubscribe(this.RedisChannelPub);
    }

    private void subThread() {
        new Thread(() -> {
            final Jedis jedis = this.pool.getResource();
            jedisPubSub = this.sub();
            jedis.subscribe(jedisPubSub, this.RedisChannelPub);
        }).start();
    }

    public JedisPubSub sub() {
        return new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                super.onMessage(channel, message);
                if (channel.equalsIgnoreCase(RedisChannelPub)) {
                    Bukkit.getPluginManager().callEvent(new RedisPubAPI(channel, message));
                }
            }
        };
    }

    public void publishRedis(String value) {
        if (this.isConnected) {
            this.publishRedisCustomChannel(this.RedisChannelPub, value);
        }
    }

    public void publishRedisCustomChannel(String channel, String value) {
        if (this.isConnected == false) return;
        new Thread(() -> {
            final Jedis jedis = this.pool.getResource();
            try {
                jedis.publish(channel, value);
            } catch (Exception exception) {
                this.pool.returnBrokenResource(jedis);
            } finally {
                this.pool.returnResource(jedis);
            }
        }).start();
    }

    public static String[] parseImplementation(String... value) {
        return value.clone();
    }

    protected String getDatabase_redis_hostname() {
        return GemsEconomy.getInstance().getConfig().getString("redis.hostname");
    }

    protected int getDatabase_redis_port() {
        return GemsEconomy.getInstance().getConfig().getInt("redis.port");
    }

    protected String getDatabase_redis_password() {
        return GemsEconomy.getInstance().getConfig().getString("redis.password");
    }
    protected int getDatabase_redis_maxPool() {
        return GemsEconomy.getInstance().getConfig().getInt("redis.maxPool");
    }
    protected boolean isDatabase_redis_useSSL() {
        return GemsEconomy.getInstance().getConfig().getBoolean("redis.useSSL");
    }
    protected int getDatabase_redis_timeOut() {
        return GemsEconomy.getInstance().getConfig().getInt("redis.timeOut");
    }
    protected String getDatabase_redis_channel() {
        return GemsEconomy.getInstance().getConfig().getString("redis.channel");
    }
}
