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

package com.threerings.parlor;

import com.threerings.util.Name;

import com.threerings.presents.client.Client;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.parlor.client.Invitation;
import com.threerings.parlor.client.InvitationHandler;
import com.threerings.parlor.client.InvitationResponseObserver;
import com.threerings.parlor.client.ParlorDirector;
import com.threerings.parlor.game.data.GameConfig;
import com.threerings.parlor.util.ParlorContext;

import static com.threerings.parlor.Log.log;

public class TestClient extends com.threerings.crowd.client.TestClient
    implements InvitationHandler, InvitationResponseObserver
{
    public TestClient (String username)
    {
        super(username);

        // create the handles on our various services
        _pardtr = new ParlorDirector(_ctx);

        // register ourselves as the invitation handler
        _pardtr.setInvitationHandler(this);
    }

    @Override
    public void clientDidLogon (Client client)
    {
        // we intentionally don't call super()

        log.info("Client did logon [client=" + client + "].");

        // get a casted reference to our body object
        _body = (BodyObject)client.getClientObject();

        // if we're the inviter, do some inviting
        if (_body.username.equals("inviter")) {
            // send the invitation
            TestConfig config = new TestConfig();
            _pardtr.invite(new Name("invitee"), config, this);
        }
    }

    public void invitationReceived (Invitation invite)
    {
        log.info("Invitation received [invite=" + invite + "].");

        // accept the invitation. we're game...
        invite.accept();
    }

    public void invitationCancelled (Invitation invite)
    {
        log.info("Invitation cancelled [invite=" + invite + "].");
    }

    public void invitationAccepted (Invitation invite)
    {
        log.info("Invitation accepted [invite=" + invite + "].");
    }

    public void invitationRefused (Invitation invite, String message)
    {
        log.info("Invitation refused [invite=" + invite +
                 ", message=" + message + "].");
    }

    public void invitationCountered (Invitation invite, GameConfig config)
    {
        log.info("Invitation countered [invite=" + invite +
                 ", config=" + config + "].");
    }

    @Override
    protected CrowdContext createContext ()
    {
        return (_ctx = new ParlorContextImpl());
    }

    public static void main (String[] args)
    {
        // create our test client
        TestClient tclient = new TestClient(
            System.getProperty("username", "test"));
        // start it running
        tclient.run();
    }

    protected class ParlorContextImpl
        extends com.threerings.crowd.client.TestClient.CrowdContextImpl
        implements ParlorContext
    {
        public ParlorDirector getParlorDirector ()
        {
            return _pardtr;
        }
    }

    protected ParlorContext _ctx;
    protected ParlorDirector _pardtr;
    protected BodyObject _body;
}
