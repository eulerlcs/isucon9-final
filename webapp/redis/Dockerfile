FROM redis:5.0.10

COPY ./conf.d/redis-5.0.10.conf /usr/local/etc/redis/redis.conf

RUN set -x && \
    touch /var/log/redis.log && \
    chown redis:root /var/log/redis.log && \
    chmod 664 /var/log/redis.log

CMD [ "redis-server", "/usr/local/etc/redis/redis.conf" ]