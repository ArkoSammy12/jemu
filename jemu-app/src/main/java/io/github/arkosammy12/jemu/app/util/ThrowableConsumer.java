package io.github.arkosammy12.jemu.app.util;

public interface ThrowableConsumer<T> {

    void acceptThrowing(T t) throws Exception;

}
