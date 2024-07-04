package org.jolokia.server.core.request;

/*
 * Copyright 2009-2013 Roland Huss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.*;

import org.jolokia.server.core.util.EscapeUtil;
import org.jolokia.server.core.util.RequestType;
import org.json.JSONObject;
import org.testng.annotations.*;

import static org.jolokia.server.core.request.JolokiaRequestBuilder.createMap;
import static org.testng.Assert.*;


/**
 * @author roland
 * @since Apr 15, 2010
 */
public class JolokiaRequestTest {

    ProcessingParameters procParams;

    @BeforeClass
    public void setup() {
        procParams = TestProcessingParameters.create();
    }
    @Test
    public void testPathSplitting() {
        List<String> paths = EscapeUtil.parsePath("hello/world");
        assertEquals(paths.size(),2);
        assertEquals(paths.get(0),"hello");
        assertEquals(paths.get(1),"world");

        paths = EscapeUtil.parsePath("hello!/world/second");
        assertEquals(paths.size(),2);
        assertEquals(paths.get(0),"hello/world");
        assertEquals(paths.get(1),"second");
    }

    @Test
    public void testPathGlueing() {
        String path = EscapeUtil.combineToPath(Arrays.asList("hello/world", "second"));
        assertEquals(path,"hello!/world/second");
    }


    // =================================================================================

