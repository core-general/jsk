-- Compare-and-set update for a Redis Hash (optimistic concurrency)
-- KEYS[1] = Redis key
-- ARGV[1] = expected version (as string)
-- ARGV[2] = new value (v field)
-- ARGV[3] = new rawValue (rv field)
-- ARGV[4] = updatedAt epoch ms (uat field)
-- ARGV[5] = new TTL epoch ms (empty string = no TTL / remove existing TTL)
--
-- Returns: 1 = updated, 0 = version mismatch, -1 = key not found

local cur = redis.call('HGET', KEYS[1], 'ver')
if cur == false then
    return -1
end
if cur ~= ARGV[1] then
    return 0
end

local newVer = tostring(tonumber(ARGV[1]) + 1)

if ARGV[5] ~= '' then
    redis.call('HSET', KEYS[1],
        'v', ARGV[2], 'rv', ARGV[3], 'ver', newVer,
        'uat', ARGV[4], 'ttl', ARGV[5])
    redis.call('PEXPIREAT', KEYS[1], tonumber(ARGV[5]))
else
    redis.call('HSET', KEYS[1],
        'v', ARGV[2], 'rv', ARGV[3], 'ver', newVer,
        'uat', ARGV[4])
    redis.call('HDEL', KEYS[1], 'ttl')
    redis.call('PERSIST', KEYS[1])
end

return 1
