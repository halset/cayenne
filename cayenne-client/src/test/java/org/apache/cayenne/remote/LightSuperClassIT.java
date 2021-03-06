/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.remote;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.RefreshQuery;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.remote.service.LocalConnection;
import org.apache.cayenne.testdo.persistent.Continent;
import org.apache.cayenne.testdo.persistent.Country;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * Test for entities that are implemented in same class on client and server
 */
@UseServerRuntime(CayenneProjects.PERSISTENT_PROJECT)
@RunWith(value=Parameterized.class)
public class LightSuperClassIT extends RemoteCayenneCase {

    private boolean server;

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {LocalConnection.HESSIAN_SERIALIZATION, true},
                {LocalConnection.JAVA_SERIALIZATION, true},
                {LocalConnection.NO_SERIALIZATION, true},
                {LocalConnection.NO_SERIALIZATION, false},
        });
    }

    public LightSuperClassIT(int serializationPolicy, boolean server) {
        super.serializationPolicy = serializationPolicy;
        this.server = server;
    }

    private ObjectContext createContext() {
        if (server) {
            return serverContext;
        }
        else {
            return createROPContext();
        }
    }

    @Test
    public void testServer() throws Exception {
        ObjectContext context = createContext();
        Continent continent = context.newObject(Continent.class);
        continent.setName("Europe");

        Country country = new Country();
        context.registerNewObject(country);

        // TODO: setting property before object creation does not work on ROP (CAY-1320)
        country.setName("Russia");

        country.setContinent(continent);
        assertEquals(continent.getCountries().size(), 1);

        context.commitChanges();

        context.deleteObjects(country);
        assertEquals(continent.getCountries().size(), 0);
        continent.setName("Australia");

        context.commitChanges();
        context.performQuery(new RefreshQuery());

        assertEquals(context.performQuery(new SelectQuery(Country.class)).size(), 0);
        assertEquals(context.performQuery(new SelectQuery(Continent.class)).size(), 1);
    }
}
