package com.dark.roostergenics.api.events;

import com.dark.roostergenics.arena.DuelArenaSession;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class RoosterDuelAbortEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final DuelArenaSession session;
    private final String reason;

    public RoosterDuelAbortEvent(DuelArenaSession session, String reason) {
        this.session = session;
        this.reason = reason;
    }

    public DuelArenaSession getSession() {
        return session;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
