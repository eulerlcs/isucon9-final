FROM redis:6.0.10

COPY ./config/redis.conf /usr/local/etc/redis/redis.conf

RUN set -x && \
    touch /var/log/redis.log && \
    chown redis:root /var/log/redis.log && \
    chmod 664 /var/log/redis.log

CMD [ "redis-server", "/usr/local/etc/redis/redis.conf" ]