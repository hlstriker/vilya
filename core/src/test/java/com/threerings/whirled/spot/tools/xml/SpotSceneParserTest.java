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

package com.threerings.whirled.spot.tools.xml;

import junit.framework.Test;
import junit.framework.TestCase;

import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.spot.data.Location;
import com.threerings.whirled.tools.xml.SceneParser;

import com.threerings.stage.data.StageLocation;

public class SpotSceneParserTest extends TestCase
{
    public SpotSceneParserTest ()
    {
        super(SpotSceneParserTest.class.getName());
    }

    @Override
    public void runTest ()
    {
        try {
            SceneParser parser = new SceneParser("scene");
            parser.registerAuxRuleSet(new SpotSceneRuleSet() {
                @Override
                protected Location createLocation () {
                    return new StageLocation(); // breaks package, but ok
                }
            });
            SceneModel scene = parser.parseScene(
                getClass().getClassLoader().getResourceAsStream(TEST_SCENE_PATH));
            System.out.println("Parsed " + scene + ".");

        } catch (Exception e) {
            e.printStackTrace();
            fail("Test threw exception");
        }
    }

    public static Test suite ()
    {
        return new SpotSceneParserTest();
    }

    public static void main (String[] args)
    {
        SpotSceneParserTest test = new SpotSceneParserTest();
        test.runTest();
    }

    protected static final String TEST_SCENE_PATH =
        "rsrc/whirled/spot/tools/xml/scene.xml";
}
