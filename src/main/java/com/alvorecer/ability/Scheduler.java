package com.alvorecer.ability;

import java.util.ArrayList;
import java.util.List;

/** Tarefas atrasadas no thread do servidor (ondas de feitiços, durações). Só é acessado no thread principal. */
public final class Scheduler {
    private record Task(long runAt, Runnable action) {
    }

    private static final List<Task> TASKS = new ArrayList<>();
    private static long tick;

    private Scheduler() {
    }

    public static void after(int ticks, Runnable action) {
        TASKS.add(new Task(tick + Math.max(1, ticks), action));
    }

    public static void tick() {
        tick++;
        if (TASKS.isEmpty()) return;
        List<Task> due = new ArrayList<>();
        TASKS.removeIf(t -> {
            if (t.runAt() <= tick) {
                due.add(t);
                return true;
            }
            return false;
        });
        due.forEach(t -> t.action().run());
    }

    public static void clear() {
        TASKS.clear();
    }
}
