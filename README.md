# hyperblock

A theoretically-infinitely-scalable Skyblock plugin.

## wat

Worlds are persisted in S3 buckets, write-through cached in Redis. Player data
lives in MongoDB because let's be real, doing anything SQL with Java fucking
sucks.