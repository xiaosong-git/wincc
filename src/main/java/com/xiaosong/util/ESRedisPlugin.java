package com.xiaosong.util;

import com.jfinal.kit.PropKit;
import com.jfinal.plugin.redis.RedisPlugin;

/**
 * Redis加载类
 * @author wgm
 * 2019-11-06
 *
 */
public class ESRedisPlugin{
	
	private String hosts;//redis服务器地址
	private int port;//端口号
	private int timeout;//过期时间  单位：秒
	private String password;//密码
	private int maxTotal;//最大实例总数
	private int maxIdle;//空闲数 单位：秒
	private int maxWait;//等待时间 单位：秒
	private int dataBase ;//缓存库编号 单位：秒
	private String defaultCacheName = "REDIS";//默认缓存库名称
	
	RedisPlugin redisPlugin;
	
	public RedisPlugin config(){
		try {
			hosts = PropKit.get("redis.hosts");
			port = PropKit.getInt("redis.port");
			timeout = PropKit.getInt("redis.timeout");
			password = PropKit.get("redis.password");
			maxTotal = PropKit.getInt("redis.maxTotal");
			maxIdle = PropKit.getInt("redis.maxIdle");
			maxWait = PropKit.getInt("redis.maxWaitMillis");
			dataBase = PropKit.getInt("redis.database");
			redisPlugin = new RedisPlugin(defaultCacheName, hosts, port, timeout*1000, password, dataBase);
			redisPlugin.getJedisPoolConfig().setMaxTotal(maxTotal);
			redisPlugin.getJedisPoolConfig().setMaxIdle(maxIdle);
			redisPlugin.getJedisPoolConfig().setMaxWaitMillis(maxWait*1000);
		} catch (Exception e) {
	        throw new RuntimeException("init RedisPlugin config exception ",e);
		}
		return redisPlugin;
	}
	
	
	
}
