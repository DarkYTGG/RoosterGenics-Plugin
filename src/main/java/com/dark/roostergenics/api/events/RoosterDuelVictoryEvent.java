package com.dark.roostergenics.api.events;

import com.dark.roostergenics.arena.DuelArenaSession;
import com.dark.roostergenics.model.RoosterData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class RoosterDuelVictoryEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final DuelArenaSession session;
    private final Player winner;
    private final RoosterData winnerRooster;
    private final Player loser;
    private final RoosterData loserRooster;

    public RoosterDuelVictoryEvent(DuelArenaSession session, Player winner, RoosterData winnerRooster,
                                  Player loser, RoosterData loserRooster) {
        this.session = session;
        this.winner = winner;
        this.winnerRooster = winnerRooster;
        this.loser = loser;
        this.loserRooster = loserRooster;
    }

    public DuelArenaSession getSession() {
        return session;
    }

    public Player getWinner() {
        return winner;
    }

    public RoosterData getWinnerRooster() {
        return winnerRooster;
    }

    public Player getLoser() {
        return loser;
    }

    public RoosterData getLoserRooster() {
        return loserRooster;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
