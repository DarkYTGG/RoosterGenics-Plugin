package com.dark.roostergenics.api.events;

import com.dark.roostergenics.arena.DuelArenaSession;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class RoosterDuelDrawEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final DuelArenaSession session;

    public RoosterDuelDrawEvent(DuelArenaSession session) {
        this.session = session;
    }

    public DuelArenaSession getSession() {
        return session;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
