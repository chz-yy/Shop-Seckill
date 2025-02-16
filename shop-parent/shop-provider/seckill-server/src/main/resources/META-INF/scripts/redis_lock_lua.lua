-- string 的key
local stringKey = KEYS[1]
-- 对value的变动补偿， 可以为负数
local stringVal = tonumber(ARGV[1])
-- 过期时间
local expireAt = tonumber(ARGV[2])
-- check 值是否已存在, 不存在先插入key，并初始化值
local keyExist = redis.call("SETNX", KEYS[1],stringVal);
if (keyExist >= 1) then
    -- 设置过期时间
    redis.call("EXPIRE", KEYS[1], expireAt)
    return true
end

-- 返回最新结果，由于使用 stringRedisTemplate，返回值用string，否则值转换有问题
return false
