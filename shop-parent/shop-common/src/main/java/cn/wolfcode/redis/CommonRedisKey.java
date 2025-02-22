package cn.wolfcode.redis;

import lombok.Getter;

import java.util.concurrent.TimeUnit;

/**
 * Created by wolfcode
 */
@Getter
public enum CommonRedisKey {
    /**
     * 用户认证 token key
     */
    USER_TOKEN("userToken:", TimeUnit.HOURS,30);  //枚举常量调用枚举类构造方法

    CommonRedisKey(String prefix, TimeUnit unit, int expireTime){
        this.prefix = prefix;
        this.unit = unit;
        this.expireTime = expireTime;
    }

    public String getRealKey(String key){
        return this.prefix+key;
    }

    private String prefix;
    private TimeUnit unit;
    private int expireTime;

}
