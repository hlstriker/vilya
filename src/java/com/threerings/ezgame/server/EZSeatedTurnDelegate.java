//
// $Id: EZGameTurnDelegate.java 523 2007-12-07 21:41:22Z ray $
//
// Vilya library - tools for developing networked games
// Copyright (C) 2002-2007 Three Rings Design, Inc., All Rights Reserved
// http://www.threerings.net/code/vilya/
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.ezgame.server;

import com.samskivert.util.ListUtil;
import com.samskivert.util.RandomUtil;

import com.threerings.util.Name;

import com.threerings.crowd.data.BodyObject;

import com.threerings.parlor.turn.server.TurnGameManagerDelegate;

/**
 * A special turn delegate for seated ez games.
 */
public class EZSeatedTurnDelegate extends TurnGameManagerDelegate
    implements EZGameTurnDelegate
{
    // from EZGameTurnDelegate
    public void endTurn (int nextPlayerId)
    {
        _nextPlayerId = nextPlayerId;
        endTurn();
    }

    @Override
    protected void setFirstTurnHolder ()
    {
        // make it nobody's turn
        _turnIdx = -1;
    }

    @Override
    protected void setNextTurnHolder ()
    {
        // if the user-supplied value seems to make sense, use it!
        if (_nextPlayerId != 0) {
            // clear out _nextPlayerId.
            int nextId = _nextPlayerId;
            _nextPlayerId = 0;

            BodyObject nextPlayer = ((EZGameManager) _plmgr).getPlayerByOid(nextId);
            if (nextPlayer != null) {
                int index = ListUtil.indexOf(_turnGame.getPlayers(), nextPlayer.getVisibleName());
                if (index != -1) {
                    _turnIdx = index;
                    return;
                }
            }
        }
        
        // Otherwise, if it's nobody's turn- randomly pick a turn holder
        if (_turnIdx == -1) {
            assignTurnRandomly();
            return;
        }

        // otherwise, do the default behavior
        super.setNextTurnHolder();
    }

    /** An override next turn holder, or 0. */
    protected int _nextPlayerId;
}
