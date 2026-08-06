package io.github.arkosammy12.jemu.app.util;

public interface ThrowableConsumer<T> {

    void accept(T t) throws Exception;

}
