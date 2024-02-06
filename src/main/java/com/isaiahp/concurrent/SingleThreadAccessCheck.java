package com.isaiahp.concurrent;

@FunctionalInterface
public interface SingleThreadAccessCheck {
    SingleThreadAccessCheck NO_OP = () -> true;
    boolean check();
}
