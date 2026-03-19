-- Atomic create-if-not-exists for a Redis Hash
-- KEYS[1] = Redis key
-- ARGV[1] = value (v field)
-- ARGV[2] = rawValue (rv field), may be empty string
-- ARGV[3] = createdAt epoch ms (cat field)
-- ARGV[4] = TTL epoch ms (empty string = no TTL)
--
-- Returns: 1 = created, 0 = already exists

local exists = redis.call('EXISTS', KEYS[1])
if exists == 1 then
    return 0
end

if ARGV[4] ~= '' then
    redis.call('HSET', KEYS[1],
        'v', ARGV[1], 'rv', ARGV[2], 'ver', '1',
        'cat', ARGV[3], 'uat', ARGV[3], 'ttl', ARGV[4])
    redis.call('PEXPIREAT', KEYS[1], tonumber(ARGV[4]))
else
    redis.call('HSET', KEYS[1],
        'v', ARGV[1], 'rv', ARGV[2], 'ver', '1',
        'cat', ARGV[3], 'uat', ARGV[3])
end

return 1