    @Test
    public void readRequest() {
        for (JolokiaReadRequest req : new JolokiaReadRequest[] {
                JolokiaRequestFactory.createGetRequest("read/java.lang:type=Memory/HeapMemoryUsage/used", procParams),
                JolokiaRequestFactory.createPostRequest(
                        createMap("type", "read", "mbean", "java.lang:type=Memory",
                                  "attribute","HeapMemoryUsage",
                                  "path","used"),procParams)
        }) {
            assertEquals(req.getType(), RequestType.READ);
            assertEquals(req.getObjectNameAsString(),"java.lang:type=Memory");
            assertEquals(req.getAttributeName(),"HeapMemoryUsage");
            assertEquals(req.getPath(),"used");

            verify(req,"type","read");
            verify(req,"mbean","java.lang:type=Memory");
            verify(req,"attribute","HeapMemoryUsage");
            verify(req,"path","used");
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidPathEndingWithWildcardGet() {
        JolokiaRequestFactory.createGetRequest("read/java.lang:type=Memory/HeapMemoryUsage/*", procParams);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidPathEndingWithWildcardPost() {
        JolokiaRequestFactory.createPostRequest(
                createMap(
                        "type", "read",
                        "mbean", "java.lang:type=Memory",
                        "attribute", "HeapMemoryUsage",
                        "path", "used/*"), procParams);
    }

    @Test
    public void testToStringFix() {
        JolokiaReadRequest req = JolokiaRequestFactory.createPostRequest(
            createMap("type", "read", "mbean", "java.lang:type=Memory",
                      "attribute",Collections.singletonList("NonHeapMemoryUsage")),procParams);
        assertTrue(req.toString().contains("NonHeapMemoryUsage"));
    }

    @Test
    public void readRequestMultiAttributes() {
        for (JolokiaReadRequest req : new JolokiaReadRequest[] {
                JolokiaRequestFactory.createGetRequest("read/java.lang:type=Memory/Heap!/Memory!/Usage,NonHeapMemoryUsage", procParams),
                JolokiaRequestFactory.createPostRequest(
                        createMap("type", "read", "mbean", "java.lang:type=Memory",
                                  "attribute",Arrays.asList("Heap/Memory/Usage","NonHeapMemoryUsage")),procParams)
        }) {
            assertTrue(req.isMultiAttributeMode());

            for (List<?> list : new List[] {req.toJSON().getJSONArray("attribute").toList(), req.getAttributeNames() }) {
                assertEquals(list.size(), 2);
                assertTrue(list.contains("Heap/Memory/Usage"));
                assertTrue(list.contains("NonHeapMemoryUsage"));
                assertTrue(req.toString().contains("attribute=["));
                try {
                    req.getAttributeName();
                    fail();
                } catch (IllegalStateException exp) {
                    assertTrue(exp.getMessage().contains("getAttributeNames"));
                }
            }
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class,expectedExceptionsMessageRegExp = ".*Map.*")
    public void readRequestInvalidArguments() {
        JolokiaRequestFactory.createPostRequest(
                createMap("type", "read", "mbean", "java.lang:type=Memory",
                          "attribute",createMap("bla","blub")),procParams);
    }

    @Test
    public void readRequestNullArguments() {
        for (JolokiaReadRequest req : new JolokiaReadRequest[] {
                JolokiaRequestFactory.createGetRequest("read/java.lang:type=Memory", procParams),
                JolokiaRequestFactory.createPostRequest(
                        createMap("type", "read", "mbean", "java.lang:type=Memory"),procParams)
        }) {
            assertFalse(req.isMultiAttributeMode());
            assertFalse(req.hasAttribute());
            assertNull(req.getAttributeName());
            for (List<?> list : new List[] { (List<?>) req.toJSON().opt("attribute"), req.getAttributeNames() }) {
                assertNull(list);
            }
        }
    }

    @Test
    public void readRequestMultiNullList() {
        List<?> args = new ArrayList<>();
        args.add(null);
        JolokiaReadRequest req = JolokiaRequestFactory.createPostRequest(
                createMap("type", "read", "mbean", "java.lang:type=Memory",
                          "attribute",args),procParams);
        assertFalse(req.isMultiAttributeMode());
        assertNull(req.getAttributeName());
        assertNull(req.getAttributeNames());

    }

    @Test
    public void writeRequest() {
        for (JolokiaWriteRequest req : new JolokiaWriteRequest[] {
                JolokiaRequestFactory.createGetRequest("write/java.lang:type=Memory/Verbose/true/bla", procParams),
                JolokiaRequestFactory.createPostRequest(
                        createMap("type", "write", "mbean", "java.lang:type=Memory",
                                  "attribute","Verbose",
                                  "value", "true",
                                  "path","bla"),procParams)
        }) {
            assertEquals(req.getType(),RequestType.WRITE);
            assertEquals(req.getObjectNameAsString(),"java.lang:type=Memory");
            assertEquals(req.getAttributeName(),"Verbose");
            assertEquals(req.getValue(),"true");
            assertEquals(req.getPath(),"bla");

            verify(req,"type","write");
            verify(req,"mbean","java.lang:type=Memory");
            verify(req,"attribute","Verbose");
            verify(req,"value","true");
            verify(req,"path","bla");
        }
    }

    @Test
    public void listRequest() {
        for (JolokiaListRequest req : new JolokiaListRequest[] {
                JolokiaRequestFactory.createGetRequest("list/java.lang:type=Memory", procParams),
                JolokiaRequestFactory.createPostRequest(
                        createMap("type", "list", "path", "java.lang:type=Memory"),procParams)
        }) {
            assertEquals(req.getType(), RequestType.LIST);
            assertEquals(req.getPath(),"java.lang:type=Memory");

            verify(req,"type","list");
            verify(req,"path","java.lang:type=Memory");
        }
    }

    @Test
    public void versionRequest() {
        for (JolokiaVersionRequest req : new JolokiaVersionRequest[] {
                JolokiaRequestFactory.createGetRequest("version/java.lang:type=Memory", procParams),
                JolokiaRequestFactory.createPostRequest(
                        createMap("type", "version", "path", "java.lang:type=Memory"),procParams)
        }) {
            assertEquals(req.getType(),RequestType.VERSION);
            verify(req,"type","version");
        }
    }

    @Test
    public void execRequest() {
        List<?> args = Arrays.asList(null,"","normal");
        for (JolokiaExecRequest req : new JolokiaExecRequest[] {
                JolokiaRequestFactory.createGetRequest("exec/java.lang:type=Runtime/operation/[null]/\"\"/normal", procParams),
                JolokiaRequestFactory.createPostRequest(
                        createMap("type", "exec", "mbean", "java.lang:type=Runtime",
                                  "operation","operation","arguments", args), procParams)
        }) {
            assertEquals(req.getType(),RequestType.EXEC);
            assertEquals(req.getOperation(),"operation");
            assertNull(req.getArguments().get(0));
            assertEquals(req.getArguments().get(1), "");
            assertEquals(req.getArguments().get(2), "normal");

            verify(req,"type","exec");
            verify(req,"operation","operation");
            verify(req, "mbean", "java.lang:type=Runtime");
            JSONObject json = req.toJSON();
            List<?> args2 = json.getJSONArray("arguments").toList();
            assertNull(args2.get(0));
            assertEquals(args2.get(1),"");
            assertEquals(args2.get(2),"normal");
        }
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void invalidExecRequest() {
        JolokiaRequestFactory.createGetRequest("exec/java.lang:type=Runtime", procParams);
    }

    @Test
    public void searchRequest() {

        for (JolokiaSearchRequest req : new JolokiaSearchRequest[] {
                JolokiaRequestFactory.createGetRequest("search/java.lang:*", procParams),
                JolokiaRequestFactory.createPostRequest(
                        createMap("type", "search", "mbean", "java.lang:*"),procParams)
        }) {
            assertEquals(req.getType(),RequestType.SEARCH);
            assertEquals(req.getObjectName().getCanonicalName(),"java.lang:*");
            assertTrue(req.getObjectName().isPattern());

            verify(req,"type","search");
            verify(req,"mbean","java.lang:*");
        }
    }

    private void verify(JolokiaRequest pReq, String pKey, String pValue) {
        JSONObject json = pReq.toJSON();
        assertEquals(json.get(pKey),pValue);
        String info = pReq.toString();
        if (pKey.equals("type")) {
            String val = pValue.substring(0,1).toUpperCase() + pValue.substring(1);
            assertTrue(info.contains(val));
        } else {
            assertTrue(info.contains(pValue));
        }
    }


}
