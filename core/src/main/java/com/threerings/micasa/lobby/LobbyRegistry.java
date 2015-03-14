//
// $Id$
//
// Vilya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
// http://code.google.com/p/vilya/
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

package com.threerings.micasa.lobby;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.samskivert.util.StringUtil;

import com.threerings.util.StreamableArrayList;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.InvocationManager;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.server.PlaceRegistry;

import com.threerings.micasa.lobby.LobbyMarshaller;
import com.threerings.micasa.lobby.LobbyService.CategoriesListener;
import com.threerings.micasa.lobby.LobbyService.LobbiesListener;
import com.threerings.micasa.server.MiCasaConfig;

import static com.threerings.micasa.Log.log;

/**
 * The lobby registry is the primary class that coordinates the lobby services on the client. It
 * sets up the necessary invocation services and keeps track of the lobbies in operation on the
 * server. Only one lobby registry should be created on a server.
 *
 * <p> Presently, the lobby registry is configured with lobbies via the server configuration. An
 * example configuration follows:
 *
 * <pre>{@code
 * lobby_ids = foolobby, barlobby, bazlobby
 *
 * foolobby.mgrclass = com.threerings.micasa.lobby.LobbyManager
 * foolobby.ugi = <universal game identifier>
 * foolobby.name = <human readable lobby name>
 * foolobby.config1 = some config value
 * foolobby.config2 = some other config value
 *
 * barlobby.mgrclass = com.threerings.micasa.lobby.LobbyManager
 * barlobby.ugi = <universal game identifier>
 * barlobby.name = <human readable lobby name>
 * ...
 * }</pre>
 *
 * This information will be loaded from the MiCasa server configuration which means that it should
 * live in <code>rsrc/config/micasa/server.properties</code> somwhere in the classpath where it
 * will override the default MiCasa server properties file.
 *
 * <p> The <code>UGI</code> or universal game identifier is a string that is used to uniquely
 * identify every type of game and also to classify it according to meaningful keywords. It is best
 * described with a few examples:
 *
 * <pre>{@code
 * backgammon,board,strategy
 * spades,card,partner
 * yahtzee,dice
 * }</pre>
 *
 * As you can see, a UGI should start with an identifier uniquely identifying the type of game and
 * can be followed by a list of keywords that classify it as a member of a particular category of
 * games (eg. board, card, dice, partner game, strategy game). A game can belong to multiple
 * categories.
 *
 * <p> As long as the UGIs in use by a particular server make some kind of sense, the client will
 * be able to use them to search for lobbies containing games of similar types using the provided
 * facilities.
 */
public class LobbyRegistry
    implements LobbyProvider
{
    @Inject public LobbyRegistry (InvocationManager invmgr)
    {
        // register ourselves as an invocation service handler
        invmgr.registerProvider(this, LobbyMarshaller.class, InvocationCodes.GLOBAL_GROUP);
    }

    /**
     * Initializes the registry, creating our default lobbies.
     */
    public void init ()
    {
        // create our lobby managers
        String[] lmgrs = null;
        lmgrs = MiCasaConfig.config.getValue(LOBIDS_KEY, lmgrs);
        if (lmgrs == null || lmgrs.length == 0) {
            log.warning("No lobbies specified in config file (via '" + LOBIDS_KEY + "' parameter).");
        } else {
            for (String lmgr : lmgrs) {
                loadLobby(lmgr);
            }
        }
    }

    /**
     * Returns the oid of the default lobby.
     */
    public int getDefaultLobbyOid ()
    {
        return _defLobbyOid;
    }

    /**
     * Extracts the properties for a lobby from the server config and creates and initializes the
     * lobby manager.
     */
    protected void loadLobby (String lobbyId)
    {
        try {
            // extract the properties for this lobby
            Properties props = MiCasaConfig.config.getSubProperties(lobbyId);

            // get the lobby manager class and UGI
            String cfgClass = props.getProperty("config");
            if (StringUtil.isBlank(cfgClass)) {
                throw new Exception("Missing 'config' definition in lobby configuration.");
            }

            // create and initialize the lobby config object
            LobbyConfig config = (LobbyConfig)Class.forName(cfgClass).newInstance();
            config.init(props);

            // create and initialize the lobby manager. it will call lobbyReady() when it has
            // obtained a reference to its lobby object and is ready to roll
            LobbyManager lobmgr = (LobbyManager)_plreg.createPlace(config);
            lobmgr.init(this, props);

        } catch (Exception e) {
            log.warning("Unable to create lobby manager [lobbyId=" + lobbyId +
                        ", error=" + e + "].");
        }
    }

    /**
     * Returns information about all lobbies hosting games in the specified category.
     *
     * @param requester the body object of the client requesting the lobby list (which can be used
     * to filter the list based on their capabilities).
     * @param category the category of game for which the lobbies are desired.
     * @param target the list into which the matching lobbies will be deposited.
     */
    public void getLobbies (BodyObject requester, String category, List<Lobby> target)
    {
        List<Lobby> list = _lobbies.get(category);
        if (list != null) {
            target.addAll(list);
        }
    }

    /**
     * Returns an array containing the category identifiers of all the categories in which lobbies
     * have been registered with the registry.
     *
     * @param requester the body object of the client requesting the category list (which can be
     * used to filter the list based on their capabilities).
     */
    public String[] getCategories (BodyObject requester)
    {
        String[] cats = new String[_lobbies.size()];
        Iterator<String> iter = _lobbies.keySet().iterator();
        for (int ii = 0; iter.hasNext(); ii++) {
            cats[ii] = iter.next();
        }
        return cats;
    }

    /**
     * Processes a request by the client to obtain a list of the lobby categories available on this
     * server.
     */
    public void getCategories (ClientObject caller, CategoriesListener listener)
    {
        listener.gotCategories(getCategories((BodyObject)caller));
    }

    /**
     * Processes a request by the client to obtain a list of lobbies matching the supplied category
     * string.
     */
    public void getLobbies (ClientObject caller, String category, LobbiesListener listener)
    {
        List<Lobby> target = StreamableArrayList.newList();
        List<Lobby> list = _lobbies.get(category);
        if (list != null) {
            target.addAll(list);
        }
        listener.gotLobbies(target);
    }

    /**
     * Called by our lobby managers once they have started up and are ready to do their lobby
     * duties.
     */
    protected void lobbyReady (int placeOid, String gameIdent, String name)
    {
        // create a lobby record
        Lobby record = new Lobby(placeOid, gameIdent, name);

        // if we don't already have a default lobby, this one is the big winner
        if (_defLobbyOid == -1) {
            _defLobbyOid = placeOid;
        }

        // and register it in all the right lobby tables
        StringTokenizer tok = new StringTokenizer(gameIdent, ",");
        while (tok.hasMoreTokens()) {
            String category = tok.nextToken();
            registerLobby(category, record);
        }
    }

    /** Registers the supplied lobby in the specified category table. */
    protected void registerLobby (String category, Lobby record)
    {
        List<Lobby> catlist = _lobbies.get(category);
        if (catlist == null) {
            catlist = Lists.newArrayList();
            _lobbies.put(category, catlist);
        }
        catlist.add(record);
        log.info("Registered lobby", "cat", category, "record", record);
    }

    @Inject protected PlaceRegistry _plreg;

    /** A table containing references to our lobby records (in the form of category lists. */
    protected Map<String,List<Lobby>> _lobbies = Maps.newHashMap();

    /** The oid of the default lobby. */
    protected int _defLobbyOid = -1;

    /** The configuration key for the lobby managers list. */
    protected static final String LOBIDS_KEY = "lobby_ids";
}
